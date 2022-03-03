package com.ceep.id.ui


import android.app.KeyguardManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.hardware.biometrics.BiometricPrompt
import android.os.Build
import android.os.Bundle
import android.os.CancellationSignal
import android.util.Log
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.view.WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS
import android.widget.*
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import com.ceep.id.R
import com.ceep.id.infra.Constants.DATA.BASIC_INFORMATIONS
import com.ceep.id.infra.Constants.DATA.USER_ID
import com.ceep.id.infra.SecurityPreferences
import com.ceep.id.infra.auth.FirebaseConfig
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


class MainActivity : AppCompatActivity() {

    private lateinit var mSecurityPreferences: SecurityPreferences
    private var auth: FirebaseAuth? = null
    private lateinit var googleSignInClient: GoogleSignInClient
    private var usuarioRef: DatabaseReference? = null
    private var cancellationSignal: CancellationSignal? = null

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

        val editTurma = findViewById<Spinner>(R.id.editTurma)
        val buttonContinuar = findViewById<Button>(R.id.button_continuar)
        val signInButton = findViewById<SignInButton>(R.id.sign_in_button)
        val editAno = findViewById<Spinner>(R.id.editAno)

        signInButton.setOnClickListener {
            signIn()
        }

        buttonContinuar.setOnClickListener {
            val idU = GoogleSignIn.getLastSignedInAccount(this)?.id
            val nome = usuarioRef!!.child("usuarios").child(idU!!).child("nome")
            val turma = usuarioRef!!.child("usuarios").child(idU).child("turma")
            val ano = usuarioRef!!.child("usuarios").child(idU).child("ano")
            val sala = usuarioRef!!.child("usuarios").child(idU).child("sala")
            val liberado = usuarioRef!!.child("usuarios").child(idU).child("liberado")

            val nomeSel = findViewById<EditText>(R.id.editNome).text
            val salaSel = findViewById<Spinner>(R.id.editSala)

            if (editTurma.selectedItemId.toInt() != 0 && editAno.selectedItemId.toInt() != 0) {
                nome.setValue(nomeSel.toString())
                ano.setValue(editAno.selectedItem.toString())
                turma.setValue(editTurma.selectedItem.toString())
                sala.setValue(salaSel.selectedItem.toString())
                liberado.setValue(false)

                mSecurityPreferences.storeString(USER_ID, idU.toString())
                mSecurityPreferences.storeInt(BASIC_INFORMATIONS, 1)
                updateUI()
            } else {
                Toast.makeText(this, "Preencha todos os campos primeiro.", Toast.LENGTH_LONG).show()
            }
        }
        editTurma.onItemSelectedListener =
            object : AdapterView.OnItemSelectedListener {

                override fun onItemSelected(p0: AdapterView<*>?, p1: View?, p2: Int, p3: Long) {
                    spinnerSelector(p2)
                }

                override fun onNothingSelected(p0: AdapterView<*>?) {

                }

            }
        editAno.onItemSelectedListener =
            object : AdapterView.OnItemSelectedListener {
                override fun onItemSelected(p0: AdapterView<*>?, p1: View?, p2: Int, p3: Long) {
                    spinnerSelector(editTurma.selectedItemId.toInt())
                }

