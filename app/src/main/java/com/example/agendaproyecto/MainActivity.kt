package com.example.agendaproyecto

import android.app.Activity
import android.app.AlarmManager
import android.app.AlertDialog
import android.app.PendingIntent
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.os.Bundle
import android.speech.RecognizerIntent
import android.util.Log
import android.view.LayoutInflater
import android.widget.Button
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.text.ParseException
import java.text.SimpleDateFormat
import java.util.*


class MainActivity : AppCompatActivity() {

    private lateinit var userList: ArrayList<UserData>
    private lateinit var userAdapter: UserAdapter
    private lateinit var recv: RecyclerView
    private val VOICE_RECOGNITION_REQUEST_CODE = 3
    private val ADD_ACTIVITY_REQUEST_CODE = 1
    private val MODIFY_ACTIVITY_REQUEST_CODE = 2
    private val REQUEST_RECORD_AUDIO_PERMISSION = 200
    private var permissionToRecordAccepted = false
    private val permissions: Array<String> = arrayOf(android.Manifest.permission.RECORD_AUDIO)


    companion object {
        private var currentId = 0 // Variable estática para el último ID generado
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        // Iniciar pantalla de carga
        val screenSplash = installSplashScreen()
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Duración de la pantalla de carga usando Handler en lugar de Thread.sleep
        Thread.sleep(1000)
        screenSplash.setKeepOnScreenCondition { false }

        // Configuración del RecyclerView
        recv = findViewById(R.id.rvTareas)
        recv.layoutManager = LinearLayoutManager(this)

        // Cargar la lista de usuarios desde SharedPreferences
        loadUserListFromPreferences()

        // Configurar el adaptador con la lista cargada
        userAdapter = UserAdapter(this, userList)
        recv.adapter = userAdapter

        // Obtener datos de AgregarActividad e insertarlos en la lista
        obtenerDatosDeAgregarActividad()

        // Ir a la pantalla donde se crean nuevas tareas
        val btnIrACrearActividadActivity = findViewById<FloatingActionButton>(R.id.btnCrearActividad)
        btnIrACrearActividadActivity.setOnClickListener { irAcrearTarea() }

        // Configurar el botón de entrada por voz
        val btnVoiceInput: FloatingActionButton = findViewById(R.id.btnVoiceInput)
        btnVoiceInput.setOnClickListener { startVoiceRecognition() }

    }

    override fun onStop() {
        super.onStop()
        // Guardar la lista de tareas en SharedPreferences
        saveUserListToPreferences()
    }

    private fun saveUserListToPreferences() {
        val sharedPreferences: SharedPreferences = getSharedPreferences("user_prefs", Context.MODE_PRIVATE)
        val editor = sharedPreferences.edit()
        val gson = Gson()
        val json = gson.toJson(userList)
        editor.putString("user_list", json)
        editor.apply()
    }

    private fun loadUserListFromPreferences() {
        val sharedPreferences: SharedPreferences = getSharedPreferences("user_prefs", Context.MODE_PRIVATE)
        val gson = Gson()
        val json = sharedPreferences.getString("user_list", null)
        val type = object : TypeToken<ArrayList<UserData>>() {}.type
        userList = if (json != null) {
            gson.fromJson(json, type)
        } else {
            ArrayList()
        }
        // Establece el currentId con el máximo ID de la lista cargada
        if (userList.isNotEmpty()) {
            currentId = userList.maxOf { it.id }
        }
    }

    // Ir a la activity AgregarActividad
    private fun irAcrearTarea() {
        val intent = Intent(this, AgregarActividadActivity::class.java)
        startActivityForResult(intent, ADD_ACTIVITY_REQUEST_CODE)
    }

    // Obtener datos de AgregarActividad e insertarlos en la lista
    private fun obtenerDatosDeAgregarActividad() {
        val nombreTarea: String = intent.extras?.getString("TAREA").orEmpty()
        val date: String = intent.extras?.getString("DATE").orEmpty()
        val time: String = intent.extras?.getString("TIME").orEmpty()

        if (nombreTarea.isNotEmpty()) {
            val id = generateUniqueId()
            val newUserData = UserData(id, nombreTarea, date, time)
            userList.add(newUserData)
            userAdapter.notifyItemInserted(userList.size - 1)
            // Programar la notificación usando el mismo ID
            scheduleNotification(id, nombreTarea, date, time)
        }
    }


    // Generar un ID único para la tarea
    private fun generateUniqueId(): Int {
        return ++currentId
    }


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


