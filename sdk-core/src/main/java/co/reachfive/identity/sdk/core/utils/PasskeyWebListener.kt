package co.reachfive.identity.sdk.core.utils

import android.annotation.SuppressLint
import android.app.Activity
import android.net.Uri
import android.util.Log
import android.webkit.WebView
import androidx.annotation.UiThread
import androidx.credentials.CreatePublicKeyCredentialRequest
import androidx.credentials.CreatePublicKeyCredentialResponse
import androidx.credentials.CredentialManager
import androidx.credentials.GetCredentialRequest
import androidx.credentials.GetPublicKeyCredentialOption
import androidx.credentials.PublicKeyCredential
import androidx.credentials.exceptions.CreateCredentialException
import androidx.credentials.exceptions.GetCredentialException
import androidx.webkit.JavaScriptReplyProxy
import androidx.webkit.WebMessageCompat
import androidx.webkit.WebViewCompat
import co.reachfive.identity.sdk.core.ReachFive.Companion.TAG
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject

// See https://developer.android.com/training/sign-in/credential-manager-webview

class PasskeyWebListener(
    private val originWebAuthn: String,
    private val activity: Activity,
    private val coroutineScope: CoroutineScope,
) : WebViewCompat.WebMessageListener {
    private val cm: CredentialManager = CredentialManager.create(activity)

    private var havePendingRequest: Boolean = false
    private var pendingRequestIsDoomed: Boolean = false
    private var replyChannel: ReplyChannel? = null

    fun onPageStarted() {
        if (havePendingRequest) {
            pendingRequestIsDoomed = true
        }
    }

    @UiThread
    override fun onPostMessage(
        view: WebView,
        message: WebMessageCompat,
        sourceOrigin: Uri,
        isMainFrame: Boolean,
        replyProxy: JavaScriptReplyProxy
    ) {
        val messageData = message.data ?: return
        onRequest(messageData, sourceOrigin, isMainFrame, JavaScriptReplyChannel(replyProxy))
    }

    private fun onRequest(
        msg: String,
        sourceOrigin: Uri,
        isMainFrame: Boolean,
        reply: ReplyChannel
    ) {
        val jsonObj = JSONObject(msg)
        val type = jsonObj.getString(TYPE_KEY)
        val message = jsonObj.getString(REQUEST_KEY)

        if (havePendingRequest) {
            postErrorMessage(reply, "The request already in progress", type)
            return
        }

        replyChannel = reply
        if (!isMainFrame) {
            reportFailure("Requests form subframes are not supported", type)
            return
        }

        val originScheme = sourceOrigin.scheme
        if (originScheme == null || originScheme.lowercase() != "https") {
            reportFailure("WebAuthn not permitted for current URL", type)
            return
        }

        if (!isTrustedOrigin(sourceOrigin)) {
            reportFailure("URL incompatible with WebAuthn configuration", type)
            return
        }

        havePendingRequest = true
        pendingRequestIsDoomed = false

        val replyCurrent = replyChannel
        if (replyCurrent == null) {
            Log.i(TAG, "The reply channel was null, cannot continue")
            return
        }

        when (type) {
            CREATE_UNIQUE_KEY ->
                this.coroutineScope.launch {
                    handleCreateFlow(message, replyCurrent)
                }

            GET_UNIQUE_KEY ->
                this.coroutineScope.launch {
                    handleGetFlow(message, replyCurrent)
                }

            else -> Log.i(TAG, "Incorrect request json")
        }
    }

    // Allowed origins are prefixes of the RP id
    private fun isTrustedOrigin(originSource: Uri): Boolean {
        val originIsSuffixToRpId = Uri.parse(originWebAuthn)?.host?.let{
            originSource.host?.endsWith(it)
        } ?: false

        return originIsSuffixToRpId
    }

    private suspend fun handleCreateFlow(
        message: String,
        reply: ReplyChannel,
    ) {
        try {
            havePendingRequest = false
            pendingRequestIsDoomed = false

            val createRequest = CreatePublicKeyCredentialRequest(message)

            val response =
                cm.createCredential(activity, createRequest) as CreatePublicKeyCredentialResponse

            val successArray = ArrayList<Any>()
            successArray.add("success")
            successArray.add(JSONObject(response.registrationResponseJson))
            successArray.add(CREATE_UNIQUE_KEY)
            reply.send(JSONArray(successArray).toString())
            replyChannel = null
        } catch (e: CreateCredentialException) {
            reportFailure(
                "Error: ${e.errorMessage} w type: ${e.type} w obj: $e",
                CREATE_UNIQUE_KEY
            )
        } catch (t: Throwable) {
            reportFailure("Error: ${t.message}", CREATE_UNIQUE_KEY)
        }
    }

    private suspend fun handleGetFlow(
        message: String,
        reply: ReplyChannel
    ) {
        try {
            havePendingRequest = true
            pendingRequestIsDoomed = false

            val getRequest =
                GetCredentialRequest(listOf(GetPublicKeyCredentialOption(message, null)))

            val response = cm.getCredential(activity, getRequest)
            val publicKeyCredentialResponse = response.credential as PublicKeyCredential

            val successArray = ArrayList<Any>()
            successArray.add("success")
            successArray.add(JSONObject(publicKeyCredentialResponse.authenticationResponseJson))
            successArray.add(GET_UNIQUE_KEY)
            reply.send(JSONArray(successArray).toString())
            replyChannel = null
        } catch (e: GetCredentialException) {
            // For error handling use guidance from https://developer.android.com/training/sign-in/passkeys
            Log.i(TAG, "Error retrieving credential for WebView: ${e.errorMessage} w type: ${e.type} w obj: $e")
            reportFailure(
                "Error: ${e.errorMessage} w type: ${e.type} w obj: $e",
                GET_UNIQUE_KEY
            )
        } catch (t: Throwable) {
            Log.i(TAG, "Error retrieving credential for WebView: ${t.message}")
            reportFailure("Error: ${t.message}", GET_UNIQUE_KEY)
        }
    }

    private fun reportFailure(message: String, type: String) {
        havePendingRequest = false
        pendingRequestIsDoomed = false
        val reply: ReplyChannel = replyChannel!!
        replyChannel = null
        postErrorMessage(reply, message, type)
    }

    private fun postErrorMessage(reply: ReplyChannel, errorMessage: String, type: String) {
        Log.i(TAG, "Credential Manager interface for WebView (request:$type): $errorMessage")
        val array: MutableList<Any?> = ArrayList()
        array.add("error")
        array.add(errorMessage)
        array.add("type")
        reply.send(JSONArray(array).toString())
    }

    private class JavaScriptReplyChannel(private val reply: JavaScriptReplyProxy) :
        ReplyChannel {
        @SuppressLint("RequiresFeature") // Ensured in the redirection activity
        override fun send(message: String?) {
            try {
                reply.postMessage(message!!)
            } catch (t: Throwable) {
                Log.i(TAG, "Reply failure due to: " + t.message)
            }
        }
    }


    interface ReplyChannel {
        fun send(message: String?)
    }

    companion object {
        const val INTERFACE_NAME = "__webauthn_interface__"
        const val TYPE_KEY = "type"
        const val REQUEST_KEY = "request"
        const val CREATE_UNIQUE_KEY = "create"
        const val GET_UNIQUE_KEY = "get"

        // encode.js processed with uglifyjs
        const val INJECTED_VAL = """
            var __webauthn_interface__;var __webauthn_hooks__;(function(__webauthn_hooks__){__webauthn_interface__.addEventListener("message",onReply);var pendingResolveGet=null;var pendingResolveCreate=null;var pendingRejectGet=null;var pendingRejectCreate=null;function create(request){if(!("publicKey"in request)){return __webauthn_hooks__.originalCreateFunction(request)}var ret=new Promise(function(resolve,reject){pendingResolveCreate=resolve;pendingRejectCreate=reject});var temppk=request.publicKey;if(temppk.hasOwnProperty("challenge")){var str=CM_base64url_encode(temppk.challenge);temppk.challenge=str}if(temppk.hasOwnProperty("user")&&temppk.user.hasOwnProperty("id")){var encodedString=CM_base64url_encode(temppk.user.id);temppk.user.id=encodedString}if(temppk.hasOwnProperty("excludeCredentials")&&Array.isArray(temppk.excludeCredentials)){var stringIds=[];temppk.excludeCredentials.forEach(function(arrayId){var str=CM_base64url_encode(arrayId.id);stringIds.push({id:str,type:"public-key"})});temppk.excludeCredentials=stringIds}var jsonObj={type:"create",request:temppk};var json=JSON.stringify(jsonObj);__webauthn_interface__.postMessage(json);return ret}__webauthn_hooks__.create=create;function get(request){if(!("publicKey"in request)){return __webauthn_hooks__.originalGetFunction(request)}var ret=new Promise(function(resolve,reject){pendingResolveGet=resolve;pendingRejectGet=reject});var temppk=request.publicKey;if(temppk.hasOwnProperty("challenge")){var str=CM_base64url_encode(temppk.challenge);temppk.challenge=str}if(temppk.hasOwnProperty("allowCredentials")&&Array.isArray(temppk.allowCredentials)){var stringIds=[];temppk.allowCredentials.forEach(function(arrayId){var str=CM_base64url_encode(arrayId.id);stringIds.push({id:str,type:"public-key"})});temppk.allowCredentials=stringIds}var jsonObj={type:"get",request:temppk};var json=JSON.stringify(jsonObj);__webauthn_interface__.postMessage(json);return ret}__webauthn_hooks__.get=get;function onReply(msg){var reply=JSON.parse(msg.data);var type=reply[2];if(type==="get"){onReplyGet(reply)}else if(type==="create"){onReplyCreate(reply)}else{console.log("Incorrect response format for reply")}}function onReplyGet(reply){if(pendingResolveGet===null||pendingRejectGet===null){console.log("Reply failure: Resolve: "+pendingResolveCreate+" and reject: "+pendingRejectCreate);return}if(reply[0]!="success"){var reject=pendingRejectGet;pendingResolveGet=null;pendingRejectGet=null;reject(new DOMException(reply[1],"NotAllowedError"));return}var cred=credentialManagerDecode(reply[1]);var resolve=pendingResolveGet;pendingResolveGet=null;pendingRejectGet=null;resolve(cred)}__webauthn_hooks__.onReplyGet=onReplyGet;function CM_base64url_decode(value){var m=value.length%4;return Uint8Array.from(atob(value.replace(/-/g,"+").replace(/_/g,"/").padEnd(value.length+(m===0?0:4-m),"=")),function(c){return c.charCodeAt(0)}).buffer}__webauthn_hooks__.CM_base64url_decode=CM_base64url_decode;function CM_base64url_encode(buffer){return btoa(Array.from(new Uint8Array(buffer),function(b){return String.fromCharCode(b)}).join("")).replace(/\+/g,"-").replace(/\//g,"_").replace(/=+${'$'}/,"")}__webauthn_hooks__.CM_base64url_encode=CM_base64url_encode;function onReplyCreate(reply){if(pendingResolveCreate===null||pendingRejectCreate===null){console.log("Reply failure: Resolve: "+pendingResolveCreate+" and reject: "+pendingRejectCreate);return}if(reply[0]!="success"){var reject=pendingRejectCreate;pendingResolveCreate=null;pendingRejectCreate=null;reject(new DOMException(reply[1],"NotAllowedError"));return}var cred=credentialManagerDecode(reply[1]);var resolve=pendingResolveCreate;pendingResolveCreate=null;pendingRejectCreate=null;resolve(cred)}__webauthn_hooks__.onReplyCreate=onReplyCreate;function credentialManagerDecode(decoded_reply){decoded_reply.rawId=CM_base64url_decode(decoded_reply.rawId);decoded_reply.response.clientDataJSON=CM_base64url_decode(decoded_reply.response.clientDataJSON);if(decoded_reply.response.hasOwnProperty("attestationObject")){decoded_reply.response.attestationObject=CM_base64url_decode(decoded_reply.response.attestationObject)}if(decoded_reply.response.hasOwnProperty("authenticatorData")){decoded_reply.response.authenticatorData=CM_base64url_decode(decoded_reply.response.authenticatorData)}if(decoded_reply.response.hasOwnProperty("signature")){decoded_reply.response.signature=CM_base64url_decode(decoded_reply.response.signature)}if(decoded_reply.response.hasOwnProperty("userHandle")){decoded_reply.response.userHandle=CM_base64url_decode(decoded_reply.response.userHandle)}decoded_reply.getClientExtensionResults=function getClientExtensionResults(){return{}};return decoded_reply}})(__webauthn_hooks__||(__webauthn_hooks__={}));__webauthn_hooks__.originalGetFunction=navigator.credentials.get;__webauthn_hooks__.originalCreateFunction=navigator.credentials.create;navigator.credentials.get=__webauthn_hooks__.get;navigator.credentials.create=__webauthn_hooks__.create;window.PublicKeyCredential=function(){};window.PublicKeyCredential.isUserVerifyingPlatformAuthenticatorAvailable=function(){return Promise.resolve(false)};
        """
    }
}