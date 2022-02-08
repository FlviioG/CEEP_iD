package com.ceep.id.infra

import com.ceep.id.infra.auth.FirebaseConfig.getFirabaseDatabase
import com.google.firebase.database.Exclude


class Usuario {

    private var id: String? = null
    private var nome: String? = null
    private var turma: String? = null
    private var senha: String? = null
    private var email: String? = null
    private var situacao: Boolean = false

    //private String foto;
    private val liberado = false

    fun Usuario() {}

    fun salvar() {
        val firebaseRef = getFirabaseDatabase()
        getId()?.let { firebaseRef!!.child("usuarios").child(it) }?.setValue(this)
    }

    @Exclude // Evita salvar novamente
    fun getId(): String? {
        return id
    }

    fun setId(id: String?) {
        this.id = id
    }

    fun getNome(): String? {
        return nome
    }

    fun setNome(nome: String?) {
        this.nome = nome
    }

    fun getTurma(): String? {
        return turma
    }

    fun setTurma(turma: String?) {
        this.turma = turma
    }

    @Exclude
    fun getSenha(): String? {
        return senha
    }

    fun setSenha(senha: String?) {
        this.senha = senha
    }

    fun getEmail(): String? {
        return email
    }

    fun setEmail(email: String?) {
        this.email = email
    }

    fun getSituacao(): Boolean {
        return situacao
    }

    /*public String getFoto() {
      return foto;
  }

  public void setFoto(String foto) {
      this.foto = foto;
  }

  public boolean isLiberado() {
      return liberado;
  }

  public void setLiberado(boolean liberado) {
      this.liberado = liberado;
  }*/


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