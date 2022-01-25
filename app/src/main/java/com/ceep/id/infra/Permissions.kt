package com.ceep.id.infra

import android.app.Activity
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import java.util.ArrayList

object Permissao {
    fun validarPermissoes(
        permissoes: Array<String>,
        activity: Activity?,
        resquestCode: Int
    ): Boolean {
        if (Build.VERSION.SDK_INT >= 23) {
            val listaPermissoes: MutableList<String> = ArrayList()
            for (permissao in permissoes) { // Faz a checagem de todas as permissões
                val temPermissao = ContextCompat.checkSelfPermission(activity!!, permissao) ==
                        PackageManager.PERMISSION_GRANTED
                if (!temPermissao) {
                    listaPermissoes.add(permissao)
                }
                if (listaPermissoes.isEmpty()) {
                    return true
                }
                var novasPermissoes = arrayOfNulls<String>(listaPermissoes.size)
                novasPermissoes = listaPermissoes.toTypedArray()
                ActivityCompat.requestPermissions(activity, novasPermissoes, resquestCode)
            }
        }
        return true
    }
}