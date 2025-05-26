package ru.anb.passwordapp.features.ui.home

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import ru.anb.passwordapp.R
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContentProviderCompat.requireContext
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import ru.anb.passwordapp.features.ui.home.ChatActivity

class ProfileActivity : AppCompatActivity() {

    private lateinit var receiverUserId: String
    private lateinit var senderUserId: String
    private lateinit var currentState: String

    private lateinit var userProfileName: TextView
    private lateinit var userProfileStatus: TextView
    private lateinit var sendMessageRequestButton: Button
    private lateinit var declineMessageRequestButton: Button
    private lateinit var viewInterestsButton: Button
    private lateinit var backButton: Button
    private lateinit var userRef: DatabaseReference
    private lateinit var chatRequestRef: DatabaseReference
    private lateinit var contactsRef: DatabaseReference
    private lateinit var notificationRef: DatabaseReference
    private lateinit var mAuth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_profile)

        mAuth = FirebaseAuth.getInstance()
        userRef = FirebaseDatabase.getInstance("https://fribby-3e3f9-default-rtdb.europe-west1.firebasedatabase.app").getReference("Users")

        chatRequestRef = FirebaseDatabase.getInstance("https://fribby-3e3f9-default-rtdb.europe-west1.firebasedatabase.app").getReference("Chat Requests")
        contactsRef = FirebaseDatabase.getInstance("https://fribby-3e3f9-default-rtdb.europe-west1.firebasedatabase.app").getReference("Contacts")
        notificationRef = FirebaseDatabase.getInstance("https://fribby-3e3f9-default-rtdb.europe-west1.firebasedatabase.app").getReference("Notifications")

        receiverUserId = intent.getStringExtra("visit_user_id").toString()
        senderUserId = mAuth.currentUser?.uid.toString()

        userProfileName = findViewById(R.id.visit_user_name)
        userProfileStatus = findViewById(R.id.visit_profile_status)
        sendMessageRequestButton = findViewById(R.id.send_message_request_button)
        declineMessageRequestButton = findViewById(R.id.decline_message_request_button)
        viewInterestsButton = findViewById(R.id.profile_view_interests)
        backButton = findViewById(R.id.backButton)

        currentState = "new"

        viewInterestsButton.setOnClickListener {
            val viewInterestsIntent = Intent(this, ViewInterestsActivity::class.java)
            viewInterestsIntent.putExtra("visit_user_id", receiverUserId)
            startActivity(viewInterestsIntent)
        }
        backButton.setOnClickListener {
            onBackPressedDispatcher.onBackPressed()
        }


        retrieveUserInfo()
    }

    private fun retrieveUserInfo() {
        userRef.child(receiverUserId).addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (snapshot.exists()) {
                    val userName = snapshot.child("displayname").value.toString()
                    val userStatus = snapshot.child("status").value.toString()
                    userProfileName.text = userName
                    userProfileStatus.text = userStatus

                    manageChatRequests()
                }
            }

            override fun onCancelled(error: DatabaseError) {}
        })
    }

    private fun manageChatRequests() {
        chatRequestRef.child(senderUserId).addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (snapshot.hasChild(receiverUserId)) {
                    when (snapshot.child(receiverUserId).child("request_type").value.toString()) {
                        "received" -> {
                            currentState = "request_received"
                            sendMessageRequestButton.text = "+"
                            sendMessageRequestButton.isEnabled = true

                            declineMessageRequestButton.text = "-"
                            declineMessageRequestButton.visibility = View.VISIBLE
                            declineMessageRequestButton.isEnabled = true

                            declineMessageRequestButton.setOnClickListener { cancelChatRequest() }
                        }
                    }
                } else {
                    contactsRef.child(senderUserId).addListenerForSingleValueEvent(object : ValueEventListener {
                        override fun onDataChange(snapshot: DataSnapshot) {
                            if (snapshot.hasChild(receiverUserId)) {
                                currentState = "friends"
                                sendMessageRequestButton.text = "-"
                            }
                        }

                        override fun onCancelled(error: DatabaseError) {}
                    })
                }
            }

            override fun onCancelled(error: DatabaseError) {}
        })

        if (senderUserId != receiverUserId) {
            sendMessageRequestButton.setOnClickListener {
                sendMessageRequestButton.isEnabled = false
                when (currentState) {
                    "new" -> sendChatRequest()
                    "request_sent" -> cancelChatRequest()
                    "request_received" -> acceptChatRequest()
                    "friends" -> removeSpecificContact()
                }
            }
        } else {
            sendMessageRequestButton.visibility = View.INVISIBLE
        }
    }

    private fun sendChatRequest() {
        chatRequestRef.child(senderUserId).child(receiverUserId).child("request_type")
            .setValue("sent").addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    chatRequestRef.child(receiverUserId).child(senderUserId).child("request_type")
                        .setValue("received").addOnCompleteListener {
                            sendMessageRequestButton.isEnabled = true
                            currentState = "request_sent"
                            sendMessageRequestButton.text = "-"
                            Toast.makeText(this, getString(R.string.sendChatRequest), Toast.LENGTH_SHORT).show()
                        }
                }
            }
    }

    private fun acceptChatRequest() {
        contactsRef.child(senderUserId).child(receiverUserId).child("Contacts")
            .setValue("saved").addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    contactsRef.child(receiverUserId).child(senderUserId).child("Contacts")
                        .setValue("saved").addOnCompleteListener {
                            chatRequestRef.child(senderUserId).child(receiverUserId).removeValue()
                                .addOnCompleteListener {
                                    chatRequestRef.child(receiverUserId).child(senderUserId).removeValue()
                                        .addOnCompleteListener {
                                            sendMessageRequestButton.isEnabled = true
                                            currentState = "friends"
                                            sendMessageRequestButton.text = "-"
                                            declineMessageRequestButton.visibility = View.INVISIBLE
                                            declineMessageRequestButton.isEnabled = false

                                            Toast.makeText(this, getString(R.string.acceptChatRequest), Toast.LENGTH_SHORT).show()
                                        }
                                }
                        }
                }
            }
    }

    private fun cancelChatRequest() {
        chatRequestRef.child(senderUserId).child(receiverUserId).removeValue()
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    chatRequestRef.child(receiverUserId).child(senderUserId).removeValue()
                        .addOnCompleteListener {
                            sendMessageRequestButton.isEnabled = true
                            currentState = "new"
                            sendMessageRequestButton.text = "+"
                            declineMessageRequestButton.visibility = View.INVISIBLE
                            declineMessageRequestButton.isEnabled = false
                            Toast.makeText(this, getString(R.string.cancelChatRequest), Toast.LENGTH_SHORT).show()
                        }
                }
            }
    }

    private fun removeSpecificContact() {
        contactsRef.child(senderUserId).child(receiverUserId).removeValue()
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    contactsRef.child(receiverUserId).child(senderUserId).removeValue()
                        .addOnCompleteListener {
                            sendMessageRequestButton.isEnabled = true
                            currentState = "new"
                            sendMessageRequestButton.text = "+"
                            declineMessageRequestButton.visibility = View.INVISIBLE
                            declineMessageRequestButton.isEnabled = false

                            Toast.makeText(this, getString(R.string.contact_removed), Toast.LENGTH_SHORT).show()
                        }
                }
            }
    }
}