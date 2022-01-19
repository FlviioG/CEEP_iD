package com.ceep.id


import android.annotation.TargetApi
import android.content.Intent
import android.content.res.Configuration
import android.os.Build
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.View
import android.view.WindowManager
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.content.res.AppCompatResources
import com.flavio.ceepid.MainScreen
import com.flavio.ceepid.SecurityPreferences
import com.flavio.ceepid.Usuario
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener

class MainActivity : AppCompatActivity() {

    val FORMAT_CPF = "###.###.###-##"

    // Esse objeto abaixo é o que vai nos permitir salvar dados no firebase
    // ele recupera a referência do nosso banco de dados
    private val referencia = FirebaseDatabase.getInstance().reference
    private lateinit var mSecurityPreferences: SecurityPreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        recuperarDados()

        mSecurityPreferences = SecurityPreferences(this)
        if (mSecurityPreferences.getInt("login") == 1) {
            startActivity(Intent(this, MainScreen::class.java))
        }

        val editTextCpf = findViewById<EditText>(R.id.editTextCPF)
        val editTextSenha = findViewById<EditText>(R.id.editTextSenha)
        val background = findViewById<ImageView>(R.id.backgroud)

        window.setFlags(WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS, WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS)

        ///Configuraçoes de Tema
        if(android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            if (resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK == Configuration.UI_MODE_NIGHT_NO) {
                background.visibility = View.VISIBLE
                editTextCpf.background = AppCompatResources.getDrawable(this, R.drawable.edittext_white)
                editTextCpf.setTextColor(AppCompatResources.getColorStateList(this, R.color.black))
                editTextSenha.background =
                    AppCompatResources.getDrawable(this, R.drawable.edittext_white)
                editTextSenha.setTextColor(
                    AppCompatResources.getColorStateList(
                        this,
                        R.color.black
                    )
                )
            }
            if (resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK == Configuration.UI_MODE_NIGHT_YES) {
                background.visibility = View.INVISIBLE
                editTextCpf.background =
                    AppCompatResources.getDrawable(this, R.drawable.edittext_dark)
                editTextCpf.setTextColor(AppCompatResources.getColorStateList(this, R.color.white))
                editTextSenha.background =
                    AppCompatResources.getDrawable(this, R.drawable.edittext_dark)
                editTextSenha.setTextColor(
                    AppCompatResources.getColorStateList(
                        this,
                        R.color.white
                    )
                )
            }
        } else {
            background.visibility = View.GONE
        }

        ///Configuraçoes de Clique
        findViewById<Button>(R.id.button_Entrar).setOnClickListener {
            val cpf = editTextCpf.text
            var senha = editTextSenha.text.toString()

            if (isCPF(cpf.toString()) && senha == "teste") {
                startActivity(Intent(this, MainScreen::class.java))
                mSecurityPreferences.storeInt("login", 1)
            }
            else {
                Toast.makeText(this, "Dados incorretos, verifique e tente novamente.", Toast.LENGTH_LONG).show()
            }
        }
        findViewById<EditText>(R.id.editTextCPF).addTextChangedListener(Mask.mask(FORMAT_CPF, editTextCpf))
    }

    fun salvarDados() {
        val usuarios = referencia.child("usuarios")
        val usuario = Usuario().usuario("Lailson", "Santana")
        usuarios.child("001").setValue(usuario)
    }

    fun recuperarDados() {
        val usuarios = referencia.child("usuarios")
        usuarios.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                Log.i("FIREBASE", snapshot.value.toString())
            }

            override fun onCancelled(error: DatabaseError) {}
        })
    }

}

fun isCPF(document: String): Boolean {
    if (document.isEmpty()) return false

    val numbers = document.filter { it.isDigit() }.map {
        it.toString().toInt()
    }

    if (numbers.size != 11) return false

    //repeticao
    if (numbers.all { it == numbers[0] }) return false

    //digito 1
    val dv1 = ((0..8).sumOf { (it + 1) * numbers[it] }).rem(11).let {
        if (it >= 10) 0 else it
    }

    val dv2 = ((0..8).sumOf { it * numbers[it] }.let { (it + (dv1 * 9)).rem(11) }).let {
        if (it >= 10) 0 else it
    }

    return numbers[9] == dv1 && numbers[10] == dv2
}

class Mask{
    companion object {
        private fun replaceChars(cpfFull : String) : String{
            return cpfFull.replace(".", "").replace("-", "")
                .replace("(", "").replace(")", "")
                .replace("/", "").replace(" ", "")
                .replace("*", "")
        }


        fun mask(mask : String, etCpf : EditText) : TextWatcher {

            val textWatcher : TextWatcher = object : TextWatcher {
                var isUpdating : Boolean = false
                var oldString : String = ""
                override fun beforeTextChanged(charSequence: CharSequence, i: Int, i1: Int, i2: Int) {

                }

                override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {
                    val str = replaceChars(s.toString())
                    var cpfWithMask = ""

                    if (count == 0)//is deleting
                        isUpdating = true

                    if (isUpdating){
                        oldString = str
                        isUpdating = false
                        return
                    }

                    var i = 0
                    for (m : Char in mask.toCharArray()){
                        if (m != '#' && str.length > oldString.length){
                            cpfWithMask += m
                            continue
                        }
                        try {
                            cpfWithMask += str.get(i)
                        }catch (e : Exception){
                            break
                        }
                        i++
                    }

                    isUpdating = true
                    etCpf.setText(cpfWithMask)
                    etCpf.setSelection(cpfWithMask.length)

                }

                override fun afterTextChanged(editable: Editable) {

                }
            }

            return textWatcher
        }
    }
}