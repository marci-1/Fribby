package ru.anb.passwordapp.features.ui.home


import android.app.AlertDialog
import android.os.Bundle
import android.view.*
import android.widget.*
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.firebase.ui.database.FirebaseRecyclerAdapter
import com.firebase.ui.database.FirebaseRecyclerOptions
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import ru.anb.passwordapp.R

class RequestsFragment : Fragment() {

    private lateinit var requestsView: View
    private lateinit var myRequestsList: RecyclerView
    private lateinit var chatRequestRef: DatabaseReference
    private lateinit var userRef: DatabaseReference
    private lateinit var contactsRef: DatabaseReference
    private lateinit var mAuth: FirebaseAuth
    private lateinit var currentUserId: String

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        requestsView = inflater.inflate(R.layout.fragment_requests, container, false)

        mAuth = FirebaseAuth.getInstance()
        currentUserId = mAuth.currentUser?.uid ?: ""

        chatRequestRef = FirebaseDatabase.getInstance("https://fribby-3e3f9-default-rtdb.europe-west1.firebasedatabase.app").reference.child("Chat Requests")
        userRef = FirebaseDatabase.getInstance("https://fribby-3e3f9-default-rtdb.europe-west1.firebasedatabase.app").reference.child("Users")
        contactsRef = FirebaseDatabase.getInstance("https://fribby-3e3f9-default-rtdb.europe-west1.firebasedatabase.app").reference.child("Contacts")

        myRequestsList = requestsView.findViewById(R.id.chat_requests_list)
        myRequestsList.layoutManager = LinearLayoutManager(context)

