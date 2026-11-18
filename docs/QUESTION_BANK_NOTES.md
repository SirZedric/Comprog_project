# Question bank notes

`assets/questions.csv` contains 100 original beginner questions on basic computing and programming. This is a replaceable starter subject because a final quiz subject was not specified. Review the content against the course syllabus before using it for a graded assessment.

## File placement

Copy `questions.csv` into the Android application's `app/src/main/assets/` folder. Create the `assets` folder if it does not exist. Keep the filename and the header names unchanged so that `CsvImporter.kt` can read the bank.

## CSV columns

| Column | Meaning |
| --- | --- |
| `id` | Unique positive question identifier. The starter IDs are 1 through 100. |
| `question` | The question shown to the learner. |
| `option_a` | The text for choice A. |
| `option_b` | The text for choice B. |
| `option_c` | The text for choice C. |
| `option_d` | The text for choice D. |
| `correct_answer` | The correct choice letter: A, B, C, or D. |

The bank contains 25 correct answers at each letter position. All questions have four nonempty, distinct choices. The app should randomize the selected questions without replacement. If it later shuffles choices too, it must update the correct-answer mapping.

The file uses UTF-8 and standard CSV quoting. Some questions contain commas or quotation marks. Use the provided parser or another CSV-aware parser instead of splitting each line at commas. When editing in Excel or a similar tool, export as CSV UTF-8 and preserve the column names.

## Content covered

The questions cover binary numbers, hardware and files, algorithms, variables and expressions, conditions and loops, collections, functions and Boolean logic, SQL and relational databases, and simple quiz-score calculations. Score percentages use `score / total * 100`; for example, 20 out of 50 is 40%.

## Updating the bank

The CSV supplies the initial database contents. After successful import, admin edits are made to the SQLite database on the current device. Replacing the CSV asset does not automatically overwrite an existing installed database. This preserves admin changes. Test a new starter CSV on a fresh installation or explicitly implement a reviewed import/version-upgrade feature that preserves user accounts and attempt history.

Keep IDs unique when adding CSV rows. Keep each answer letter synchronized with its choice text. A newly generated bank for a different subject can use the same seven columns.
