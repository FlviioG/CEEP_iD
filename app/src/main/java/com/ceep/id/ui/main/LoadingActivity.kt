package com.ceep.id.ui.main

import android.app.ActivityManager
import android.content.Intent
import android.graphics.BitmapFactory
import android.os.Bundle
import android.os.Handler
import android.view.View
import android.view.animation.AlphaAnimation
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.ceep.id.R
import com.ceep.id.infra.Constants
import com.ceep.id.infra.SecurityPreferences
import com.ceep.id.infra.auth.FirebaseConfig
import com.ceep.id.ui.admin.MainScreenAdmin
import com.ceep.id.ui.user.MainScreen
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.firebase.database.DatabaseReference
import com.google.firebase.storage.StorageReference
import java.io.File

class LoadingActivity : AppCompatActivity() {

    private lateinit var mSecurityPreferences: SecurityPreferences
    private var usuarioRef: DatabaseReference? = null
    private var storageReference: StorageReference? = null
    private lateinit var idUsuario: String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_loading)
        usuarioRef = FirebaseConfig.getFirabaseDatabase()
        storageReference = FirebaseConfig.getFirebaseStorage()
        mSecurityPreferences = SecurityPreferences(this)

        val refresh = findViewById<Button>(R.id.refresh_main_button)
        val acct = GoogleSignIn.getLastSignedInAccount(this)

        if (acct != null) {
            idUsuario = acct.id!!
        }


        refresh.setOnClickListener {
            this.finish()
            startActivity(Intent(this, LoadingActivity::class.java))
            this.finishAffinity()
            overridePendingTransition(R.anim.left_to_right, R.anim.right_to_left)
        }

        updateUI()
    }

    private fun updateUI() {
        val progressBar = findViewById<ProgressBar>(R.id.progress)

        Handler().postDelayed({
            val view = findViewById<LinearLayout>(R.id.refresh_main_view)
            view.visibility = View.VISIBLE
            val animation = AlphaAnimation(0f, 1f)
            animation.duration = 800
            view.animation = animation
        }, 5000)

        val chooser = Thread {
            usuarioRef?.child("usuarios/${idUsuario}/admin")?.get()?.addOnSuccessListener {
                progressBar.progress = 20
                if (it.value == true) {
                    progressBar.progress = 100
                    startActivity(Intent(this, MainScreenAdmin::class.java))
                } else if (idUsuario != "") {
                    if (mSecurityPreferences.getInt(Constants.DATA.BASIC_INFORMATIONS) == 1) {
                        progressBar.progress = 100
                        startActivity(Intent(this, MainScreen::class.java))
                        overridePendingTransition(R.anim.right_to_left, R.anim.left_to_right)
                    } else {
                        retrieveData()
                    }
                }
            }?.addOnFailureListener {
                updateUI()
                notifyUser("Erro ao receber dados.")
            }
        }
        chooser.start()
    }

    private fun retrieveData() {
        val rD = Thread {
            val progressBar = findViewById<ProgressBar>(R.id.progress)
            ///Nome
            usuarioRef?.child("usuarios/${idUsuario}/nome")?.get()
                ?.addOnSuccessListener { nome ->

                    progressBar.progress = 40

                    if (nome.value == null) {
                        val t = getSystemService(ACTIVITY_SERVICE) as ActivityManager
                        notifyUser(  "Dados corrompidos. Limpando app...")
                        t.clearApplicationUserData()
                    }
                    mSecurityPreferences.storeString(Constants.USER.NAME, nome.value.toString())

                    ///Turma
                    usuarioRef?.child("usuarios/${idUsuario}/turma")?.get()
                        ?.addOnSuccessListener { turma ->

                            progressBar.progress = 60

                            mSecurityPreferences.storeString(
                                Constants.USER.TURMA,
                                turma.value.toString()
                            )

                            //Sala
                            usuarioRef?.child("usuarios/${idUsuario}/sala")?.get()
                                ?.addOnSuccessListener { sala ->

                                    mSecurityPreferences.storeString(
                                        Constants.USER.SALA,
                                        sala.value.toString()
                                    )

                                    ///Foto
                                    try {
                                        val pathReference =
                                            storageReference?.child("imagens/alunos/${idUsuario}/fotoPerfil.jpeg")
                                        val localFile = File(cacheDir, "fotoPerfil.jpg")
                                        pathReference?.getFile(localFile)
                                            ?.addOnSuccessListener {
                                                val bitmap =
                                                    BitmapFactory.decodeFile(localFile.absolutePath)
                                                mSecurityPreferences.storeBitmap(
                                                    Constants.DATA.PIC_PERFIL,
                                                    bitmap
                                                )
                                                progressBar.progress = 80
                                                updateUI()
                                                startActivity(
                                                    Intent(
                                                        this,
                                                        MainScreen::class.java
                                                    )
                                                )
                                                overridePendingTransition(R.anim.right_to_left, R.anim.left_to_right)
                                                mSecurityPreferences.storeInt(
                                                    Constants.DATA.BASIC_INFORMATIONS,
                                                    1
                                                )
                                            }
                                    } catch (e: Exception) {
                                        notifyUser("Erro ao carregar dados")
                                    }
                                }

                        }
                }
        }
        rD.start()
    }

    private fun notifyUser(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }
}