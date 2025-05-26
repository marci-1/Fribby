package ru.anb.passwordapp.features.ui.home

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import ru.anb.passwordapp.R
import kotlin.random.Random

class SearchFragment : Fragment() {

    private lateinit var root: View
    private lateinit var findButton: Button

    private lateinit var rootRef: DatabaseReference
    private val mAuth: FirebaseAuth = FirebaseAuth.getInstance()
    private val currentUserId: String? = mAuth.uid

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        root = inflater.inflate(R.layout.fragment_search, container, false)

        rootRef = FirebaseDatabase.getInstance("https://fribby-3e3f9-default-rtdb.europe-west1.firebasedatabase.app").reference

        findButton = root.findViewById(R.id.find_penpal_button)
        findButton.setOnClickListener {
            chooseRandomInterest()
        }

        return root
    }

    private fun safeToast(message: String) {
        if (isAdded) {
            Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
        }
    }

    private fun chooseRandomInterest() {
        val uid = currentUserId ?: return

        rootRef.child("Users").child(uid).child("interests")
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    if (!isAdded) return
                    if (!snapshot.exists()) {
                        safeToast("Нет интересов для поиска")
                        return
                    }

                    val interestList = snapshot.children.mapNotNull { it }
                    tryFindUserFromInterests(interestList)
                }

                override fun onCancelled(error: DatabaseError) {}
            })
    }

    private fun sendChatRequestsToAll(users: MutableList<String>) {
        if (!isAdded) return
        if (users.isEmpty()) {
            safeToast("Все подходящие пользователи уже в контактах!")
            return
        }

        val userId = users.removeAt(Random.nextInt(users.size))
        checkIfRandomUserInContactList(userId) {
            sendChatRequest(userId)
            sendChatRequestsToAll(users)
        }
    }

    private fun checkIfRandomUserInContactList(userID: String, onCheckDone: () -> Unit) {
        val uid = currentUserId ?: return

        rootRef.child("Contacts").child(uid).child(userID)
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    if (!isAdded) return
                    if (!snapshot.exists()) {
                        onCheckDone()
                    } else {
                        sendChatRequestsToAll(mutableListOf()) // skip
                    }
                }

                override fun onCancelled(error: DatabaseError) {}
            })
    }

    private fun tryFindUserFromInterests(interestList: List<DataSnapshot>, index: Int = 0) {
        val uid = currentUserId ?: return
        if (!isAdded) return

        if (index >= interestList.size) {
            safeToast(getString(R.string.cant_find_friend))
            return
        }

        val interestSnapshot = interestList[index]
        val type = interestSnapshot.child("type").getValue(String::class.java) ?: return
        val name = interestSnapshot.child("name").getValue(String::class.java) ?: return

        rootRef.child("Interests").child(type).child(name)
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    if (!isAdded) return

                    val users = snapshot.children.toList().filter { it.key != uid }
                    if (users.isEmpty()) {
                        tryFindUserFromInterests(interestList, index + 1)
                        return
                    }

                    rootRef.child("Chat Requests").child(uid)
                        .addListenerForSingleValueEvent(object : ValueEventListener {
                            override fun onDataChange(requestsSnapshot: DataSnapshot) {
                                if (!isAdded) return

                                val sentUsers = requestsSnapshot.children
                                    .filter { it.child("request_type").getValue(String::class.java) == "sent" }
                                    .mapNotNull { it.key }

                                val filteredUsers = users.filter { it.key !in sentUsers }

                                if (filteredUsers.isEmpty()) {
                                    tryFindUserFromInterests(interestList, index + 1)
                                } else {
                                    val randomUser = filteredUsers.random()
                                    rootRef.child("Contacts").child(uid).child(randomUser.key!!)
                                        .addListenerForSingleValueEvent(object : ValueEventListener {
                                            override fun onDataChange(contactSnapshot: DataSnapshot) {
                                                if (!isAdded) return

                                                if (!contactSnapshot.exists()) {
                                                    sendChatRequest(randomUser.key!!)
                                                } else {
                                                    tryFindUserFromInterests(interestList, index + 1)
                                                }
                                            }

                                            override fun onCancelled(error: DatabaseError) {}
                                        })
                                }
                            }

                            override fun onCancelled(error: DatabaseError) {}
                        })
                }

                override fun onCancelled(error: DatabaseError) {}
            })
    }

    private fun sendChatRequest(receiverUserId: String) {
        val senderUserId = currentUserId ?: return

        rootRef.child("Chat Requests").child(senderUserId).child(receiverUserId)
            .child("request_type").setValue("sent")
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    rootRef.child("Chat Requests").child(receiverUserId).child(senderUserId)
                        .child("request_type").setValue("received")
                        .addOnCompleteListener { innerTask ->
                            if (innerTask.isSuccessful) {
                                rootRef.child("Users").child(receiverUserId).child("displayname")
                                    .addListenerForSingleValueEvent(object : ValueEventListener {
                                        override fun onDataChange(snapshot: DataSnapshot) {
                                            if (!isAdded) return
                                            snapshot.getValue(String::class.java)?.let { displayName ->
                                                safeToast("Чат-запрос отправлен пользователю $displayName")
                                            }
                                        }

                                        override fun onCancelled(error: DatabaseError) {}
                                    })
                            }
                        }
                }
            }
    }
}
