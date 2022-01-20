package com.ceep.id.ui

import android.R.attr
import android.content.Intent
import android.content.res.Configuration
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.content.res.AppCompatResources
import androidx.core.net.toUri
import com.ceep.id.R
import com.flavio.ceepid.infra.SecurityPreferences
import com.google.android.material.floatingactionbutton.FloatingActionButton
import android.content.ContentResolver

import android.R.attr.data
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.drawable.BitmapDrawable
import android.widget.Button
import androidx.core.graphics.drawable.RoundedBitmapDrawable
import androidx.core.graphics.drawable.RoundedBitmapDrawableFactory


class MainScreen : AppCompatActivity() {

    private lateinit var mSecurityPreferences: SecurityPreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main_screen)

        mSecurityPreferences = SecurityPreferences(this)

        val background_view = findViewById<View>(R.id.background_view)
        val toolbar = findViewById<ImageView>(R.id.toolbar)
        val backgroundPic = findViewById<ImageView>(R.id.background_pic)
        val shapeStatus = findViewById<ImageView>(R.id.shape_status)
        val statusText = findViewById<TextView>(R.id.status_text)
        val buttonPic = findViewById<FloatingActionButton>(R.id.button_photo)
        val profilePic = findViewById<ImageView>(R.id.profile_pic)
        val buttonVoltar = findViewById<Button>(R.id.button_voltar)

        val image = mSecurityPreferences.getBitmap("Image")
        val roundDrawable = RoundedBitmapDrawableFactory.create(resources, image)
        roundDrawable.cornerRadius = 49F
        profilePic.setImageDrawable(roundDrawable)

        buttonPic.setOnClickListener {
            openGalleryForImage()
        }

        profilePic.setOnClickListener {
            findViewById<ImageView>(R.id.zoom_pic).visibility = View.VISIBLE
            findViewById<View>(R.id.background_zoom).visibility = View.VISIBLE
            buttonPic.visibility = View.INVISIBLE
            buttonVoltar.visibility = View.VISIBLE
            findViewById<ImageView>(R.id.zoom_pic).setImageBitmap(mSecurityPreferences.getBitmap("Image"))
        }

        buttonVoltar.setOnClickListener {
            findViewById<ImageView>(R.id.zoom_pic).visibility = View.GONE
            findViewById<View>(R.id.background_zoom).visibility = View.GONE
            buttonPic.visibility = View.VISIBLE
            buttonVoltar.visibility = View.GONE
        }

        if (resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK == Configuration.UI_MODE_NIGHT_YES) {
            background_view.setBackgroundColor(getColor(R.color.background_dark))
            toolbar.setImageDrawable(AppCompatResources.getDrawable(this, R.drawable.main_toolbar_dark))
            backgroundPic.setImageDrawable(AppCompatResources.getDrawable(this, R.drawable.pic_background_dark))
            shapeStatus.setImageDrawable(AppCompatResources.getDrawable(this, R.drawable.shape_status_dark))
            statusText.setTextColor(resources.getColor(R.color.white))
        } else {
            background_view.setBackgroundColor(getColor(R.color.background_light))
            toolbar.setImageDrawable(AppCompatResources.getDrawable(this, R.drawable.main_toolbar))
            backgroundPic.setImageDrawable(AppCompatResources.getDrawable(this, R.drawable.pic_background))
            shapeStatus.setImageDrawable(AppCompatResources.getDrawable(this, R.drawable.shape_status))
            statusText.setTextColor(resources.getColor(R.color.black))
        }
    }

    override fun onBackPressed() {
        this.finishAffinity()
    }

    private fun openGalleryForImage() {
        val intent = Intent(Intent.ACTION_PICK)
        intent.type = "image/*"
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        intent.addFlags(Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION)
        intent.putExtra(Intent.EXTRA_LOCAL_ONLY, true)
        startActivityForResult(intent, 1)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if(resultCode == RESULT_OK && requestCode == 1) {
            val profilePic = findViewById<ImageView>(R.id.profile_pic)
            profilePic.setImageURI(data?.data)

            val image = (profilePic.drawable as BitmapDrawable).bitmap
            val roundDrawable = RoundedBitmapDrawableFactory.create(resources, image)
            roundDrawable.cornerRadius = 49F
            profilePic.setImageDrawable(roundDrawable)

            mSecurityPreferences.storeBitmap("Image", image)
        }
    }
}