                override fun onNothingSelected(p0: AdapterView<*>?) {

                }

            }
    }

    override fun onStart() {
        super.onStart()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P
            && mSecurityPreferences.getInt(BASIC_INFORMATIONS) == 1
        ) {
            checkBiometricSupport()

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
        } else {
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
        }
        if (ActivityCompat.checkSelfPermission(
                this,
                android.Manifest.permission.USE_BIOMETRIC
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            notifyUser("Não há permissão para acessar o leitor biométrico")
            return false
        }
        return if (packageManager.hasSystemFeature(PackageManager.FEATURE_FINGERPRINT)) {
            true
        } else true
    }

    private fun notifyUser(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }

    private fun signIn() {
        val signInIntent = googleSignInClient.signInIntent
        startActivityForResult(signInIntent, RC_SIGN_IN)
    }

    private fun updateUI() {

        val idU = mSecurityPreferences.getString(USER_ID)

        usuarioRef?.child("usuarios/${idU}/admin")?.get()?.addOnSuccessListener {
            if (it.value == true) {
                startActivity(Intent(this, MainScreenAdmin::class.java))
            }
        }

        if (idU != "" && mSecurityPreferences.getInt(BASIC_INFORMATIONS) == 1) {
            startActivity(Intent(this, MainScreen::class.java))
        }
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
                mSecurityPreferences.storeString(
                    USER_ID,
                    GoogleSignIn.getLastSignedInAccount(this)?.id.toString()
                )
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

                    usuarioRef?.child("usuarios/${idU}/turma")?.get()?.addOnSuccessListener {

                        val snap = it.value
                        val spinner = findViewById<Spinner>(R.id.editTurma)
                        val editNome = findViewById<EditText>(R.id.editNome)
                        val continuarBt = findViewById<Button>(R.id.button_continuar)
                        val editSala = findViewById<Spinner>(R.id.editSala)
                        val editAno = findViewById<Spinner>(R.id.editAno)
                        val textCadastro = findViewById<TextView>(R.id.textCadastro)
                        val signInBt = findViewById<SignInButton>(R.id.sign_in_button)

                        if (snap != null) {
                            mSecurityPreferences.storeInt(BASIC_INFORMATIONS, 1)
                            updateUI()
                        } else {
                            continuarBt.visibility = View.VISIBLE
                            editNome.visibility = View.VISIBLE
                            editNome.isEnabled = false
                            editNome.setText(auth?.currentUser?.displayName)
                            editSala.visibility = View.VISIBLE
                            editAno.visibility = View.VISIBLE
                            textCadastro.visibility = View.VISIBLE
                            spinner.visibility = View.VISIBLE
                            signInBt.visibility = View.INVISIBLE
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

    private fun populateSpinner(sala: Int, spinner: Spinner) {
        val dataAdapter: ArrayAdapter<String> = ArrayAdapter<String>(
            this@MainActivity,
            android.R.layout.simple_spinner_item, resources.getStringArray(sala)
        )
        dataAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)

        spinner.adapter = dataAdapter
    }

    fun spinnerSelector(p2: Int) {

        val ano = findViewById<Spinner>(R.id.editAno).selectedItemId.toInt()

        when {
            p2 == 0 || ano == 0 -> {
                pSpinner(false, R.array.Selecionar)
            }
            p2 == 1 && ano == 1 -> {
                pSpinner(true, R.array.Salas_ADM_1)
            }
            p2 == 1 && ano == 2 -> {
                pSpinner(true, R.array.Salas_ADM_2)
            }
            p2 == 1 && ano == 3 -> {
                pSpinner(true, R.array.Salas_ADM_3)
            }
            p2 == 2 && ano == 1 -> {
                pSpinner(true, R.array.Salas_LOG_1)
            }
            p2 == 2 && ano == 2 -> {
                pSpinner(true, R.array.Salas_LOG_2)
            }
            p2 == 2 && ano == 3 -> {
                pSpinner(true, R.array.Salas_LOG_3)
            }
            p2 == 3 && ano == 1 -> {
                pSpinner(true, R.array.Salas_MAM_1)
            }
            p2 == 3 && ano == 2 -> {
                pSpinner(true, R.array.Salas_MAM_2)
            }
            p2 == 3 && ano == 3 -> {
                pSpinner(false, R.array.Salas_MAM_3)
            }
        }
    }

    private fun pSpinner(boolean: Boolean, array: Int) {

        val spinner = findViewById<Spinner>(R.id.editSala)
        val buttonContinuar = findViewById<Button>(R.id.button_continuar)

        spinner.isEnabled = boolean
        buttonContinuar.isEnabled = boolean
        populateSpinner(array, spinner)
    }

    companion object {
        private const val TAG = "GoogleActivity"
        private const val RC_SIGN_IN = 9001
    }

}

