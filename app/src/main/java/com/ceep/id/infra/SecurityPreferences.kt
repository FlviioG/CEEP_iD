package com.ceep.id.infra

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Base64
import java.io.ByteArrayOutputStream

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

    fun storeBitmap(key: String, value: Bitmap) {
        val baos = ByteArrayOutputStream()
        value.compress(Bitmap.CompressFormat.JPEG, 100, baos)
        val b = baos.toByteArray()

        val encodedImage = Base64.encodeToString(b, Base64.DEFAULT)
        mSharedPreferences.edit().putString(key, encodedImage).apply()
    }

    fun getBitmap(key: String): Bitmap? {
        val encodedImage = mSharedPreferences.getString(key, "") ?: ""

        return if(encodedImage != "") {
            val b: ByteArray = Base64.decode(encodedImage, Base64.DEFAULT)

            val bitmap = BitmapFactory.decodeByteArray(b, 0, b.size)

            bitmap
        } else {
            null
        }
    }
}