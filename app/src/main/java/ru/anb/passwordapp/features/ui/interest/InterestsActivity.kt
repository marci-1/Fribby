package ru.anb.passwordapp.features.ui.interest

import android.content.Intent
import android.os.Bundle
import androidx.annotation.StringRes
import androidx.appcompat.app.AppCompatActivity
import androidx.viewpager2.widget.ViewPager2
import ru.anb.passwordapp.R
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator
import ru.anb.passwordapp.features.ui.home.HomeActivity
import ru.anb.passwordapp.features.ui.interest.ui.main.SectionsPagerAdapter

class InterestsActivity : AppCompatActivity() {

    companion object {
        @StringRes
        private val TAB_TITLES = intArrayOf(
            R.string.tab_text_1,
            R.string.tab_text_2,
            R.string.tab_text_3,
            R.string.tab_text_4
        )
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_interests)

        val sectionsPagerAdapter = SectionsPagerAdapter(supportFragmentManager, lifecycle)
        val viewPager: ViewPager2 = findViewById(R.id.view_pager)
        viewPager.adapter = sectionsPagerAdapter
        viewPager.offscreenPageLimit = 3
        viewPager.isUserInputEnabled = false

        val tabs: TabLayout = findViewById(R.id.tabs)
        TabLayoutMediator(tabs, viewPager) { tab, position ->
            tab.text = getString(TAB_TITLES[position])
        }.attach()

        val fab: FloatingActionButton = findViewById(R.id.fab)
        fab.setOnClickListener {
            sendUserToMainActivity()
        }
    }

    private fun sendUserToMainActivity() {
        val intent = Intent(this, HomeActivity::class.java)
        startActivity(intent)
    }
}
