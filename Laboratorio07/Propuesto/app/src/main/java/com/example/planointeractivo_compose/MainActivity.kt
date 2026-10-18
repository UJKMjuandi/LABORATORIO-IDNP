package com.example.planointeractivo_compose

import android.app.Application
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Place
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.AndroidViewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.planointeractivo_compose.ui.theme.PlanoInteractivo_ComposeTheme
import org.json.JSONArray

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            PlanoInteractivo_ComposeTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    MainAppScreen()
                }
            }
        }
    }
}

// 1. Definición de rutas de navegación (Laboratorio 05)
sealed class Screen(val route: String, val label: String) {
    object Home : Screen("home", "Home")
    object Edificios : Screen("edificios", "Edificios")
    object Mapa : Screen("mapa", "Plano Lab 07")
}

// Data Class Marcador
data class Marcador(
    val offset: Offset,
    val radio: Float,
    val titulo: String,
    val descripcion: String
)

// 2. ViewModel del Plano (Laboratorio 07 - Ejercicio Propuesto 3)
class PlanoViewModel(application: Application) : AndroidViewModel(application) {
    var marcadores by mutableStateOf<List<Marcador>>(emptyList())
        private set

    var marcadorSeleccionado by mutableStateOf<Marcador?>(null)
        private set

    init {
        cargarAmbientesDesdeJson()
    }

    private fun cargarAmbientesDesdeJson() {
        val listaTemp = mutableListOf<Marcador>()
        try {
            val inputStream = getApplication<Application>().assets.open("ambientes.json")
            val jsonString = inputStream.bufferedReader().use { it.readText() }
            val jsonArray = JSONArray(jsonString)

            val posicionesProporcionales = listOf(
                Offset(0.25f, 0.25f),
                Offset(0.75f, 0.25f),
                Offset(0.25f, 0.75f),
                Offset(0.75f, 0.75f)
            )

            for (i in 0 until jsonArray.length()) {
                val jsonObject = jsonArray.getJSONObject(i)
                val titulo = jsonObject.getString("titulo")
                val descripcion = jsonObject.getString("descripcion")
                val offset = posicionesProporcionales.getOrElse(i) { Offset(0.5f, 0.5f) }

                listaTemp.add(Marcador(offset, 1f, titulo, descripcion))
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        marcadores = listaTemp
    }

    fun seleccionarMarcadorPorIndice(indice: Int) {
        if (indice in marcadores.indices) {
            marcadorSeleccionado = marcadores[indice]
        }
    }

    fun limpiarSeleccion() {
        marcadorSeleccionado = null
    }
}

// 3. Composable principal con NavigationBar y NavHost
@Composable
fun MainAppScreen() {
    val navController = rememberNavController()
    val items = listOf(Screen.Home, Screen.Edificios, Screen.Mapa)

    Scaffold(
        bottomBar = {
            NavigationBar {
                val navBackStackEntry by navController.currentBackStackEntryAsState()
                val currentRoute = navBackStackEntry?.destination?.route

                items.forEach { screen ->
                    val icon = when (screen) {
                        Screen.Home -> Icons.Default.Home
                        Screen.Edificios -> Icons.Default.List
                        Screen.Mapa -> Icons.Default.Place
                    }

                    NavigationBarItem(
                        selected = currentRoute == screen.route,
                        onClick = { navController.navigate(screen.route) },
                        icon = { Icon(icon, contentDescription = screen.label) },
                        label = { Text(screen.label) }
                    )
                }
            }
        }
    ) { paddingValues ->
        NavHost(
            navController = navController,
            startDestination = Screen.Home.route,
            modifier = Modifier.padding(paddingValues)
        ) {
            composable(Screen.Home.route) {
                HomeScreen(onVerPlano = { navController.navigate(Screen.Mapa.route) })
            }
            composable(Screen.Edificios.route) {
                EdificiosScreen(onEdificioClick = { navController.navigate(Screen.Mapa.route) })
            }
            composable(Screen.Mapa.route) {
                PlanoAmbienteScreen()
            }
        }
    }
}

// Pantalla Home
@Composable
fun HomeScreen(onVerPlano: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text("Bienvenido al Campus Universitario", fontSize = 20.sp, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(16.dp))
        Button(onClick = onVerPlano) {
            Text("Ver Plano Interactivo (Lab 07)")
        }
    }
}

// Pantalla Edificios utilizando Tarjetas del Laboratorio 06
@Composable
fun EdificiosScreen(onEdificioClick: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("Lista de Edificaciones", fontSize = 20.sp, fontWeight = FontWeight.Bold)

        // Tarjeta estilo Lab 06 que lleva al Plano al presionar
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = Color(0xFFE3F2FD),
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onEdificioClick() }
        ) {
            Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Pabellón de Sistemas", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                    Text("Toca para abrir el plano interactivo del ambiente", fontSize = 14.sp)
                }
                Text("Ver Plano", color = Color(0xFF1976D2), fontWeight = FontWeight.Bold)
            }
        }
    }
}

