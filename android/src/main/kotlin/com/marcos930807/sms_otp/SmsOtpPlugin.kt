package com.marcos930807.sms_otp

import android.app.Activity
import android.content.Context
import android.content.Context.RECEIVER_EXPORTED
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.util.Log
import androidx.annotation.NonNull
import androidx.core.content.ContextCompat
import com.google.android.gms.auth.api.phone.SmsRetriever

import io.flutter.embedding.engine.plugins.FlutterPlugin
import io.flutter.embedding.engine.plugins.activity.ActivityAware
import io.flutter.embedding.engine.plugins.activity.ActivityPluginBinding
import io.flutter.plugin.common.MethodCall
import io.flutter.plugin.common.MethodChannel
import io.flutter.plugin.common.MethodChannel.MethodCallHandler
import io.flutter.plugin.common.MethodChannel.Result
import io.flutter.plugin.common.PluginRegistry
import io.flutter.plugin.common.PluginRegistry.Registrar

/** SmsOtpPlugin */
class SmsOtpPlugin : FlutterPlugin, MethodCallHandler, MySmsListener, ActivityAware {
    /// The MethodChannel that will the communication between Flutter and native Android
    ///
    /// This local reference serves to register the plugin with the Flutter Engine and unregister it
    /// when the Flutter Engine is detached from the Activity
    private lateinit var channel: MethodChannel
    private lateinit var context: Context
    private var activity: Activity? = null

    private var mResult: MethodChannel.Result? = null
    private var receiver: SmsBroadcastReceiver? = null
    private var alreadyCalledSmsRetrieve = false


    override fun onAttachedToEngine(@NonNull flutterPluginBinding: FlutterPlugin.FlutterPluginBinding) {
        channel = MethodChannel(flutterPluginBinding.binaryMessenger, "sms_otp")
        context = flutterPluginBinding.applicationContext
        channel.setMethodCallHandler(this)
    }

    override fun onMethodCall(@NonNull call: MethodCall, @NonNull result: Result) {
        when (call.method) {
            "getAppSignature" -> {
                val signature = AppSignatureHelper(context).getAppSignatures()[0]
                result.success(signature)
            }
            "startListening" -> {
                this.mResult = result
                receiver = SmsBroadcastReceiver()
                startListening()

            }
            "stopListening" -> {
                alreadyCalledSmsRetrieve = false
                unregister()
            }
            else -> result.notImplemented()
        }

    }

    override fun onDetachedFromEngine(@NonNull binding: FlutterPlugin.FlutterPluginBinding) {
        channel.setMethodCallHandler(null)
    }

    private fun startListening() {
        if (activity != null) {
            val client = SmsRetriever.getClient(this.activity!! /* context */)
            val task = client.startSmsRetriever()
            task.addOnSuccessListener {
                // Successfully started retriever, expect broadcast intent
                Log.e(javaClass::getSimpleName.name, "task started")
                receiver?.setSmsListener(this)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    this.activity!!.registerReceiver(receiver, IntentFilter(SmsRetriever.SMS_RETRIEVED_ACTION),RECEIVER_EXPORTED)
                }else {
                    this.activity!!.registerReceiver(receiver, IntentFilter(SmsRetriever.SMS_RETRIEVED_ACTION))
                }

            }
        }
    }

    private fun unregister() {
        if (activity != null) {
            this.activity!!.unregisterReceiver(receiver)
        }
    }

    override fun onOtpReceived(message: String?) {
        message?.let {
            if (!alreadyCalledSmsRetrieve) {
                mResult?.success(it)
                alreadyCalledSmsRetrieve = true
            }
        }
    }

    override fun onOtpTimeout() {
        Log.e(javaClass::getSimpleName.name, "time out")
    }

    override fun onAttachedToActivity(binding: ActivityPluginBinding) {
        this.activity = binding.activity
    }

    override fun onDetachedFromActivityForConfigChanges() {

    }

    override fun onReattachedToActivityForConfigChanges(binding: ActivityPluginBinding) {

    }

    override fun onDetachedFromActivity() {
        // unRegisterBroadcastReceivers()
        activity = null
    }

}
