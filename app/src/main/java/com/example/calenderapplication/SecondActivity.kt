package com.example.calenderapplication

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment

class SecondActivity : AppCompatActivity() {
    private val homeFragment = Example4Fragment()
    var min: String? = null
    var max: String? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_second)
        min = intent.getStringExtra("min").toString()
        max = intent.getStringExtra("max").toString()
        replaceFragment()
    }

    private fun replaceFragment() {
        val bundle = Bundle()
        bundle.putString("max", max)
        bundle.putString("min", min)
        val transaction = supportFragmentManager.beginTransaction()
        homeFragment.arguments = bundle
        transaction.add(R.id.homeContainer, homeFragment)
        transaction.commit()
    }
}