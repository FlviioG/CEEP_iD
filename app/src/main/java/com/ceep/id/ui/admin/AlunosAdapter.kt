package com.ceep.id.ui.admin

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.ceep.id.R
import com.ceep.id.infra.Usuario
import com.ceep.id.infra.auth.FirebaseConfig
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.ValueEventListener

class AlunosAdapter(private val alunos: List<Usuario>, private val context: Context, private val keys: List<String?>) :

    RecyclerView.Adapter<AlunosAdapter.MyViewHolder>() {
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MyViewHolder {
        val itemLista: View =
            LayoutInflater.from(parent.context).inflate(R.layout.adapter_alunos, parent, false)
        return MyViewHolder(itemLista)
    }

    override fun onBindViewHolder(holder: MyViewHolder, position: Int) {
        val usuario = alunos[position]
        val key = keys[position]

        holder.nomeAluno.text = usuario.getNome()

        val usuarioRef = FirebaseConfig.getFirabaseDatabase()
        val postReference = usuarioRef?.child("usuarios/${key}/liberado")
        val postListener = object : ValueEventListener {

            override fun onDataChange(snapshot: DataSnapshot) {
                val post = snapshot.value
                if (post == true) {
                  holder.situacaoAluno.setImageDrawable(
                        ContextCompat.getDrawable(
                            context, R.drawable.green_ball
                        )
                    )

                } else if (post == null || post == false) {
                   holder.situacaoAluno.setImageDrawable(
                        ContextCompat.getDrawable(
                            context, R.drawable.red_ball
                        )
                    )
                }
            }

            override fun onCancelled(error: DatabaseError) {
                holder.situacaoAluno.setImageDrawable(
                    ContextCompat.getDrawable(
                        context,  R.drawable.red_ball
                    )
                )
            }

        }
        postReference?.addValueEventListener(postListener)
    }

    override fun getItemCount(): Int {
        return alunos.size
    }

    inner class MyViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        var nomeAluno: TextView = itemView.findViewById(R.id.textNomeAluno)
        var situacaoAluno: ImageView = itemView.findViewById(R.id.situacaoAluno)

    }
}