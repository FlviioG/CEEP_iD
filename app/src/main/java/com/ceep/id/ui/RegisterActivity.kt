package com.ceep.id.ui

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.Spinner
import com.ceep.id.R

class RegisterActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_register)

        val spinner: Spinner = findViewById(R.id.dropdown_menu)
        val itens = listOf<String>("teste1", "teste2", "teste3")
        spinner.setAdapter(ArrayAdapter<String>(this, android.R.layout.simple_spinner_dropdown_item, itens))
    }
}