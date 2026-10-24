package com.example.listafiltrable_compose

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
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
                    var edificioSeleccionado by remember { mutableStateOf<Edificio?>(null) }

                    if (edificioSeleccionado == null) {
                        ListaEdificios(onEdificioClick = { edificioSeleccionado = it })
                    } else {
                        DetalleLugarScreen(
                            edificio = edificioSeleccionado!!,
                            onBack = { edificioSeleccionado = null }
                        )
                    }
                }
            }
        }
    }
}

data class Edificio(
    val id: Int,
    val nombre: String,
    val categoria: String,
    val descripcion: String
)

fun cargarEdificiosDesdeArchivo(context: android.content.Context): List<Edificio> {
    val lista = mutableListOf<Edificio>()
    try {
        val inputStream = context.assets.open("edificios.txt")
        val reader = inputStream.bufferedReader()
        reader.useLines { lines ->
            lines.forEach { line ->
                val partes = line.split("|")
                if (partes.size == 4) {
                    lista.add(
                        Edificio(
                            id = partes[0].toIntOrNull() ?: 0,
                            nombre = partes[1],
                            categoria = partes[2],
                            descripcion = partes[3]
                        )
                    )
                }
            }
        }
    } catch (e: Exception) {
        e.printStackTrace()
    }
    return lista
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ListaEdificios(onEdificioClick: (Edificio) -> Unit) {
    val context = LocalContext.current
    val edificios = remember { cargarEdificiosDesdeArchivo(context) }

    var textoBusqueda by remember { mutableStateOf("") }
    var categoriaSeleccionada by remember { mutableStateOf("Todas") }
    var menuExpandido by remember { mutableStateOf(false) }

    val categorias = remember(edificios) {
        listOf("Todas") + edificios.map { it.categoria }.distinct()
    }

    val edificiosFiltrados = edificios.filter { edificio ->
        val coincideNombre = edificio.nombre.contains(textoBusqueda, ignoreCase = true)
        val coincideCategoria = categoriaSeleccionada == "Todas" || edificio.categoria == categoriaSeleccionada
        coincideNombre && coincideCategoria
    }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        OutlinedTextField(
            value = textoBusqueda,
            onValueChange = { textoBusqueda = it },
            label = { Text("Buscar edificio por nombre") },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(8.dp))

        ExposedDropdownMenuBox(
            expanded = menuExpandido,
            onExpandedChange = { menuExpandido = !menuExpandido }
        ) {
            OutlinedTextField(
                value = categoriaSeleccionada,
                onValueChange = {},
                readOnly = true,
                label = { Text("Filtrar por categoría") },
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = menuExpandido) },
                modifier = Modifier.menuAnchor().fillMaxWidth()
            )
            ExposedDropdownMenu(
                expanded = menuExpandido,
                onDismissRequest = { menuExpandido = false }
            ) {
                categorias.forEach { categoria ->
                    DropdownMenuItem(
                        text = { Text(categoria) },
                        onClick = {
                            categoriaSeleccionada = categoria
                            menuExpandido = false
                        }
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        if (edificiosFiltrados.isEmpty()) {
            Text("no se encontraron edificios.", modifier = Modifier.padding(16.dp))
        } else {
            LazyColumn(modifier = Modifier.fillMaxSize()) {
                items(edificiosFiltrados, key = { it.id }) { edificio ->
                    TarjetaLugar(
                        nombre = edificio.nombre,
                        descripcion = edificio.descripcion,
                        categoria = edificio.categoria,
                        onClick = { onEdificioClick(edificio) }
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                }
            }
        }
    }
}

fun obtenerIcono(categoria: String): ImageVector {
    return when (categoria.lowercase()) {
        "servicios" -> Icons.Default.Restaurant
        "académico" -> Icons.Default.School
        "deportes" -> Icons.Default.SportsSoccer
        "eventos" -> Icons.Default.Event
        "administrativo" -> Icons.Default.Business
        "salud" -> Icons.Default.LocalHospital
        else -> Icons.Default.LocationOn
    }
}

@Composable
fun TarjetaLugar(
    nombre: String,
    descripcion: String,
    categoria: String,
    onClick: () -> Unit = {}
) {
    val colorFondo = when(categoria.lowercase()) {
        "servicios" -> Color(0xFFD6EAF8)
        "académico" -> Color(0xFFFDEBD0)
        "deportes" -> Color(0xFFD5F5E3)
        "eventos" -> Color(0xFFFADBD8)
        "administrativo" -> Color(0xFFE8DAEF)
        "salud" -> Color(0xFFFCF3CF)
        else -> Color(0xFFEAECEE)
    }

    Surface(
        shape = RoundedCornerShape(24.dp),
        color = colorFondo,
        modifier = Modifier.fillMaxWidth().clickable { onClick() }
    ) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(CircleShape)
                    .background(Color.White),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = obtenerIcono(categoria),
                    contentDescription = "Icono de $categoria",
                    tint = Color(0xFF2C3E50),
                    modifier = Modifier.size(32.dp)
                )
            }

            Spacer(Modifier.width(16.dp))

            Column {
                Text(
                    text = nombre,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    color = Color(0xFF1E1E1E)
                )
                Text(
                    text = categoria,
                    fontSize = 12.sp,
                    color = Color(0xFF5D6D7E),
                    fontWeight = FontWeight.Medium
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = descripcion,
                    fontSize = 14.sp,
                    color = Color(0xFF424949)
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DetalleLugarScreen(edificio: Edificio, onBack: () -> Unit) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("detalle del lugar") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "volver")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Surface(
                shape = RoundedCornerShape(24.dp),
                color = MaterialTheme.colorScheme.primaryContainer,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        modifier = Modifier
                            .size(120.dp)
                            .clip(CircleShape)
                            .background(Color.White),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = obtenerIcono(edificio.categoria),
                            contentDescription = "Icono de ${edificio.categoria}",
                            tint = Color(0xFF2C3E50),
                            modifier = Modifier.size(64.dp)
                        )
                    }
                    Spacer(Modifier.height(16.dp))
                    Text(edificio.nombre, fontSize = 24.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
                    Text(edificio.categoria, fontSize = 16.sp, color = MaterialTheme.colorScheme.primary)
                    Spacer(Modifier.height(16.dp))
                    Text(edificio.descripcion, fontSize = 16.sp, textAlign = TextAlign.Center)
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun ListaEdificiosPreview() {
    ListaFiltrable_ComposeTheme {
        Surface(modifier = Modifier.fillMaxSize()) {
            Text("requiere emulador para leer assets", modifier = Modifier.padding(16.dp))
        }
    }
}