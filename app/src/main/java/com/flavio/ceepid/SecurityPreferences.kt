package com.flavio.ceepid

import android.content.Context

class SecurityPreferences(context: Context) {
    private val mSharedPreferences = context.getSharedPreferences("CEEP_id", Context.MODE_PRIVATE)

    fun storeString(key: String, value: String) {
        mSharedPreferences.edit().putString(key, value).apply()
    }

    fun getString(key: String): String {
        return mSharedPreferences.getString(key, "") ?: ""
    }

    fun storeInt(key: String, value: Int) {
        mSharedPreferences.edit().putInt(key, value).apply()
    }

    fun getInt(key: String): Int {
        return mSharedPreferences.getInt(key, 0)
    }
}