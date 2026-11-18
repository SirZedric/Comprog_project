**SimpleQuiz group ownership**

Every member needs the entire repository to build and run the app. File ownership identifies maintenance responsibility. Keep shared names and interfaces consistent.

All Kotlin sources below live in `app/src/main/java/com/example/simplequiz/`. Layouts live in `app/src/main/res/layout/`.

| Member | Kotlin files | Layouts/other files |
| --- | --- | --- |
| Member 1 | `DashboardActivity.kt`, `BaseActivity.kt`, `Session.kt`, `Ui.kt`, `Models.kt` | `activity_dashboard.xml`, manifest, colors, and coordinated project/Gradle/workflow changes |
| Member 2 — Mark | `LoginActivity.kt`, `SignupActivity.kt`, `ProfileActivity.kt`, `PasswordHasher.kt` | `activity_login.xml`, `activity_signup.xml`, `activity_profile.xml` |
| Member 3 | `QuizSetupActivity.kt`, `QuizActivity.kt`, `ResultActivity.kt` | `activity_quiz_setup.xml`, `activity_quiz.xml`, `activity_result.xml` |
| Member 4 | `DatabaseHelper.kt`, `CsvImporter.kt`, `AdminQuestionsActivity.kt`, `QuestionEditorActivity.kt` | `activity_admin_questions.xml`, `activity_question_editor.xml`, `app/src/main/assets/questions.csv` |

Member 2's database dependencies are `hasAdmins`, `initializeAdmins`, `signUp`, `login`, `getUser`, `updateProfile`, and `ensureQuestionsSeeded`. Member 4 maintains one shared implementation. Member 1 maintains the worker/session/photo helpers and Dashboard refresh/logout.

Keep `PasswordHasher.create` and `PasswordHasher.verify` compatible with their database callers. Use the supplied background worker for password derivation and database operations.

Use the assigned task branch, commit actual changes, push, and request a review. Test the combined app after merges. Screenshots and test results must come from the running app; do not label unexecuted checks passed.