        return requestsView
    }

    override fun onStart() {
        super.onStart()

        val options = FirebaseRecyclerOptions.Builder<Contacts>()
            .setQuery(chatRequestRef.child(currentUserId), Contacts::class.java)
            .build()

        val adapter = object : FirebaseRecyclerAdapter<Contacts, RequestsViewHolder>(options) {
            override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RequestsViewHolder {
                val view = LayoutInflater.from(parent.context)
                    .inflate(R.layout.users_request_display_layout, parent, false)
                return RequestsViewHolder(view)
            }

            override fun onBindViewHolder(holder: RequestsViewHolder, position: Int, model: Contacts) {
                val listUserId = getRef(position).key ?: return
                val getTypeRef = getRef(position).child("request_type").ref

                getTypeRef.addValueEventListener(object : ValueEventListener {
                    override fun onDataChange(snapshot: DataSnapshot) {
                        if (!isAdded) return
                        if (snapshot.exists()) {
                            val type = snapshot.value.toString()

                            if (type == "received") {
                                userRef.child(listUserId).addValueEventListener(object : ValueEventListener {
                                    override fun onDataChange(userSnapshot: DataSnapshot) {
                                        if (!isAdded) return
                                        val userName = userSnapshot.child("displayname").value.toString()
                                        val userStatus = userSnapshot.child("status").value.toString()

                                        context?.let {
                                            holder.userName.text = userName
                                            holder.userStatus.text = it.getString(R.string.wants_connect)
                                        }

                                        holder.acceptChat.setOnClickListener {
                                            contactsRef.child(currentUserId).child(listUserId).child("Contacts").setValue("saved")
                                                .addOnCompleteListener { task1 ->
                                                    if (task1.isSuccessful) {
                                                        contactsRef.child(listUserId).child(currentUserId).child("Contacts").setValue("saved")
                                                            .addOnCompleteListener { task2 ->
                                                                if (task2.isSuccessful) {
                                                                    chatRequestRef.child(currentUserId).child(listUserId).removeValue()
                                                                    chatRequestRef.child(listUserId).child(currentUserId).removeValue()
                                                                    context?.let {
                                                                        Toast.makeText(it, "New contact added", Toast.LENGTH_SHORT).show()
                                                                    }
                                                                }
                                                            }
                                                    }
                                                }
                                        }

                                        holder.declineChat.setOnClickListener {
                                            chatRequestRef.child(currentUserId).child(listUserId).removeValue()
                                                .addOnCompleteListener { task1 ->
                                                    if (task1.isSuccessful) {
                                                        chatRequestRef.child(listUserId).child(currentUserId).removeValue()
                                                        context?.let {
                                                            Toast.makeText(it, "Contact request declined", Toast.LENGTH_SHORT).show()
                                                        }
                                                    }
                                                }
                                        }

                                        holder.itemView.setOnClickListener {
                                            if (!isAdded) return@setOnClickListener
                                            context?.let {
                                                val options = arrayOf("Accept", "Cancel")
                                                AlertDialog.Builder(it)
                                                    .setTitle("$userName Chat Request")
                                                    .setItems(options) { _, which ->
                                                        when (which) {
                                                            0 -> acceptRequest(listUserId)
                                                            1 -> cancelRequest(listUserId)
                                                        }
                                                    }
                                                    .show()
                                            }
                                        }
                                    }

                                    override fun onCancelled(error: DatabaseError) {}
                                })
                            } else if (type == "sent") {
                                if (!isAdded) return

                                context?.let {
                                    holder.acceptChat.text = it.getString(R.string.req_sent_button)
                                }
                                holder.declineChat.visibility = View.INVISIBLE

                                userRef.child(listUserId).addValueEventListener(object : ValueEventListener {
                                    override fun onDataChange(userSnapshot: DataSnapshot) {
                                        if (!isAdded) return
                                        val userName = userSnapshot.child("displayname").value.toString()
                                        val userStatus = userSnapshot.child("status").value.toString()

                                        context?.let {
                                            holder.userName.text = userName
                                            holder.userStatus.text = it.getString(R.string.req_sent, userName)
                                        }

                                        holder.itemView.setOnClickListener {
                                            if (!isAdded) return@setOnClickListener
                                            context?.let {
                                                val options = arrayOf("Cancel Chat Request")
                                                AlertDialog.Builder(it)
                                                    .setTitle("Already sent request")
                                                    .setItems(options) { _, which ->
                                                        if (which == 0) cancelRequest(listUserId)
                                                    }
                                                    .show()
                                            }
                                        }
                                    }

                                    override fun onCancelled(error: DatabaseError) {}
                                })
                            }
                        }
                    }

                    override fun onCancelled(error: DatabaseError) {}
                })
            }
        }

        myRequestsList.adapter = adapter
        adapter.startListening()
    }

    private fun acceptRequest(userId: String) {
        contactsRef.child(currentUserId).child(userId).child("Contacts")
            .setValue("saved").addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    contactsRef.child(userId).child(currentUserId).child("Contacts")
                        .setValue("saved").addOnCompleteListener {
                            chatRequestRef.child(currentUserId).child(userId)
                                .removeValue().addOnCompleteListener {
                                    chatRequestRef.child(userId).child(currentUserId)
                                        .removeValue().addOnCompleteListener {
                                            Toast.makeText(context, "New contact added", Toast.LENGTH_SHORT).show()
                                        }
                                }
                        }
                }
            }
    }

    private fun cancelRequest(userId: String) {
        chatRequestRef.child(currentUserId).child(userId)
            .removeValue().addOnCompleteListener {
                if (it.isSuccessful) {
                    chatRequestRef.child(userId).child(currentUserId)
                        .removeValue().addOnCompleteListener {
                            Toast.makeText(context, "Request cancelled", Toast.LENGTH_SHORT).show()
                        }
                }
            }
    }

    class RequestsViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val userName: TextView = itemView.findViewById(R.id.user_name_request)
        val userStatus: TextView = itemView.findViewById(R.id.user_status_request)
        val acceptChat: Button = itemView.findViewById(R.id.request_accept_button)
        val declineChat: Button = itemView.findViewById(R.id.request_decline_button)
    }
}