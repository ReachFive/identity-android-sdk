package com.reach5.identity.sdk.demo

import kotlinx.android.synthetic.main.activity_main.*

import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import com.reach5.identity.sdk.ReachFive
import com.reach5.identity.sdk.facebook.FacebookProvider

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)


        val reach5 = ReachFive(listOf(FacebookProvider()))

        val facebookProvider = reach5.getByName(FacebookProvider.NAME)
        if (facebookProvider != null) {
            helloWorld.text = facebookProvider.version()
        }
    }
}
