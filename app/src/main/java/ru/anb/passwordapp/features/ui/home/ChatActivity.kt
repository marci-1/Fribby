package ru.anb.passwordapp.features.ui.home

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.text.TextUtils
import android.view.LayoutInflater
import android.view.MenuItem
import android.widget.EditText
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import ru.anb.passwordapp.R
import java.text.SimpleDateFormat
import java.util.*

class ChatActivity : AppCompatActivity() {

    private lateinit var messageReceiverId: String
    private lateinit var messageReceiverName: String
    private lateinit var messageSenderId: String
    private lateinit var saveCurrentTime: String
    private lateinit var saveCurrentDate: String

    private lateinit var userName: TextView
    private lateinit var userLastSeen: TextView
    private lateinit var chatToolbar: Toolbar
    private lateinit var mAuth: FirebaseAuth
    private lateinit var rootRef: DatabaseReference

    private lateinit var sendMessageButton: ImageButton
    private lateinit var userMessageInput: EditText

    private val messagesList = mutableListOf<Messages>()
    private lateinit var linearLayoutManager: LinearLayoutManager
    private lateinit var messageAdapter: MessageAdapter
    private lateinit var userMessagesList: RecyclerView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_chat)

        mAuth = FirebaseAuth.getInstance()
        messageSenderId = mAuth.currentUser?.uid.orEmpty()
        rootRef = FirebaseDatabase.getInstance("https://fribby-3e3f9-default-rtdb.europe-west1.firebasedatabase.app").reference

        messageReceiverId = intent.getStringExtra("visit_user_id").toString()
        messageReceiverName = intent.getStringExtra("visit_user_name").toString()

        initializeControllers()

        userName.text = messageReceiverName
        sendMessageButton.setOnClickListener { sendMessage() }
        displayLastSeen()
    }

    private fun initializeControllers() {
        chatToolbar = findViewById(R.id.chat_bar_layout)
        setSupportActionBar(chatToolbar)
        supportActionBar?.title = null
        supportActionBar?.apply {
            setDisplayHomeAsUpEnabled(true)
            setDisplayShowCustomEnabled(true)
        }

        val layoutInflater = getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
        val actionBarView = layoutInflater.inflate(R.layout.custom_chat_bar, null)
        supportActionBar?.customView = actionBarView

        userName = findViewById(R.id.custom_profile_name)
        userLastSeen = findViewById(R.id.custom_user_last_seen)
        sendMessageButton = findViewById(R.id.send_message_button)
        userMessageInput = findViewById(R.id.input_message)

        messageAdapter = MessageAdapter(messagesList)
        userMessagesList = findViewById(R.id.messages_list_users)
        linearLayoutManager = LinearLayoutManager(this)
        userMessagesList.layoutManager = linearLayoutManager
        userMessagesList.adapter = messageAdapter

        val calendar = Calendar.getInstance()
        saveCurrentDate = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()).format(calendar.time)
        saveCurrentTime = SimpleDateFormat("hh:mm a", Locale.getDefault()).format(calendar.time)

        userName.setOnClickListener {
            sendUserToProfileActivity()
        }
    }
    private fun sendUserToProfileActivity() {
        val mainIntent = Intent(this, ProfileActivity::class.java)
        mainIntent.putExtra("visit_user_id", messageReceiverId)
        mainIntent.putExtra("visit_user_name", messageReceiverName)
        startActivity(mainIntent)
    }

    private fun sendMessage() {
        val messageText = userMessageInput.text.toString()
        if (TextUtils.isEmpty(messageText)) {
            Toast.makeText(this, getString(R.string.First_write_your_message), Toast.LENGTH_SHORT).show()
        } else {
            val messageSenderRef = "Messages/$messageSenderId/$messageReceiverId"
            val messageReceiverRef = "Messages/$messageReceiverId/$messageSenderId"
            val userMessageKeyRef = rootRef.child("Messages").child(messageSenderId).child(messageReceiverId).push()
            val messagePushId = userMessageKeyRef.key.orEmpty()

            val messageTextBody = hashMapOf(
                "message" to messageText,
                "type" to "text",
                "from" to messageSenderId,
                "to" to messageReceiverId,
                "messageID" to messagePushId,
                "time" to saveCurrentTime,
                "date" to saveCurrentDate
            )

            val messageBodyDetails = hashMapOf(
                "$messageSenderRef/$messagePushId" to messageTextBody,
                "$messageReceiverRef/$messagePushId" to messageTextBody
            )

            rootRef.updateChildren(messageBodyDetails as Map<String, Any>).addOnCompleteListener {
                val msg = if (it.isSuccessful) getString(R.string.Message_Sent_Successfully) else "Error"
                Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
                userMessageInput.setText("")
            }
        }
    }

    private fun displayLastSeen() {
        rootRef.child("Users").child(messageReceiverId).addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (snapshot.child("userState").hasChild("state")) {
                    val state = snapshot.child("userState/state").value.toString()
                    val date = snapshot.child("userState/date").value.toString()
                    val time = snapshot.child("userState/time").value.toString()
                    userLastSeen.text = if (state == "online") "online" else "Last seen: $date $time"
                } else {
                    userLastSeen.text = "offline"
                }
            }

            override fun onCancelled(error: DatabaseError) {}
        })
    }

    override fun onStart() {
        super.onStart()
        rootRef.child("Messages").child(messageSenderId).child(messageReceiverId)
            .addChildEventListener(object : ChildEventListener {
                override fun onChildAdded(snapshot: DataSnapshot, previousChildName: String?) {
                    val message = snapshot.getValue(Messages::class.java)
                    message?.let {
                        messagesList.add(it)
                        messageAdapter.notifyDataSetChanged()
                        userMessagesList.smoothScrollToPosition(userMessagesList.adapter!!.itemCount)
                    }
                }

                override fun onChildChanged(snapshot: DataSnapshot, previousChildName: String?) {}
                override fun onChildRemoved(snapshot: DataSnapshot) {}
                override fun onChildMoved(snapshot: DataSnapshot, previousChildName: String?) {}
                override fun onCancelled(error: DatabaseError) {}
            })
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            android.R.id.home -> {
                onBackPressedDispatcher.onBackPressed()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        messagesList.clear()
    }

    override fun onPause() {
        super.onPause()
        messagesList.clear()
    }

    override fun onResume() {
        super.onResume()
        messagesList.clear()
    }
}