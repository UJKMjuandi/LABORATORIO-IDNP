package com.example.campusnavigator_compose

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.Image
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
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.*
import com.example.campusnavigator_compose.ui.theme.CampusNavigator_ComposeTheme
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

// ==================== ACTIVITY ====================
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            CampusNavigator_ComposeTheme { MainScreen() }
        }
    }
}

// ==================== VIEWMODEL ====================
class SeleccionViewModel : ViewModel() {
    private val _edificio = MutableStateFlow("Ninguno")
    val edificio = _edificio.asStateFlow()
    fun seleccionar(nombre: String) { _edificio.value = nombre }
}

// ==================== MODELO ====================
data class Edificio(
    val nombre: String,
    val descripcion: String,
    val imagen: Int,
    val color: Color
)

val edificios = listOf(
    Edificio("Biblioteca Central", "Centro de estudio y consulta.", android.R.drawable.ic_menu_gallery, Color(0xFFE3F2FD)),
    Edificio("Pabellón A",         "Aulas de Ingeniería de Sistemas.", android.R.drawable.ic_menu_gallery, Color(0xFFF1F8E9)),
    Edificio("Pabellón B",         "Laboratorios de cómputo y redes.", android.R.drawable.ic_menu_gallery, Color(0xFFFFF3E0)),
    Edificio("Auditorio",          "Eventos académicos y conferencias.", android.R.drawable.ic_menu_gallery, Color(0xFFF3E5F5))
)

// ==================== NAVEGACIÓN ====================
sealed class Screen(val route: String, val label: String) {
    object Home     : Screen("home", "Home")
    object Edificios: Screen("edificios", "Edificios")
    object Mapa     : Screen("mapa", "Mapa")
    object Perfil   : Screen("perfil", "Perfil")
}

@Composable
fun MainScreen(vm: SeleccionViewModel = viewModel()) {
    val nav = rememberNavController()
    val tabs = listOf(Screen.Home, Screen.Edificios, Screen.Mapa, Screen.Perfil)
    val iconos = listOf(Icons.Default.Home, Icons.Default.List, Icons.Default.Place, Icons.Default.Person)

    Scaffold(
        bottomBar = {
            NavigationBar {
                val ruta = nav.currentBackStackEntryAsState().value?.destination?.route
                tabs.forEachIndexed { i, s ->
                    NavigationBarItem(
                        selected = ruta == s.route,
                        onClick = { nav.navigate(s.route) },
                        icon = { Icon(iconos[i], s.label) },
                        label = { Text(s.label) }
                    )
                }
            }
        }
    ) { pad ->
        NavHost(nav, startDestination = Screen.Home.route, modifier = Modifier.padding(pad)) {
            composable(Screen.Home.route)     { HomeScreen(vm) }
            composable(Screen.Edificios.route){ EdificiosScreen(vm) { nav.navigate("detalle/$it") } }
            composable(Screen.Mapa.route)     { SimpleScreen("Mapa (pendiente)") }
            composable(Screen.Perfil.route)   { SimpleScreen("Perfil de Usuario") }
            composable("detalle/{nombre}")    { DetalleScreen(it.arguments?.getString("nombre").orEmpty()) }
        }
    }
}

// ==================== PANTALLAS ====================
@Composable
fun HomeScreen(vm: SeleccionViewModel) {
    val edificio by vm.edificio.collectAsState()
    Column(Modifier.fillMaxSize().padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Text("Bienvenido a la app de Edificios")
        Text("Último edificio consultado: $edificio")
    }
}

