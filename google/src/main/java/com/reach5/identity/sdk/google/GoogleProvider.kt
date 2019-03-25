package com.reach5.identity.sdk.google

import com.reach5.identity.sdk.core.Provider

class GoogleProvider : Provider {
    companion object {
        const val NAME = "google"
    }

    override val name: String = NAME

    override fun version(): String {
        return "$name 0.1.0"
    }
}