package com.ceep.id.infra

import com.ceep.id.infra.auth.FirebaseConfig.getFirabaseDatabase


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
}