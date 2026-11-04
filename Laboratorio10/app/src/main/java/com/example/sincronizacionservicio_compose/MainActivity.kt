package com.example.sincronizacionservicio_compose

import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import android.os.IBinder
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Relation
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.Update
import com.example.sincronizacionservicio_compose.ui.theme.SincronizacionServicio_ComposeTheme
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlin.random.Random

const val ACCION_SYNC_LISTA = "com.example.sincronizacionservicio_compose.ACCION_SYNC_LISTA"
const val ACCION_SYNC_ERROR = "com.example.sincronizacionservicio_compose.ACCION_SYNC_ERROR"
const val EXTRA_EDIFICIOS = "edificios_actualizados"
const val EXTRA_MENSAJE_ERROR = "mensaje_error"

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val db = Room.databaseBuilder(
            applicationContext,
            AppDatabase::class.java,
            "app.db"
        ).build()
        val dao = db.edificioDao()

        setContent {
            SincronizacionServicio_ComposeTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    PantallaSincronizacion(dao)
                }
            }
        }
    }
}

@Composable
fun PantallaSincronizacion(dao: EdificioDao) {
    val context = LocalContext.current

    var resultado by remember { mutableStateOf<String?>(null) }
    var enCurso by remember { mutableStateOf(false) }

    DisposableEffect(Unit) {
        val receiverExito = object : BroadcastReceiver() {
            override fun onReceive(ctx: Context?, intent: Intent?) {
                Log.d("SYNC", "3. Aviso recibido en la pantalla")
                val cantidad = intent?.getIntExtra(EXTRA_EDIFICIOS, 0) ?: 0
                resultado = "Sincronización completa: $cantidad edificios actualizados"
                enCurso = false
            }
        }
        val receiverError = object : BroadcastReceiver() {
            override fun onReceive(ctx: Context?, intent: Intent?) {
                Log.d("SYNC", "3b. Aviso de error recibido")
                val mensaje = intent?.getStringExtra(EXTRA_MENSAJE_ERROR) ?: "Error desconocido"
                resultado = "⚠️ $mensaje"
                enCurso = false
            }
        }

        val filtroExito = IntentFilter(ACCION_SYNC_LISTA)
        val filtroError = IntentFilter(ACCION_SYNC_ERROR)

        ContextCompat.registerReceiver(
            context, receiverExito, filtroExito,
            ContextCompat.RECEIVER_NOT_EXPORTED
        )
        ContextCompat.registerReceiver(
            context, receiverError, filtroError,
            ContextCompat.RECEIVER_NOT_EXPORTED
        )

        onDispose {
            context.unregisterReceiver(receiverExito)
            context.unregisterReceiver(receiverError)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Button(onClick = {
            resultado = null
            enCurso = true
            context.startService(
                Intent(context, SincronizacionService::class.java)
            )
        }) {
            Text("Sincronizar")
        }

        Spacer(modifier = Modifier.height(8.dp))

        Button(
            onClick = {
                context.stopService(
                    Intent(context, SincronizacionService::class.java)
                )
                resultado = "Sincronización detenida manualmente"
                enCurso = false
                Log.d("SYNC", "Servicio detenido por el usuario")
            },
            enabled = enCurso
        ) {
            Text("Detener sincronización")
        }

        Spacer(modifier = Modifier.height(16.dp))

        if (resultado == null) {
            CircularProgressIndicator()
        } else {
            Text(resultado ?: "")
        }
    }
}

class SincronizacionService : Service() {

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d("SYNC", "1. Servicio iniciado")

        val db = Room.databaseBuilder(
            applicationContext,
            AppDatabase::class.java,
            "app.db"
        ).build()
        val dao = db.edificioDao()

        CoroutineScope(Dispatchers.IO).launch {
            try {
                delay(3000)

                val hayError = Random.nextInt(1, 100) <= 25
                if (hayError) {
                    val intentError = Intent(ACCION_SYNC_ERROR)
                    intentError.setPackage(packageName)
                    intentError.putExtra(
                        EXTRA_MENSAJE_ERROR,
                        "Falló la sincronización con el servidor. Reintente."
                    )
                    sendBroadcast(intentError)
                    Log.d("SYNC", "2b. Aviso de ERROR enviado")
                    stopSelf()
                    return@launch
                }

                val edificios = dao.obtenerTodosConAmbientes()

                var actualizados = 0
                for (edificioConAmbientes in edificios) {
                    val cumpleCondicion =
                        edificioConAmbientes.ambientes.isNotEmpty() &&
                                edificioConAmbientes.edificio.categoria.isNotBlank()

                    if (cumpleCondicion) {
                        val actualizado = edificioConAmbientes.edificio.copy(
                            nombre = edificioConAmbientes.edificio.nombre
                                .substringBefore(" [SYNC]") + " [SYNC]"
                        )
                        dao.actualizarEdificio(actualizado)
                        actualizados++
                    }
                }

                val resultado = Intent(ACCION_SYNC_LISTA)
                resultado.setPackage(packageName)
                resultado.putExtra(EXTRA_EDIFICIOS, actualizados)
                sendBroadcast(resultado)
                Log.d("SYNC", "2. Aviso enviado: $actualizados edificios reales")

                stopSelf()
            } catch (e: Exception) {
                Log.e("SYNC", "Error en el servicio", e)
                stopSelf()
            }
        }

        return START_NOT_STICKY
    }
}

@Entity
data class EdificioEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val nombre: String,
    val categoria: String
)

@Entity(
    foreignKeys = [
        ForeignKey(
            entity = EdificioEntity::class,
            parentColumns = ["id"],
            childColumns = ["edificioId"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class AmbienteEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val edificioId: Int,
    val nombre: String
)

data class EdificioConAmbientes(
    @Embedded val edificio: EdificioEntity,
    @Relation(
        parentColumn = "id",
        entityColumn = "edificioId"
    )
    val ambientes: List<AmbienteEntity>
)

@Dao
interface EdificioDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertarEdificio(edificio: EdificioEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertarAmbiente(ambiente: AmbienteEntity): Long

    @Update
    suspend fun actualizarEdificio(edificio: EdificioEntity)

    @Update
    suspend fun actualizarAmbiente(ambiente: AmbienteEntity)

    @Query("SELECT * FROM EdificioEntity")
    suspend fun obtenerTodos(): List<EdificioEntity>

    @Query("SELECT COUNT(*) FROM EdificioEntity")
    suspend fun contarEdificios(): Int

    @androidx.room.Transaction
    @Query("SELECT * FROM EdificioEntity")
    suspend fun obtenerTodosConAmbientes(): List<EdificioConAmbientes>

    @androidx.room.Transaction
    @Query("SELECT * FROM EdificioEntity WHERE id = :id")
    suspend fun obtenerPorIdConAmbientes(id: Int): EdificioConAmbientes?
}

@Database(
    entities = [EdificioEntity::class, AmbienteEntity::class],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun edificioDao(): EdificioDao
}