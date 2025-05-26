package ru.anb.passwordapp.features.ui.interest

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import ru.anb.passwordapp.R
import com.firebase.ui.database.FirebaseRecyclerAdapter
import com.firebase.ui.database.FirebaseRecyclerOptions
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import ru.anb.passwordapp.features.ui.interest.model.Interest

class SelectedInterestsFragment : Fragment() {

    private lateinit var root: View
    private lateinit var mRecyclerView: RecyclerView

    private val mAuth: FirebaseAuth by lazy { FirebaseAuth.getInstance() }
    private val currentUser: String by lazy { mAuth.currentUser?.uid ?: "" }
    private val interestsRef: DatabaseReference by lazy {
        FirebaseDatabase.getInstance("https://fribby-3e3f9-default-rtdb.europe-west1.firebasedatabase.app").reference.child("Users").child(currentUser).child("interests")
    }

    private lateinit var adapter: FirebaseRecyclerAdapter<Interest, InterestsViewHolder>

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        root = inflater.inflate(R.layout.fragment_selected_interests, container, false)
        mRecyclerView = root.findViewById(R.id.interests_selected_recyclerView)
        mRecyclerView.layoutManager = LinearLayoutManager(context)
        return root
    }

    override fun onStart() {
        super.onStart()

        val options = FirebaseRecyclerOptions.Builder<Interest>()
            .setQuery(interestsRef, Interest::class.java)
            .build()

        adapter = object : FirebaseRecyclerAdapter<Interest, InterestsViewHolder>(options) {
            override fun onBindViewHolder(
                holder: InterestsViewHolder,
                position: Int,
                model: Interest
            ) {
                val hobbyNameKey = getRef(position).key ?: return

                interestsRef.child(hobbyNameKey).addValueEventListener(object : ValueEventListener {
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

                            holder.imageClicked.visibility = View.VISIBLE
                            holder.deleteButton.visibility = View.VISIBLE
                        }
                    }

                    override fun onCancelled(error: DatabaseError) {}
                })

                holder.deleteButton.setOnClickListener {
                    interestsRef.child(hobbyNameKey).removeValue()
                        .addOnSuccessListener {
                            Toast.makeText(
                                holder.itemView.context,
                                "Интерес удалён",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                        .addOnFailureListener {
                            Toast.makeText(
                                holder.itemView.context,
                                "Ошибка при удалении",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                }
            }
            override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): InterestsViewHolder {
                val view = LayoutInflater.from(parent.context)
                    .inflate(R.layout.interest_cardview, parent, false)
                return InterestsViewHolder(view)
            }
        }

        mRecyclerView.adapter = adapter
        adapter.startListening()
    }

    override fun onStop() {
        super.onStop()
        if (::adapter.isInitialized) {
            adapter.stopListening()
        }
    }

    class InterestsViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val hobbyName: TextView = itemView.findViewById(R.id.interest_name)
        val hobbyImage: ImageView = itemView.findViewById(R.id.interest_imageView)
        val imageClicked: ImageView = itemView.findViewById(R.id.interest_clicked)
        val deleteButton: ImageView = itemView.findViewById(R.id.interest_delete)
    }
}