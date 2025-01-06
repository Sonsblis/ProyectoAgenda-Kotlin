package com.example.agendaproyecto

import android.app.Dialog
import android.app.TimePickerDialog
import android.content.res.Configuration
import android.os.Bundle
import androidx.fragment.app.DialogFragment
import android.view.WindowManager
import android.widget.TimePicker
import java.util.Calendar
import java.text.SimpleDateFormat
import java.util.Locale

class TimePickerFragment(val listener: (String) -> Unit) : DialogFragment(), TimePickerDialog.OnTimeSetListener {

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val calendar: Calendar = Calendar.getInstance()
        val hour: Int = calendar.get(Calendar.HOUR_OF_DAY)
        val minute = calendar.get(Calendar.MINUTE)

        // Cambiar a formato de 12 horas
        val dialog = TimePickerDialog(requireActivity(), R.style.TimePickerTheme, this, hour, minute, false)

        dialog.setOnShowListener {
            adjustDialogSize(dialog)
        }

        return dialog
    }

    private fun adjustDialogSize(dialog: TimePickerDialog) {
        val orientation = resources.configuration.orientation
        val width = if (orientation == Configuration.ORIENTATION_LANDSCAPE) {
            // Ajustar el ancho al 90% del ancho de la pantalla en modo horizontal
            (resources.displayMetrics.widthPixels * 0.7).toInt()
        } else {
            // Ajustar el ancho al 80% del ancho de la pantalla en modo vertical
            (resources.displayMetrics.widthPixels * 0.8).toInt()
        }
        dialog.window?.setLayout(width, WindowManager.LayoutParams.WRAP_CONTENT)
    }

    override fun onTimeSet(view: TimePicker?, hourOfDay: Int, minute: Int) {
        // Crear un Calendar para manipular la fecha y hora
        val calendar = Calendar.getInstance()
        calendar.set(Calendar.HOUR_OF_DAY, hourOfDay)
        calendar.set(Calendar.MINUTE, minute)

        // Formatear la hora según el formato especificado: %I:%M %p
        val timeFormat = SimpleDateFormat("hh:mm a", Locale.getDefault())
        val formattedTime = timeFormat.format(calendar.time)

        // Pasar la hora formateada al listener
        listener(formattedTime)
    }
}