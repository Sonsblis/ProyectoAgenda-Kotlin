package com.example.agendaproyecto

import android.app.DatePickerDialog
import android.app.Dialog
import android.content.Context
import android.os.Bundle
import android.widget.DatePicker
import androidx.fragment.app.DialogFragment
import java.util.Calendar

class DatePickerFragment(val listener: (day: Int, month: Int, year: Int) -> Unit) : DialogFragment(), DatePickerDialog.OnDateSetListener {

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        // Obtener la fecha actual
        val c = Calendar.getInstance()
        val year = c.get(Calendar.YEAR)
        val month = c.get(Calendar.MONTH)
        val day = c.get(Calendar.DAY_OF_MONTH)

        // Crear el DatePickerDialog con el tema personalizado
        val picker = DatePickerDialog(requireContext(), R.style.DatePickerTheme, this, year, month, day)

        // Establecer la fecha mínima como la fecha actual
        picker.datePicker.minDate = c.timeInMillis

        // Establecer la fecha máxima como 31 de diciembre de 2030
        val maxDate = Calendar.getInstance().apply {
            set(2030, Calendar.DECEMBER, 31)
        }.timeInMillis

        // Asignar la fecha máxima al DatePicker
        picker.datePicker.maxDate = maxDate

        return picker
    }

    override fun onDateSet(view: DatePicker, year: Int, month: Int, day: Int) {
        // Llamar al listener con la fecha seleccionada
        listener(day, month, year)
    }
}