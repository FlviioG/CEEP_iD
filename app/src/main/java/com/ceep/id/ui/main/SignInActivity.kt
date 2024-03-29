package com.ceep.id.ui.main

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.webkit.WebView
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.ceep.id.R
import com.ceep.id.infra.Constants
import com.ceep.id.infra.Constants.DATA.BASIC_INFORMATIONS
import com.ceep.id.infra.Constants.DATABASE.TERMO_B
import com.ceep.id.infra.Constants.USER.NAME
import com.ceep.id.infra.Constants.USER.SALA
import com.ceep.id.infra.Constants.USER.TURMA
import com.ceep.id.infra.SecurityPreferences
import com.ceep.id.infra.Usuario
import com.ceep.id.infra.auth.FirebaseConfig
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DatabaseReference
import com.google.firebase.storage.StorageReference

class GoogleSignInActivity : AppCompatActivity() {

    private var auth: FirebaseAuth? = null
    private lateinit var mSecurityPreferences: SecurityPreferences
    private var usuarioRef: DatabaseReference? = null
    private var storageReference: StorageReference? = null
    private lateinit var googleSignInClient: GoogleSignInClient

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_sign_up)

        storageReference = FirebaseConfig.getFirebaseStorage()
        mSecurityPreferences = SecurityPreferences(this)
        usuarioRef = FirebaseConfig.getFirabaseDatabase()

        val editTurma = findViewById<Spinner>(R.id.editTurma)
        val chkTermo = findViewById<CheckBox>(R.id.checkTermo)
        val buttonContinuar = findViewById<Button>(R.id.button_continuar)
        val editAno = findViewById<Spinner>(R.id.editAno)
        val editNome = findViewById<TextView>(R.id.editNome)
        val termoText = findViewById<TextView>(R.id.textTermo)
        val politicaText = findViewById<TextView>(R.id.textPolitica)
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(getString(R.string.default_web_client_id))
            .requestEmail()
            .build()

        googleSignInClient = GoogleSignIn.getClient(this, gso)
        auth = FirebaseConfig.getFirebaseAuth()
        editNome.isEnabled = false
        editNome.text = auth?.currentUser?.displayName

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
        termoText.setOnClickListener {
            val webView = findViewById<WebView>(R.id.webView)
            webView.visibility = View.VISIBLE
            webView.loadUrl(getString(R.string.termo_url))
        }
        politicaText.setOnClickListener {
            val webView = findViewById<WebView>(R.id.webView)
            webView.visibility = View.VISIBLE
            webView.loadUrl(getString(R.string.politica_url))
        }
        buttonContinuar.setOnClickListener {
            val idU = GoogleSignIn.getLastSignedInAccount(this)?.id
            val parent = usuarioRef!!.child("usuarios").child(idU!!)
            val nome = parent.child("nome")
            val turma = parent.child("turma")
            val ano = parent.child("ano")
            val sala = parent.child("sala")
            val liberado = parent.child("liberado")
            val booleanTermo = parent.child(TERMO_B)
            val termo = parent.child(Constants.DATABASE.TERMO)
            val nomeSel = editNome.text.toString()
            val salaSel = findViewById<Spinner>(R.id.editSala)

            if (editTurma.selectedItemId.toInt() != 0 && editAno.selectedItemId.toInt() != 0 && chkTermo.isChecked) {

                //Gravando...
                try {
                    //Dados no banco
                    nome.setValue(nomeSel)
                    ano.setValue(editAno.selectedItem.toString())
                    turma.setValue(editTurma.selectedItem.toString())
                    sala.setValue(salaSel.selectedItem.toString())
                    termo.setValue("Aceito em ${Usuario().getDay()}, ás ${Usuario().getHour()}.")
                    liberado.setValue(false)
                    booleanTermo.setValue(true)

                    //Dados no telefone
                    mSecurityPreferences.storeString(Constants.DATA.USER_ID, idU.toString())
                    mSecurityPreferences.storeString(NAME, nomeSel)
                    mSecurityPreferences.storeString(TURMA, editTurma.selectedItem.toString())
                    mSecurityPreferences.storeString(SALA, salaSel.selectedItem.toString())

                    mSecurityPreferences.storeInt(BASIC_INFORMATIONS, 2)
                    nextScreen()

                } catch (e: Exception) {
                   notifyUser("Houve um erro ao carregar os dados, verifique sua conexão.")
                }
            } else {
                if (!chkTermo.isChecked) {
                    notifyUser("Aceite os termos de uso primeiro.")
                } else {
                    notifyUser("Preencha todos os campos primeiro.")
                }
            }
        }
    }

    override fun onBackPressed() {
        val webView = findViewById<WebView>(R.id.webView)

        if (webView.visibility == View.VISIBLE) {
            webView.visibility = View.INVISIBLE
        } else {
            super.onBackPressed()
        }
    }

    private fun populateSpinner(sala: Int, spinner: Spinner) {
        val dataAdapter: ArrayAdapter<String> = ArrayAdapter<String>(
            this,
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
                pSpinner(true, R.array.Salas_MAM_3)
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

    private fun notifyUser(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }

    private fun nextScreen() {
        startActivity(Intent(this, LoadingActivity::class.java))
        overridePendingTransition(R.anim.right_to_left, R.anim.left_to_right)
    }
}