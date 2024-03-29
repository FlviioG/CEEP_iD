package com.ceep.id.ui.user

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.ceep.id.R
import com.ceep.id.infra.Constants.DATA.PIC_TO_CROP
import com.ceep.id.infra.Constants.DATA.PIC_TO_REVIEW
import com.ceep.id.infra.SecurityPreferences
import com.edmodo.cropper.CropImageView
import com.google.android.material.floatingactionbutton.FloatingActionButton

class EditPictureActivity : AppCompatActivity() {

    private lateinit var mSecurityPreferences: SecurityPreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_edit_picture)

        mSecurityPreferences = SecurityPreferences(this)

        val cropImageView = findViewById<CropImageView>(R.id.cropFoto)
        val buttonRotate = findViewById<FloatingActionButton>(R.id.buttonRt)
        val buttonSave = findViewById<FloatingActionButton>(R.id.buttonSv)

        with(cropImageView) {
            setAspectRatio(1, 1)
            setFixedAspectRatio(true)
            setGuidelines(2)
        }
        cropImageView.setImageBitmap(mSecurityPreferences.getBitmap(PIC_TO_CROP))

        buttonRotate.setOnClickListener {
            cropImageView.rotateImage(-90)
        }

        buttonSave.setOnClickListener {
            mSecurityPreferences.storeBitmap(PIC_TO_REVIEW, cropImageView.croppedImage)
            val returnIntent = Intent()
            setResult(RESULT_OK, returnIntent)
            mSecurityPreferences.remove(PIC_TO_CROP)
            finish()
        }

    }

    override fun onBackPressed() {
        val returnIntent = Intent()
        setResult(RESULT_CANCELED, returnIntent)
        finish()
    }
}