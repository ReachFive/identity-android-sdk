package com.reach5.identity.sdk.demo

import kotlinx.android.synthetic.main.activity_main.*

import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import com.reach5.identity.sdk.ReachFive
import com.reach5.identity.sdk.facebook.FacebookProvider
import com.reach5.identity.sdk.google.GoogleProvider

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val reach5 = ReachFive(listOf(FacebookProvider(), GoogleProvider()))

        helloWorld.text = reach5.providers.map { p -> p.version() }.joinToString(", ")
    }
}
