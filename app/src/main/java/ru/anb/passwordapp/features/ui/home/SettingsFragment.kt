package ru.anb.passwordapp.features.ui.home

import android.app.ProgressDialog
import android.content.Context
import android.os.Bundle
import android.text.TextUtils
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.fragment.app.Fragment
import ru.anb.passwordapp.R
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import ru.anb.passwordapp.features.ui.interest.InterestsActivity
import android.content.Intent
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import java.util.*

class SettingsFragment : Fragment() {

    private lateinit var displayName: EditText
    private lateinit var status: EditText
    private lateinit var updateAccountSettings: Button
    private lateinit var editInterests: Button
    private lateinit var mAuth: FirebaseAuth
    private lateinit var userRef: DatabaseReference
    private lateinit var progressDialog: ProgressDialog
    private lateinit var profileDisplayPic: ImageView

    private var currentUserId: String = ""

    private fun isInternetAvailable(): Boolean {
        val connectivityManager =
            requireContext().getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val network = connectivityManager.activeNetwork ?: return false
        val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return false
        return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_settings, container, false)

        mAuth = FirebaseAuth.getInstance()
        currentUserId = mAuth.currentUser?.uid ?: ""
        userRef = FirebaseDatabase.getInstance("https://fribby-3e3f9-default-rtdb.europe-west1.firebasedatabase.app").reference.child("Users").child(currentUserId)

        displayName = view.findViewById(R.id.setting_displayname)
        status = view.findViewById(R.id.setting_status)
        updateAccountSettings = view.findViewById(R.id.edit_button)
        editInterests = view.findViewById(R.id.edit_interests)
        profileDisplayPic = view.findViewById(R.id.profile_display_pic)
        progressDialog = ProgressDialog(requireContext())

        if (isInternetAvailable()) {
            retrieveUserInfo()
        } else {
            val (localName, localStatus) = loadUserInfoLocally()
            displayName.setText(localName)
            status.setText(localStatus)
            displayName.isEnabled = false
            status.isEnabled = false
            updateAccountSettings.isEnabled = false
            Toast.makeText(requireContext(), "Нет подключения к интернету. Профиль доступен только для чтения", Toast.LENGTH_LONG).show()
        }

        updateAccountSettings.setOnClickListener { updateAccountInformation() }

        editInterests.setOnClickListener {
            if (isInternetAvailable()) {
                startActivity(Intent(requireContext(), InterestsActivity::class.java))
            } else {
                Toast.makeText(requireContext(), "Нет подключения к интернету", Toast.LENGTH_SHORT).show()
            }
        }
//        profileDisplayPic.setOnClickListener {
//            val intent = Intent(this, ImagePickerActivity::class.java)
//            startActivityForResult(intent, REQUEST_CODE_PICK_IMAGE)
//        }
        return view
    }

    private fun updateAccountInformation() {
        val displayname = displayName.text.toString()
        val statusInfo = status.text.toString()

        when {
            TextUtils.isEmpty(displayname) -> {
                Toast.makeText(requireContext(), "Please enter your display name...", Toast.LENGTH_SHORT).show()
            }
            TextUtils.isEmpty(statusInfo) -> {
                Toast.makeText(requireContext(), "Please enter your status...", Toast.LENGTH_SHORT).show()
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
                        Toast.makeText(requireContext(), "Your account is updated successfully!", Toast.LENGTH_LONG).show()
                    } else {
                        val message = task.exception?.message
                        Toast.makeText(requireContext(), "Error Occurred: $message", Toast.LENGTH_LONG).show()
                    }
                    progressDialog.dismiss()
                }
            }
        }
    }

    private fun saveUserInfoLocally(name: String, status: String) {
        if (!isAdded) return // 💡 фрагмент не привязан — выходим

        val sharedPref = requireContext().getSharedPreferences("user_info", Context.MODE_PRIVATE)
        with(sharedPref.edit()) {
            putString("displayname", name)
            putString("status", status)
            apply()
        }
    }

    private fun loadUserInfoLocally(): Pair<String, String> {
        val sharedPref = requireContext().getSharedPreferences("user_info", Context.MODE_PRIVATE)
        val name = sharedPref.getString("displayname", "") ?: ""
        val status = sharedPref.getString("status", "") ?: ""
        return Pair(name, status)
    }

    private fun retrieveUserInfo() {
        userRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (snapshot.exists() && snapshot.hasChild("displayname")) {
                    val retrievedDisplayName = snapshot.child("displayname").value.toString()
                    val retrievedStatus = snapshot.child("status").value.toString()

                    displayName.setText(retrievedDisplayName)
                    status.setText(retrievedStatus)

                    saveUserInfoLocally(retrievedDisplayName, retrievedStatus)

                } else {
                    Toast.makeText(requireContext(), "Update your profile information.", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onCancelled(error: DatabaseError) {}
        })
    }
}
