package ru.anb.passwordapp.features.ui.interest.ui.main

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import ru.anb.passwordapp.R
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import ru.anb.passwordapp.features.ui.interest.InterestsAdapter
import ru.anb.passwordapp.features.ui.interest.model.Interest

class PlaceholderFragment : Fragment() {

    private lateinit var root: View
    private lateinit var mRecyclerView: RecyclerView
    private lateinit var mAdapter: InterestsAdapter
    private lateinit var mLayoutManager: RecyclerView.LayoutManager
    private lateinit var pageViewModel: PageViewModel

    private val mAuth: FirebaseAuth = FirebaseAuth.getInstance()
    private val currentUser: String = mAuth.currentUser?.uid ?: ""
    private val interestsRef: DatabaseReference = FirebaseDatabase.getInstance("https://fribby-3e3f9-default-rtdb.europe-west1.firebasedatabase.app").getReference("Users").child(currentUser).child("interests")
    private val rootRef: DatabaseReference = FirebaseDatabase.getInstance("https://fribby-3e3f9-default-rtdb.europe-west1.firebasedatabase.app").getReference()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        pageViewModel = ViewModelProvider(this)[PageViewModel::class.java]

        val index = arguments?.getInt(ARG_SECTION_NUMBER) ?: 0

        val interestsTypes = listOf("home", "indoors", "outdoors", "education")
        val type = interestsTypes.getOrNull(index) ?: "home"

        val context = requireContext()
        val hm = hashMapOf<String, Any>("context" to context, "type" to type)

        pageViewModel.setmMap(hm)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        root = inflater.inflate(R.layout.fragment_interests, container, false)

        mRecyclerView = root.findViewById(R.id.interests_recyclerView)
        mRecyclerView.setHasFixedSize(true)
        mLayoutManager = LinearLayoutManager(context)

        pageViewModel.getmInterestList().observe(viewLifecycleOwner) { interests ->
            mAdapter = InterestsAdapter(interests)
            mAdapter.setOnItemClickListener(object : InterestsAdapter.OnItemClickListener {
                override fun onItemClick(position: Int) {
                    val clickedInterest = interests[position]
                    if (clickedInterest.mInterestClicked) {
                        addInterestToDatabase(clickedInterest)
                    } else {
                        removeInterestFromDatabase(clickedInterest)
                    }
                }
            })
            mAdapter.notifyDataSetChanged()
            mRecyclerView.layoutManager = mLayoutManager
            mRecyclerView.adapter = mAdapter

            val simpleCallback = object : ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT or ItemTouchHelper.RIGHT) {
                override fun onMove(
                    recyclerView: RecyclerView,
                    viewHolder: RecyclerView.ViewHolder,
                    target: RecyclerView.ViewHolder
                ): Boolean = false

                override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
                    val position = viewHolder.adapterPosition
                    when (direction) {
                        ItemTouchHelper.LEFT -> {
                            interests[position].mInterestClicked = false
                            mAdapter.notifyItemChanged(position)
                            removeInterestFromDatabase(interests[position])
                        }
                        ItemTouchHelper.RIGHT -> {
                            interests[position].mInterestClicked = true
                            mAdapter.notifyItemChanged(position)
                            addInterestToDatabase(interests[position])
                        }
                    }
                }
            }

            ItemTouchHelper(simpleCallback).attachToRecyclerView(mRecyclerView)


            mRecyclerView.recycledViewPool.setMaxRecycledViews(0, 0)
        }

        return root
    }

    private fun addInterestToDatabase(interest: Interest) {
        val interestsMap = hashMapOf(
            "name" to interest.mInterestName,
            "image" to interest.mImageLink,
            "type" to interest.mInterestType
        )

        interestsRef.child(interest.mInterestName.toString()).updateChildren(interestsMap as Map<String, Any>)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    Snackbar.make(root, "Interest Added", Snackbar.LENGTH_SHORT).show()
                    rootRef.child("Interests").child(interest.mInterestType.toString()).child(
                        interest.mInterestName.toString()
                    ).child(currentUser).setValue(currentUser)
                } else {
                    Snackbar.make(root, "Error: ${task.exception?.message}", Snackbar.LENGTH_SHORT).show()
                }
            }
    }

    private fun removeInterestFromDatabase(interest: Interest) {
        interestsRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (snapshot.hasChild(interest.mInterestName.toString())) {
                    interestsRef.child(interest.mInterestName.toString()).removeValue().addOnCompleteListener { task ->
                        if (task.isSuccessful) {
                            rootRef.child("Interests").child(interest.mInterestType.toString()).child(
                                interest.mInterestName.toString()
                            ).child(currentUser).removeValue()
                            Snackbar.make(root, "Interest Deselected", Snackbar.LENGTH_SHORT)
                                .setAction("UNDO") {
                                    addInterestToDatabase(interest)
                                    interest.mInterestClicked = true
                                    mAdapter.notifyDataSetChanged()
                                }.show()
                        }
                    }
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e("PlaceholderFragment", "Database error: ${error.message}")
            }
        })
    }

    companion object {
        private const val ARG_SECTION_NUMBER = "section_number"

        fun newInstance(index: Int): PlaceholderFragment {
            val fragment = PlaceholderFragment()
            fragment.arguments = Bundle().apply {
                putInt(ARG_SECTION_NUMBER, index)
            }
            return fragment
        }
    }
}
