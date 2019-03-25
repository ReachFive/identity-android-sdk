package com.reach5.identity.sdk.facebook

import com.reach5.identity.sdk.core.Provider

class FacebookProvider : Provider {
    companion object {
        const val NAME = "facebook"
    }

    override val name: String = NAME

    override fun version(): String {
        return "facebook 0.1.0"
    }
}
