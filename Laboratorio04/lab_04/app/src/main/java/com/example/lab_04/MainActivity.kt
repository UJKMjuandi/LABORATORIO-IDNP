package com.example.lab_04

import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.example.lab_04.ui.theme.IdnpTheme

private const val ACCION_ACTUALIZAR = "com.tuapp.ACTUALIZAR_BATERIA"

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            IdnpTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    BatteryScreen(modifier = Modifier.padding(innerPadding))
                }
            }
        }
    }
}

@Composable
fun BatteryScreen(modifier: Modifier = Modifier) {
    var porcentaje by remember { mutableStateOf(0) }
    var cargando by remember { mutableStateOf(false) }
    val context = LocalContext.current

    // DisposableEffect 1: receiver automático del sistema
    DisposableEffect(Unit) {
        val receiverSistema = object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                val nivel = intent?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: -1
                val escala = intent?.getIntExtra(BatteryManager.EXTRA_SCALE, -1) ?: -1
                if (nivel != -1 && escala != -1) {
                    porcentaje = (nivel * 100) / escala
                }
                val status = intent?.getIntExtra(BatteryManager.EXTRA_STATUS, -1) ?: -1
                cargando = status == BatteryManager.BATTERY_STATUS_CHARGING ||
                        status == BatteryManager.BATTERY_STATUS_FULL
            }
        }
        context.registerReceiver(
            receiverSistema,
            IntentFilter(Intent.ACTION_BATTERY_CHANGED)
        )
        Log.d("BatteryScreen", "Receiver del sistema registrado")
        onDispose {
            context.unregisterReceiver(receiverSistema)
            Log.d("BatteryScreen", "Receiver del sistema desregistrado")
        }
    }

    // DisposableEffect 2: receiver de la acción personalizada
    DisposableEffect(Unit) {
        val receiverManual = object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                if (intent?.action == ACCION_ACTUALIZAR) {
                    Log.d("BatteryScreen", "Broadcast manual recibido")
                    actualizarBateriaManual(context, { porcentaje = it }, { cargando = it })
                }
            }
        }
        val filtro = IntentFilter(ACCION_ACTUALIZAR)
        ContextCompat.registerReceiver(
            context,
            receiverManual,
            filtro,
            ContextCompat.RECEIVER_NOT_EXPORTED
        )
        Log.d("BatteryScreen", "Receiver manual registrado")
        onDispose {
            context.unregisterReceiver(receiverManual)
            Log.d("BatteryScreen", "Receiver manual desregistrado")
        }
    }

    Column(
        modifier = modifier.padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(text = "Batería: $porcentaje%")
        Text(text = if (cargando) "Estado: Cargando" else "Estado: No cargando")
        Button(onClick = {
            val intent = Intent(ACCION_ACTUALIZAR).setPackage(context.packageName)
            val pending = PendingIntent.getBroadcast(
                context,
                0,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            try {
                pending.send()
                Log.d("BatteryScreen", "PendingIntent enviado")
            } catch (e: PendingIntent.CanceledException) {
                Log.e("BatteryScreen", "Error al enviar PendingIntent", e)
            }
        }) {
            Text("Actualizar manualmente")
        }
    }
}

private fun actualizarBateriaManual(
    context: Context?,
    setPorcentaje: (Int) -> Unit,
    setCargando: (Boolean) -> Unit
) {
    val bm = context?.getSystemService(Context.BATTERY_SERVICE) as? BatteryManager ?: return
    val nivel = bm.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY)
    if (nivel in 0..100) setPorcentaje(nivel)

    val sticky = context.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
    val status = sticky?.getIntExtra(BatteryManager.EXTRA_STATUS, -1) ?: -1
    setCargando(
        status == BatteryManager.BATTERY_STATUS_CHARGING ||
                status == BatteryManager.BATTERY_STATUS_FULL
    )
}

@Preview(showBackground = true)
@Composable
fun BatteryScreenPreview() {
    IdnpTheme {
        BatteryScreen()
    }
}