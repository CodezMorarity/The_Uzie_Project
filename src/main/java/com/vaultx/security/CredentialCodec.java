package com.vaultx.security;

import com.vaultx.exception.VaultException;
import com.vaultx.model.Credential;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Arrays;
import java.util.UUID;

public final class CredentialCodec {
    private static final int MAX_FIELD_BYTES = 64 * 1024;
    private static final byte[] MAGIC = {'V', 'X', 'C', 1};

    public byte[] encode(Credential credential) {
        try (ByteArrayOutputStream bytes = new ByteArrayOutputStream();
             DataOutputStream out = new DataOutputStream(bytes)) {
            out.write(MAGIC);
            out.writeLong(credential.id().getMostSignificantBits());
            out.writeLong(credential.id().getLeastSignificantBits());
            writeString(out, credential.name());
            writeString(out, credential.url());
            writeString(out, credential.username());
            char[] password = credential.password();
            byte[] encodedPassword = null;
            try {
                encodedPassword = encodeChars(password);
                writeBytes(out, encodedPassword);
            } finally {
                Arrays.fill(password, '\0');
                KeyDerivation.clear(encodedPassword);
            }
            writeString(out, credential.category());
            writeString(out, credential.notes());
            writeInstant(out, credential.createdAt());
            writeInstant(out, credential.modifiedAt());
            out.writeBoolean(credential.favorite());
            out.flush();
            return bytes.toByteArray();
        } catch (IOException ex) {
            throw new VaultException("Could not encode credential data.", ex);
        }
    }

    public Credential decode(byte[] payload) {
        char[] password = null;
        try (DataInputStream in = new DataInputStream(new ByteArrayInputStream(payload))) {
            byte[] magic = in.readNBytes(MAGIC.length);
            if (!Arrays.equals(magic, MAGIC)) throw new VaultException("Credential data version is not supported.");
            UUID id = new UUID(in.readLong(), in.readLong());
            String name = readString(in);
            String url = readString(in);
            String username = readString(in);
            byte[] passwordBytes = readBytes(in);
            try { password = decodeChars(passwordBytes); }
            finally { KeyDerivation.clear(passwordBytes); }
            String category = readString(in);
            String notes = readString(in);
            Instant created = readInstant(in);
            Instant modified = readInstant(in);
            boolean favorite = in.readBoolean();
            if (in.available() != 0) throw new VaultException("Credential data contains unexpected trailing content.");
            return new Credential(id, name, url, username, password, category, notes, created, modified, favorite);
        } catch (IOException | RuntimeException ex) {
            if (ex instanceof VaultException vaultException) throw vaultException;
            throw new VaultException("Vault data is invalid or damaged.", ex);
        } finally {
            if (password != null) Arrays.fill(password, '\0');
        }
    }

    public byte[] encodeList(java.util.List<Credential> credentials) {
        try (ByteArrayOutputStream bytes = new ByteArrayOutputStream();
             DataOutputStream out = new DataOutputStream(bytes)) {
            out.write(new byte[]{'V', 'X', 'B', 1});
            out.writeInt(credentials.size());
            for (Credential credential : credentials) {
                byte[] item = encode(credential);
                try {
                    writeBytes(out, item);
                } finally {
                    KeyDerivation.clear(item);
                }
            }
            out.flush();
            return bytes.toByteArray();
        } catch (IOException ex) {
            throw new VaultException("Could not encode backup data.", ex);
        }
    }

    public java.util.List<Credential> decodeList(byte[] payload) {
        java.util.List<Credential> result = new java.util.ArrayList<>();
        try (DataInputStream in = new DataInputStream(new ByteArrayInputStream(payload))) {
            if (!Arrays.equals(in.readNBytes(4), new byte[]{'V', 'X', 'B', 1})) {
                throw new VaultException("Backup format is not supported.");
            }
            int count = in.readInt();
            if (count < 0 || count > 100_000) throw new VaultException("Backup contains an invalid number of records.");
            java.util.Set<UUID> ids = new java.util.HashSet<>();
            for (int i = 0; i < count; i++) {
                byte[] item = readBytes(in);
                try {
                    Credential credential = decode(item);
                    if (!ids.add(credential.id())) {
                        credential.close();
                        throw new VaultException("Backup contains duplicate credential identifiers.");
                    }
                    result.add(credential);
                } finally {
                    KeyDerivation.clear(item);
                }
            }
            if (in.available() != 0) throw new VaultException("Backup contains unexpected trailing content.");
            return result;
        } catch (IOException | RuntimeException ex) {
            result.forEach(Credential::close);
            if (ex instanceof VaultException vaultException) throw vaultException;
            throw new VaultException("Backup content is invalid or damaged.", ex);
        }
    }

    private static void writeString(DataOutputStream out, String value) throws IOException, CharacterCodingException {
        byte[] bytes = value.getBytes(StandardCharsets.UTF_8);
        try { writeBytes(out, bytes); } finally { KeyDerivation.clear(bytes); }
    }

    private static String readString(DataInputStream in) throws IOException {
        byte[] bytes = readBytes(in);
        try { return StandardCharsets.UTF_8.newDecoder().decode(ByteBuffer.wrap(bytes)).toString(); }
        catch (CharacterCodingException ex) { throw new IOException("Invalid UTF-8 field.", ex); }
        finally { KeyDerivation.clear(bytes); }
    }

    private static void writeBytes(DataOutputStream out, byte[] bytes) throws IOException {
        if (bytes.length > MAX_FIELD_BYTES) throw new IOException("Field exceeds the maximum size.");
        out.writeInt(bytes.length);
        out.write(bytes);
    }

    private static byte[] readBytes(DataInputStream in) throws IOException {
        int length = in.readInt();
        if (length < 0 || length > MAX_FIELD_BYTES) throw new IOException("Invalid field length.");
        byte[] bytes = in.readNBytes(length);
        if (bytes.length != length) throw new IOException("Unexpected end of field.");
        return bytes;
    }

    private static byte[] encodeChars(char[] chars) throws CharacterCodingException {
        ByteBuffer encoded = StandardCharsets.UTF_8.newEncoder()
                .onMalformedInput(CodingErrorAction.REPORT).onUnmappableCharacter(CodingErrorAction.REPORT)
                .encode(CharBuffer.wrap(chars));
        byte[] bytes = new byte[encoded.remaining()];
        encoded.get(bytes);
        if (encoded.hasArray()) Arrays.fill(encoded.array(), (byte) 0);
        return bytes;
    }

    private static char[] decodeChars(byte[] bytes) throws CharacterCodingException {
        CharBuffer decoded = StandardCharsets.UTF_8.newDecoder()
                .onMalformedInput(CodingErrorAction.REPORT).onUnmappableCharacter(CodingErrorAction.REPORT)
                .decode(ByteBuffer.wrap(bytes));
        char[] chars = new char[decoded.remaining()];
        decoded.get(chars);
        if (decoded.hasArray()) Arrays.fill(decoded.array(), '\0');
        return chars;
    }

    private static void writeInstant(DataOutputStream out, Instant instant) throws IOException {
        out.writeLong(instant.getEpochSecond());
        out.writeInt(instant.getNano());
    }

    private static Instant readInstant(DataInputStream in) throws IOException {
        return Instant.ofEpochSecond(in.readLong(), in.readInt());
    }
}
