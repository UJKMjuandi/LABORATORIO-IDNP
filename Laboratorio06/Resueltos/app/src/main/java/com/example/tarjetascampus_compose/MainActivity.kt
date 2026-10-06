package com.example.tarjetascampus_compose

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.example.tarjetascampus_compose.ui.theme.TarjetasCampus_ComposeTheme
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.tarjetascampus_compose.ui.theme.TarjetasCampus_ComposeTheme
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            TarjetasCampus_ComposeTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    PantallaTarjetas()
                }
            }
        }
    }
}
@Composable
fun PantallaTarjetas() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Estilo 1: tarjetas con Row (imagen circular + texto)
        ListaTarjetasLugar()
        Spacer(modifier = Modifier.height(8.dp))
        // Estilo 2: tarjetas apiladas con Box (imagen de fondo + scrim + texto)
        TarjetaCompacta(
            nombre = "Biblioteca Central",
            imagen = painterResource(id = R.drawable.biblioteca)
        )
        Spacer(modifier = Modifier.height(12.dp))
        TarjetaCompacta(
            nombre = "Comedor Universitario",
            imagen = painterResource(id = R.drawable.comedor)
        )
    }
}
@Preview(showBackground = true)
@Composable
fun PantallaTarjetasPreview() {
    TarjetasCampus_ComposeTheme {
        PantallaTarjetas()
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
    TarjetasCampus_ComposeTheme {
        Greeting("Android")
    }
}

@Composable
fun TarjetaLugar(
    nombre: String,
    descripcion: String,
    imagen: Painter,
    colorFondo: Color
) {
    Surface(
        shape = RoundedCornerShape(24.dp),
        color = colorFondo
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(16.dp)
        ) {
            Image(
                painter = imagen,
                contentDescription = nombre,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(90.dp)
                    .clip(CircleShape)
            )
            Spacer(modifier = Modifier.width(16.dp))
            Column {
                Text(
                    text = nombre,
                    fontWeight = FontWeight.Bold,
                    fontSize = 20.sp
                )
                Text(
                    text = descripcion,
                    fontSize = 14.sp
                )
            }

        }

    }
}

@Composable
fun ListaTarjetasLugar() {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        TarjetaLugar(
            nombre = "Biblioteca Central",
            descripcion = "Zona de estudio y préstamo de libros",
            imagen = painterResource(id = R.drawable.biblioteca),
            colorFondo = Color(0xFFE3F2FD)
        )
        TarjetaLugar(
            nombre = "Comedor Universitario",
            descripcion = "Servicio de alimentación para estudiantes",
            imagen = painterResource(id = R.drawable.comedor),
            colorFondo = Color(0xFFFFF3E0)
        )
    }
}

@Composable
fun TarjetaCompacta(
    nombre: String,
    imagen: Painter
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(140.dp)
            .clip(RoundedCornerShape(16.dp))
    ) {
        Image(
            painter = imagen,
            contentDescription = nombre,
            contentScale = ContentScale.Crop,
            modifier = Modifier.matchParentSize()
        )
        Box(
            modifier = Modifier
                .matchParentSize()
                .background(Color.Black.copy(alpha = 0.35f))
        )
        Text(
            text = nombre,
            color = Color.White,
            fontWeight = FontWeight.Bold,
            fontSize = 18.sp,
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(12.dp)
        )
    }
}

