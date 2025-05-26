package ru.anb.passwordapp.features.ui.home

import android.content.Intent
import android.os.Bundle
import android.provider.ContactsContract
import android.view.*
import android.widget.EditText
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import ru.anb.passwordapp.R
import com.firebase.ui.database.FirebaseRecyclerAdapter
import com.firebase.ui.database.FirebaseRecyclerOptions
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase

class FindFriendsActivity : AppCompatActivity() {
    private lateinit var mAuth: FirebaseAuth
    private var currentUserId: String = ""
    private lateinit var mToolbar: Toolbar
    private lateinit var findFriendsRecyclerList: RecyclerView
    private lateinit var userRef: DatabaseReference

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_find_friends)
        mAuth = FirebaseAuth.getInstance()

        currentUserId = mAuth.currentUser?.uid ?: ""
        userRef = FirebaseDatabase.getInstance("https://fribby-3e3f9-default-rtdb.europe-west1.firebasedatabase.app").reference.child("Users")

        findFriendsRecyclerList = findViewById(R.id.find_friends_recycler_list)
        findFriendsRecyclerList.layoutManager = LinearLayoutManager(this)

        mToolbar = findViewById(R.id.find_friends_toolbar)
        setSupportActionBar(mToolbar)
        supportActionBar?.apply {
            setDisplayHomeAsUpEnabled(true)
            setDisplayShowHomeEnabled(true)
            title = "Find Friends"
        }
    }

    override fun onStart() {
        super.onStart()

        val options = FirebaseRecyclerOptions.Builder<Contacts>()
            .setQuery(userRef, Contacts::class.java)
            .build()

        val adapter = object : FirebaseRecyclerAdapter<Contacts, FindFriendsViewHolder>(options) {
            override fun onBindViewHolder(
                holder: FindFriendsViewHolder,
                position: Int,
                model: Contacts
            ) {
                holder.userName.text = model.displayname
                holder.userStatus.text = model.status

                holder.itemView.setOnClickListener {
                    val visitUserId = getRef(position).key
                    val profileIntent = Intent(this@FindFriendsActivity, ProfileActivity::class.java)
                    profileIntent.putExtra("visit_user_id", visitUserId)
                    startActivity(profileIntent)
                }
            }

            override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FindFriendsViewHolder {
                val view = LayoutInflater.from(parent.context)
                    .inflate(R.layout.users_display_layout, parent, false)
                return FindFriendsViewHolder(view)
            }
        }

        findFriendsRecyclerList.adapter = adapter
        adapter.startListening()
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
    private fun sendUserToMainActivity() {
        val intent = Intent(this, HomeActivity::class.java)
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_NEW_TASK)
        startActivity(intent)
        finish()
    }
    class FindFriendsViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val userName: TextView = itemView.findViewById(R.id.user_name)
        val userStatus: TextView = itemView.findViewById(R.id.user_status)
    }
}