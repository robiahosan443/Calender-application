package com.example.calenderapplication

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.calenderapplication.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {
    private var binding: ActivityMainBinding? = null


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding!!.root)


        binding!!.saveButton.setOnClickListener {
            var max = binding!!.edtMax.text.toString()
            var min = binding!!.edtMin.text.toString()
            if (max.isEmpty() && min.isEmpty()) {
                Toast.makeText(this, "Please Enter Minimum and Maximum Value", Toast.LENGTH_SHORT).show()
            } else {
                var intent = Intent(this, SecondActivity::class.java)
                intent.putExtra("max", max)
                intent.putExtra("min", min)
                startActivity(intent)
            }

        }


    }

}