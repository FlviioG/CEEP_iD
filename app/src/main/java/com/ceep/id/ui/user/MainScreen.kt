package com.ceep.id.ui.user

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.PendingIntent.FLAG_IMMUTABLE
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
import android.util.Log
import android.view.View
import android.view.WindowManager
import android.view.animation.Animation
import android.view.animation.LinearInterpolator
import android.view.animation.RotateAnimation
import android.widget.*
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.content.res.AppCompatResources
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.core.content.res.ResourcesCompat
import androidx.core.graphics.drawable.RoundedBitmapDrawableFactory
import androidx.core.graphics.drawable.toBitmap
import com.bumptech.glide.Glide
import com.ceep.id.R
import com.ceep.id.infra.NotificationService
import com.ceep.id.infra.Permissao
import com.ceep.id.infra.SecurityPreferences
import com.ceep.id.infra.Usuario
import com.ceep.id.infra.auth.FirebaseConfig
import com.ceep.id.ui.MainActivity
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
import java.util.*

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
    private var conection: Int? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main_screen)

        createNotificationChannel()

        window.setFlags(
            WindowManager.LayoutParams.FLAG_SECURE,
            WindowManager.LayoutParams.FLAG_SECURE
        )

        Permissao.validarPermissoes(permissoesNecessarias, this, 1)
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
        mSecurityPreferences.storeString("idU", acct?.id.toString())

        val statusText = findViewById<TextView>(R.id.status_text)
        val buttonPic = findViewById<FloatingActionButton>(R.id.button_photo)
        val profilePic = findViewById<ImageView>(R.id.profile_pic)
        val buttonPerfil = findViewById<ImageView>(R.id.button_perfil)
        val textName = findViewById<TextView>(R.id.textNome)
        val textTurma = findViewById<TextView>(R.id.textTurma)
        val buttonRefresh = findViewById<ImageButton>(R.id.refresh_button)

        theme()

        conection = checkConection()
        if (conection == 0 || conection == 2) {
            getData(textName, textTurma, profilePic, statusText)
        }

        buttonPic.setOnClickListener {
            openGalleryForImage()
        }
        buttonPerfil.setOnClickListener {
            Usuario().liberar(idUsuario)
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
            }
        }
    }

    override fun onBackPressed() {
        this.finishAffinity()
    }

    override fun onStop() {
        startService(Intent(this, NotificationService::class.java))
        super.onStop()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        val PICK_IMAGE_REQUEST = 100
        val CROP_IMAGE_REQUEST = 200

        if (resultCode == RESULT_OK && requestCode == PICK_IMAGE_REQUEST) {
            val image = MediaStore.Images.Media.getBitmap(contentResolver, data?.data)
            mSecurityPreferences.storeBitmap("picToCrop", image)
            val photoCropIntent = Intent(
                this, EditPictureActivity::class.java
            )
            startActivityForResult(photoCropIntent, CROP_IMAGE_REQUEST)

        }

        if (resultCode == RESULT_OK && requestCode == CROP_IMAGE_REQUEST) {
            val profilePic = findViewById<ImageView>(R.id.profile_pic)
            profilePic.visibility = View.INVISIBLE
            findViewById<ProgressBar>(R.id.progress).visibility = View.VISIBLE
            val image = mSecurityPreferences.getBitmap("fotoPerfil")!!

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
                        findViewById<ProgressBar>(R.id.progress).visibility = View.GONE
                        profilePic.visibility = View.VISIBLE
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
                            profilePic.visibility = View.VISIBLE
                            findViewById<ProgressBar>(R.id.progress).visibility = View.GONE
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
                            mSecurityPreferences.storeBitmap("fotoPerfil", image)
                            findViewById<ProgressBar>(R.id.progress).visibility = View.GONE
                            profilePic.visibility = View.VISIBLE
                        }
                    }
                    else -> {
                        Toast.makeText(
                            this@MainScreen,
                            "Escolha uma imagem em que voce esteja sozinho.",
                            Toast.LENGTH_LONG
                        ).show()
                        findViewById<ProgressBar>(R.id.progress).visibility = View.GONE
                    }
                }

            }.addOnFailureListener {
                Toast.makeText(this, "Erro", Toast.LENGTH_LONG).show()
                profilePic.visibility = View.VISIBLE
                findViewById<ProgressBar>(R.id.progress).visibility = View.INVISIBLE
                profilePic.visibility = View.VISIBLE
            }
        }
    }

    private fun openGalleryForImage() {
        val PICK_IMAGE_REQUEST = 100
        val photoPickerIntent = Intent(
            Intent.ACTION_PICK,
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI
        )
        photoPickerIntent.type = "image/*"
        startActivityForResult(photoPickerIntent, PICK_IMAGE_REQUEST)
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
                textTurma.text = "$turmaUsuario - ${it.value.toString()}"
            }


        }
        ///Foto
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
        ///Status
        val postReference = usuarioRef?.child("usuarios/${idUsuario}/liberado")
        val postListener = object : ValueEventListener {

            override fun onDataChange(snapshot: DataSnapshot) {
                val post = snapshot.value

                val contentIntent = PendingIntent.getActivity(
                    this@MainScreen,
                    0,
                    Intent(this@MainScreen, MainActivity::class.java),
                    FLAG_IMMUTABLE
                )

                val CHANNEL_ID = "default_id"
                val builder = NotificationCompat.Builder(this@MainScreen, CHANNEL_ID)
                    .setSmallIcon(R.mipmap.ic_launcher)
                    .setContentTitle("Situação:")
                    .setContentText("Liberado!")
                    .setPriority(NotificationCompat.PRIORITY_MAX)
                    .setDefaults(NotificationCompat.DEFAULT_ALL)
                    .setContentIntent(contentIntent)
                    .setAutoCancel(true)

                if (post == true) {
                    findViewById<ImageView>(R.id.led_indicator).setImageDrawable(
                        ContextCompat.getDrawable(
                            this@MainScreen, R.drawable.green_ball
                        )
                    )

                    val date = Calendar.getInstance()
                    val hour = date.get(Calendar.HOUR_OF_DAY)
                    val minute = date.get(Calendar.MINUTE)

                    statusText.text = "Liberado. Atualizado às $hour:$minute."

                    with(NotificationManagerCompat.from(this@MainScreen)) {
                        // notificationId is a unique int for each notification that you must define
                        notify(1, builder.build())
                    }
                } else if (post == null || post == false) {
                    findViewById<ImageView>(R.id.led_indicator).setImageDrawable(
                        ContextCompat.getDrawable(
                            this@MainScreen, R.drawable.red_ball
                        )
                    )
                    statusText.text = "Em aula/Fora do horario de aula"
                    with(NotificationManagerCompat.from(this@MainScreen)) {
                        // notificationId is a unique int for each notification that you must define
                        cancel(1)
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
                    Log.i("Internet", "NetworkCapabilities.TRANSPORT_CELLULAR")
                    return true
                }
                capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> {
                    Log.i("Internet", "NetworkCapabilities.TRANSPORT_WIFI")
                    return true
                }
                capabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) -> {
                    Log.i("Internet", "NetworkCapabilities.TRANSPORT_ETHERNET")
                    return true
                }
            }
        }
        return false
    }

    private fun theme() {

        val backgroundView = findViewById<ScrollView>(R.id.background_scrollview)
        val toolbar = findViewById<ImageView>(R.id.toolbar)
        val backgroundPic = findViewById<ImageView>(R.id.background_pic)
        val shapeStatus = findViewById<ImageView>(R.id.shape_status)
        val shapeNome = findViewById<ImageView>(R.id.shape_nome)
        val statusText = findViewById<TextView>(R.id.status_text)
        val buttonRefresh = findViewById<ImageButton>(R.id.refresh_button)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            if (resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK == Configuration.UI_MODE_NIGHT_YES) {
                backgroundView.setBackgroundColor(getColor(R.color.background_dark))
                toolbar.setImageDrawable(
                    AppCompatResources.getDrawable(
                        this,
                        R.drawable.main_toolbar_dark
                    )
                )
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
            val name = "Sitaçao"
            val descriptionText = "Situaçao do aluno"
            val CHANNEL_ID = "default_id"
            val importance = NotificationManager.IMPORTANCE_DEFAULT
            val channel = NotificationChannel(CHANNEL_ID, name, importance).apply {
                description = descriptionText
            }
            // Register the channel with the system
            val notificationManager: NotificationManager =
                getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }
}

///cropView.setBitmap(image)
//    cropView.addOnCropListener(object : OnCropListener {
//        override fun onFailure(e: Exception) {
//            Toast.makeText(this@MainScreen, "Erro ao carregar imagem.", Toast.LENGTH_SHORT).show()
//        }
//
//        override fun onSuccess(bitmap: Bitmap) {
//            cropView.isOffFrame()
//            cropView.crop()
//
//        }
