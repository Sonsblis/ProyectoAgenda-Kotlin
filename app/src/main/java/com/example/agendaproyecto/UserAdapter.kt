package com.example.agendaproyecto

import android.app.Activity
import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.PopupMenu
import android.widget.PopupWindow
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.example.agendaproyecto.ModificarActividadAgregadaActivity.Companion.MODIFY_ACTIVITY_REQUEST_CODE
import java.text.SimpleDateFormat
import java.util.Locale

class UserAdapter(private val context: Context, private val userList: ArrayList<UserData>) :
    RecyclerView.Adapter<UserAdapter.UserViewHolder>() {

    // ViewHolder que contiene las vistas de cada elemento
    class UserViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tareaTextView: TextView = itemView.findViewById(R.id.textViewTarea)
        val fechaTextView: TextView = itemView.findViewById(R.id.textViewFecha)
        val horaTextView: TextView = itemView.findViewById(R.id.textViewHora)
        val menuImageView: ImageView = itemView.findViewById(R.id.imageViewMenu)
        val tvID: TextView = itemView.findViewById(R.id.textViewID)
    }

    // Inflar el layout para cada elemento de la lista
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): UserViewHolder {
        val itemView =
            LayoutInflater.from(context).inflate(R.layout.card_recyclerview, parent, false)
        return UserViewHolder(itemView)
    }

    // Vincular los datos a las vistas
    override fun onBindViewHolder(holder: UserViewHolder, position: Int) {
        val currentItem = userList[position]
        holder.tvID.text = "ID: ${currentItem.id}"
        holder.tareaTextView.text = currentItem.tarea
        holder.fechaTextView.text = currentItem.fecha
        holder.horaTextView.text = currentItem.hora

        // Configurar el clic en la imagen
        holder.menuImageView.setOnClickListener { view ->
            showPopupMenu(view, position)
        }
    }


    // Metodo para mostrar el PopupMenu
    private fun showPopupMenu(view: View, position: Int) {
        // Infla el diseño personalizado
        val popupView = LayoutInflater.from(context).inflate(R.layout.custom_popup_menu, null)
        val popupWindow = PopupWindow(
            popupView,
            ViewGroup.LayoutParams.WRAP_CONTENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )

        val item1 = popupView.findViewById<TextView>(R.id.item1)
        val item2 = popupView.findViewById<TextView>(R.id.item2)

        // Cambiar el tamaño del drawable
        val drawable1 = ContextCompat.getDrawable(context, R.drawable.modificar)?.apply {
            setBounds(0, 0, 48, 48) // Ajusta el tamaño aquí
        }
        val drawable2 = ContextCompat.getDrawable(context, R.drawable.eliminar)?.apply {
            setBounds(0, 0, 48, 48) // Ajusta el tamaño aquí
        }

        item1.setCompoundDrawables(drawable1, null, null, null)
        item2.setCompoundDrawables(drawable2, null, null, null)

        // Manejar clics en los elementos del menú
        item1.setOnClickListener {
            sendDataToActivity(position)
            popupWindow.dismiss()
        }

        item2.setOnClickListener {
            removeItem(position)
            popupWindow.dismiss()
        }

        popupWindow.isFocusable = true
        popupWindow.showAsDropDown(view, -115, -50)


    }


    private fun sendDataToActivity(position: Int) {
        val currentItem = userList[position]
        val intent = Intent(context, ModificarActividadAgregadaActivity::class.java).apply {
            putExtra("TAREA", currentItem.tarea)
            putExtra("FECHA", currentItem.fecha)
            putExtra("HORA", currentItem.hora)
            putExtra("POSITION", position)
        }
        (context as Activity).startActivityForResult(intent, MODIFY_ACTIVITY_REQUEST_CODE)
    }


    private fun removeItem(position: Int) {
        AlertDialog.Builder(context)
            .setTitle("Eliminar tarea")
            .setMessage("¿Estás seguro de que deseas eliminar esta tarea?")
            .setPositiveButton("Sí") { _, _ ->
                val taskId = userList[position].id
                userList.removeAt(position)
                notifyItemRemoved(position)
                notifyItemRangeChanged(position, userList.size)
                Toast.makeText(context, "Tarea eliminada", Toast.LENGTH_SHORT).show()
                // Eliminar el ID de la tarea de SharedPreferences
                (context as MainActivity).removeTaskIdFromPreferences(taskId)
            }
            .setNegativeButton("No", null)
            .show()
    }


    // Retornar el tamaño de la lista
    override fun getItemCount(): Int {
        return userList.size
    }


}