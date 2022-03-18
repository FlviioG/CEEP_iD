package com.ceep.id.ui.user

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import com.ceep.id.R
import com.ceep.id.infra.Constants
import com.ceep.id.infra.SecurityPreferences
import com.ceep.id.infra.Usuario
import com.ceep.id.infra.auth.FirebaseConfig
import com.google.android.gms.auth.api.signin.GoogleSignIn
import java.util.*


/**
 * Implementation of App Widget functionality.
 */
class StatusWidget : AppWidgetProvider() {

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        // There may be multiple widgets active, so update all of them
        for (appWidgetId in appWidgetIds) {
            updateAppWidget(context, appWidgetManager, appWidgetId)
        }
    }

    override fun onEnabled(context: Context) {
        val account = GoogleSignIn.getLastSignedInAccount(context)
        if (account != null) {
            val manager = AppWidgetManager.getInstance(context)
            val id = manager.getAppWidgetIds(ComponentName(context, StatusWidget::class.java))

            val views = RemoteViews(context.packageName, R.layout.status_widget)
            val intent = Intent(Constants.REQUESTS.WIDGET_BUTTON)
            val pendingIntent = PendingIntent.getBroadcast(context, 0, intent,
                PendingIntent.FLAG_UPDATE_CURRENT
            )
            views.setOnClickPendingIntent(R.id.refresh_widget, pendingIntent)

            id.forEach {
                updateAppWidget(context, manager, it)
            }
        }
    }

        override fun onDisabled(context: Context) {
            super.onDisabled(context)
        }

        override fun onReceive(context: Context?, intent: Intent?) {
            super.onReceive(context, intent)
            val manager = AppWidgetManager.getInstance(context)
            val id = manager.getAppWidgetIds(ComponentName(context!!, StatusWidget::class.java))

                id.forEach {
                    updateAppWidget(context, manager, it)
                }

        }
    }

    internal fun updateAppWidget(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetId: Int
    ) {
        val mSecurityPreferences = SecurityPreferences(context)
        val idUsuario = mSecurityPreferences.getString(Constants.DATA.USER_ID)
        lateinit var status: String
        val usuarioRef = FirebaseConfig.getFirabaseDatabase()

        ///Status
        if (idUsuario != "") {
            usuarioRef?.child("usuarios/${idUsuario}/liberado")?.get()
                ?.addOnSuccessListener { post ->

                    if (post.value == true) {
                        status = "Liberado. Atualizado às ${Usuario().getHour()}."
                        val views = RemoteViews(context.packageName, R.layout.status_widget)
                        views.setTextViewText(R.id.status_text, status)
                        appWidgetManager.updateAppWidget(appWidgetId, views)

                    } else if (post.value == null || post.value == false) {
                        usuarioRef.child("usuarios/${idUsuario}/sala").get()
                            .addOnSuccessListener {
                                val date = Calendar.getInstance()
                                val day = date.get(Calendar.DAY_OF_WEEK)
                                val hour = date.get(Calendar.HOUR_OF_DAY)
                                val minutes = date.get(Calendar.MINUTE)

                                status = when (day) {
                                    in 2..6 -> {
                                        if (it.toString().contains('V')) {
                                            when (hour) {
                                                in 13..17 -> {
                                                    context.getString(R.string.em_aula)
                                                }
                                                18 -> {
                                                    when (minutes) {
                                                        in 0..20 -> {
                                                            context.getString(R.string.em_aula)
                                                        }
                                                        else -> {
                                                            context.getString(R.string.fora_do_horario)
                                                        }
                                                    }
                                                }
                                                else -> {
                                                    context.getString(R.string.fora_do_horario)
                                                }
                                            }
                                        } else {
                                            when (hour) {
                                                in 7..11 -> {
                                                    context.getString(R.string.em_aula)
                                                }
                                                12 -> {
                                                    when (minutes) {
                                                        in 0..20 -> {
                                                            context.getString(R.string.em_aula)
                                                        }
                                                        else -> {
                                                            context.getString(R.string.fora_do_horario)
                                                        }
                                                    }
                                                }
                                                else -> {
                                                    context.getString(R.string.fora_do_horario)
                                                }
                                            }
                                        }
                                    }
                                    else -> {
                                        context.getString(R.string.fora_do_horario)
                                    }
                                }
                                val views = RemoteViews(context.packageName, R.layout.status_widget)
                                views.setTextViewText(R.id.status_text, status)
                                appWidgetManager.updateAppWidget(appWidgetId, views)
                            }
                    }
                }
        } else {
            status = "Faça login primeiro"
            val views = RemoteViews(context.packageName, R.layout.status_widget)
            views.setTextViewText(R.id.status_text, status)
            appWidgetManager.updateAppWidget(appWidgetId, views)
        }
    }