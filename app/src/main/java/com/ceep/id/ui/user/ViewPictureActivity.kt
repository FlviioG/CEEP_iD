package com.ceep.id.ui.user

import android.os.Bundle
import android.view.WindowManager.LayoutParams.BRIGHTNESS_OVERRIDE_FULL
import android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
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

        with(window)
        {
            addFlags(FLAG_KEEP_SCREEN_ON)
            attributes = attributes.also {
                it.screenBrightness = BRIGHTNESS_OVERRIDE_FULL
            }
        }

        val mSecurityPreferences = SecurityPreferences(this)
        val profilePic = findViewById<ImageView>(R.id.zoom_pic)
        val buttonVoltar = findViewById<Button>(R.id.button_voltar)

        profilePic.setImageBitmap(mSecurityPreferences.getBitmap(PIC_PERFIL))

        buttonVoltar.setOnClickListener {
            onBackPressed()
        }

    }

    override fun onBackPressed() {
        super.onBackPressed()
        overridePendingTransition(R.anim.to_down_1, R.anim.to_down_2)
    }
}