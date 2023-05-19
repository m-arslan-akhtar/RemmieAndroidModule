package com.remmie.server

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import android.widget.Button
import com.remmie.server.databinding.ActivityMainBinding
import org.freedesktop.gstreamer.tutorials.tutorial_2.Tutorial2

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
//        setContentView(R.layout.activity_main)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val start_server = findViewById<Button>(R.id.start_server)
        start_server.setOnClickListener {
            startActivity(Intent(this, Tutorial2::class.java))
        }

    }
}