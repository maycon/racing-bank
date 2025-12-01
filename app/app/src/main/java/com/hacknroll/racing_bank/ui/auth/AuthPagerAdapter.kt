package com.hacknroll.racing_bank.ui.auth

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentPagerAdapter

class AuthPagerAdapter(fm: FragmentManager) : FragmentPagerAdapter(fm, BEHAVIOR_RESUME_ONLY_CURRENT_FRAGMENT) {
    
    private val fragments = listOf(
        LoginFragment(),
        RegisterFragment()
    )
    
    private val titles = listOf(
        "LOGIN",
        "REGISTER"
    )
    
    override fun getCount(): Int = fragments.size
    
    override fun getItem(position: Int): Fragment = fragments[position]
    
    override fun getPageTitle(position: Int): CharSequence = titles[position]
    
    fun getFragmentAt(position: Int): Fragment? {
        return if (position in fragments.indices) {
            fragments[position]
        } else {
            null
        }
    }
}