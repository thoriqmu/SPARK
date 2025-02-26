package com.spark.edtech

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.spark.edtech.databinding.ActivityMainBinding
import com.spark.edtech.view.ChatFragment
import com.spark.edtech.view.HomeFragment
import com.spark.edtech.view.NotificationFragment
import com.spark.edtech.view.ProfileFragment

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        loadFragment(HomeFragment())

        binding.bottomNavigationView.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.homeFragment -> loadFragment(HomeFragment())
                R.id.chatFragment -> loadFragment(ChatFragment())
                R.id.notificationFragment -> loadFragment(NotificationFragment())
                R.id.profileFragment -> loadFragment(ProfileFragment())
                else -> throw IllegalArgumentException("Invalid item ID")
            }
            true
        }
    }

    private fun loadFragment(fragment: Fragment) {
        val fragmentManager = supportFragmentManager
        val transaction = fragmentManager.beginTransaction()
        transaction.replace(R.id.nav_host_fragment, fragment)
        transaction.commit()
    }
}