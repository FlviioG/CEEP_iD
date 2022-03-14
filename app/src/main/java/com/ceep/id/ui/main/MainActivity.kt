package com.ceep.id.ui.main

import android.app.KeyguardManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.hardware.biometrics.BiometricPrompt
import android.hardware.fingerprint.FingerprintManager
import android.os.*
import android.util.Log
import android.widget.*
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import com.ceep.id.R
import com.ceep.id.infra.Constants.DATA.BASIC_INFORMATIONS
import com.ceep.id.infra.Constants.DATA.USER_ID
import com.ceep.id.infra.Constants.DATABASE.TERMO_B
import com.ceep.id.infra.SecurityPreferences
import com.ceep.id.infra.auth.FirebaseConfig
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.SignInButton
import com.google.android.gms.common.api.ApiException
import com.google.firebase.FirebaseApp
import com.google.firebase.appcheck.FirebaseAppCheck
import com.google.firebase.appcheck.debug.DebugAppCheckProviderFactory
import com.google.firebase.appcheck.debug.internal.DebugAppCheckProvider
import com.google.firebase.appcheck.safetynet.SafetyNetAppCheckProviderFactory
import com.google.firebase.appcheck.safetynet.internal.SafetyNetAppCheckProvider
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.database.DatabaseReference
import com.google.firebase.storage.StorageReference
import java.net.URL
import java.util.*


class MainActivity : AppCompatActivity() {

    private lateinit var idUsuario: String
    private lateinit var mSecurityPreferences: SecurityPreferences
    private var auth: FirebaseAuth? = null
    private lateinit var googleSignInClient: GoogleSignInClient
    private var usuarioRef: DatabaseReference? = null
    private var cancellationSignal: CancellationSignal? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        FirebaseApp.initializeApp(this)
        val firebaseAppCheck = FirebaseAppCheck.getInstance()
        firebaseAppCheck.installAppCheckProviderFactory(
            SafetyNetAppCheckProviderFactory.getInstance()
        )

        firebaseAppCheck.setTokenAutoRefreshEnabled(true)

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
        mSecurityPreferences = SecurityPreferences(this)

        val signInButton = findViewById<SignInButton>(R.id.sign_in_button)

        val acct = GoogleSignIn.getLastSignedInAccount(this)
        if (acct != null) {
            idUsuario = acct.id!!
        }

        signInButton.setOnClickListener {
            signIn()
        }
    }

    override fun onStart() {
        super.onStart()

        val biometricSupport = Thread {
            if ((Build.VERSION.SDK_INT >= Build.VERSION_CODES.P
                        && mSecurityPreferences.getInt(BASIC_INFORMATIONS) == 1) && checkBiometricSupport()
            ) {

                val authenticationCallback: BiometricPrompt.AuthenticationCallback =
                    object : BiometricPrompt.AuthenticationCallback() {

                        override fun onAuthenticationError(
                            errorCode: Int,
                            errString: CharSequence?
                        ) {
                            super.onAuthenticationError(errorCode, errString)
                            notifyUser("Erro de autenticação : $errString")
                            finish()
                        }

                        override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult?) {
                            super.onAuthenticationSucceeded(result)
                            startActivity(Intent(this@MainActivity, LoadingActivity::class.java))
                            overridePendingTransition(R.anim.right_to_left, R.anim.left_to_right)
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
                startActivity(Intent(this, LoadingActivity::class.java))
                overridePendingTransition(R.anim.right_to_left, R.anim.left_to_right)
            }
        }
        biometricSupport.start()
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

    private fun signIn() {
        val signInIntent = googleSignInClient.signInIntent
        startActivityForResult(signInIntent, RC_SIGN_IN)
    }

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
                idUsuario = GoogleSignIn.getLastSignedInAccount(this)?.id.toString()
                mSecurityPreferences.storeString(USER_ID, idUsuario)
            } catch (e: ApiException) {
                // Google Sign In failed, update UI appropriately
                Log.w(TAG, "O login do Google falhou.", e)
            }
        }
    }

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
                            startActivity(Intent(this, LoadingActivity::class.java))
                            overridePendingTransition(R.anim.right_to_left, R.anim.left_to_right)
                        } else {
                            startActivity(Intent(this, GoogleSignInActivity::class.java))
                            overridePendingTransition(R.anim.right_to_left, R.anim.left_to_right)
                        }
                    }
                } else {
                    notifyUser("Houve um problema com o login, tente novamente")
                }
            }
    }

    companion object {
        private const val TAG = "GoogleActivity"
        private const val RC_SIGN_IN = 9001
    }

}

