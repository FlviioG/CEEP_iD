package com.ceep.id.ui


import android.app.ActivityManager
import android.app.KeyguardManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.drawable.BitmapDrawable
import android.hardware.biometrics.BiometricPrompt
import android.hardware.fingerprint.FingerprintManager
import android.net.Uri
import android.os.*
import android.provider.MediaStore
import android.util.Log
import android.view.View
import android.widget.*
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.graphics.BitmapCompat
import com.ceep.id.R
import com.ceep.id.infra.Constants
import com.ceep.id.infra.Constants.DATA.BASIC_INFORMATIONS
import com.ceep.id.infra.Constants.DATA.PIC_PERFIL
import com.ceep.id.infra.Constants.DATA.USER_ID
import com.ceep.id.infra.Constants.DATABASE.TERMO
import com.ceep.id.infra.Constants.DATABASE.TERMO_B
import com.ceep.id.infra.Constants.USER.NAME
import com.ceep.id.infra.Constants.USER.SALA
import com.ceep.id.infra.Constants.USER.TURMA
import com.ceep.id.infra.SecurityPreferences
import com.ceep.id.infra.Usuario
import com.ceep.id.infra.auth.FirebaseConfig
import com.ceep.id.infra.auth.GoogleSignInActivity
import com.ceep.id.ui.admin.MainScreenAdmin
import com.ceep.id.ui.user.MainScreen
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.SignInButton
import com.google.android.gms.common.api.ApiException
import com.google.firebase.FirebaseApp
import com.google.firebase.appcheck.FirebaseAppCheck
import com.google.firebase.appcheck.safetynet.SafetyNetAppCheckProviderFactory
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.database.DatabaseReference
import com.google.firebase.storage.StorageReference
import java.io.File
import java.net.URI
import java.net.URL
import java.util.*


class MainActivity : AppCompatActivity() {

    private lateinit var idUsuario: String
    private lateinit var photoUsuario: URL
    private lateinit var mSecurityPreferences: SecurityPreferences
    private var auth: FirebaseAuth? = null
    private lateinit var googleSignInClient: GoogleSignInClient
    private var usuarioRef: DatabaseReference? = null
    private var cancellationSignal: CancellationSignal? = null
    private var storageReference: StorageReference? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        mSecurityPreferences = SecurityPreferences(this)

        FirebaseApp.initializeApp(this)
        val firebaseAppCheck = FirebaseAppCheck.getInstance()
        firebaseAppCheck.installAppCheckProviderFactory(
            SafetyNetAppCheckProviderFactory.getInstance()
        )

        MobileAds.initialize(this) {}
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(getString(R.string.default_web_client_id))
            .requestEmail()
            .requestId()
            .build()
        googleSignInClient = GoogleSignIn.getClient(this, gso)
        auth = FirebaseConfig.getFirebaseAuth()
        auth = FirebaseConfig.getFirebaseAuth()
        usuarioRef = FirebaseConfig.getFirabaseDatabase()
        storageReference = FirebaseConfig.getFirebaseStorage()

        val signInButton = findViewById<SignInButton>(R.id.sign_in_button)

        val acct = GoogleSignIn.getLastSignedInAccount(this)
        if (acct != null) {
            idUsuario = acct.id!!
            photoUsuario = URL(acct.photoUrl.toString())
        }

