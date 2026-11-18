**Checks and demonstration**

The included GitHub Actions job runs ten JVM tests, Android lint, and APK assembly. These checks require dependency downloads and have not been run in the preparation workspace.

The password tests cover valid/incorrect passwords, password case, significant whitespace, independent salts for equal passwords, password-record sizes/work factor, and malformed records. CSV tests validate the bundled 100-question resource, quoted commas/escaped quotes/BOM, duplicate IDs, invalid answers, blank questions, bad headers, and unclosed quotes.

After a successful build, install the downloaded debug APK and perform these manual checks:

1. On fresh app data, complete all four admin passwords. Test a blank/short password first; confirm setup blocks it. Wait for the creation result.
2. Log in/out as `admin1` through `admin4`; confirm Admin role. Relaunch and confirm setup does not recur.
3. Sign up as a regular account. Confirm immediate Dashboard navigation and User role. Check invalid username/password, case-insensitive duplicates, and reserved admin names.
4. Check login with the wrong password, changed password case, and changed username case. Only the latter should work when the original password is correct.
5. Save a display name. Return to Dashboard and confirm it refreshes. Reject a blank name. Leave an unsaved draft and confirm it is not stored.
6. Select a valid profile image, cancel selection, and replace it. Confirm the chosen image saves immediately and remains after relogin. Selecting a picture does not save an unsaved display-name draft.
7. Test two accounts with different names/photos. Confirm the profiles remain separate. Repeat profile editing as an admin.
8. Start a short quiz, check green/red answer feedback, finish, and confirm one history entry with the correct fraction and percentage.
9. As an admin, add/edit/delete a question. Confirm the bank count changes and regular users lack management access.
10. Check logout/session restoration, scrolling with the keyboard visible, screen rotation, and Back behavior.

Use `Member2_Manual_Test_Checklist.csv` for 58 account/profile/build cases. Actual-result fields are blank. The source's known rotation behavior is described below; record observations rather than assuming every robustness case passes.

Implementation details to describe accurately:

- Username: trimmed, 3–30 ASCII letters/numbers/underscores, case-insensitive uniqueness. Password: 8–128 characters, case-sensitive, not trimmed. No confirmation-password field or change/reset flow is implemented.
- Names save by button; photos save immediately. The photo helper stores a private JPEG thumbnail with a longest side of at most 512 pixels.
- The photo helper does not apply EXIF orientation; check a phone-camera image and coordinate any correction with Member 1.
- Password fields intentionally clear after Activity recreation. Display-name drafts have saved-state support.
- A submitted background operation can finish after its Activity is destroyed. In particular, rotating during signup can create the account without automatic navigation. Try logging in before repeating signup. Wait for save success during demonstrations.
- All four admins are created transactionally. There are no default admin passwords.
- Clearing app data/uninstalling resets the installation, including local accounts, photos, results, and question-bank changes.

Capture actual Login, Signup, Profile, and refreshed Dashboard screenshots for Mark's contribution. Keep actual passwords out of reports and logs.
