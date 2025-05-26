package ru.anb.passwordapp.features.ui.home

import android.app.ProgressDialog
import android.content.Intent
import android.os.Bundle
import android.text.TextUtils
import android.view.MenuItem
import android.view.View
import android.widget.*
import ru.anb.passwordapp.R
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import ru.anb.passwordapp.features.ui.home.SetupActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import ru.anb.passwordapp.core.ui.MainActivity
import ru.anb.passwordapp.features.ui.interest.InterestsActivity
import java.util.*

class SettingsActivity: AppCompatActivity() {
    private lateinit var displayName: EditText
    private lateinit var status: EditText
    private lateinit var updateAccountSettings: Button
    private lateinit var editInterests: Button
    private lateinit var mAuth: FirebaseAuth
    private lateinit var userRef: DatabaseReference
    private lateinit var progressDialog: ProgressDialog
    private lateinit var mToolbar: Toolbar

    private var currentUserId: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)

        mAuth = FirebaseAuth.getInstance()
        currentUserId = mAuth.currentUser?.uid ?: ""
        userRef = FirebaseDatabase.getInstance("https://fribby-3e3f9-default-rtdb.europe-west1.firebasedatabase.app").reference.child("Users").child(currentUserId)
        displayName = findViewById(R.id.setting_displayname)
        status = findViewById(R.id.setting_status)
        updateAccountSettings = findViewById(R.id.edit_button)
        editInterests = findViewById(R.id.edit_interests)

        progressDialog = ProgressDialog(this)

        retrieveUserInfo()

        updateAccountSettings.setOnClickListener { updateAccountInformation() }

        editInterests.setOnClickListener {
            startActivity(Intent(this, InterestsActivity::class.java))
        }

    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            android.R.id.home -> {
                sendUserToMainActivity()
                true
            }

            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun updateAccountInformation() {
        val displayname = displayName.text.toString()
        val statusInfo = status.text.toString()

        when {
            TextUtils.isEmpty(displayname) -> {
                Toast.makeText(this, "Please enter your display name...", Toast.LENGTH_SHORT).show()
            }
            TextUtils.isEmpty(statusInfo) -> {
                Toast.makeText(this, "Please enter your status...", Toast.LENGTH_SHORT).show()
            }
            else -> {
                progressDialog.setTitle("Updating Account Information")
                progressDialog.setMessage("Please wait while we are updating your information")
                progressDialog.setCanceledOnTouchOutside(false)
                progressDialog.show()

                val userMap = hashMapOf(
                    "displayname" to displayname,
                    "status" to statusInfo
                )

                userRef.updateChildren(userMap as Map<String, Any>).addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        sendUserToMainActivity()
                        Toast.makeText(this, "Your account is updated successfully!", Toast.LENGTH_LONG).show()
                    } else {
                        val message = task.exception?.message
                        Toast.makeText(this, "Error Occurred: $message", Toast.LENGTH_LONG).show()
                    }
                    progressDialog.dismiss()
                }
            }
        }
    }

    private fun retrieveUserInfo() {
        userRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (snapshot.exists() && snapshot.hasChild("displayname")) {
                    val retrievedDisplayName = snapshot.child("displayname").value.toString()
                    val retrieveStatus = snapshot.child("status").value.toString()

                    displayName.hint = retrievedDisplayName
                    status.hint = retrieveStatus

                } else {
                    Toast.makeText(this@SettingsActivity, "Update your profile information.", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onCancelled(error: DatabaseError) {}
        })
    }

    private fun sendUserToMainActivity() {
        val intent = Intent(this, HomeActivity::class.java)
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_NEW_TASK)
        startActivity(intent)
        finish()
    }
}