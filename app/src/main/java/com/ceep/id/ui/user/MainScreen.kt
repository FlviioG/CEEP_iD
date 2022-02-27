package com.ceep.id.ui.user

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.Uri
import android.os.Build
import android.os.Build.VERSION_CODES.M
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.MediaStore
import android.view.View
import android.view.animation.Animation
import android.view.animation.LinearInterpolator
import android.view.animation.RotateAnimation
import android.widget.*
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.content.res.AppCompatResources
import androidx.cardview.widget.CardView
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.core.content.res.ResourcesCompat
import androidx.core.graphics.drawable.RoundedBitmapDrawableFactory
import androidx.core.graphics.drawable.toBitmap
import androidx.core.widget.NestedScrollView
import com.bumptech.glide.Glide
import com.ceep.id.R
import com.ceep.id.infra.Constants.DATA.CHANNEL_ID
import com.ceep.id.infra.Constants.DATA.PIC_PERFIL
import com.ceep.id.infra.Constants.DATA.PIC_TO_CROP
import com.ceep.id.infra.Constants.DATA.PIC_TO_REVIEW
import com.ceep.id.infra.Constants.DATA.USER_ID
import com.ceep.id.infra.Constants.REQUESTS.CAMERA_REQUEST
import com.ceep.id.infra.Constants.REQUESTS.CROP_IMAGE_REQUEST
import com.ceep.id.infra.Constants.REQUESTS.PICK_IMAGE_REQUEST
import com.ceep.id.infra.Permissao
import com.ceep.id.infra.SecurityPreferences
import com.ceep.id.infra.auth.FirebaseConfig
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdView
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.ValueEventListener
import com.google.firebase.storage.StorageReference
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.face.FaceDetection
import com.google.mlkit.vision.face.FaceDetectorOptions
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.*

class MainScreen : AppCompatActivity() {

