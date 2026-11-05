# UI screenshots

These images are captured from real JavaFX application screens using `ScreenshotCapture`, a development-only harness under `src/test/java`. It creates a temporary SQLite database outside the repository and fictional demo entries; the saved report images contain no real vault data. Generator output is masked in the screenshot; those random demo values were never saved to an account.

Regenerate on a graphical desktop from the project root after compiling tests:

```bash
mvn test-compile dependency:copy-dependencies -DincludeGroupIds=org.openjfx -DincludeScope=runtime -DoutputDirectory=target/javafx-modules
java --module-path target/javafx-modules --add-modules javafx.controls -cp "target/test-classes:target/classes:<sqlite-jdbc.jar>:<slf4j-api.jar>:<slf4j-simple.jar>" com.vaultx.view.ScreenshotCapture
```

Replace the three `<...jar>` entries with Maven’s resolved runtime JAR paths. On Windows, use `;` between classpath entries. The harness uses a reduced KDF iteration count for its fictional demo vault; it is test-only and is not packaged with the application.
