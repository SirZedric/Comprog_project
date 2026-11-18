package com.example.simplequiz

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import java.io.File

/** Names are saved with the button. Selected photos are copied and saved immediately. */
class ProfileActivity : BaseActivity() {
    private lateinit var displayNameInput: EditText
    private lateinit var photoView: ImageView
    private lateinit var choosePhotoButton: Button
    private lateinit var saveButton: Button
    private var currentUser: QuizUser? = null
    private var pendingPhotoUri: Uri? = null
    private var restoredDisplayName: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_profile)
        displayNameInput = findViewById(R.id.profileDisplayName)
        photoView = findViewById(R.id.profilePhoto)
        choosePhotoButton = findViewById(R.id.choosePhotoButton)
        saveButton = findViewById(R.id.saveProfileButton)
        restoredDisplayName = savedInstanceState?.getString("display_name")
        setBusy(true)

        findViewById<Button>(R.id.profileBackButton).setOnClickListener { finish() }
        choosePhotoButton.setOnClickListener {
            val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
                addCategory(Intent.CATEGORY_OPENABLE)
                type = "image/*"
            }
            @Suppress("DEPRECATION")
            startActivityForResult(intent, PICK_PHOTO)
        }
        saveButton.setOnClickListener { saveDisplayName() }

        requireUser { user ->
            currentUser = user
            findViewById<TextView>(R.id.profileUsername).text = "Username: ${user.username}"
            displayNameInput.setText(restoredDisplayName ?: user.displayName)
            Ui.setPhoto(photoView, user.photoFile)
            setBusy(false)
            pendingPhotoUri?.let { uri ->
                pendingPhotoUri = null
                savePhoto(uri)
            }
        }
    }

    private fun saveDisplayName() {
        val user = currentUser ?: return
        val newName = displayNameInput.text.toString().trim()
        if (newName.isEmpty() || newName.length > 60) {
            toast("Enter a display name containing 1 to 60 characters.")
            return
        }
        setBusy(true)
        runTask(
            work = {
                db.updateProfile(user.id, newName, user.photoFile)
                db.getUser(user.id)
            },
            done = { updatedUser ->
                currentUser = updatedUser
                toast("Display name saved.")
            },
            finished = { setBusy(false) }
        )
    }

    @Deprecated("Uses the platform result API to keep this sample dependency-free.")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode != PICK_PHOTO || resultCode != Activity.RESULT_OK) return
        val uri = data?.data ?: return
        if (currentUser == null) pendingPhotoUri = uri else savePhoto(uri)
    }

    private fun savePhoto(uri: Uri) {
        val user = currentUser ?: return
        setBusy(true)
        runTask(
            work = {
                val photoFile = Ui.importProfilePhoto(this, uri, user.id)
                try {
                    db.updateProfile(user.id, user.displayName, photoFile)
                } catch (error: Exception) {
                    deleteOwnedPhoto(photoFile)
                    throw error
                }
                if (user.photoFile != photoFile) deleteOwnedPhoto(user.photoFile)
                db.getUser(user.id)
            },
            done = { updatedUser ->
                currentUser = updatedUser
                Ui.setPhoto(photoView, updatedUser?.photoFile)
                toast("Profile photo saved.")
            },
            finished = { setBusy(false) }
        )
    }

    private fun deleteOwnedPhoto(filename: String?) {
        if (filename != null && filename.startsWith("profile_") &&
            !filename.contains('/') && !filename.contains('\\')) {
            File(filesDir, filename).delete()
        }
    }

    private fun setBusy(busy: Boolean) {
        choosePhotoButton.isEnabled = !busy
        saveButton.isEnabled = !busy
        displayNameInput.isEnabled = !busy
    }

    override fun onSaveInstanceState(outState: Bundle) {
        outState.putString("display_name", displayNameInput.text.toString())
        super.onSaveInstanceState(outState)
    }

    companion object {
        private const val PICK_PHOTO = 100
    }
}
