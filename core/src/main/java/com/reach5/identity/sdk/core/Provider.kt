package com.reach5.identity.sdk.core

interface Provider {
    val name: String
    fun version(): String
}
