package com.ceep.id.ui


import android.content.Intent
import android.content.res.Configuration
import android.os.Build
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.view.WindowManager
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.content.res.AppCompatResources
import com.ceep.id.R
import com.flavio.ceepid.infra.SecurityPreferences

class MainActivity : AppCompatActivity() {

    val FORMAT_CPF = "###.###.###-##"
    private lateinit var mSecurityPreferences: SecurityPreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
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
                editTextCpf.background = AppCompatResources.getDrawable(this,
                    R.drawable.edittext_white
                )
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
        editTextCpf.addTextChangedListener(Mask.mask(FORMAT_CPF, editTextCpf))
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