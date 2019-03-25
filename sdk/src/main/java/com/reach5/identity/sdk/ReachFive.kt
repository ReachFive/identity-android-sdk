package com.reach5.identity.sdk

import com.reach5.identity.sdk.core.Provider

class ReachFive constructor(val providers: List<Provider>) {

    fun getByName(name: String): Provider? {
        return this.providers.find { p -> p.name == name }
    }

}
