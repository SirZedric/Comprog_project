**SimpleQuiz — complete Android project and automatic GitHub APK build**

This project assembles the complete Kotlin/XML/SQLite quiz app from the supplied group components. The Android project, resources, question bank, Gradle configuration, and GitHub Actions workflow are already arranged. No manual project creation or source-file placement is required.

**Upload and get the APK**

1. Extract `SimpleQuiz_Ready_To_Upload.zip` on your computer.
2. On GitHub, create an empty repository, for example `SimpleQuiz-Group`. Use the visibility required by your group. Leave the optional README, license, and .gitignore initialization unchecked because these project files are supplied.
3. On the new repository page, select **uploading an existing file**, or **Add file > Upload files**.
4. Open the extracted `SimpleQuiz_Ready_To_Upload` folder. Drag **all of its contents** into the upload page, including `.github`, `app`, `gradle`, and `scripts`. Upload the contents, rather than the ZIP itself or one extra enclosing folder. Commit to the default branch, normally `main`.
5. At the repository root, confirm that `settings.gradle.kts`, `build.gradle.kts`, and the `app` folder are visible. Open `.github/workflows/android-build.yml` to confirm the workflow was included.
6. Open the **Actions** tab. Select **Build Android APK**, then open the newest run. Allow it to finish. If it has not started, select **Run workflow** on the default branch. If Actions is disabled by repository/organization policy, enable or request access to the required workflow actions in repository settings.
7. After a successful run, scroll to **Artifacts** and download **SimpleQuiz-debug**. Extract that downloaded artifact ZIP to get `SimpleQuiz-debug.apk` and its SHA-256 checksum.
8. Install the APK on an Android 8.0/API 26 or newer device. On first launch, choose passwords for all four local admins, then log in or create a regular account.

You do not need Android Studio, a local Android SDK, personal tokens, or repository secrets for this GitHub-hosted debug-build route. GitHub must be able to run Actions for the repository and download the public build dependencies. The generated APK is debug-signed for development/demonstration; no release/store signing is configured.

If you prefer GitHub Desktop, copy these project contents into your repository's working folder, commit them, and push. The included workflow starts on pushes and pull requests, and can also be started manually. If updating an existing group repository, compare changes before replacing newer work.

**What the automatic workflow does**

- Checks out the project and sets up JDK 17.
- Installs Android platform 35, build tools 35.0.0, and platform tools; the SDK setup action handles license acceptance.
- Configures Gradle caching.
- Fetches the official Gradle wrapper JAR on first use and verifies its published SHA-256 checksum. The pinned Gradle distribution also has a published SHA-256 check.
- Runs the included password/CSV unit tests, Android lint, and debug APK assembly.
- Uploads the APK as `SimpleQuiz-debug` after successful checks.
- Uploads available build logs and check reports as `SimpleQuiz-build-reports`, including after a build/check failure.

The workflow does not create accounts, choose passwords, create member branches, invite collaborators, merge code, or publish a release. Those are separate account/group actions. No administrator credentials are hardcoded.

**Included application features**

| Area | Behavior |
| --- | --- |
| First launch | Imports 100 questions and requires passwords for `admin1` through `admin4`. |
| Signup | Creates a regular user; validates username/password length; rejects case-insensitive duplicate and reserved names; signs the user in. |
| Login | Verifies salted PBKDF2-HMAC-SHA256 password records and restores a valid saved local session. |
| Profile | Saves a display name by button and a selected picture immediately as a private thumbnail. |
| Dashboard | Shows the account's profile, quiz history, average percentage, and best percentage. |
| Quiz | Accepts a question count, randomly selects questions without repeats within an attempt, gives immediate feedback, and saves completed results. |
| Admin | Adds, edits, and deletes questions; regular users cannot use admin operations. |

Accounts, passwords, question changes, photos, and scores belong to each installation. They do not synchronize through GitHub. Uninstalling or clearing app data removes that installation's data.

**Pinned build configuration**

