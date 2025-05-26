package ru.anb.passwordapp.features.ui.interest.ui.main

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.Lifecycle
import androidx.viewpager2.adapter.FragmentStateAdapter
import ru.anb.passwordapp.features.ui.interest.SelectedInterestsFragment

class SectionsPagerAdapter(fragmentManager: FragmentManager, lifecycle: Lifecycle) :
    FragmentStateAdapter(fragmentManager, lifecycle) {

    override fun createFragment(position: Int): Fragment {
        return if (position == 0) {
            SelectedInterestsFragment()
        } else {
            PlaceholderFragment.newInstance(position)
        }
    }

    override fun getItemCount(): Int = 4
}