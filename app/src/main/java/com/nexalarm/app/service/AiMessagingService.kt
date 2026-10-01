package com.nexalarm.app.service

import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.nexalarm.app.data.AiDeviceRepository

class AiMessagingService : FirebaseMessagingService() {
    override fun onMessageReceived(message: RemoteMessage) {
        if (message.data["type"] == "alarm_sync") AiDeviceRepository.enqueue(applicationContext)
    }

    override fun onNewToken(token: String) {
        getSharedPreferences("ai_device", MODE_PRIVATE).edit().putString("fcm_token", token).apply()
        AiDeviceRepository.enqueue(applicationContext)
    }
}