| Component | Version/value |
| --- | --- |
| Android Gradle plugin | 8.10.1 |
| Kotlin | 2.2.21 |
| Gradle | 8.11.1 |
| CI Java | Temurin JDK 17 |
| Compile/target SDK | 35 |
| Minimum SDK | 26 |
| Package and application ID | `com.example.simplequiz` |
| Runtime UI/database | Android platform Views and SQLite; no additional UI/database dependency |
| Test dependency | JUnit 4.13.2 |

The official wrapper JAR is fetched automatically because this preparation workspace could not download binary build tools. It is not embedded in the source ZIP. The checked-in launchers verify it before execution; they also work on later builds with the downloaded JAR. Keep `gradle/wrapper/gradle-wrapper.properties` and the launchers in the repository. You may commit the downloaded official JAR after a local build.

**Open locally, if desired**

1. Install a compatible Android Studio and open this project's root folder.
2. Before the first command-line build, the supplied launcher obtains the official wrapper automatically. On Windows you can run `powershell.exe -NoProfile -ExecutionPolicy Bypass -File scripts\prepare-wrapper.ps1` to prepare only the wrapper before opening Studio.
3. Let Android Studio use/download Gradle 8.11.1. Select a compatible JDK and install Android platform 35/build tools 35.0.0 through SDK Manager if prompted. Keep machine-specific SDK paths in your uncommitted `local.properties`.
4. Run the `app` configuration on an API 26+ device/emulator. Java 17 is used in CI; a compatible Studio-bundled JDK can also run Gradle locally.

Windows debug build, after Android Studio/SDK installation:

```powershell
.\gradlew.bat --no-daemon :app:assembleDebug
```

Alternatively double-click `BUILD_APK.bat`. It checks the usual local Android SDK/Studio Java locations. For a custom installation, configure the SDK/JDK in Android Studio or your environment.

Linux/macOS debug build:

```bash
bash ./gradlew --no-daemon :app:assembleDebug
```

Run the CI checks locally:

```bash
bash ./gradlew --no-daemon :app:testDebugUnitTest :app:lintDebug :app:assembleDebug
```

On Windows, replace `bash ./gradlew` with `.\gradlew.bat`. A local APK appears at `app/build/outputs/apk/debug/app-debug.apk`.

**Group workflow**

After upload, invite the other three members as collaborators. Each member clones the complete project. Create the agreed task branches from the current shared baseline:

| Member | Branch |
| --- | --- |
| Member 1 | `dashboard-integration` |
| Member 2 — Mark | `accounts-profile` |
| Member 3 | `quiz-gameplay` |
| Member 4 | `database-admin` |

File ownership and shared interfaces are in [TEAM_TASKS.md](docs/TEAM_TASKS.md). Manual account/profile checks are in [Member2_Manual_Test_Checklist.csv](docs/Member2_Manual_Test_Checklist.csv). See [TESTING.md](docs/TESTING.md) for the full demonstration sequence.

**If the automatic build fails**

Open the failed Actions run, expand the first failed step, and download `SimpleQuiz-build-reports` if available. The artifact contains `build-log.txt`, available unit-test results, and lint reports. Use the first meaningful error rather than only the final `BUILD FAILED` line. If the workflow itself cannot start, check that `.github/workflows/android-build.yml` is at the repository root and allowed by repository settings.

**Validation status**

The packaged source/resources and workflow structure were checked locally; details are in [BUILD_STATUS.md](docs/BUILD_STATUS.md). An Android build, JVM unit-test run, and device/emulator demonstration have not been completed in the preparation workspace. Network restrictions blocked its build-tool downloads. The first successful GitHub Actions run is the first full build/check result for this assembled project. Device testing remains necessary for UI, storage, and lifecycle behavior.

**Build references**

- [Android Gradle plugin 8.10 compatibility](https://developer.android.com/build/releases/agp-8-10-0-release-notes)
- [Kotlin Gradle plugin compatibility](https://kotlinlang.org/docs/gradle-configure-project.html)
- [Gradle's published distribution and wrapper checksums](https://gradle.org/release-checksums/)
- [Android SDK setup action](https://github.com/android-actions/setup-android)
- [Gradle setup action](https://github.com/gradle/actions)
- [GitHub artifact uploads](https://github.com/actions/upload-artifact)