// Pantalla del Plano Interactivo con Canvas (Laboratorio 07)
@Composable
fun PlanoAmbienteScreen() {
    val context = LocalContext.current
    val viewModel = remember { PlanoViewModel(context.applicationContext as Application) }

    val marcadores = viewModel.marcadores
    val marcadorSeleccionado = viewModel.marcadorSeleccionado

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(12.dp)
    ) {
        Text("PLANO INTERACTIVO DE PABELLÓN", fontSize = 18.sp, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(4.dp))
        Text("Toca un ambiente para consultar su información.")

        Spacer(modifier = Modifier.height(8.dp))

        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .pointerInput(marcadores) {
                    detectTapGestures { toque ->
                        val ancho = size.width.toFloat()
                        val alto = size.height.toFloat()

                        val columna = if (toque.x < ancho / 2f) 0 else 1
                        val fila = if (toque.y < alto / 2f) 0 else 1
                        val indice = fila * 2 + columna

                        viewModel.seleccionarMarcadorPorIndice(indice)
                    }
                }
        ) {
            val ancho = size.width
            val alto = size.height
            val mitadX = ancho / 2f
            val mitadY = alto / 2f

            val colores = listOf(
                Color(0xFFBBDEFB),
                Color(0xFFC8E6C9),
                Color(0xFFFFE0B2),
                Color(0xFFFFF9C4)
            )

            val posiciones = listOf(
                Offset(0f, 0f),
                Offset(mitadX, 0f),
                Offset(0f, mitadY),
                Offset(mitadX, mitadY)
            )

            posiciones.forEachIndexed { indice, pos ->
                drawRect(
                    color = colores.getOrElse(indice) { Color.LightGray },
                    topLeft = pos,
                    size = Size(mitadX, mitadY)
                )

                drawRect(
                    color = Color.DarkGray,
                    topLeft = pos,
                    size = Size(mitadX, mitadY),
                    style = Stroke(width = 4f)
                )
            }

            // Paredes divisorias
            drawLine(
                color = Color.DarkGray,
                start = Offset(mitadX, 0f),
                end = Offset(mitadX, alto),
                strokeWidth = 6f
            )

            drawLine(
                color = Color.DarkGray,
                start = Offset(0f, mitadY),
                end = Offset(ancho, mitadY),
                strokeWidth = 6f
            )

            // Ubicación de marcadores y textos
            val centros = listOf(
                Offset(mitadX / 2f, mitadY / 2f),
                Offset(mitadX * 1.5f, mitadY / 2f),
                Offset(mitadX / 2f, mitadY * 1.5f),
                Offset(mitadX * 1.5f, mitadY * 1.5f)
            )

            marcadores.forEachIndexed { indice, marcador ->
                if (indice < centros.size) {
                    val centro = centros[indice]

                    drawContext.canvas.nativeCanvas.drawText(
                        marcador.titulo.uppercase(),
                        centro.x,
                        centro.y - 20f,
                        android.graphics.Paint().apply {
                            color = android.graphics.Color.BLACK
                            textSize = 22f
                            isAntiAlias = true
                            textAlign = android.graphics.Paint.Align.CENTER
                            isFakeBoldText = true
                        }
                    )

                    drawCircle(
                        color = Color.Red,
                        radius = 18f,
                        center = centro
                    )

                    drawCircle(
                        color = Color.White,
                        radius = 5f,
                        center = centro
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        Text("Ambientes cargados desde JSON: ${marcadores.size}", fontWeight = FontWeight.Bold)
        marcadores.forEach { marcador ->
            Text("• ${marcador.titulo}: ${marcador.descripcion}", fontSize = 12.sp)
        }
    }

    marcadorSeleccionado?.let { marcador ->
        AlertDialog(
            onDismissRequest = { viewModel.limpiarSeleccion() },
            title = { Text(marcador.titulo) },
            text = { Text(marcador.descripcion) },
            confirmButton = {
                TextButton(onClick = { viewModel.limpiarSeleccion() }) {
                    Text("Cerrar")
                }
            }
        )
    }
}