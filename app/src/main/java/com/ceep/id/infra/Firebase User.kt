package com.ceep.id.infra

import com.google.firebase.auth.FirebaseAuth

object UsuarioFirebase {
    val identificadorUsuario: String
        get() {
            val usuario: FirebaseAuth? = FirebaseConfig.getFirebaseAuth()
            val email = usuario!!.currentUser!!.email
            return email?.let { Base64Custom.codificarBase64(it) }!!
        }
}