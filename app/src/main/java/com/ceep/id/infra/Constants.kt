package com.ceep.id.infra

import androidx.appcompat.app.AppCompatActivity

class Constants : AppCompatActivity() {
    object REQUESTS {
        const val PICK_IMAGE_REQUEST = 100
        const val CROP_IMAGE_REQUEST = 200
        const val CAMERA_REQUEST = 300
    }

    object DATA {
        const val USER_ID = "userId"
        const val CHANNEL_ID = "default_id"
        const val BASIC_INFORMATIONS = "basicInformations"
        const val PIC_TO_CROP = "picToCrop"
        const val PIC_TO_REVIEW = "picToReview"
        const val PIC_PERFIL = "fotoPerfil"
    }
}