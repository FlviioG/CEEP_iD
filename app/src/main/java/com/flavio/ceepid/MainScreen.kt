package com.flavio.ceepid

import android.content.res.Configuration
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.content.res.AppCompatResources
import com.ceep.id.R

class MainScreen : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main_screen)

        val background_view = findViewById<View>(R.id.background_view)
        val toolbar = findViewById<ImageView>(R.id.toolbar)
        val backgroundPic = findViewById<ImageView>(R.id.background_pic)
        val shapeStatus = findViewById<ImageView>(R.id.shape_status)
        val statusText = findViewById<TextView>(R.id.status_text)

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
}