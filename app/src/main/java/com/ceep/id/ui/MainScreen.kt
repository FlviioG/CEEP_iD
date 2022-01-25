package com.ceep.id.ui

import android.Manifest
import android.content.Intent
import android.content.res.Configuration
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.MediaStore
import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.content.res.AppCompatResources
import androidx.core.graphics.drawable.RoundedBitmapDrawableFactory
import androidx.core.graphics.drawable.toBitmap
import com.bumptech.glide.Glide
import com.ceep.id.R
import com.ceep.id.infra.FirebaseConfig
import com.ceep.id.infra.Permissao
import com.ceep.id.infra.Usuario
import com.flavio.ceepid.infra.SecurityPreferences
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.ValueEventListener
import com.google.firebase.storage.StorageReference
import java.io.ByteArrayOutputStream
import java.io.File


class MainScreen : AppCompatActivity() {

    private val permissoesNecessarias = arrayOf(
        Manifest.permission.READ_EXTERNAL_STORAGE,
        Manifest.permission.WRITE_EXTERNAL_STORAGE,
        Manifest.permission.INTERNET
    )

    private lateinit var mSecurityPreferences: SecurityPreferences
    private var usuarioRef: DatabaseReference? = null
    private var storageReference: StorageReference? = null
    private lateinit var idUsuario: String
    private lateinit var photoUsuario: Uri
    private lateinit var nomeUsuario: String
    private lateinit var turmaUsuario: String