    // Agregar nuevos datos a la lista y generar nuevo recyclerview
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (resultCode == Activity.RESULT_OK && data != null) {
            when (requestCode) {
                ADD_ACTIVITY_REQUEST_CODE -> {
                    val nombreTarea = data.getStringExtra("TAREA").orEmpty()
                    val date = data.getStringExtra("DATE").orEmpty()
                    val time = data.getStringExtra("TIME").orEmpty()

                    if (nombreTarea.isNotEmpty()) {
                        val id = generateUniqueId()
                        val newUserData = UserData(id, nombreTarea, date, time)
                        userList.add(newUserData)
                        userAdapter.notifyDataSetChanged()
                        // Programar la notificación usando el mismo ID
                        scheduleNotification(id, nombreTarea, date, time)
                    }
                }

                MODIFY_ACTIVITY_REQUEST_CODE -> {
                    val position = data.getIntExtra("POSITION", -1)
                    val updatedTarea = data.getStringExtra("UPDATED_TAREA").orEmpty()
                    val updatedFecha = data.getStringExtra("UPDATED_DATE").orEmpty()
                    val updatedHora = data.getStringExtra("UPDATED_TIME").orEmpty()

                    if (position != -1 && updatedTarea.isNotEmpty()) {
                        val updatedUserData = userList[position].apply {
                            tarea = updatedTarea
                            fecha = updatedFecha
                            hora = updatedHora
                        }
                        userAdapter.notifyItemChanged(position)
                        // Programar la notificación usando el mismo ID de la tarea existente
                        scheduleNotification(updatedUserData.id, updatedTarea, updatedFecha, updatedHora)
                    }
                }
                VOICE_RECOGNITION_REQUEST_CODE -> {
                    val result = data.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
                    if (result != null && result.isNotEmpty()) {
                        val nombreTarea = result[0]
                        showVoiceInputDialog(nombreTarea)
                    }
                }
            }
        }
    }

    private fun showVoiceInputDialog(voiceInput: String) {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_voice_input, null)
        val dialogBuilder = AlertDialog.Builder(this, R.style.CustomDialog) // Aplicar el estilo personalizado
            .setView(dialogView)
            .setCancelable(false) // Evitar que el diálogo se cierre con el botón de atrás

        val alertDialog = dialogBuilder.create()

        // Configurar los elementos del diálogo
        val tvVoiceInput: TextView = dialogView.findViewById(R.id.tvVoiceInput)
        val btnRepeat: Button = dialogView.findViewById(R.id.btnRepeat)
        val btnAccept: Button = dialogView.findViewById(R.id.btnAccept)
        val btnClose: ImageButton = dialogView.findViewById(R.id.btnClose)

        // Capitalizar la primera letra del texto
        val capitalizedVoiceInput = voiceInput.replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }
        tvVoiceInput.text = capitalizedVoiceInput

        btnRepeat.setOnClickListener {
            startVoiceRecognition()
            alertDialog.dismiss()
        }

        btnAccept.setOnClickListener {
            val id = generateUniqueId()
            val newUserData = UserData(id, capitalizedVoiceInput, "", "")
            userList.add(newUserData)
            userAdapter.notifyItemInserted(userList.size - 1)
            alertDialog.dismiss()
        }

        btnClose.setOnClickListener {
            alertDialog.dismiss()
        }

        alertDialog.show()
    }


    private fun scheduleNotification(id: Int, tarea: String, date: String, time: String) {
        val notificationIntent = Intent(this, NotificationReceiver::class.java).apply {
            putExtra("tarea", tarea)
            putExtra("notificationId", id) // Pasar el ID de la notificación
        }

        val pendingIntent = PendingIntent.getBroadcast(this, id, notificationIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)

        // Combine date and time into a single string
        val dateTimeString = "$date $time"
        Log.d("MainActivity", "Cadena de fecha y hora: $dateTimeString") // Depuración adicional

        // Define the date format
        val dateFormat = SimpleDateFormat("dd/MM/yyyy hh:mm a", Locale.getDefault())

        // Enable strict parsing to catch errors
        dateFormat.isLenient = false

        try {
            val dateTime = dateFormat.parse(dateTimeString)
            if (dateTime != null) {
                Log.d("MainActivity", "Programando notificación para: $dateTimeString (timestamp: ${dateTime.time})")

                // Programar la notificación
                val alarmManager = getSystemService(Context.ALARM_SERVICE) as AlarmManager
                alarmManager.setExact(AlarmManager.RTC_WAKEUP, dateTime.time, pendingIntent)

                // Guardar el ID de la tarea en SharedPreferences
                saveTaskIdToPreferences(id)
            } else {
                Log.e("MainActivity", "Error al parsear la fecha y hora: $dateTimeString")
            }
        } catch (e: ParseException) {
            Log.e("MainActivity", "Error al parsear la fecha y hora: ${e.message}")
        }
    }

    private fun saveTaskIdToPreferences(id: Int) {
        val sharedPreferences: SharedPreferences = getSharedPreferences("user_prefs", Context.MODE_PRIVATE)
        val editor = sharedPreferences.edit()
        val taskIds = sharedPreferences.getStringSet("task_ids", mutableSetOf()) ?: mutableSetOf()
        taskIds.add(id.toString())
        editor.putStringSet("task_ids", taskIds)
        editor.apply()
    }

    fun removeTaskIdFromPreferences(id: Int) {
        val sharedPreferences: SharedPreferences = getSharedPreferences("user_prefs", Context.MODE_PRIVATE)
        val editor = sharedPreferences.edit()
        val taskIds = sharedPreferences.getStringSet("task_ids", mutableSetOf()) ?: mutableSetOf()
        taskIds.remove(id.toString())
        editor.putStringSet("task_ids", taskIds)
        editor.apply()
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
}








