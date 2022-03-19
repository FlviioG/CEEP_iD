package com.ceep.id.infra

import androidx.annotation.Keep
import com.ceep.id.infra.auth.FirebaseConfig.getFirabaseDatabase
import java.io.File
import java.io.Serializable
import java.util.*

@Keep
class Usuario: Serializable {
    private var nome: String? = null
    private var ano: String? = null
    private var sala: String? = null
    private var turma: String? = null
    private var termo: String? = null
    private var liberado: Boolean? = null
    private var termo_aceito: Boolean? = null

    fun getNome(): String? {
        return nome
    }

//    fun setNome(nome: String) {
//        this.nome = nome
//    }
//
//    fun getAno(): String? {
//        return ano
//    }
//
//    fun setAno(ano: String) {
//        this.ano = ano
//    }
//
//    fun getTurma(): String? {
//        return turma
//    }
//
//    fun setTurma(turma: String) {
//        this.turma = turma
//    }
//
//    fun getSala(): String? {
//        return sala
//    }
//
//    fun setSala(sala: String) {
//        this.sala = sala
//    }
//
//    fun getTermoData(): String? {
//        return termo
//    }
//
//    fun setTermoData(termo: String) {
//        this.termo = termo
//    }
//
//    fun getTermoAceite(): Boolean? {
//        return termo_aceito
//    }
//
//    fun setTermoAceite(termo_aceito: Boolean) {
//        this.termo_aceito = termo_aceito
//    }
//
//    fun getLiberacao(): Boolean? {
//        return liberado
//    }

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

    fun getHour (): String {
        val date = Calendar.getInstance()
        val hour = date.get(Calendar.HOUR_OF_DAY)
        val minute = date.get(Calendar.MINUTE)

        return when {
            hour <= 9 && minute > 9 -> "0$hour:$minute"
            hour > 9 && minute <= 9 -> "$hour:0$minute"
            hour <= 9 && minute <= 9 -> "0$hour:0$minute"
            else -> "$hour:$minute"
        }
    }

    fun getDay(): String {
        val calendar = Calendar.getInstance()
        val day = calendar.get(Calendar.DAY_OF_MONTH)
        var month = calendar.get(Calendar.MONTH)
        val year = calendar.get(Calendar.YEAR)

        if(month in 1..11) {
            month++
        } else if(month == 12) {
            month = 1
        }

        return "$day/$month/$year"
    }
}