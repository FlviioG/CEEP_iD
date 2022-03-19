package com.ceep.id.infra

import androidx.appcompat.app.AppCompatActivity

class Constants : AppCompatActivity() {
    object REQUESTS {
        const val PICK_IMAGE_REQUEST = 100
        const val CROP_IMAGE_REQUEST = 200
        const val CAMERA_REQUEST = 300
        const val WIDGET_BUTTON = "com.ceep.id.WIDGET_BUTTON"
    }

    object DATA {
        const val USER_ID = "user_id"
        const val CHANNEL_ID = "default_id"
        const val BASIC_INFORMATIONS = "basic_informations"
        const val PIC_TO_CROP = "pic_to_crop"
        const val PIC_TO_REVIEW = "pic_to_review"
        const val PIC_PERFIL = "perfil_pic"
        const val FIRST_OPENING = "first_time"
    }

    object USER {
        const val IS_ADM = "is_admin"
        const val NAME = "nome"
        const val TURMA = "turma"
        const val SALA = "sala"
    }

    object DATABASE {
        const val TERMO_B = "termo_aceito"
        const val TERMO = "termo"
    }
}