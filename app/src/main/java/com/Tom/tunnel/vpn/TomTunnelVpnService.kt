package com.tom.tunnel.vpn

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Intent
import android.net.VpnService
import android.os.Build
import android.os.ParcelFileDescriptor

/**
 * Security-first shell: establishes no fake "connected" state.
 * The Xray Android native engine and a complete TUN-to-Xray packet bridge
 * must be integrated before this service can route traffic.
 */
class TomTunnelVpnService : VpnService() {
    private var tun: ParcelFileDescriptor? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == "STOP") stopTunnel()
        else if (intent?.action == "START") {
            startForeground(77, notification("Xray engine unavailable"))
            // Do not create a misleading tunnel or claim connectivity.
            stopSelf()
        }
        return START_NOT_STICKY
    }

    private fun stopTunnel() {
        try { tun?.close() } catch (_: Exception) {}
        tun = null
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    private fun notification(message: String): Notification {
        val manager = getSystemService(NotificationManager::class.java)
        if (Build.VERSION.SDK_INT >= 26) {
            manager.createNotificationChannel(NotificationChannel("tom_tunnel", "TOM_TUNNEL VPN", NotificationManager.IMPORTANCE_LOW))
        }
        return if (Build.VERSION.SDK_INT >= 26) Notification.Builder(this, "tom_tunnel")
            .setContentTitle("TOM_TUNNEL").setContentText(message).setSmallIcon(android.R.drawable.stat_sys_warning).build()
        else @Suppress("DEPRECATION") Notification.Builder(this)
            .setContentTitle("TOM_TUNNEL").setContentText(message).setSmallIcon(android.R.drawable.stat_sys_warning).build()
    }
}
