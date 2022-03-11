package com.ceep.id.infra

import com.ceep.id.infra.auth.FirebaseConfig.getFirabaseDatabase
import java.io.File
import java.util.*


class Usuario {

    private var nome: String? = null

    fun getNome(): String? {
        return nome
    }

    fun liberar(idU: String?) {
        val firebaseRef = getFirabaseDatabase()
        val usuario = firebaseRef!!.child("usuarios").child(idU!!)
            .child("liberado")
        usuario.setValue(true)

    }

    fun desliberar(idU: String?) {
        val firebaseRef = getFirabaseDatabase()
        val usuario = firebaseRef!!.child("usuarios").child(idU!!)
            .child("liberado")
        usuario.setValue(false)
    }

    fun deleteTempFiles (cacheDir: File) {
        if(cacheDir.isDirectory) {
            val files = cacheDir.listFiles()
            if(files != null) {
                for (file in files) {
                    file.delete()
                }
            }

        }
    }

    fun getDay(): String {
        val date = Calendar.getInstance()
        return "${date.get(Calendar.DAY_OF_MONTH)}/${date.get(Calendar.MONTH)}/${date.get(Calendar.YEAR)}"
    }

    fun getHour (): String {
        val date = Calendar.getInstance()
        val hour = date.get(Calendar.HOUR_OF_DAY)
        val minute = date.get(Calendar.MINUTE)

        return when {
            hour <= 9 && minute > 9 -> "0$hour:$minute"
            hour > 9 && minute <= 9 -> "$hour:0$minute"
            hour <= 9 && minute <= 9 -> "0$hour:0$minute"
            else -> "$hour:$minute."
        }
    }
}