    @RequiresApi(Build.VERSION_CODES.M)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main_screen)

        Permissao.validarPermissoes(permissoesNecessarias, this, 1)
        mSecurityPreferences = SecurityPreferences(this)
        storageReference = FirebaseConfig.getFirebaseStorage()
        usuarioRef = FirebaseConfig.getFirabaseDatabase()

        val acct = GoogleSignIn.getLastSignedInAccount(this)
        if (acct != null) {
            idUsuario = acct.id!!
            photoUsuario = acct.photoUrl!!

        }

        mSecurityPreferences.storeString("idU", acct?.id.toString())

        val backgroundView = findViewById<View>(R.id.background_view)
        val toolbar = findViewById<ImageView>(R.id.toolbar)
        val backgroundPic = findViewById<ImageView>(R.id.background_pic)
        val shapeStatus = findViewById<ImageView>(R.id.shape_status)
        val shapeNome = findViewById<ImageView>(R.id.shape_nome)
        val statusText = findViewById<TextView>(R.id.status_text)
        val buttonPic = findViewById<FloatingActionButton>(R.id.button_photo)
        val profilePic = findViewById<ImageView>(R.id.profile_pic)
        val buttonVoltar = findViewById<Button>(R.id.button_voltar)
        val buttonPerfil = findViewById<ImageView>(R.id.button_perfil)
        val textName = findViewById<TextView>(R.id.textNome)
        val textTurma = findViewById<TextView>(R.id.textTurma)

        usuarioRef?.child("usuarios/${idUsuario}/nome")?.get()?.addOnSuccessListener {
            nomeUsuario = it.value.toString()
            textName.text = nomeUsuario

        }

        usuarioRef?.child("usuarios/${idUsuario}/turma")?.get()?.addOnSuccessListener {
            turmaUsuario = it.value.toString()
            textTurma.text = turmaUsuario

        }

        if (mSecurityPreferences.getBitmap("fotoPerfil") == null) {
            try {
                val pathReference =
                    storageReference?.child("imagens/alunos/${idUsuario}/fotoPerfil.jpeg")
                val localFile: File = File.createTempFile("images", "jpg")
                pathReference?.getFile(localFile)
                    ?.addOnSuccessListener {
                        val bitmap = BitmapFactory.decodeFile(localFile.absolutePath)
                        val roundDrawable = RoundedBitmapDrawableFactory.create(resources, bitmap)
                        roundDrawable.cornerRadius = 49F
                        profilePic.setImageDrawable(roundDrawable)
                        mSecurityPreferences.storeBitmap(
                            "fotoPerfil",
                            profilePic.drawable.toBitmap()
                        )
                    }?.addOnFailureListener{
                        Glide.with(this).load(photoUsuario).into(profilePic)
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                            Handler.createAsync(Looper.getMainLooper()).postDelayed({
                                val roundDrawable = RoundedBitmapDrawableFactory.create(
                                    resources,
                                    profilePic.drawable.toBitmap()
                                )
                                roundDrawable.cornerRadius = 49F
                                profilePic.setImageDrawable(roundDrawable)
                                mSecurityPreferences.storeBitmap(
                                    "fotoPerfil",
                                    profilePic.drawable.toBitmap()
                                )
                            }, 5000)
                        }
                    }
            } catch (e: Exception) {

            }
        } else {
            val image = mSecurityPreferences.getBitmap("fotoPerfil")
            val roundDrawable = RoundedBitmapDrawableFactory.create(resources, image)
            roundDrawable.cornerRadius = 49F
            profilePic.setImageDrawable(roundDrawable)
        }

        buttonPic.setOnClickListener {
            openGalleryForImage()
        }
        buttonPerfil.setOnClickListener {
            Usuario().liberar(idUsuario)
        }
        profilePic.setOnClickListener {
            findViewById<View>(R.id.zoom_view).visibility = View.VISIBLE
            buttonPic.visibility = View.INVISIBLE
            findViewById<ImageView>(R.id.zoom_pic).setImageBitmap(mSecurityPreferences.getBitmap("fotoPerfil"))
        }
        buttonVoltar.setOnClickListener {
            findViewById<View>(R.id.zoom_view).visibility = View.INVISIBLE
            buttonPic.visibility = View.VISIBLE
        }

        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        if (resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK == Configuration.UI_MODE_NIGHT_YES) {
            backgroundView.setBackgroundColor(getColor(R.color.background_dark))
            toolbar.setImageDrawable(
                AppCompatResources.getDrawable(
                    this,
                    R.drawable.main_toolbar_dark
                )
            )
            backgroundPic.setImageDrawable(
                AppCompatResources.getDrawable(
                    this,
                    R.drawable.pic_background_dark
                )
            )
            shapeStatus.setImageDrawable(
                AppCompatResources.getDrawable(
                    this,
                    R.drawable.shape_status_dark
                )
            )
            shapeNome.setImageDrawable(
                AppCompatResources.getDrawable(
                    this,
                    R.drawable.shape_nome_dark
                )
            )
            statusText.setTextColor(resources.getColor(R.color.white))
        } else {
            backgroundView.setBackgroundColor(getColor(R.color.background_light))
            toolbar.setImageDrawable(AppCompatResources.getDrawable(this, R.drawable.main_toolbar))
            backgroundPic.setImageDrawable(
                AppCompatResources.getDrawable(
                    this,
                    R.drawable.pic_background
                )
            )
            shapeStatus.setImageDrawable(
                AppCompatResources.getDrawable(
                    this,
                    R.drawable.shape_status
                )
            )
            shapeNome.setImageDrawable(
                AppCompatResources.getDrawable(
                    this,
                    R.drawable.shape_nome
                )
            )
            statusText.setTextColor(resources.getColor(R.color.black))
        }
        }

        val postReference =  usuarioRef?.child("usuarios/${idUsuario}/liberado")
        val postListener = object: ValueEventListener {

            override fun onDataChange(snapshot: DataSnapshot) {
                val post = snapshot.getValue()
                    if (post == true) {
                        findViewById<ImageView>(R.id.led_indicator).setImageDrawable(
                            resources.getDrawable(
                                R.drawable.green_ball
                            )
                        )
                        statusText.text = "Liberado"
                    } else if (post == null || post == false) {
                        findViewById<ImageView>(R.id.led_indicator).setImageDrawable(
                            resources.getDrawable(
                                R.drawable.red_ball
                            )
                        )
                        statusText.text = "Em aula/Fora do horario de aula"
                    }
                }

            override fun onCancelled(error: DatabaseError) {
                TODO("Not yet implemented")
            }

        }

        postReference?.addValueEventListener(postListener)
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
        if (resultCode == RESULT_OK && requestCode == 1) {
            val profilePic = findViewById<ImageView>(R.id.profile_pic)
            profilePic.setImageURI(data?.data)

            val image = MediaStore.Images.Media.getBitmap(contentResolver, data?.data)
            val roundDrawable = RoundedBitmapDrawableFactory.create(resources, image)
            roundDrawable.cornerRadius = 49F
            profilePic.setImageDrawable(roundDrawable)

            val baos = ByteArrayOutputStream()
            image.compress(Bitmap.CompressFormat.JPEG, 100, baos)
            val b = baos.toByteArray()

            //Salvando a imagem no banco de dados (Firebase)

            val imagemRef = storageReference!!.child("imagens")
                .child("alunos").child(idUsuario)
                .child("fotoPerfil.jpeg")

            val uploadTask = imagemRef.putBytes(b)
            uploadTask.addOnFailureListener {
                Toast.makeText(
                    this,
                    "ERRO AO FAZER O UPLOAD DA IMAGEM", Toast.LENGTH_SHORT
                ).show()
            }.addOnSuccessListener {
                Toast.makeText(
                    this,
                    "SUCESSO AO FAZER O UPLOAD DA IMAGEM", Toast.LENGTH_SHORT
                ).show()
            }

            mSecurityPreferences.storeBitmap("fotoPerfil", profilePic.drawable.toBitmap())
        }
    }

}