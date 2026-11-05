# Windows installation and packaging

## For end users

Download `VaultX-1.0.0.msi` from a successful Windows workflow run or release, run it, and follow the installer. The MSI includes a Java runtime and JavaFX modules, so users do not need to install Java, Maven, or JavaFX. Launch VaultX from the Start menu. The first launch asks you to create a master password.

The portable distribution is `VaultX-portable.zip`. Extract it and run `VaultX\VaultX.exe`. Keep the extracted application directory together. Both distributions store data separately from the program under `%LOCALAPPDATA%\VaultX`.

## Build prerequisites

- Windows 10 or 11, 64-bit.
- A 64-bit JDK 21 with `java`, `jpackage`, and `jlink` available (set `JAVA_HOME` and add `%JAVA_HOME%\bin` to `PATH`).
- Maven 3.9 or later on `PATH`.
- WiX Toolset 3.x on `PATH` to generate the MSI. The portable app image does not require WiX.
- Internet access for the initial Maven dependency download.

## Local build

From the project root in PowerShell:

```powershell
scripts\build-windows.ps1
```

This runs `mvn clean test package`, stages the shaded application JAR, lets `jpackage` create a self-contained app image, zips the portable app, and creates the MSI when WiX is available. Results are placed in `target\dist`. To run source directly, use `scripts\run-windows.ps1`; it checks Java/Maven and runs `mvn javafx:run`.

## Native Windows CI

`.github/workflows/windows-build.yml` runs the same build on `windows-latest`, uploads the MSI and portable ZIP, and is the supported validation path for packaging. A Linux development host cannot produce or validate a native Windows MSI. The current development session is Linux and GitHub credentials are invalid, so there is no completed Windows artifact to download from this checkout.

## Data and uninstall

Uninstalling the app does not imply that local vault data is deleted. The database remains in `%LOCALAPPDATA%\VaultX`; remove it manually only after exporting and verifying a backup if you intend to retain its contents. Protect the device account and disk with Windows security controls.
