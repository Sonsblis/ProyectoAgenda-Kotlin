package com.example.agendaproyecto

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.speech.RecognizerIntent
import android.view.Gravity
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.android.material.floatingactionbutton.FloatingActionButton
import java.text.SimpleDateFormat
import java.util.Locale
import android.util.Log
import java.text.ParseException
import java.util.Calendar

class AgregarActividadActivity : AppCompatActivity() {

    companion object {
        const val REQUEST_CODE = 1 // Define el REQUEST_CODE aquí
        private const val REQUEST_RECORD_AUDIO_PERMISSION = 200
        private const val VOICE_RECOGNITION_REQUEST_CODE = 100
    }

    private lateinit var etNombreTarea: EditText
    private var permissionToRecordAccepted = false
    private val permissions: Array<String> = arrayOf(android.Manifest.permission.RECORD_AUDIO)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_agregar_actividad)

        // Inicializar vistas
        etNombreTarea = findViewById(R.id.etNombreTarea)

        // Flecha para regresar a la pantalla anterior
        val flechaRegresar: ImageView = findViewById(R.id.flechaRegresar)
        flechaRegresar.setOnClickListener { super.onBackPressed() }

        // Llamar DatePicker
        val etDate: EditText = findViewById(R.id.etDate)
        etDate.setOnClickListener { showDatePickerDialog() }

        // Llamar TimePicker
        val etTime: EditText = findViewById(R.id.etTime)
        etTime.setOnClickListener { showTimePickerDialog() }

        // Configurar el botón de entrada de voz
        val buttonVoiceInput: Button = findViewById(R.id.buttonVoiceInput)
        val drawable = ContextCompat.getDrawable(this, R.drawable.microfono1)
        drawable?.setTint(ContextCompat.getColor(this, android.R.color.white))
        buttonVoiceInput.setCompoundDrawablesWithIntrinsicBounds(null, drawable, null, null)
        buttonVoiceInput.setOnClickListener {
            startVoiceRecognition()
        }

        // Enviar datos del formulario
        val btnAgregarActividad: FloatingActionButton = findViewById(R.id.btnAgregarActividad)
        btnAgregarActividad.setOnClickListener {
            val nombreTarea = etNombreTarea.text.toString()
            val date = etDate.text.toString()
            val time = etTime.text.toString()

            if (nombreTarea.isNotEmpty()) {
                // Verificar si la fecha y hora seleccionadas son anteriores a la hora actual
                val dateTimeString = "$date $time"
                val dateFormat = SimpleDateFormat("dd/MM/yyyy hh:mm a", Locale.getDefault())
                dateFormat.isLenient = false

                try {
                    val selectedDateTime = dateFormat.parse(dateTimeString)
                    val currentDateTime = Calendar.getInstance().time

                    if (selectedDateTime != null && selectedDateTime.after(currentDateTime)) {
                        // La fecha y hora seleccionadas son posteriores a la hora actual, enviar los datos
                        val resultIntent = Intent(this, MainActivity::class.java).apply {
                            putExtra("TAREA", nombreTarea)
                            putExtra("DATE", date)
                            putExtra("TIME", time)
                        }
                        setResult(Activity.RESULT_OK, resultIntent)
                        finish()
                    } else {
                        // Mostrar un mensaje de error si la fecha y hora son anteriores a la hora actual
                        mostrarMensajeError("Debe escoger una hora posterior a la hora actual")
                    }
                } catch (e: ParseException) {
                    Log.e("AgregarActividad", "Error al analizar la fecha y hora: $dateTimeString", e)
                    // Mostrar un mensaje de error si hay un problema al analizar la fecha y hora
                    mostrarMensajeError("Debe ingresar una fecha y una hora")
                }
            } else {
                mostrarMensajeError("El campo ¿Qué hay que hacer? es obligatorio")
            }
        }
    }

    // Metodo para iniciar el reconocimiento de voz
    private fun startVoiceRecognition() {
        // Solicitar permisos de grabación de audio si no han sido otorgados
        if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, permissions, REQUEST_RECORD_AUDIO_PERMISSION)
        } else {
            startVoiceRecognitionIntent()
        }
    }

    // Iniciar la intención de reconocimiento de voz
    private fun startVoiceRecognitionIntent() {
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault())
            putExtra(RecognizerIntent.EXTRA_PROMPT, "Habla ahora...") // Cambia el mensaje de prompt
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true) // Para obtener resultados parciales
            putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_COMPLETE_SILENCE_LENGTH_MILLIS, 3000L) // Ajusta el tiempo de silencio
            putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_MINIMUM_LENGTH_MILLIS, 3000L) // Ajusta el tiempo mínimo de entrada
        }

        try {
            startActivityForResult(intent, VOICE_RECOGNITION_REQUEST_CODE)
        } catch (e: ActivityNotFoundException) {
            Toast.makeText(this, "Lo siento, tu dispositivo no soporta el reconocimiento de voz", Toast.LENGTH_SHORT).show()
        }
    }

    // Manejar el resultado del reconocimiento de voz
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == VOICE_RECOGNITION_REQUEST_CODE && resultCode == Activity.RESULT_OK && data != null) {
            val result = data.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
            if (result != null && result.isNotEmpty()) {
                val recognizedText = result[0]
                val capitalizedText = recognizedText.replaceFirstChar {
                    if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString()
                }
                etNombreTarea.setText(capitalizedText)
            }
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        permissionToRecordAccepted = if (requestCode == REQUEST_RECORD_AUDIO_PERMISSION) {
            grantResults[0] == PackageManager.PERMISSION_GRANTED
        } else {
            false
        }

        if (permissionToRecordAccepted) {
            startVoiceRecognitionIntent()
        } else {
            Toast.makeText(this, "Permiso de grabación denegado", Toast.LENGTH_SHORT).show()
        }
    }

    // Mostrar DatePicker
    private fun showDatePickerDialog() {
        val datePicker = DatePickerFragment { day, month, year -> onDateSelected(day, month, year) }
        datePicker.show(supportFragmentManager, "datePicker")
    }

    private fun onDateSelected(day: Int, month: Int, year: Int) {
        val etDate: EditText = findViewById(R.id.etDate)
        etDate.setText(String.format("%02d/%02d/%04d", day, month + 1, year))
    }

    // Mostrar TimePicker
    private fun showTimePickerDialog() {
        val timePicker = TimePickerFragment { onTimeSelected(it) }
        timePicker.show(supportFragmentManager, "timePicker")
    }

    private fun onTimeSelected(time: String) {
        val etTime: EditText = findViewById(R.id.etTime)
        etTime.setText(time)
    }

    // Función para mostrar mensaje de error
    private fun mostrarMensajeError(mensaje: String) {
        val layout = layoutInflater.inflate(R.layout.toast_error_agregar_actividad, null)
        val text = layout.findViewById<TextView>(R.id.tvToastError)
        text.text = mensaje
        val toast = Toast(this)
        toast.setGravity(Gravity.CENTER, 0, 450)
        toast.duration = Toast.LENGTH_LONG
        toast.view = layout
        toast.show()
    }
}