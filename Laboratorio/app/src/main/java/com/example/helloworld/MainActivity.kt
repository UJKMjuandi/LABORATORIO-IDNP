package com.example.helloworld

import android.content.Context
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import java.io.BufferedReader
import java.io.InputStreamReader

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    RegistroLibroScreen()
                }
            }
        }
    }
}

@Composable
fun RegistroLibroScreen() {
    val context = LocalContext.current
    val nombreArchivo = "registro_libro.txt"

    var titulo by remember { mutableStateOf("") }
    var autor by remember { mutableStateOf("") }
    var paginas by remember { mutableStateOf("") }
    var registroLeido by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(text = "Registro de lectura actual", style = MaterialTheme.typography.titleLarge)

        OutlinedTextField(
            value = titulo,
            onValueChange = { titulo = it },
            label = { Text("Título del libro") },
            modifier = Modifier.fillMaxWidth()
        )

        OutlinedTextField(
            value = autor,
            onValueChange = { autor = it },
            label = { Text("Autor") },
            modifier = Modifier.fillMaxWidth()
        )

        OutlinedTextField(
            value = paginas,
            onValueChange = { paginas = it },
            label = { Text("Páginas leídas") },
            modifier = Modifier.fillMaxWidth()
        )

        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Button(onClick = {
                val contenido = "Título: $titulo\nAutor: $autor\nPáginas leídas: $paginas"
                context.openFileOutput(nombreArchivo, Context.MODE_PRIVATE).use { salida ->
                    salida.write(contenido.toByteArray())
                }
            }) {
                Text("Guardar")
            }

            Button(onClick = {
                registroLeido = try {
                    val entrada = context.openFileInput(nombreArchivo)
                    val lector = BufferedReader(InputStreamReader(entrada))
                    val contenido = lector.readText()
                    lector.close()
                    Log.d("RegistroLibro", contenido)
                    contenido
                } catch (e: Exception) {
                    Log.d("RegistroLibro", "No se encontró ningún registro guardado.")
                    "No hay ningún registro guardado todavía."
                }
            }) {
                Text("Ver registro")
            }
        }

        HorizontalDivider()

        Text(text = "Contenido del registro:", style = MaterialTheme.typography.titleMedium)
        Text(text = registroLeido)
    }
}