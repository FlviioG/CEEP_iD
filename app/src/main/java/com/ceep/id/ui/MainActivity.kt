package com.ceep.id.ui


import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.view.WindowManager
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.ceep.id.R
import com.ceep.id.infra.FirebaseConfig
import com.flavio.ceepid.infra.SecurityPreferences
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.SignInButton
import com.google.android.gms.common.api.ApiException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.database.DatabaseReference


class MainActivity : AppCompatActivity() {

    private lateinit var mSecurityPreferences: SecurityPreferences
    private var auth: FirebaseAuth? = null
    private lateinit var googleSignInClient: GoogleSignInClient
    private var usuarioRef: DatabaseReference? = null


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        mSecurityPreferences = SecurityPreferences(this)

        auth = FirebaseConfig.getFirebaseAuth()

        window.setFlags(
            WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS
        )

        // [START config_signin]
        // Configure Google Sign In
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(getString(R.string.default_web_client_id))
            .requestEmail()
            .requestId()
            .build()

        googleSignInClient = GoogleSignIn.getClient(this, gso)

        // [END config_signin]

        // [START initialize_auth]
        // Initialize Firebase Auth
        auth = FirebaseConfig.getFirebaseAuth()
        usuarioRef = FirebaseConfig.getFirabaseDatabase()
        // [END initialize_auth]

        findViewById<SignInButton>(R.id.sign_in_button).setOnClickListener {
            signIn()
        }
        findViewById<Button>(R.id.button_continuar).setOnClickListener {
            val idU = GoogleSignIn.getLastSignedInAccount(this)?.id
            val nome = usuarioRef!!.child("usuarios").child(idU!!).child("nome")
            val turma = usuarioRef!!.child("usuarios").child(idU).child("turma")

            val nomeEdit = findViewById<EditText>(R.id.editNome).text
            nome.setValue(nomeEdit.toString())
            val turmaEdit = findViewById<Spinner>(R.id.editTurma).selectedItem
            turma.setValue(turmaEdit.toString())
            mSecurityPreferences.storeString("idU", idU.toString())
            mSecurityPreferences.storeInt("basicInformations", 1)
            updateUI(auth?.currentUser)
        }
    }

    // [START on_start_check_user]
    override fun onStart() {
        super.onStart()
        // Check if user is signed in (non-null) and update UI accordingly.
        val currentUser = auth?.currentUser

        updateUI(currentUser)

    }

    // [END on_start_check_user]

    // [START signin]
    private fun signIn() {
        val signInIntent = googleSignInClient.signInIntent
        startActivityForResult(signInIntent, RC_SIGN_IN)
    }
    // [END signin]

    private fun updateUI(user: FirebaseUser?) {

        val idU = mSecurityPreferences.getString("idU")

        if (idU != "" && mSecurityPreferences.getInt("basicInformations") == 1) {
            startActivity(Intent(this, MainScreen::class.java))
        }

    }


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
                    "idU",
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
                    val user = auth?.currentUser
                    val idU = mSecurityPreferences.getString("idU")

                    usuarioRef?.child("usuarios/${idU}/turma")?.get()?.addOnSuccessListener {

                        val snap = it.value

                        if (snap != null) {
                            mSecurityPreferences.storeInt("basicInformations", 1)
                            updateUI(user)
                        } else {
                            val spinner = findViewById<Spinner>(R.id.editTurma)

                            findViewById<Button>(R.id.button_continuar).visibility =
                                View.VISIBLE
                            findViewById<EditText>(R.id.editNome).visibility = View.VISIBLE
                            findViewById<EditText>(R.id.editNome).setText(auth?.currentUser?.displayName)
                            findViewById<SignInButton>(R.id.sign_in_button).visibility =
                                View.INVISIBLE
                            spinner.visibility = View.VISIBLE
                        }
                    }
                } else {
                    // If sign in fails, display a message to the user.
                    Log.w(TAG, "signInWithCredential:failure", task.exception)
                    updateUI(null)
                }
            }
    }
// [END auth_with_google]

    companion object {
        private const val TAG = "GoogleActivity"
        private const val RC_SIGN_IN = 9001
    }
}