    private var conection: Int? = null
    private var usuarioRef: DatabaseReference? = null
    private var storageReference: StorageReference? = null
    private lateinit var currentPhotoPath: String
    private lateinit var idUsuario: String
    private lateinit var photoUsuario: Uri
    private lateinit var nomeUsuario: String
    private lateinit var turmaUsuario: String
    private lateinit var salaUsuario: String
    private lateinit var mSecurityPreferences: SecurityPreferences
    private lateinit var takePictureIntent: ActivityResultLauncher<Uri>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main_screen)

        createNotificationChannel()

        ///window.setFlags(
        //  WindowManager.LayoutParams.FLAG_SECURE,
        //  WindowManager.LayoutParams.FLAG_SECURE
        //)

        mSecurityPreferences = SecurityPreferences(this)
        storageReference = FirebaseConfig.getFirebaseStorage()
        usuarioRef = FirebaseConfig.getFirabaseDatabase()
        val acct = GoogleSignIn.getLastSignedInAccount(this)
        if (acct != null) {
            idUsuario = acct.id!!
            photoUsuario = acct.photoUrl!!
        }
        ///"ca-app-pub-6136253738426934/1523049245"
        val adView = findViewById<AdView>(R.id.ad)
        adView.loadAd(AdRequest.Builder().build())
        mSecurityPreferences.storeString(USER_ID, acct?.id.toString())

        val statusText = findViewById<TextView>(R.id.status_text)
        val buttonPic = findViewById<FloatingActionButton>(R.id.button_photo)
        val profilePic = findViewById<ImageView>(R.id.profile_pic)
        val textName = findViewById<TextView>(R.id.textNome)
        val textTurma = findViewById<TextView>(R.id.textTurma)
        val buttonRefresh = findViewById<ImageButton>(R.id.refresh_button)
        val cameraBut = findViewById<TextView>(R.id.cameraBut)
        val galeriaBut = findViewById<TextView>(R.id.galeriaBut)
        val cardView = findViewById<CardView>(R.id.cardView)

        theme()

        conection = checkConection()
        if (conection == 0 || conection == 2) {
            getData(textName, textTurma, profilePic, statusText)
        }

        takePictureIntent =
            registerForActivityResult(ActivityResultContracts.TakePicture()) { sucess: Boolean ->
                if (sucess) {
                    val image = BitmapFactory.decodeFile(currentPhotoPath)
                    mSecurityPreferences.storeBitmap(PIC_TO_CROP, image)
                    val photoCropIntent = Intent(
                        this, EditPictureActivity::class.java
                    )
                    startActivityForResult(photoCropIntent, 200)
                }
            }

        buttonPic.setOnClickListener {
            if (cardView.visibility == View.VISIBLE) {
                cardView.visibility = View.GONE
            } else {
                cardView.visibility = View.VISIBLE
            }
        }
        cameraBut.setOnClickListener {

            if (Permissao.validarPermissoes(this, 1)) {
                val cameraIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
                cameraIntent.also { takePictureIntent ->
                    takePictureIntent.resolveActivity(packageManager)?.also {

                        val file = try {
                            createImageFile()
                        } catch (ex: IOException) {
                            null
                        }
                        file.also {
                            val photoURI: Uri = FileProvider.getUriForFile(
                                this,
                                "com.ceep.id.fileprovider",
                                it!!
                            )
                            takePictureIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                            takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI)
                            startActivityForResult(takePictureIntent, CAMERA_REQUEST)
                        }

                    }
                }
                cardView.visibility = View.GONE
            } else {
                Toast.makeText(this, "Permita acesso a câmera primeiro.", Toast.LENGTH_LONG).show()
            }
        }
        galeriaBut.setOnClickListener {
            openGalleryForImage()
            cardView.visibility = View.GONE
        }
        profilePic.setOnClickListener {
            startActivity(Intent(this, ViewPictureActivity::class.java))
        }
        buttonRefresh.setOnClickListener {
            val rotate = RotateAnimation(
                0F,
                360F, Animation.RELATIVE_TO_SELF, 0.5f, Animation.RELATIVE_TO_SELF, 0.5f
            )
            rotate.duration = 800
            rotate.interpolator = LinearInterpolator()
            buttonRefresh.startAnimation(rotate)
            conection = checkConection()
            if (conection == 0 || conection == 2) {
                getData(textName, textTurma, profilePic, statusText)
                adView.loadAd(AdRequest.Builder().build())
            }
        }

    }

    override fun onBackPressed() {
        this.finishAffinity()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (resultCode == RESULT_OK && requestCode == CAMERA_REQUEST) {
            val image = BitmapFactory.decodeFile(currentPhotoPath)

            mSecurityPreferences.storeBitmap(PIC_TO_CROP, image)
            val photoCropIntent = Intent(
                this, EditPictureActivity::class.java
            )
            startActivityForResult(photoCropIntent, CROP_IMAGE_REQUEST)
        }

        if (resultCode == RESULT_OK && requestCode == PICK_IMAGE_REQUEST) {

            val image = MediaStore.Images.Media.getBitmap(contentResolver, data?.data)

            mSecurityPreferences.storeBitmap(PIC_TO_CROP, image)
            val photoCropIntent = Intent(
                this, EditPictureActivity::class.java
            )
            startActivityForResult(photoCropIntent, CROP_IMAGE_REQUEST)
        }

        if (resultCode == RESULT_OK && requestCode == CROP_IMAGE_REQUEST) {
            val profilePic = findViewById<ImageView>(R.id.profile_pic)
            profilePic.visibility = View.INVISIBLE
            val photoButton = findViewById<FloatingActionButton>(R.id.button_photo)
            photoButton.visibility = View.INVISIBLE
            val progressBar = findViewById<ProgressBar>(R.id.progress)
            progressBar.visibility = View.VISIBLE
            val image = mSecurityPreferences.getBitmap(PIC_TO_REVIEW)!!

            val imageP = InputImage.fromBitmap(image, 0)
            val options = FaceDetectorOptions.Builder()
                .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_ACCURATE)
                .setLandmarkMode(FaceDetectorOptions.LANDMARK_MODE_ALL)
                .setClassificationMode(FaceDetectorOptions.CLASSIFICATION_MODE_ALL)
                .setContourMode(FaceDetectorOptions.CONTOUR_MODE_ALL)
                .build()

            val detector = FaceDetection.getClient(options)

            detector.process(imageP).addOnSuccessListener {

                when (it.size) {
                    0 -> {
                        Toast.makeText(
                            this@MainScreen,
                            "Escolha uma imagem nitida que contenha o seu rosto.",
                            Toast.LENGTH_LONG
                        ).show()
                        progressBar.visibility = View.GONE
                        photoButton.visibility = View.VISIBLE
                        profilePic.visibility = View.VISIBLE
                        mSecurityPreferences.remove(PIC_TO_REVIEW)
                    }
                    1 -> {
                        ///MediaStore.Images.Media.getBitmap(this.contentResolver, data?.data)

                        val baos = ByteArrayOutputStream()
                        image.compress(Bitmap.CompressFormat.JPEG, 70, baos)
                        val b = baos.toByteArray()

                        //Salvando a imagem no banco de dados (Firebase)
                        val imagemRef = storageReference!!.child("imagens")
                            .child("alunos").child(idUsuario)
                            .child("fotoPerfil.jpeg")

                        val uploadTask = imagemRef.putBytes(b)
                        uploadTask.addOnFailureListener {
                            Toast.makeText(
                                this@MainScreen,
                                "Ocorreu um erro aao fazer o upload da imagem.", Toast.LENGTH_SHORT
                            ).show()
                            progressBar.visibility = View.GONE
                            photoButton.visibility = View.VISIBLE
                            profilePic.visibility = View.VISIBLE
                            mSecurityPreferences.remove(PIC_TO_REVIEW)
                        }.addOnSuccessListener {
                            val roundDrawable =
                                RoundedBitmapDrawableFactory.create(resources, image)
                            roundDrawable.cornerRadius = 49F
                            profilePic.setImageDrawable(roundDrawable)
                            profilePic.visibility = View.VISIBLE
                            Toast.makeText(
                                this@MainScreen,
                                "Imagem salva.", Toast.LENGTH_SHORT
                            ).show()
                            mSecurityPreferences.storeBitmap(PIC_PERFIL, image)
                            progressBar.visibility = View.GONE
                            photoButton.visibility = View.VISIBLE
                            profilePic.visibility = View.VISIBLE
                            mSecurityPreferences.remove(PIC_TO_REVIEW)
                        }
                    }
                    else -> {
                        Toast.makeText(
                            this@MainScreen,
                            "Escolha uma imagem em que voce esteja sozinho.",
                            Toast.LENGTH_LONG
                        ).show()
                        progressBar.visibility = View.GONE
                        photoButton.visibility = View.VISIBLE
                        profilePic.visibility = View.VISIBLE
                        mSecurityPreferences.remove(PIC_TO_REVIEW)
                    }
                }

            }.addOnFailureListener {
                Toast.makeText(this, "Erro", Toast.LENGTH_LONG).show()
                profilePic.visibility = View.GONE
                photoButton.visibility = View.VISIBLE
                profilePic.visibility = View.VISIBLE
                mSecurityPreferences.remove(PIC_TO_REVIEW)
            }
        }
    }

    private fun openGalleryForImage() {
        val galleryIntent = Intent(
            Intent.ACTION_PICK,
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI
        )
        galleryIntent.type = "image/*"

        startActivityForResult(galleryIntent, PICK_IMAGE_REQUEST)
    }

    private fun checkConection(): Int {
        return if (Build.VERSION.SDK_INT >= M) {
            val isOnline = isOnline(this@MainScreen)
            if (isOnline) {
                0
            } else {
                findViewById<TextView>(R.id.status_text).text = "Sem conexão com a internet."
                1
            }
        } else {
            2
        }
    }

    private fun getData(
        textName: TextView,
        textTurma: TextView,
        profilePic: ImageView,
        statusText: TextView
    ) {
        ///Nome
        usuarioRef?.child("usuarios/${idUsuario}/nome")?.get()?.addOnSuccessListener {
            nomeUsuario = it.value.toString()
            textName.text = nomeUsuario
        }

        ///Turma
        usuarioRef?.child("usuarios/${idUsuario}/turma")?.get()?.addOnSuccessListener { it ->

            turmaUsuario = it.value.toString()

            usuarioRef?.child("usuarios/${idUsuario}/sala")?.get()?.addOnSuccessListener {

                salaUsuario = it.value as String
                val turmaFormatada = "$turmaUsuario - $salaUsuario"
                textTurma.text = turmaFormatada
            }
        }
        ///Foto
        if (mSecurityPreferences.getBitmap(PIC_PERFIL) == null) {
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
                            PIC_PERFIL,
                            profilePic.drawable.toBitmap()
                        )
                    }?.addOnFailureListener {
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
                                    PIC_PERFIL,
                                    profilePic.drawable.toBitmap()
                                )
                            }, 5000)
                        }
                    }
            } catch (e: Exception) {

            }
        } else {
            val image = mSecurityPreferences.getBitmap(PIC_PERFIL)
            val roundDrawable = RoundedBitmapDrawableFactory.create(resources, image)
            roundDrawable.cornerRadius = 49F
            profilePic.setImageDrawable(roundDrawable)
        }
        ///Status
        val postReference = usuarioRef?.child("usuarios/${idUsuario}/liberado")
        val postListener = object : ValueEventListener {

            override fun onDataChange(snapshot: DataSnapshot) {
                val post = snapshot.value

                if (post == true) {
                    findViewById<ImageView>(R.id.led_indicator).setImageDrawable(
                        ContextCompat.getDrawable(
                            this@MainScreen, R.drawable.green_ball
                        )
                    )

                    val date = Calendar.getInstance()
                    val hour = date.get(Calendar.HOUR_OF_DAY)
                    val minute = date.get(Calendar.MINUTE)

                    when {
                        hour <= 9 && minute > 9 -> statusText.text =
                            "Liberado. Atualizado às 0$hour:$minute."
                        hour > 9 && minute <= 9 -> statusText.text =
                            "Liberado. Atualizado às $hour:0$minute."
                        hour <= 9 && minute <= 9 -> statusText.text =
                            "Liberado. Atualizado às 0$hour:0$minute."
                        else -> statusText.text = "Liberado. Atualizado às $hour:$minute."
                    }
                } else if (post == null || post == false) {
                    findViewById<ImageView>(R.id.led_indicator).setImageDrawable(
                        ContextCompat.getDrawable(
                            this@MainScreen, R.drawable.red_ball
                        )
                    )

                    val date = Calendar.getInstance()
                    val hour = date.get(Calendar.HOUR_OF_DAY)
                    val minutes = date.get(Calendar.MINUTE)

                    usuarioRef?.child("usuarios/${idUsuario}/sala")?.get()
                        ?.addOnSuccessListener { it ->
                            if (it.toString().contains('V')) {
                                when (hour) {
                                    in 13..17 -> {
                                        statusText.text = "Em aula"
                                    }
                                    18 -> {
                                        when (minutes) {
                                            in 0..20 -> {
                                                statusText.text = "Em aula"
                                            }
                                        }
                                    }
                                    else -> {
                                        statusText.text = "Fora do horário de aula"
                                    }
                                }
                            } else {
                                when (hour) {
                                    in 7..11 -> {
                                        statusText.text = "Em aula"
                                    }
                                    12 -> {
                                        when (minutes) {
                                            in 0..20 -> {
                                                statusText.text = "Em aula"
                                            }
                                        }
                                    }
                                    else -> {
                                        statusText.text = "Fora do horário de aula"
                                    }
                                }
                            }
                        }
                }
            }

            override fun onCancelled(error: DatabaseError) {
                findViewById<ImageView>(R.id.led_indicator).setImageDrawable(
                    ContextCompat.getDrawable(
                        this@MainScreen, R.drawable.red_ball
                    )
                )
                statusText.text = "Erro ao receber dados."
            }

        }
        postReference?.addValueEventListener(postListener)
    }

    @RequiresApi(M)
    fun isOnline(context: Context): Boolean {
        val connectivityManager =
            context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val capabilities =
            connectivityManager.getNetworkCapabilities(connectivityManager.activeNetwork)
        if (capabilities != null) {
            when {
                capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> {
                    return true
                }
                capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> {
                    return true
                }
                capabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) -> {
                    return true
                }
            }
        }
        return false
    }

    private fun theme() {

        val backgroundView = findViewById<NestedScrollView>(R.id.background_scrollview)
        val toolbar = findViewById<ImageView>(R.id.toolbar)
        val backgroundPic = findViewById<ImageView>(R.id.background_pic)
        val shapeStatus = findViewById<ImageView>(R.id.shape_status)
        val shapeNome = findViewById<ImageView>(R.id.shape_nome)
        val statusText = findViewById<TextView>(R.id.status_text)
        val buttonRefresh = findViewById<ImageButton>(R.id.refresh_button)
        val cardView = findViewById<CardView>(R.id.cardView)
        val linha = findViewById<View>(R.id.linhaCardView)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            if (resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK == Configuration.UI_MODE_NIGHT_YES) {
                backgroundView.setBackgroundColor(getColor(R.color.background_dark))
                toolbar.setImageDrawable(
                    AppCompatResources.getDrawable(
                        this,
                        R.drawable.main_toolbar_dark
                    )
                )
                cardView.setCardBackgroundColor(getColor(R.color.dark_blue))
                linha.setBackgroundColor(getColor(R.color.background_dark))
                buttonRefresh.setColorFilter(getColor(R.color.white))
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
                statusText.setTextColor(ResourcesCompat.getColor(resources, R.color.white, theme))
            } else {
                backgroundView.setBackgroundColor(getColor(R.color.background_light))
                cardView.setCardBackgroundColor(getColor(R.color.white))
                linha.setBackgroundColor(getColor(R.color.background_light))
                buttonRefresh.setColorFilter(getColor(R.color.black))
                toolbar.setImageDrawable(
                    AppCompatResources.getDrawable(
                        this,
                        R.drawable.main_toolbar
                    )
                )
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
                statusText.setTextColor(ResourcesCompat.getColor(resources, R.color.black, theme))
            }
        }
    }

    private fun createNotificationChannel() {
        // Create the NotificationChannel, but only on API 26+ because
        // the NotificationChannel class is new and not in the support library
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = "Situaçao"
            val descriptionText = "Situaçao do aluno"
            val importance = NotificationManager.IMPORTANCE_HIGH
            val channel = NotificationChannel(CHANNEL_ID, name, importance).apply {
                description = descriptionText
            }
            // Register the channel with the system
            val notificationManager: NotificationManager =
                getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    @Throws(IOException::class)
    private fun createImageFile(): File {
        // Create an image file name
        val timeStamp: String = SimpleDateFormat("yyyyMMdd_HHmmss").format(Date())
        val storageDir: File? = applicationContext.cacheDir
        return File.createTempFile(
            "CEEPiD_${timeStamp}_", /* prefix */
            ".jpg", /* suffix */
            storageDir /* directory */
        ).apply {
            // Save a file: path for use with ACTION_VIEW intents
            currentPhotoPath = absolutePath
        }
    }
}
