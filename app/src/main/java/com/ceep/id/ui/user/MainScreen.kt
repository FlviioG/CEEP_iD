package com.ceep.id.ui.user

import android.app.Activity
import android.app.ActivityManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.view.View
import android.view.WindowManager
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
import com.ceep.id.R
import com.ceep.id.infra.Constants
import com.ceep.id.infra.Constants.DATA.CHANNEL_ID
import com.ceep.id.infra.Constants.DATA.FIRST_OPENING
import com.ceep.id.infra.Constants.DATA.PIC_PERFIL
import com.ceep.id.infra.Constants.DATA.PIC_TO_CROP
import com.ceep.id.infra.Constants.DATA.USER_ID
import com.ceep.id.infra.Constants.REQUESTS.CAMERA_REQUEST
import com.ceep.id.infra.Constants.REQUESTS.CROP_IMAGE_REQUEST
import com.ceep.id.infra.Constants.REQUESTS.PICK_IMAGE_REQUEST
import com.ceep.id.infra.Permissao
import com.ceep.id.infra.SecurityPreferences
import com.ceep.id.infra.Usuario
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
import com.skydoves.balloon.*
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
    private lateinit var balloon: Balloon
    private lateinit var mSecurityPreferences: SecurityPreferences
    private lateinit var cropImageIntent: ActivityResultLauncher<Intent>
    private lateinit var pickImageIntent: ActivityResultLauncher<Intent>
    private lateinit var cameraIntent: ActivityResultLauncher<Intent>


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main_screen)

        createNotificationChannel()

        with(window) {
//            setFlags(FLAG_SECURE, FLAG_SECURE)
            addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            attributes = attributes.also {
                it.screenBrightness = WindowManager.LayoutParams.BRIGHTNESS_OVERRIDE_NONE
            }
        }

        mSecurityPreferences = SecurityPreferences(this)
        storageReference = FirebaseConfig.getFirebaseStorage()
        usuarioRef = FirebaseConfig.getFirabaseDatabase()
        val acct = GoogleSignIn.getLastSignedInAccount(this)
        if (acct != null) {
            idUsuario = acct.id!!
        }
        ///"ca-app-pub-6136253738426934/1523049245"
        val adView = findViewById<AdView>(R.id.ad)
        adView.loadAd(AdRequest.Builder().build())
        mSecurityPreferences.storeString(USER_ID, acct?.id.toString())

        val buttonPic = findViewById<FloatingActionButton>(R.id.button_photo)
        val profilePic = findViewById<ImageView>(R.id.profile_pic)
        val buttonRefresh = findViewById<ImageButton>(R.id.refresh_button)
        val cameraBut = findViewById<TextView>(R.id.cameraBut)
        val galeriaBut = findViewById<TextView>(R.id.galeriaBut)
        val cardView = findViewById<CardView>(R.id.cardView)

        conection = checkConection()
        if (conection == 0 || conection == 2) {
            update()
        }

        balloon = Balloon.Builder(applicationContext)
            .setArrowSize(10)
            .setArrowOrientation(ArrowOrientation.TOP)
            .setIsVisibleArrow(true)
            .setArrowPositionRules(ArrowPositionRules.ALIGN_ANCHOR)
            .setArrowPosition(0.5f)
            .setWidthRatio(0.4f)
            .setWidth(BalloonSizeSpec.WRAP)
            .setHeight(70)
            .setMarginTop(6)
            .setText("Adicione uma foto")
            .setTextSize(15f)
            .setCornerRadius(20f)
            .setAutoDismissDuration(3500)
            .setTextColor(ContextCompat.getColor(this, R.color.background_dark))
            .setBackgroundColor(ContextCompat.getColor(this, R.color.rose))
            .setBalloonAnimation(BalloonAnimation.OVERSHOOT)
            .build()

        cropImageIntent = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                val image = mSecurityPreferences.getBitmap(Constants.DATA.PIC_TO_REVIEW)!!
                facialRecognition(image)
            }
        }
        pickImageIntent = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if(result.resultCode == RESULT_OK) {
                val image = MediaStore.Images.Media.getBitmap(contentResolver, result.data?.data)
                mSecurityPreferences.storeBitmap(PIC_TO_CROP, image)
                activityResult(CROP_IMAGE_REQUEST)
            }
        }
        cameraIntent = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if(result.resultCode == RESULT_OK) {
                val image = BitmapFactory.decodeFile(currentPhotoPath)
                mSecurityPreferences.storeBitmap(PIC_TO_CROP, image)
                activityResult(CROP_IMAGE_REQUEST)
            }
        }

        buttonPic.setOnClickListener {
            if (cardView.visibility == View.VISIBLE) {
                cardView.visibility = View.GONE
            } else {
                if (mSecurityPreferences.getInt(FIRST_OPENING) == 0) {
                    balloon.showAlignTop(buttonPic)
                    mSecurityPreferences.storeInt(FIRST_OPENING, 1)
                }
                cardView.visibility = View.VISIBLE
            }
        }
        cameraBut.setOnClickListener {
            if (Permissao.validarPermissoes(this, 1)) {
               activityResult(CAMERA_REQUEST)
                cardView.visibility = View.GONE
            } else {
                Toast.makeText(this, "Permita acesso a câmera primeiro.", Toast.LENGTH_SHORT).show()
            }
        }
        galeriaBut.setOnClickListener {
            activityResult(PICK_IMAGE_REQUEST)
            cardView.visibility = View.GONE
        }
        profilePic.setOnClickListener {
            if (mSecurityPreferences.getBitmap(PIC_PERFIL) == null) {
                balloon.showAlignTop(buttonPic)
            } else {
                startActivity(Intent(this, ViewPictureActivity::class.java))
                overridePendingTransition(R.anim.to_up_1, R.anim.to_up_2)
            }
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
                update(1)
                adView.loadAd(AdRequest.Builder().build())
            }
        }
    }

    override fun onBackPressed() {
        finishAffinity()
    }

    private fun activityResult(code: Int) {
        when(code) {
            CROP_IMAGE_REQUEST -> {
                val intent = Intent(this, EditPictureActivity::class.java)
                cropImageIntent.launch(intent)
            }
            PICK_IMAGE_REQUEST -> {
                val galleryIntent = Intent(
                    Intent.ACTION_PICK,
                    MediaStore.Images.Media.EXTERNAL_CONTENT_URI
                )
                galleryIntent.type = "image/*"

                pickImageIntent.launch(galleryIntent)
            }
            CAMERA_REQUEST -> {
                val cIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
                cIntent.also { takePictureIntent ->
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
                            cameraIntent.launch(takePictureIntent)
                        }

                    }
                }
            }
        }
    }

    private fun update(refresh: Int = 0) {
        val statusText = findViewById<TextView>(R.id.status_text)
        val profilePic = findViewById<ImageView>(R.id.profile_pic)
        val textName = findViewById<TextView>(R.id.textNome)
        val textTurma = findViewById<TextView>(R.id.textTurma)

        getData(
            textName,
            textTurma,
            profilePic,
            statusText,
            refresh
        )
    }

    private fun facialRecognition(image: Bitmap) {
        val profilePic = findViewById<ImageView>(R.id.profile_pic)
        profilePic.visibility = View.INVISIBLE
        val profilePicView = findViewById<CardView>(R.id.profile_pic_view)
        profilePicView.visibility = View.INVISIBLE
        val photoButton = findViewById<FloatingActionButton>(R.id.button_photo)
        photoButton.visibility = View.INVISIBLE
        val progressBar = findViewById<ProgressBar>(R.id.progress)
        progressBar.visibility = View.VISIBLE

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
                    profilePicView.visibility = View.VISIBLE
                    mSecurityPreferences.remove(Constants.DATA.PIC_TO_REVIEW)
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
                            "Ocorreu um erro aao fazer o upload da imagem.",
                            Toast.LENGTH_SHORT
                        ).show()
                        progressBar.visibility = View.GONE
                        photoButton.visibility = View.VISIBLE
                        profilePic.visibility = View.VISIBLE
                        profilePicView.visibility = View.VISIBLE
                        mSecurityPreferences.remove(Constants.DATA.PIC_TO_REVIEW)
                    }.addOnSuccessListener {
                        profilePic.setImageBitmap(image)
                        profilePicView.visibility = View.VISIBLE
                        profilePic.visibility = View.VISIBLE
                        Toast.makeText(
                            this@MainScreen,
                            "Imagem salva.", Toast.LENGTH_SHORT
                        ).show()
                        mSecurityPreferences.storeBitmap(PIC_PERFIL, image)
                        progressBar.visibility = View.GONE
                        photoButton.visibility = View.VISIBLE
                        profilePic.visibility = View.VISIBLE
                        profilePicView.visibility = View.VISIBLE
                        mSecurityPreferences.remove(Constants.DATA.PIC_TO_REVIEW)
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
                    profilePicView.visibility = View.VISIBLE
                    mSecurityPreferences.remove(Constants.DATA.PIC_TO_REVIEW)
                }
            }

        }.addOnFailureListener {
            Toast.makeText(this, "Erro", Toast.LENGTH_LONG).show()
            profilePic.visibility = View.GONE
            photoButton.visibility = View.VISIBLE
            profilePic.visibility = View.VISIBLE
            profilePicView.visibility = View.VISIBLE
            profilePicView.visibility = View.VISIBLE
            mSecurityPreferences.remove(Constants.DATA.PIC_TO_REVIEW)
        }
    }

    private fun getData(
        textName: TextView,
        textTurma: TextView,
        profilePic: ImageView,
        statusText: TextView,
        refreshCode: Int
    ) {

        if (refreshCode == 0) {
            ///Nome
            textName.text = mSecurityPreferences.getString(Constants.USER.NAME)

            ///Turma
            val turma = mSecurityPreferences.getString(Constants.USER.TURMA)
            val sala = mSecurityPreferences.getString(Constants.USER.SALA)
            val format = "$turma - $sala"
            textTurma.text = format

            ///Foto
            val foto = mSecurityPreferences.getBitmap(PIC_PERFIL)
            if (foto != null) {
                profilePic.setImageBitmap(foto)
            } else {
                profilePic.setImageDrawable(
                    AppCompatResources.getDrawable(
                        this,
                        R.drawable.perfil_empty
                    )
                )
            }
        } else if (refreshCode == 1) {
            retrieveData()
        }

        ///Status

        val postReference = usuarioRef?.child("usuarios/${idUsuario}/liberado")
        val postListener = object : ValueEventListener {

            override fun onDataChange(snapshot: DataSnapshot) {
                val post = snapshot.value
                val textSituacao = findViewById<TextView>(R.id.textSituacao)

                if (post == true) {
                    findViewById<ImageView>(R.id.shape_status).backgroundTintList =
                        AppCompatResources.getColorStateList(this@MainScreen, R.color.green)
                    statusText.setTextColor(Color.WHITE)
                    textSituacao.setTextColor(Color.WHITE)
                    val text = "Liberado. Atualizado às ${Usuario().getHour()}."
                    statusText.text = text

                } else if (post == null || post == false) {
                    val nightMode =
                        resources.configuration.uiMode.and(Configuration.UI_MODE_NIGHT_MASK)

                    if (nightMode == Configuration.UI_MODE_NIGHT_YES) {
                        findViewById<ImageView>(R.id.shape_status).backgroundTintList =
                            AppCompatResources.getColorStateList(this@MainScreen, R.color.dark_blue)
                        statusText.setTextColor(Color.WHITE)
                        textSituacao.setTextColor(Color.WHITE)
                    } else {
                        findViewById<ImageView>(R.id.shape_status).backgroundTintList =
                            AppCompatResources.getColorStateList(this@MainScreen, R.color.white)
                        statusText.setTextColor(Color.BLACK)
                        textSituacao.setTextColor(Color.BLACK)
                    }

                    usuarioRef?.child("usuarios/${idUsuario}/sala")?.get()
                        ?.addOnSuccessListener { it ->
                            state(it, statusText)
                        }
                }
            }

            override fun onCancelled(error: DatabaseError) {

                val textSituacao = findViewById<TextView>(R.id.textSituacao)

                findViewById<ImageView>(R.id.shape_status).backgroundTintList =
                    AppCompatResources.getColorStateList(this@MainScreen, R.color.red)
                statusText.setTextColor(Color.WHITE)
                textSituacao.setTextColor(ContextCompat.getColor(this@MainScreen, R.color.white))
                statusText.text = getString(R.string.erro)
            }

        }
        postReference?.addValueEventListener(postListener)
    }

    private fun state(it: DataSnapshot, statusText: TextView) {
        val date = Calendar.getInstance()
        val day = date.get(Calendar.DAY_OF_WEEK)
        val hour = date.get(Calendar.HOUR_OF_DAY)
        val minutes = date.get(Calendar.MINUTE)

        when (day) {
            in 2..6 -> {
                if (it.toString().contains('V')) {
                    when (hour) {
                        in 13..17 -> {
                            statusText.text = getString(R.string.em_aula)
                        }
                        18 -> {
                            when (minutes) {
                                in 0..20 -> {
                                    statusText.text = getString(R.string.em_aula)
                                }
                                else -> {
                                    statusText.text = getString(R.string.fora_do_horario)
                                }
                            }
                        }
                        else -> {
                            statusText.text = getString(R.string.fora_do_horario)
                        }
                    }
                } else {
                    when (hour) {
                        in 7..11 -> {
                            statusText.text = getString(R.string.em_aula)
                        }
                        12 -> {
                            when (minutes) {
                                in 0..20 -> {
                                    statusText.text = getString(R.string.em_aula)
                                }
                                else -> {
                                statusText.text = getString(R.string.fora_do_horario)
                            }
                            }
                        }
                        else -> {
                            statusText.text = getString(R.string.fora_do_horario)
                        }
                    }
                }
            }
            else -> {
                statusText.text = getString(R.string.fora_do_horario)
            }
        }
    }

    private fun retrieveData() {
        val rD = Thread {
            ///Nome
            usuarioRef?.child("usuarios/${idUsuario}/nome")?.get()?.addOnSuccessListener { nome ->

                if (nome.value == null) {
                    val t = getSystemService(ACTIVITY_SERVICE) as ActivityManager
                    t.clearApplicationUserData()
                }
                mSecurityPreferences.storeString(Constants.USER.NAME, nome.value.toString())

                ///Turma
                usuarioRef?.child("usuarios/${idUsuario}/turma")?.get()
                    ?.addOnSuccessListener { turma ->

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
                                                PIC_PERFIL,
                                                bitmap
                                            )
                                            update()
                                        }?.addOnFailureListener {
                                            mSecurityPreferences.remove(PIC_PERFIL)
                                            mSecurityPreferences.storeInt(FIRST_OPENING, 0)
                                            update()
                                        }
                                } catch (e: Exception) {
                                    Toast.makeText(this, "erro", Toast.LENGTH_LONG).show()
                                }
                            }
                    }
            }
        }
        rD.start()
    }

    private fun createNotificationChannel() {

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = "Situação"
            val descriptionText = "Situação do aluno"
            val importance = NotificationManager.IMPORTANCE_HIGH
            val channel = NotificationChannel(CHANNEL_ID, name, importance).apply {
                description = descriptionText
            }

            val notificationManager: NotificationManager =
                getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    @Throws(IOException::class)
    private fun createImageFile(): File {
        val timeStamp: String = SimpleDateFormat("ddMMyyyy_HHmmss", Locale.US).format(Date())
        val storageDir: File? = applicationContext.cacheDir
        return File.createTempFile(
            "CEEPiD_${timeStamp}_", /* prefix */
            ".jpg", /* suffix */
            storageDir /* directory */
        ).apply {
            currentPhotoPath = absolutePath
        }
    }

    private fun checkConection(): Int {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val isOnline = isOnline(this)
            if (isOnline) {
                0
            } else {
                findViewById<ImageView>(R.id.shape_status).backgroundTintList =
                    AppCompatResources.getColorStateList(this@MainScreen, R.color.red)
                findViewById<TextView>(R.id.status_text).setTextColor(Color.WHITE)
                findViewById<TextView>(R.id.textSituacao).setTextColor(ContextCompat.getColor(this, R.color.white))
                findViewById<TextView>(R.id.status_text).text = getString(R.string.internet)
                1
            }
        } else {
            2
        }
    }

    @RequiresApi(Build.VERSION_CODES.M)
    private fun isOnline(context: Context): Boolean {
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
}