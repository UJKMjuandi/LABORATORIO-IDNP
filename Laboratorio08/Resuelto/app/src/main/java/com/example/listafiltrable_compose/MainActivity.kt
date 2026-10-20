package com.example.listafiltrable_compose


import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import com.example.listafiltrable_compose.ui.theme.ListaFiltrable_ComposeTheme

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.listafiltrable_compose.ui.theme.ListaFiltrable_ComposeTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            ListaFiltrable_ComposeTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    ListaEdificios()
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun ListaEdificiosPreview() {
    ListaFiltrable_ComposeTheme {
        ListaEdificios()
    }
}

@Composable
fun Greeting(name: String, modifier: Modifier = Modifier) {
    Text(
        text = "Hello $name!",
        modifier = modifier
    )
}

@Preview(showBackground = true)
@Composable
fun GreetingPreview() {
    ListaFiltrable_ComposeTheme {
        Greeting("Android")
    }
}

data class Edificio(
    val id: Int,
    val nombre: String,
    val categoria: String,
    val descripcion: String
)

@Composable
fun ListaEdificios() {
    val edificios = remember {
        listOf(
            Edificio(1, "Biblioteca Central", "Servicios", "Préstamo de libros y salas de estudio"),
            Edificio(2, "Comedor Universitario", "Servicios", "Alimentación para estudiantes"),
            Edificio(3, "Facultad de Ingeniería", "Académico", "Aulas y laboratorios de ingeniería"),
            Edificio(4, "Facultad de Ciencias", "Académico", "Aulas y laboratorios de ciencias"),
            Edificio(5, "Coliseo Universitario", "Deportes", "Actividades deportivas y eventos"),
            Edificio(6, "Auditorio Central", "Eventos", "Conferencias y ceremonias")
        )
    }
    var texto by remember { mutableStateOf("") }
    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        OutlinedTextField(
            value = texto,
            onValueChange = { texto = it },
            label = { Text("Buscar edificio") },
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(12.dp))
        val edificiosFiltrados = edificios.filter {
            it.nombre.contains(texto, ignoreCase = true)
        }
        LazyColumn(modifier = Modifier.fillMaxSize()) {
            items(edificiosFiltrados, key = { it.id }) { edificio -> FilaEdificio(edificio) }
        }
    }
}

@Composable
fun FilaEdificio(edificio: Edificio) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(
                text = edificio.nombre,
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp
            )
            Text(
                text = edificio.categoria,
                fontSize = 14.sp,
                color = Color.Gray
            )
            Text(
                text = edificio.descripcion,
                fontSize = 14.sp
            )
        }
    }
}