package com.ceep.id.infra

import com.ceep.id.infra.auth.FirebaseConfig.getFirabaseDatabase
import java.io.File


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
}