        signInButton.setOnClickListener {
            signIn()
        }

    }

    override fun onStart() {
        super.onStart()

        if ((Build.VERSION.SDK_INT >= Build.VERSION_CODES.P
                    && mSecurityPreferences.getInt(BASIC_INFORMATIONS) == 1) && checkBiometricSupport()
        ) {

            val authenticationCallback: BiometricPrompt.AuthenticationCallback =
                object : BiometricPrompt.AuthenticationCallback() {

                    override fun onAuthenticationError(errorCode: Int, errString: CharSequence?) {
                        super.onAuthenticationError(errorCode, errString)
                        notifyUser("Erro de autenticação : $errString")
                        finish()
                    }

                    override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult?) {
                        super.onAuthenticationSucceeded(result)
                        updateUI()
                    }
                }

            val biometricPrompt = BiometricPrompt.Builder(this)
                .setTitle("Verificaçao biometrica")
                .setSubtitle("Desbloqueie seu app")
                .setNegativeButton(
                    "Cancelar",
                    this.mainExecutor
                ) { _, _ ->
                    notifyUser("Processo cancelado")
                    this.finishAffinity()
                }.build()

            biometricPrompt.authenticate(
                getCancellationSignal(),
                mainExecutor,
                authenticationCallback
            )
        } else if (mSecurityPreferences.getInt(BASIC_INFORMATIONS) == 1) {
            updateUI()
        }
    }

    private fun getCancellationSignal(): CancellationSignal {
        cancellationSignal = CancellationSignal()
        cancellationSignal?.setOnCancelListener {
            notifyUser("Autenticação cancelada pelo usuário")
        }
        return cancellationSignal as CancellationSignal
    }

    @RequiresApi(Build.VERSION_CODES.M)
    private fun checkBiometricSupport(): Boolean {
        val keyguardManager = getSystemService(Context.KEYGUARD_SERVICE) as KeyguardManager
        if (!keyguardManager.isDeviceSecure) {
            notifyUser("O leitor biométrico ainda não foi configurado")
            return false
        } else {
            return if (ActivityCompat.checkSelfPermission(
                    this,
                    android.Manifest.permission.USE_BIOMETRIC
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                notifyUser("Não há permissão para acessar o leitor biométrico")
                false
            } else {
                if (packageManager.hasSystemFeature(PackageManager.FEATURE_FINGERPRINT)) {
                    val finger: FingerprintManager =
                        getSystemService(FINGERPRINT_SERVICE) as FingerprintManager
                    finger.hasEnrolledFingerprints()
                } else false
            }
        }
    }

    private fun notifyUser(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }

    private fun updateUI() {

        val idU = mSecurityPreferences.getString(USER_ID)
        findViewById<SignInButton>(R.id.sign_in_button).visibility = View.GONE
        findViewById<ProgressBar>(R.id.progress).visibility = View.VISIBLE

        usuarioRef?.child("usuarios/${idU}/admin")?.get()?.addOnSuccessListener {
            if (it.value == true) {
                startActivity(Intent(this, MainScreenAdmin::class.java))
            }
        }

        if (idU != "" && mSecurityPreferences.getInt(BASIC_INFORMATIONS) == 1) {
            ///Nome
            usuarioRef?.child("usuarios/${idU}/nome")?.get()?.addOnSuccessListener { nome ->

               if(nome.value == null) {
                val t = getSystemService(ACTIVITY_SERVICE) as ActivityManager
                   t.clearApplicationUserData()
            }
                mSecurityPreferences.storeString(NAME, nome.value.toString())

                ///Turma
                usuarioRef?.child("usuarios/${idU}/turma")?.get()?.addOnSuccessListener { turma ->

                    mSecurityPreferences.storeString(TURMA, turma.value.toString())

                    //Sala
                    usuarioRef?.child("usuarios/${idU}/sala")?.get()?.addOnSuccessListener { sala ->

                        mSecurityPreferences.storeString(SALA, sala.value.toString())

                        ///Foto
                        try {
                            val pathReference =
                                storageReference?.child("imagens/alunos/${idU}/fotoPerfil.jpeg")
                            val localFile = File(cacheDir, "fotoPerfil.jpg")
                            pathReference?.getFile(localFile)
                                ?.addOnSuccessListener {
                                    val bitmap =
                                        BitmapFactory.decodeFile(localFile.absolutePath)
                                    mSecurityPreferences.storeBitmap(PIC_PERFIL, bitmap)
                                    startActivity(Intent(this, MainScreen::class.java))
                                }?.addOnFailureListener {
                                    startActivity(Intent(this, MainScreen::class.java))
                                }
                        } catch (e: Exception) {
                            Toast.makeText(this, "erro", Toast.LENGTH_LONG).show()
                        }
                    }
                }
            }
        }
    }


    // [START signin]
    fun signIn() {
        val signInIntent = googleSignInClient.signInIntent
        startActivityForResult(signInIntent, RC_SIGN_IN)
    }
    // [END signin]

    // [START onactivityresult]
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        // Result returned from launching the Intent from GoogleSignInApi.getSignInIntent(...);
        if (requestCode == RC_SIGN_IN) {
            val task = GoogleSignIn.getSignedInAccountFromIntent(data)
            try {
                // Google Sign In was successful, authenticate with Firebase
                val account = task.getResult(ApiException::class.java)!!
                Log.d(TAG, "firebaseAuthWithGoogle:" + account.id)
                firebaseAuthWithGoogle(account.idToken!!)
                mSecurityPreferences.storeString(
                    Constants.DATA.USER_ID,
                    GoogleSignIn.getLastSignedInAccount(this)?.id.toString()
                )
            } catch (e: ApiException) {
                // Google Sign In failed, update UI appropriately
                Log.w(TAG, "O login do Google falhou.", e)
            }
        }
    }

    // [END onactivityresult]

    // [START auth_with_google]
    private fun firebaseAuthWithGoogle(idToken: String) {
        val credential = GoogleAuthProvider.getCredential(idToken, null)
        auth?.signInWithCredential(credential)
            ?.addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    // Sign in success, update UI with the signed-in user's information
                    Log.d(TAG, "signInWithCredential:success")
                    val idU = mSecurityPreferences.getString(USER_ID)

                    usuarioRef?.child("usuarios/${idU}/$TERMO_B")?.get()?.addOnSuccessListener {

                        val snap = it.value

                        if (snap == true) {
                            mSecurityPreferences.storeInt(BASIC_INFORMATIONS, 1)
                            updateUI()
                        } else {
                            startActivity(Intent(this, GoogleSignInActivity::class.java))
                        }
                    }
                } else {
                    Toast.makeText(
                        this,
                        "Houve um problema com o login, tente novamente",
                        Toast.LENGTH_LONG
                    ).show()
                    updateUI()
                }
            }
    }
    // [END auth_with_google]

    companion object {
        private const val TAG = "GoogleActivity"
        private const val RC_SIGN_IN = 9001
    }

}

