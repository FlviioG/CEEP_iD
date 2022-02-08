package com.ceep.id.ui.admin

import android.graphics.Color
import android.os.Bundle
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.Spinner
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.ceep.id.R
import com.ceep.id.infra.Usuario
import com.ceep.id.infra.auth.FirebaseConfig
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.ValueEventListener

class MainScreenAdmin : AppCompatActivity() {

    private var recyclerAlunos: RecyclerView? = null
    private var adapter: AlunosAdapter? = null
    private val listaAlunos: ArrayList<Usuario> = ArrayList()
    private val keyAlunos: ArrayList<String?> = ArrayList()
    private var referenciaAlunos: DatabaseReference? = null
    private var valueEventListener: ValueEventListener? = null
    private val keysSelecionadas: ArrayList<String?> = ArrayList()
    private var isSelectedMode = false
    private var usuarioRef: DatabaseReference? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main_screen_admin)

        usuarioRef = FirebaseConfig.getFirabaseDatabase()

        recyclerAlunos = findViewById(R.id.recyclerAlunos)
        referenciaAlunos = FirebaseConfig.getFirabaseDatabase()?.child("usuarios")
        adapter = AlunosAdapter(listaAlunos, this, keyAlunos)
        configuraRecyclerView()

        val spinnerAno = findViewById<Spinner>(R.id.spinnerAno)
        val spinnerTurma = findViewById<Spinner>(R.id.spinnerTurma)
        val spinnerSala = findViewById<Spinner>(R.id.spinnerSala)
        val buttonLiberar = findViewById<Button>(R.id.liberar_button)

        spinnerTurma.onItemSelectedListener =
            object : AdapterView.OnItemSelectedListener {

                override fun onItemSelected(p0: AdapterView<*>?, p1: View?, p2: Int, p3: Long) {
                    spinnerSelector(p2)
                }

                override fun onNothingSelected(p0: AdapterView<*>?) {
                    recuperarContatos(p0?.selectedItem.toString())
                }

            }

        spinnerAno.onItemSelectedListener =
            object : AdapterView.OnItemSelectedListener {
                override fun onItemSelected(p0: AdapterView<*>?, p1: View?, p2: Int, p3: Long) {
                    spinnerSelector(spinnerTurma.selectedItemId.toInt())
                }

                override fun onNothingSelected(p0: AdapterView<*>?) {
                    recuperarContatos(p0?.selectedItem.toString())
                }

            }

        spinnerSala.onItemSelectedListener =
            object : AdapterView.OnItemSelectedListener {
                override fun onItemSelected(p0: AdapterView<*>?, p1: View?, p2: Int, p3: Long) {
                    keyAlunos.clear()
                    keysSelecionadas.clear()
                    recyclerAlunos!!.setBackgroundColor(Color.TRANSPARENT)
                    recuperarContatos(p0?.selectedItem.toString())
                }

                override fun onNothingSelected(p0: AdapterView<*>?) {
                recuperarContatos(p0?.selectedItem.toString())
                }

            }

        buttonLiberar.setOnClickListener {
            keysSelecionadas.forEach { key ->
                usuarioRef?.child("usuarios/${key}/liberado")?.get()?.addOnSuccessListener {
                    if (it.value == true) {
                        Usuario().desliberar(key)
                    } else {
                        Usuario().liberar(key)
                    }
                }
            }
        }
    }


    override fun onStop() {
        super.onStop()
        referenciaAlunos!!.removeEventListener(valueEventListener!!)
    }

    override fun onBackPressed() {
        this.finishAffinity()
    }

    private fun configuraRecyclerView() {
        val layoutManager: RecyclerView.LayoutManager = LinearLayoutManager(applicationContext)
        recyclerAlunos!!.layoutManager = layoutManager
        recyclerAlunos!!.setHasFixedSize(true)
        //recyclerAlunos.addItemDecoration(new DividerItemDecoration(this.getApplicationContext(),));
        recyclerAlunos!!.adapter = adapter
        recyclerAlunos!!.addOnItemTouchListener(
            RecyclerItemClickListener(
                applicationContext,
                recyclerAlunos!!,
                object : RecyclerItemClickListener.OnItemClickListener {
                    override fun onItemClick(view: View?, position: Int): Unit {

                        if (isSelectedMode) {
                            isSelectedMode = false
                            val keySelecionada = keyAlunos[position]
                            keysSelecionadas.remove(keySelecionada)
                            view?.setBackgroundColor(Color.TRANSPARENT)
                        } else {
                            isSelectedMode = true

                            val keySelecionada = keyAlunos[position]
                            keysSelecionadas.add(keySelecionada)
                            view?.setBackgroundColor(resources.getColor(R.color.really_light_blue))
                        }
                    }

                    override fun onLongItemClick(view: View?, position: Int) {

                    }
                })
        )

    }

    private fun recuperarContatos(sala: String?) {
        val usuariosPesquisa = referenciaAlunos!!.orderByChild("sala").equalTo(sala)

        valueEventListener = usuariosPesquisa.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                for (dados in snapshot.children) {
                    val usuario: Usuario? = dados.getValue(Usuario::class.java)
                    if (usuario != null) {
                        listaAlunos.clear()
                        keysSelecionadas.clear()
                        keyAlunos.clear()
                        keyAlunos.add(dados.key)
                        listaAlunos.add(usuario)
                    }
                }
                adapter?.notifyDataSetChanged()
            }

            override fun onCancelled(error: DatabaseError) {}
        })
    }

    fun spinnerSelector(p2: Int) {

        val ano = findViewById<Spinner>(R.id.spinnerAno).selectedItemId.toInt()
        val spinner = findViewById<Spinner>(R.id.spinnerSala)

        when {
            p2 == 0 || ano == 0 -> {
                populateSpinner(R.array.Selecionar, spinner)
            }
            p2 == 1 && ano == 1 -> {
                populateSpinner(R.array.Salas_ADM_1, spinner)
            }
            p2 == 1 && ano == 2 -> {
                populateSpinner(R.array.Salas_ADM_2, spinner)
            }
            p2 == 1 && ano == 3 -> {
                populateSpinner(R.array.Salas_ADM_3, spinner)
            }
            p2 == 2 && ano == 1 -> {
                populateSpinner(R.array.Salas_LOG_1, spinner)
            }
            p2 == 2 && ano == 2 -> {
                populateSpinner(R.array.Salas_LOG_2, spinner)
            }
            p2 == 2 && ano == 3 -> {
                populateSpinner(R.array.Salas_LOG_3, spinner)
            }
            p2 == 3 && ano == 1 -> {
                populateSpinner(R.array.Salas_MAM_1, spinner)
            }
            p2 == 3 && ano == 2 -> {
                populateSpinner(R.array.Salas_MAM_2, spinner)
            }
            p2 == 3 && ano == 3 -> {
                populateSpinner(R.array.Salas_MAM_3, spinner)
            }
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

}