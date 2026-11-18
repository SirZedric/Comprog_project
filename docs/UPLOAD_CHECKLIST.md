**Repository upload checklist**

1. Extract the ZIP and open its `SimpleQuiz_Ready_To_Upload` folder.
2. Create an empty GitHub repository. Keep optional repository initialization unchecked.
3. Select **uploading an existing file** or **Add file > Upload files**.
4. Drag the contents of the extracted project folder into the upload page. Include `.github`, `.gitignore`, and `.gitattributes` alongside `app`, `gradle`, `scripts`, and the other root files.
5. Commit the uploaded files to the default branch.
6. Confirm these exact paths exist in the repository:

| Path | Purpose |
| --- | --- |
| `settings.gradle.kts` | Declares the project and `app` module |
| `build.gradle.kts` | Pins Android and Kotlin plugins |
| `gradle/wrapper/gradle-wrapper.properties` | Pins the Gradle distribution |
| `gradlew` and `gradlew.bat` | First-run wrapper launchers |
| `scripts/prepare-wrapper.ps1` | Windows wrapper preparation |
| `app/build.gradle.kts` | Android module settings/dependencies |
| `app/src/main/AndroidManifest.xml` | App and Activity configuration |
| `app/src/main/assets/questions.csv` | Initial question bank |
| `.github/workflows/android-build.yml` | Automatic APK build |

7. Open **Actions > Build Android APK** and wait for the run. Use **Run workflow** on the default branch if needed.
8. After success, download **Artifacts > SimpleQuiz-debug** and extract `SimpleQuiz-debug.apk`.
9. If a check fails, inspect the failed step and download **SimpleQuiz-build-reports** if available.

If your upload has an extra project folder above all these paths, move/reupload the contents into the repository root. If you only committed a ZIP, extract it locally and upload the contents. GitHub runs the workflow file from the repository's `.github/workflows` path.
