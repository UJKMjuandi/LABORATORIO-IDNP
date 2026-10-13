package com.example.planointeractivo_compose

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import com.example.planointeractivo_compose.ui.theme.PlanoInteractivo_ComposeTheme
import androidx.activity.compose.setContent
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.planointeractivo_compose.ui.theme.PlanoInteractivo_ComposeTheme
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            PlanoInteractivo_ComposeTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    PlanoAmbiente()
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun PlanoAmbientePreview() {
    PlanoInteractivo_ComposeTheme {
        PlanoAmbiente()
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
    PlanoInteractivo_ComposeTheme {
        Greeting("Android")
    }
}

data class Marcador(
    val offset: Offset,
    val radio: Float,
    val titulo: String,
    val descripcion: String
)

@Composable
fun PlanoAmbiente() {
    val marcadores = remember {
        listOf(
            Marcador(Offset(100f, 150f), 40f, "Sala A", "Sala de cómputo 1"),
            Marcador(Offset(250f, 150f), 40f, "Sala B", "Sala de cómputo 2")
        )
    }
    var marcadorSeleccionado by remember { mutableStateOf<Marcador?>(null) }
    Canvas(
        modifier = Modifier
            .fillMaxWidth()
            .height(300.dp)
            .pointerInput(Unit) {
                detectTapGestures { offset ->
                    marcadores.forEach { marcador ->
                        if ((offset - marcador.offset).getDistance() <= marcador.radio) {
                            marcadorSeleccionado = marcador
                        }
                    }                }
            }
    ) {

        // Salón 1
        drawRect(
            color = Color(0xFFBBDEFB),
            topLeft = Offset(20f, 20f),
            size = Size(150f, 250f)
        )
        // Salón 2
        drawRect(
            color = Color(0xFFC8E6C9),
            topLeft = Offset(180f, 20f),
            size = Size(150f, 250f)
        )
        // Pared divisoria
        drawLine(
            color = Color.DarkGray,
            start = Offset(170f, 20f),
            end = Offset(170f, 270f),
            strokeWidth = 4f
        )

        marcadores.forEach { marcador ->
            drawCircle(
                color = Color.Red,
                radius = marcador.radio,
                center = marcador.offset
            )
        }


    }

    marcadorSeleccionado?.let { marcador ->
        AlertDialog(
            onDismissRequest = { marcadorSeleccionado = null },
            title = { Text(marcador.titulo) },
            text = { Text(marcador.descripcion) },
            confirmButton = {
                TextButton(onClick = { marcadorSeleccionado = null }) {
                    Text("Cerrar")
                }
            }
        )
    }
}