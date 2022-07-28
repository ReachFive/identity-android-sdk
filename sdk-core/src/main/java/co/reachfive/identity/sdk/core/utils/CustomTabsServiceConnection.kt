package co.reachfive.identity.sdk.core.utils

import android.content.ComponentName
import androidx.browser.customtabs.CustomTabsClient
import androidx.browser.customtabs.CustomTabsServiceConnection

internal class R5CustomTabsServiceConnection: CustomTabsServiceConnection() {

    private lateinit var client: CustomTabsClient

    fun warmup(): Boolean = client.warmup(0L)

    override fun onCustomTabsServiceConnected(name: ComponentName, client: CustomTabsClient) {
        this.client = client
    }

    override fun onServiceDisconnected(name: ComponentName?) {
        // do nothing
    }
}