package ru.anb.passwordapp.features.ui.home

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.firebase.ui.database.FirebaseRecyclerAdapter
import com.firebase.ui.database.FirebaseRecyclerOptions
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import ru.anb.passwordapp.R

class ChatsFragment : Fragment() {
    private lateinit var chatsView: View
    private lateinit var chatsList: RecyclerView
    private lateinit var contactsRef: DatabaseReference
    private lateinit var userRef: DatabaseReference
    private lateinit var mAuth: FirebaseAuth
    private lateinit var currentUserId: String

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View? {
        chatsView = inflater.inflate(R.layout.fragment_chats, container, false)

        mAuth = FirebaseAuth.getInstance()
        currentUserId = mAuth.currentUser?.uid ?: ""

        contactsRef = FirebaseDatabase.getInstance("https://fribby-3e3f9-default-rtdb.europe-west1.firebasedatabase.app")
            .getReference("Contacts").child(currentUserId)
        userRef = FirebaseDatabase.getInstance("https://fribby-3e3f9-default-rtdb.europe-west1.firebasedatabase.app")
            .getReference("Users")

        chatsList = chatsView.findViewById(R.id.chats_list)
        chatsList.layoutManager = LinearLayoutManager(context)

        val itemDecoration = DividerItemDecoration(context, DividerItemDecoration.VERTICAL)
        itemDecoration.setDrawable(resources.getDrawable(R.drawable.recyclerview_divider, null))
        chatsList.addItemDecoration(itemDecoration)

        return chatsView
    }

    override fun onStart() {
        super.onStart()

        val options = FirebaseRecyclerOptions.Builder<Contacts>()
            .setQuery(contactsRef, Contacts::class.java)
            .build()

        val adapter = object : FirebaseRecyclerAdapter<Contacts, ChatsViewHolder>(options) {
            override fun onBindViewHolder(
                holder: ChatsViewHolder, position: Int, model: Contacts
            ) {
                val userId = getRef(position).key ?: return

                userRef.child(userId).addValueEventListener(object : ValueEventListener {
                    override fun onDataChange(snapshot: DataSnapshot) {
                        if (snapshot.exists()) {
                            val userName = snapshot.child("displayname").getValue(String::class.java) ?: ""
                            holder.userName.text = userName

                            val messagesRef = FirebaseDatabase.getInstance("https://fribby-3e3f9-default-rtdb.europe-west1.firebasedatabase.app")
                                .getReference("Messages")
                                .child(currentUserId)
                                .child(userId)

                            messagesRef.orderByKey().limitToLast(1)
                                .addChildEventListener(object : ChildEventListener {
                                    override fun onChildAdded(snapshot: DataSnapshot, previousChildName: String?) {
                                        if (snapshot.exists()) {
                                            val message = snapshot.getValue(Messages::class.java)
                                            val lastMsg = when (message?.type) {
                                                "image" -> "image"
                                                else -> message?.message ?: "Start a conversation!"
                                            }
                                            holder.lastmessage.text = lastMsg
                                        } else {
                                            holder.lastmessage.text = "Start a conversation!"
                                        }
                                    }

                                    override fun onChildChanged(snapshot: DataSnapshot, previousChildName: String?) {}
                                    override fun onChildRemoved(snapshot: DataSnapshot) {}
                                    override fun onChildMoved(snapshot: DataSnapshot, previousChildName: String?) {}
                                    override fun onCancelled(error: DatabaseError) {}
                                })

                            holder.itemView.setOnClickListener {
                                val chatIntent = Intent(context, ChatActivity::class.java)
                                chatIntent.putExtra("visit_user_id", userId)
                                chatIntent.putExtra("visit_user_name", userName)
                                startActivity(chatIntent)
                            }
                        }
                    }

                    override fun onCancelled(error: DatabaseError) {}
                })
            }

            override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ChatsViewHolder {
                val view = LayoutInflater.from(parent.context)
                    .inflate(R.layout.users_display_layout, parent, false)
                return ChatsViewHolder(view)
            }
        }

        chatsList.adapter = adapter
        adapter.startListening()
    }

    class ChatsViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val userName: TextView = itemView.findViewById(R.id.user_name)
        val lastmessage: TextView = itemView.findViewById(R.id.user_status)
    }

    class Messages {
        var message: String? = null
        var type: String? = null
    }
}
