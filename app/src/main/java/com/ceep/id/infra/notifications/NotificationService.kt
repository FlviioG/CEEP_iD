package com.ceep.id.infra.notifications

import android.app.PendingIntent
import android.app.PendingIntent.FLAG_IMMUTABLE
import android.app.Service
import android.content.Intent
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.ceep.id.R
import com.ceep.id.infra.SecurityPreferences
import com.ceep.id.infra.auth.FirebaseConfig
import com.ceep.id.ui.MainActivity
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.ValueEventListener


class NotificationService : Service() {

    private lateinit var mSecurityPreferences: SecurityPreferences
    var tag = "NotificationService"

    override fun onBind(arg0: Intent?): IBinder? {
        return null
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.e(tag, "onStartCommand")
        mSecurityPreferences = SecurityPreferences(this)
        super.onStartCommand(intent, flags, startId)
        return START_STICKY
    }

    override fun onCreate() {
        Log.e(tag, "onCreate")
        super.onCreate()

        val usuarioRef: DatabaseReference? = FirebaseConfig.getFirabaseDatabase()
        val idUsuario = GoogleSignIn.getLastSignedInAccount(this@NotificationService)?.id
        val postReference = usuarioRef?.child("usuarios/${idUsuario}/liberado")
        val postListener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {

                val post = snapshot.value

                if (post == true) {
                    val contentIntent = PendingIntent.getActivity(
                        this@NotificationService,
                        0,
                        Intent(this@NotificationService, MainActivity::class.java),
                        FLAG_IMMUTABLE
                    )
                    val channelId = "default_id"
                    val builder =
                        NotificationCompat.Builder(this@NotificationService, channelId)
                            .setSmallIcon(R.mipmap.ic_launcher)
                            .setContentTitle("Situação:")
                            .setContentText("Liberado!")
                            .setPriority(NotificationCompat.PRIORITY_MAX)
                            .setContentIntent(contentIntent)
                            .setAutoCancel(true)

                        Log.e(tag, "notify")
                        with(NotificationManagerCompat.from(this@NotificationService)) {
                            notify(1, builder.build())
                    }
                } else {
                    Log.e(tag, "cancelNotify")
                    NotificationManagerCompat.from(this@NotificationService).cancel(1)
                }
            }

            override fun onCancelled(error: DatabaseError) {
            }

        }
        postReference?.addValueEventListener(postListener)

    }

    override fun onDestroy() {
        Log.e(tag, "restarting...")
        Intent(this, NotificationService::class.java).also {
            startService(it)
        }
    }
}

