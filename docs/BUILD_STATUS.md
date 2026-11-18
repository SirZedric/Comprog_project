**Build and verification status**

This complete Android project was assembled from the component ZIP attached to this conversation. The original 16 Kotlin source files, nine layouts, colors, and question CSV retain their supplied contents. The manifest was adapted to add the application name resource and launcher icon. Build configuration, bootstrap launchers, tests, documentation, and the GitHub Actions workflow were added.

**Checks actually completed in the preparation workspace**

- All 13 Android XML files parse.
- All 55 distinct Kotlin view-ID references resolve.
- All Kotlin layout resource references resolve.
- All Kotlin color resource references resolve.
- All 9 Activities resolve; LoginActivity is the sole launcher.
- Bundled CSV has 100 unique question IDs and valid required fields/answers.
- All 16 original Kotlin files, 9 original layouts, colors, and CSV remain byte-identical.
- Workflow YAML parses with push, pull-request, and manual triggers and APK/report outputs.
- Ten JVM test methods are included (execution pending).
- Both sh and bash parse the Unix Gradle launcher.
- Gradle distribution and bootstrap JAR checksums match the retrieved official reference.
- Unix launcher rejects a corrupt cached JAR and corrupt download without invoking Java, including paths with spaces; temporary download is cleaned up.

**Checks not completed here**

- Gradle/Kotlin/Android compilation and APK generation.
- Execution of the ten included JVM tests.
- Android lint execution.
- Windows batch/PowerShell execution.
- Installation, UI testing, or emulator/phone demonstration.

The workspace did not have an Android SDK or Gradle installation. Direct downloads from the public build-tool hosts were blocked by its network restrictions. No APK is included or claimed as built. GitHub Actions is configured to perform the full build/check command on upload, publish an APK after success, and retain available failure logs/reports.

**First complete build**

Upload the extracted project contents to the repository root, including `.github/workflows/android-build.yml`. Open Actions > Build Android APK. A successful run provides the actual compilation, test, lint, and APK result. Use the manual checklist on a real device/emulator after installing that APK.

**Wrapper bootstrap**

The official wrapper JAR is downloaded automatically on first CLI/CI use. Its bytes are checked against Gradle's published SHA-256 before Java executes it. The pinned distribution also has a published SHA-256. The source ZIP includes the launchers and wrapper properties; the JAR binary itself was not downloadable in the preparation workspace.

Published reference values used:

- Gradle version: `8.11.1`
- Binary distribution SHA-256: `f397b287023acdba1e9f6fc5ea72d22dd63669d59ed4a289a29b1a76eee151c6`
- Wrapper JAR SHA-256: `2db75c40782f5e8ba1fc278a5574bab070adccb2d21ca5a6e5ed840888448046`

[Official Gradle checksum reference](https://gradle.org/release-checksums/)
