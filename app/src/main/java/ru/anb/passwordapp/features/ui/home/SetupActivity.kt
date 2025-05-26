package ru.anb.passwordapp.features.ui.home

import android.app.ProgressDialog
import android.content.Intent
import android.os.Bundle
import android.text.TextUtils
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.Spinner
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import ru.anb.passwordapp.R
import java.util.Locale

class SetupActivity : AppCompatActivity() {

    private lateinit var status: EditText
    private lateinit var displayName: EditText
    private lateinit var saveInformationButton: Button
    private lateinit var progressDialog: ProgressDialog

    private lateinit var mAuth: FirebaseAuth
    private var selectedCountry: String = ""
    private var isCountrySelected = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_setup)

        mAuth = FirebaseAuth.getInstance()
        val currentUserId = mAuth.currentUser?.uid ?: return
        val userRef = FirebaseDatabase.getInstance("https://fribby-3e3f9-default-rtdb.europe-west1.firebasedatabase.app").reference.child("Users").child(currentUserId)

        // Инициализация UI
        status = findViewById(R.id.setup_status)
        displayName = findViewById(R.id.setup_displayname)
        saveInformationButton = findViewById(R.id.setup_information_button)

        progressDialog = ProgressDialog(this)

        saveInformationButton.setOnClickListener {
            saveAccountSetupInformation(userRef, currentUserId)
        }

    }

    private fun saveAccountSetupInformation(userRef: com.google.firebase.database.DatabaseReference, currentUserId: String) {

        val userStatus = status.text.toString().ifEmpty { "Hello! I'm using Fribby" }
        val displayname = displayName.text.toString()

        if (TextUtils.isEmpty(displayname)) {
            Toast.makeText(this, "Please enter your display name...", Toast.LENGTH_SHORT).show()
            return
        }

        progressDialog.setTitle("Saving Information")
        progressDialog.setCanceledOnTouchOutside(true)
        progressDialog.show()

        val userMap = hashMapOf(
            "uid" to currentUserId,
            "displayname" to displayname,
            "status" to userStatus
        )

        userRef.updateChildren(userMap as Map<String, Any>).addOnCompleteListener { task ->
            if (task.isSuccessful) {
                sendUserToMainActivity()
                Toast.makeText(this, "Your account is created successfully!", Toast.LENGTH_LONG).show()
            } else {
                Toast.makeText(this, "Error Occurred: ${task.exception?.message}", Toast.LENGTH_LONG).show()
            }
            progressDialog.dismiss()
        }
    }

    private fun sendUserToMainActivity() {
        val mainIntent = Intent(this, HomeActivity::class.java)
        mainIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
        startActivity(mainIntent)
        finish()
    }
}
