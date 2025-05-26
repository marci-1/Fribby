package ru.anb.passwordapp.features.ui.home

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import ru.anb.passwordapp.R
import com.firebase.ui.database.FirebaseRecyclerAdapter
import com.firebase.ui.database.FirebaseRecyclerOptions
import com.google.firebase.database.*
import ru.anb.passwordapp.features.ui.interest.model.Interest

class ViewInterestsActivity : AppCompatActivity() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var receiverUserId: String
    private lateinit var interestsRef: DatabaseReference
    private lateinit var adapter: FirebaseRecyclerAdapter<Interest, InterestsViewHolder>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_view_interests)

        receiverUserId = intent.getStringExtra("visit_user_id").toString()

        recyclerView = findViewById(R.id.view_interests_recycler_view)
        recyclerView.layoutManager = LinearLayoutManager(this)

        interestsRef = FirebaseDatabase.getInstance("https://fribby-3e3f9-default-rtdb.europe-west1.firebasedatabase.app")
            .getReference("Users")
            .child(receiverUserId)
            .child("interests")
    }

    override fun onStart() {
        super.onStart()

        val options = FirebaseRecyclerOptions.Builder<Interest>()
            .setQuery(interestsRef, Interest::class.java)
            .build()

        adapter = object : FirebaseRecyclerAdapter<Interest, InterestsViewHolder>(options) {
            override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): InterestsViewHolder {
                val view = LayoutInflater.from(parent.context)
                    .inflate(R.layout.interest_cardview, parent, false)
                return InterestsViewHolder(view)
            }

            override fun onBindViewHolder(
                holder: InterestsViewHolder,
                position: Int,
                model: Interest
            ) {
                val hobbyKey = getRef(position).key ?: return

                interestsRef.child(hobbyKey).addListenerForSingleValueEvent(object : ValueEventListener {
                    override fun onDataChange(snapshot: DataSnapshot) {
                        if (snapshot.exists()) {
                            val hobbyImageUrl = snapshot.child("image").getValue(String::class.java)
                            val hobbyName = snapshot.child("name").getValue(String::class.java)

                            val interest = Interest(mInterestName = hobbyName)
                            holder.hobbyName.text = interest.getLocalizedName()

                            Glide.with(holder.itemView.context)
                                .load(hobbyImageUrl)
                                .centerCrop()
                                .placeholder(R.drawable.profile_icon)
                                .into(holder.hobbyImage)
                        }
                    }

                    override fun onCancelled(error: DatabaseError) {
                    }
                })
            }
        }

        recyclerView.adapter = adapter
        adapter.startListening()
    }

    class InterestsViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val hobbyName: TextView = itemView.findViewById(R.id.interest_name)
        val hobbyImage: ImageView = itemView.findViewById(R.id.interest_imageView)
        val imageClicked: ImageView = itemView.findViewById(R.id.interest_clicked)
    }
}