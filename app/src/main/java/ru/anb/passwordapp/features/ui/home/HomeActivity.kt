package ru.anb.passwordapp.features.ui.home

import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.fragment.app.Fragment
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.database.*
import ru.anb.passwordapp.R
import ru.anb.passwordapp.core.ui.MainActivity
import ru.anb.passwordapp.features.ui.home.ChatsFragment
import ru.anb.passwordapp.features.ui.interest.InterestsActivity
import java.text.SimpleDateFormat
import java.util.*

class HomeActivity : AppCompatActivity() {

    private lateinit var mToolbar: Toolbar
    private var selectedFragment: Fragment? = null
    private lateinit var mAuth: FirebaseAuth
    private lateinit var rootRef: DatabaseReference
    private lateinit var userRef: DatabaseReference
    private lateinit var groupRef: DatabaseReference
    private lateinit var currentUserId: String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.fragment_home)

        mAuth = FirebaseAuth.getInstance()
        currentUserId = mAuth.currentUser?.uid ?: ""

        rootRef = FirebaseDatabase.getInstance("https://fribby-3e3f9-default-rtdb.europe-west1.firebasedatabase.app").reference
        userRef = rootRef.child("Users")
        groupRef = rootRef.child("Groups")

        mToolbar = findViewById(R.id.main_page_toolbar)
        setSupportActionBar(mToolbar)
        supportActionBar?.title = getString(R.string.app_name)
        supportActionBar?.setDisplayShowTitleEnabled(true)
        mToolbar.showOverflowMenu()

        val bottomNav: BottomNavigationView = findViewById(R.id.bottom_navigation_view)
        bottomNav.setOnItemSelectedListener(navListener)

        supportFragmentManager.beginTransaction().replace(
            R.id.fragment_container,
            SearchFragment()
        ).commit()
    }

    private val navListener = { item: MenuItem ->
        selectedFragment = when (item.itemId) {
            R.id.bottom_nav_home -> ChatsFragment()
            R.id.bottom_nav_search_penpal -> SearchFragment()
            R.id.bottom_nav_profile -> RequestsFragment()
            R.id.bottom_nav_requests -> SettingsFragment()
            else -> null
        }

        selectedFragment?.let {
            supportFragmentManager.beginTransaction().replace(
                R.id.fragment_container, it
            ).commit()
        }
        true
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.overflow_menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.overflow_find_friends -> {
                Toast.makeText(this, "Find Friends", Toast.LENGTH_SHORT).show()
                sendUserToFindActivity()
                true
            }
            R.id.overflow_logout -> {
                Toast.makeText(this, "Signing Out...", Toast.LENGTH_SHORT).show()
                updateUserStatus("offline")
                mAuth.signOut()
                FirebaseAuth.getInstance().signOut()
                sendUserToWelcomeActivity()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    override fun onStart() {
        super.onStart()
        val currentUser = mAuth.currentUser
        if (currentUser == null) {
            sendUserToLoginActivity()
        } else {
            updateUserStatus("online")
            checkUserExistenceInDatabase()
        }
    }

    override fun onStop() {
        super.onStop()
        mAuth.currentUser?.let {
            updateUserStatus("offline")
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        mAuth.currentUser?.let {
            updateUserStatus("offline")
        }
    }

    private fun checkUserExistenceInDatabase() {
        val userId = mAuth.currentUser?.uid ?: return
        userRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                when {
                    !snapshot.child(userId).hasChild("displayname") -> {
                        updateUserStatus("online")
                    }
                    !snapshot.child(userId).hasChild("interests") -> {
                        updateUserStatus("online")
                        sendUserToInterestsActivity()
                    }
                }
            }

            override fun onCancelled(error: DatabaseError) {}
        })
    }

    private fun sendUserToSetupActivity() {
        val intent = Intent(this, SetupActivity::class.java)
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
        startActivity(intent)
        finish()
    }

    private fun sendUserToLoginActivity() {
        val intent = Intent(this, MainActivity::class.java)
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
        startActivity(intent)
        finish()
    }

    private fun sendUserToWelcomeActivity() {
        val intent = Intent(this, MainActivity::class.java)
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
        startActivity(intent)
        finish()
    }

    private fun sendUserToSettingsActivity() {
        val mainIntent = Intent(this, SettingsActivity::class.java)
        mainIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
        startActivity(mainIntent)
    }

    private fun sendUserToInterestsActivity() {
        val intent = Intent(this, InterestsActivity::class.java)
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
        startActivity(intent)
    }

    private fun sendUserToFindActivity() {
        val intent = Intent(this, FindFriendsActivity::class.java)
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
        startActivity(intent)
    }

    private fun updateUserStatus(state: String) {
        val calendar = Calendar.getInstance()
        val currentDate = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()).format(calendar.time)
        val currentTime = SimpleDateFormat("hh:mm a", Locale.getDefault()).format(calendar.time)

        val onlineStateMap = hashMapOf<String, Any>(
            "time" to currentTime,
            "date" to currentDate,
            "state" to state
        )

        rootRef.child("Users").child(currentUserId).child("userState")
            .updateChildren(onlineStateMap)
    }
}
