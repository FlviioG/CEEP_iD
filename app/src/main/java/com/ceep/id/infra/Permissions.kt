package com.ceep.id.infra

import android.Manifest
import android.content.pm.PackageManager
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat

private val permissoesNecessarias = arrayOf(
    Manifest.permission.CAMERA,
    Manifest.permission.READ_EXTERNAL_STORAGE,
    Manifest.permission.WRITE_EXTERNAL_STORAGE,
    Manifest.permission.INTERNET
)

object Permissao {
    fun validarPermissoes(
        activity: AppCompatActivity?,
        resquestCode: Int
    ): Boolean {
        val listaPermissoes: MutableList<String> = ArrayList()
        for (permissao in permissoesNecessarias) { // Faz a checagem de todas as permissões
            val temPermissao = ContextCompat.checkSelfPermission(activity!!, permissao) ==
                    PackageManager.PERMISSION_GRANTED
            if (!temPermissao) {
                listaPermissoes.add(permissao)
            }
        }
        return if (listaPermissoes.isEmpty()) {
            true
        } else {
            ActivityCompat.requestPermissions(
                activity!!,
                listaPermissoes.toTypedArray(),
                resquestCode
            )
            false
        }
    }
}
