package com.ceep.id.ui.user

import android.content.res.Configuration
import android.os.Build
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity
import com.ceep.id.R
import com.ceep.id.infra.Constants.DATA.PIC_PERFIL
import com.ceep.id.infra.SecurityPreferences

class ViewPictureActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_view_picture)

        val mSecurityPreferences = SecurityPreferences(this)
        val profilePic = findViewById<ImageView>(R.id.zoom_pic)
        val buttonVoltar = findViewById<Button>(R.id.button_voltar)
        val background = findViewById<View>(R.id.zoom_view)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            if (resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK == Configuration.UI_MODE_NIGHT_YES) {
                background.setBackgroundColor(getColor(R.color.background_dark))
            } else {
                background.setBackgroundColor(getColor(R.color.background_light))
            }
        }

        profilePic.setImageBitmap(mSecurityPreferences.getBitmap(PIC_PERFIL))

        buttonVoltar.setOnClickListener {
            finish()
        }

    }
}