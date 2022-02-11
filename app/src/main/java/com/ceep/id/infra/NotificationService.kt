package com.ceep.id.infra

import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.os.Handler
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.ceep.id.R
import com.ceep.id.infra.auth.FirebaseConfig
import com.ceep.id.ui.MainActivity
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.ValueEventListener
import java.util.*


class NotificationService : Service() {
    var timer: Timer? = null
    var timerTask: TimerTask? = null
    var TAG = "Timers"
    var Your_X_SECS = 5
    override fun onBind(arg0: Intent?): IBinder? {
        return null
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.e(TAG, "onStartCommand")
        super.onStartCommand(intent, flags, startId)
        startTimer()
        return START_STICKY
    }

    override fun onCreate() {
        Log.e(TAG, "onCreate")
    }

    override fun onDestroy() {
        Log.e(TAG, "onDestroy")
        stoptimertask()
        super.onDestroy()
    }

    //we are going to use a handler to be able to run in our TimerTask
    val handler: Handler = Handler()
    fun startTimer() {
        //set a new Timer
        timer = Timer()

        //initialize the TimerTask's job
        initializeTimerTask()

        //schedule the timer, after the first 5000ms the TimerTask will run every 10000ms
        timer!!.schedule(timerTask, 5000, (Your_X_SECS * 1000).toLong()) //
        //timer.schedule(timerTask, 5000,1000); //
    }

    fun stoptimertask() {
        //stop the timer, if it's not already null
        if (timer != null) {
            timer!!.cancel()
            timer = null
        }
    }

    fun initializeTimerTask() {
        timerTask = object : TimerTask() {
            override fun run() {
                //use a handler to run a toast that shows the current timestamp
                handler.post(Runnable {

                    var usuarioRef: DatabaseReference? = FirebaseConfig.getFirabaseDatabase()
                    val idUsuario = GoogleSignIn.getLastSignedInAccount(this@NotificationService)?.id
                    val postReference = usuarioRef?.child("usuarios/${idUsuario}/liberado")
                    val postListener = object : ValueEventListener {
                        override fun onDataChange(snapshot: DataSnapshot) {
                            val post = snapshot.value
                            if(post == true) {
                                val contentIntent = PendingIntent.getActivity(
                                    this@NotificationService,
                                    0,
                                    Intent(this@NotificationService, MainActivity::class.java),
                                    0
                                )
                                val CHANNEL_ID = "default_id"
                                val builder =
                                    NotificationCompat.Builder(this@NotificationService, CHANNEL_ID)
                                        .setSmallIcon(R.mipmap.ic_launcher)
                                        .setContentTitle("Situação:")
                                        .setContentText("Liberado!")
                                        .setPriority(NotificationCompat.PRIORITY_MAX)
                                        .setDefaults(NotificationCompat.DEFAULT_ALL)
                                        .setContentIntent(contentIntent)
                                        .setAutoCancel(true)

                                with(NotificationManagerCompat.from(this@NotificationService)) {
                                    // notificationId is a unique int for each notification that you must define
                                    notify(1, builder.build())
                                }
                            }
                        }

                        override fun onCancelled(error: DatabaseError) {
                            TODO("Not yet implemented")
                        }

                    }
                    postReference?.addValueEventListener(postListener)
                })



            }
        }
    }
}