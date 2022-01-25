package com.ceep.id.infra

import android.util.Log
import com.ceep.id.infra.Base64Custom.codificarBase64
import com.ceep.id.infra.FirebaseConfig.getFirabaseDatabase
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.ValueEventListener

class ControleLiberaco {
    private var usuarioRef: DatabaseReference? = null
    private var usuarioX: Usuario? = null

    fun liberarTurma(turma: String?) {
        usuarioRef = getFirabaseDatabase()!!.child("usuarios")
        val usuariosPesquisa = usuarioRef!!.orderByChild("turma").equalTo(turma)
        usuariosPesquisa.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                for (dados in snapshot.children) {
                    usuarioX = dados.getValue(Usuario::class.java)
                    val emailCoc = codificarBase64(usuarioX!!.getEmail()!!)
                    Log.i("QUEM E", emailCoc)
                    usuarioX!!.liberar(emailCoc)
                }
            }

            override fun onCancelled(error: DatabaseError) {}
        })
    }

    fun retirarLiberacao(turma: String?) {
        usuarioRef = getFirabaseDatabase()!!.child("usuarios")
        val usuariosPesquisa = usuarioRef!!.orderByChild("turma").equalTo(turma)
        usuariosPesquisa.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                for (dados in snapshot.children) {
                    usuarioX = dados.getValue(Usuario::class.java)
                    val emailCoc = codificarBase64(usuarioX!!.getEmail()!!)
                    Log.i("QUEM E", emailCoc)
                    usuarioX!!.desliberar(emailCoc)
                }
            }

            override fun onCancelled(error: DatabaseError) {}
        })
    }
}