@Composable
fun EdificiosScreen(vm: SeleccionViewModel, onClick: (String) -> Unit) {
    LazyColumn(
        Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Text("Edificios del Campus", fontSize = 22.sp, fontWeight = FontWeight.Bold)
        }
        // Estilo 1: TarjetaLugar
        items(edificios) { e ->
            TarjetaLugar(e.nombre, e.descripcion, painterResource(e.imagen), e.color) {
                vm.seleccionar(e.nombre); onClick(e.nombre)
            }
        }
        // Estilo 2: TarjetaCompacta (destacado)
        item {
            Text("Destacado", fontSize = 18.sp, fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(top = 16.dp))
            Spacer(Modifier.height(8.dp))
            TarjetaCompacta(edificios[0].nombre, painterResource(edificios[0].imagen)) {
                vm.seleccionar(edificios[0].nombre); onClick(edificios[0].nombre)
            }
        }
        // Estilo 3 (reto): TarjetaDestacada
        item {
            Text("Recomendado", fontSize = 18.sp, fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(top = 16.dp))
            Spacer(Modifier.height(8.dp))
            TarjetaDestacada(edificios[1], "NUEVO") {
                vm.seleccionar(edificios[1].nombre); onClick(edificios[1].nombre)
            }
        }
    }
}

@Composable
fun DetalleScreen(nombre: String) {
    val e = edificios.find { it.nombre == nombre }
    Column(Modifier.fillMaxSize().padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Text("Detalle del lugar", fontSize = 22.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(16.dp))
        Text("Nombre: $nombre", fontSize = 18.sp)
        Spacer(Modifier.height(8.dp))
        Text(e?.descripcion ?: "Sin descripción", fontSize = 14.sp)
    }
}

@Composable
fun SimpleScreen(texto: String) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text(texto) }
}

// ==================== TARJETAS (Lab 06) ====================

/** Estilo 1: Row + Column + imagen circular */
@Composable
fun TarjetaLugar(
    nombre: String, descripcion: String,
    imagen: Painter, color: Color, onClick: () -> Unit = {}
) {
    Surface(
        shape = RoundedCornerShape(24.dp),
        color = color,
        modifier = Modifier.fillMaxWidth().clickable { onClick() }
    ) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Image(
                painter = imagen,
                contentDescription = nombre,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(72.dp)
                    .clip(CircleShape)        // 1° recorta
                    .background(Color.White)  // 2° pinta el fondo recortado
                    .padding(8.dp)            // 3° espacio interno
            )
            Spacer(Modifier.width(16.dp))
            Column {
                Text(nombre, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                Text(descripcion, fontSize = 14.sp)
            }
        }
    }
}

/** Estilo 2: Box superpuesto con scrim y texto */
@Composable
fun TarjetaCompacta(
    nombre: String, imagen: Painter, onClick: () -> Unit = {}
) {
    Box(
        Modifier
            .fillMaxWidth()
            .height(140.dp)
            .clip(RoundedCornerShape(16.dp))
            .clickable { onClick() }
    ) {
        Image(imagen, nombre, contentScale = ContentScale.Crop, modifier = Modifier.matchParentSize())
        Box(Modifier.matchParentSize().background(Color.Black.copy(alpha = 0.35f)))
        Text(
            nombre, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp,
            modifier = Modifier.align(Alignment.BottomStart).padding(12.dp)
        )
    }
}

/** Estilo 3 (reto): Column + Row con ícono y etiqueta */
@Composable
fun TarjetaDestacada(e: Edificio, etiqueta: String, onClick: () -> Unit = {}) {
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = Color(0xFFFFFDE7),
        modifier = Modifier.fillMaxWidth().clickable { onClick() }
    ) {
        Column(Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Star, null, tint = Color(0xFFFFA000))
                Spacer(Modifier.width(6.dp))
                Text(etiqueta, color = Color(0xFFFF6F00), fontWeight = FontWeight.Bold, fontSize = 12.sp)
            }
            Spacer(Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Image(
                    painterResource(e.imagen), e.nombre,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.size(64.dp).clip(RoundedCornerShape(12.dp))
                )
                Spacer(Modifier.width(12.dp))
                Column {
                    Text(e.nombre, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    Text(e.descripcion, fontSize = 13.sp, color = Color.DarkGray)
                }
            }
        }
    }
}