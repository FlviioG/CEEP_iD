package com.ceep.id.ui.main

import android.app.ActivityManager
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.view.animation.AlphaAnimation
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.ceep.id.R
import com.ceep.id.infra.Constants
import com.ceep.id.infra.Constants.DATA.BASIC_INFORMATIONS
import com.ceep.id.infra.Constants.USER.IS_ADM
import com.ceep.id.infra.SecurityPreferences
import com.ceep.id.infra.auth.FirebaseConfig
import com.ceep.id.ui.admin.MainScreenAdmin
import com.ceep.id.ui.user.MainScreen
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.firebase.database.DatabaseReference
import com.google.firebase.storage.StorageReference
import java.io.ByteArrayOutputStream
import java.io.File

class LoadingActivity : AppCompatActivity() {

    private lateinit var progressBar: ProgressBar
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
        progressBar = findViewById(R.id.progress)
        val refresh = findViewById<Button>(R.id.refresh_main_button)
        val acct = GoogleSignIn.getLastSignedInAccount(this)

        if (acct != null) {
            idUsuario = acct.id!!
        }

        refresh.setOnClickListener {
            this.finish()
            startActivity(Intent(this, LoadingActivity::class.java))
            overridePendingTransition(R.anim.right_to_left, R.anim.left_to_right)
            this.finishAffinity()
        }

        updateUI()
    }

    private fun updateUI() {

        Handler(Looper.getMainLooper()).postDelayed({
            val view = findViewById<LinearLayout>(R.id.refresh_main_view)
            view.visibility = View.VISIBLE
            val animation = AlphaAnimation(0f, 1f)
            animation.duration = 800
            view.animation = animation
        }, 3500)

        val chooser = Thread {
            usuarioRef?.child("usuarios/${idUsuario}/admin")?.get()?.addOnSuccessListener {
                progressBar.progress = 20
                if (it.value == true) {
                    progressBar.progress = 100
                    mSecurityPreferences.storeInt(IS_ADM, 1)
                    nextScreen(1)
                } else if (idUsuario != "") {
                    when (mSecurityPreferences.getInt(BASIC_INFORMATIONS)) {
                        0 -> {
                            retrieveData()
                        }
                        1 -> {
                            progressBar.progress = 100
                            nextScreen(0)
                        }
                        2 -> {
                            sendData()
                        }
                    }
                }
            }?.addOnFailureListener {
                updateUI()
                notifyUser("Erro ao receber dados. Tentando novamente...")
            }
        }
        chooser.start()
    }

    private fun sendData() {

        progressBar.progress = 50

        //Carregando Foto
        val pathReference =
            storageReference?.child("imagens/alunos/${idUsuario}/fotoPerfil.jpeg")
        val localFile = File(cacheDir, "fotoPerfil.jpg")
        pathReference?.getFile(localFile)
            ?.addOnSuccessListener {
                val bitmap =
                    BitmapFactory.decodeFile(localFile.absolutePath)

                progressBar.progress = 100
                mSecurityPreferences.storeBitmap(Constants.DATA.PIC_PERFIL, bitmap)
                mSecurityPreferences.storeInt(BASIC_INFORMATIONS, 1)
                nextScreen(0)
            }?.addOnFailureListener {
                progressBar.progress = 70
                val baos = ByteArrayOutputStream()
                val image =
                    BitmapFactory.decodeResource(resources, R.drawable.perfil_empty)
                image.compress(Bitmap.CompressFormat.JPEG, 70, baos)
                val b = baos.toByteArray()
                val imagemRef = storageReference!!.child("imagens")
                    .child("alunos").child(idUsuario)
                    .child("fotoPerfil.jpeg")
                imagemRef.putBytes(b)

                progressBar.progress = 100
                mSecurityPreferences.storeInt(BASIC_INFORMATIONS, 1)
                mSecurityPreferences.storeInt(Constants.DATA.FIRST_OPENING, 0)
                nextScreen(0)
            }

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
                        notifyUser("Dados corrompidos. Limpando app...")
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

                                                mSecurityPreferences.storeInt(
                                                    BASIC_INFORMATIONS,
                                                    1
                                                )

                                                nextScreen(0)
                                            }
                                    } catch (e: Exception) {
                                        notifyUser("Erro ao carregar dados, tentando novamente...")
                                        updateUI()
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

    private fun nextScreen(screen: Int) {
        when (screen) {
            0 -> {
                startActivity(Intent(this, MainScreen::class.java))
                overridePendingTransition(R.anim.right_to_left, R.anim.left_to_right)
            }
            1 -> {
                startActivity(Intent(this, MainScreenAdmin::class.java))
                overridePendingTransition(R.anim.right_to_left, R.anim.left_to_right)
            }
        }

    }
}