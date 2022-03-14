package com.ceep.id.ui.main


import android.content.Intent
import androidx.biometric.BiometricPrompt
import android.os.Bundle
import android.util.Log
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
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
import com.google.firebase.appcheck.safetynet.SafetyNetAppCheckProviderFactory
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.database.DatabaseReference
import java.util.*


class MainActivity : AppCompatActivity() {

    private lateinit var idUsuario: String
    private lateinit var mSecurityPreferences: SecurityPreferences
    private var auth: FirebaseAuth? = null
    private lateinit var googleSignInClient: GoogleSignInClient
    private var usuarioRef: DatabaseReference? = null

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
            if(mSecurityPreferences.getInt(BASIC_INFORMATIONS) == 1) {
                unlock()
            }
        }

        signInButton.setOnClickListener {
            signIn()
        }
    }

    private fun notifyUser(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }

    private fun signIn() {
        val signInIntent = googleSignInClient.signInIntent
        startActivityForResult(signInIntent, RC_SIGN_IN)
    }

    private fun unlock() {
        //Biometria
        val biometricPrompt = BiometricPrompt(
            this,
            ContextCompat.getMainExecutor(this),
            object : BiometricPrompt.AuthenticationCallback() {

                override fun onAuthenticationError(
                    errorCode: Int,
                    errString: CharSequence
                ) {
                    super.onAuthenticationError(errorCode, errString)
                    when (errorCode) {
                        BiometricPrompt.ERROR_USER_CANCELED -> {
                            notifyUser("Erro de autenticação: Processo cancelado pelo usuário.")
                            finish()
                        }
                        BiometricPrompt.ERROR_CANCELED -> {
                            notifyUser("Erro de autenticação: Autenticação cancelada.")
                            finish()
                        }
                        BiometricPrompt.ERROR_LOCKOUT -> {
                            notifyUser("Erro de autenticação: Muitas tentativas de desbloqueio.")
                            finish()
                        }
                        BiometricPrompt.ERROR_LOCKOUT_PERMANENT -> {
                            notifyUser("Erro de autenticação: Dispositivo bloqueado.")
                            finish()
                        }
                        BiometricPrompt.ERROR_NEGATIVE_BUTTON -> {
                            notifyUser("Erro de autenticação: Processo cancelado pelo usuário.")
                            finish()
                        }
                        BiometricPrompt.ERROR_NO_SPACE -> {
                            notifyUser("Erro de autenticação: Não há espaço suficiente no dispositivo.")
                            finish()
                        }
                        BiometricPrompt.ERROR_TIMEOUT -> {
                            notifyUser("Erro de autenticação: Tempo limite excedido.")
                            finish()
                        }
                        BiometricPrompt.ERROR_NO_BIOMETRICS -> {
                            notifyUser("O dispositivo não possui bloqueio, é recomendável configurar.")
                            startActivity(Intent(this@MainActivity, LoadingActivity::class.java))
                            overridePendingTransition(R.anim.right_to_left, R.anim.left_to_right)
                        }
                        BiometricPrompt.ERROR_NO_DEVICE_CREDENTIAL -> {
                            notifyUser("O dispositivo não possui bloqueio, é recomendável configurar.")
                            startActivity(Intent(this@MainActivity, LoadingActivity::class.java))
                            overridePendingTransition(R.anim.right_to_left, R.anim.left_to_right)
                        }
                     else -> {
                        notifyUser("Erro de autenticação.")
                         finish()
                    }
                    }
                }

                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    super.onAuthenticationSucceeded(result)
                    startActivity(Intent(this@MainActivity, LoadingActivity::class.java))
                    overridePendingTransition(R.anim.right_to_left, R.anim.left_to_right)
                }
            })

        val promptInfo = BiometricPrompt.PromptInfo.Builder()
            .setTitle("Verificação biometrica")
            .setSubtitle("Desbloqueie seu app para prosseguir")
            .setDeviceCredentialAllowed(true)
            .setConfirmationRequired(true)
            .build()

        biometricPrompt.authenticate(promptInfo)
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

