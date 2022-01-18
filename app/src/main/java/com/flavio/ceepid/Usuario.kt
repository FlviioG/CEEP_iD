package com.flavio.ceepid

public class Usuario {

//private String CPF;
//private String senha;
//private String turma;
//private ImageView foto;

    private lateinit var nome: String
    private lateinit var sobrenome: String

    public fun usuario(nome1: String, sobrenome1: String) {
        nome = nome1
        sobrenome = sobrenome1
    }

    fun getNome(): String? {
        return nome
    }

    fun setNome(nome1: String) {
        nome = nome1
    }

    fun getSobrenome(): String? {
        return sobrenome
    }

    fun setSobrenome(sobrenome1: String) {
        sobrenome = sobrenome1
    }
}

