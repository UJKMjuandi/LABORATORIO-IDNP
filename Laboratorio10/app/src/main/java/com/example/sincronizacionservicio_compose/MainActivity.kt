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
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
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
import androidx.room.Transaction
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
    val viewModel: EdificiosViewModel = viewModel(
        factory = EdificiosViewModelFactory(dao)
    )
    val edificios by viewModel.edificios.collectAsState()

    var resultado by remember { mutableStateOf<String?>(null) }
    var enCurso by remember { mutableStateOf(false) }
    var edificioAEditar by remember { mutableStateOf<EdificioEntity?>(null) }
    var ambienteAEditar by remember { mutableStateOf<AmbienteEntity?>(null) }
    var ambienteParaNota by remember { mutableStateOf<AmbienteEntity?>(null) }

    DisposableEffect(Unit) {
        val receiverExito = object : BroadcastReceiver() {
            override fun onReceive(ctx: Context?, intent: Intent?) {
                Log.d("SYNC", "3. Aviso recibido")
                val cantidad = intent?.getIntExtra(EXTRA_EDIFICIOS, 0) ?: 0
                resultado = "Sincronización completa: $cantidad edificios actualizados"
                enCurso = false
            }
        }
        val receiverError = object : BroadcastReceiver() {
            override fun onReceive(ctx: Context?, intent: Intent?) {
                Log.d("SYNC", "3b. Aviso de error")
                val mensaje = intent?.getStringExtra(EXTRA_MENSAJE_ERROR) ?: "Error"
                resultado = "⚠️ $mensaje"
                enCurso = false
            }
        }
        val filtroExito = IntentFilter(ACCION_SYNC_LISTA)
        val filtroError = IntentFilter(ACCION_SYNC_ERROR)

        ContextCompat.registerReceiver(context, receiverExito, filtroExito, ContextCompat.RECEIVER_NOT_EXPORTED)
        ContextCompat.registerReceiver(context, receiverError, filtroError, ContextCompat.RECEIVER_NOT_EXPORTED)

        onDispose {
            context.unregisterReceiver(receiverExito)
            context.unregisterReceiver(receiverError)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .padding(16.dp)
    ) {
        Button(onClick = {
            resultado = null
            enCurso = true
            context.startService(Intent(context, SincronizacionService::class.java))
        }) { Text("Sincronizar") }

        Spacer(Modifier.height(8.dp))

        Button(
            onClick = {
                context.stopService(Intent(context, SincronizacionService::class.java))
                resultado = "Sincronización detenida manualmente"
                enCurso = false
            },
            enabled = enCurso
        ) { Text("Detener sincronización") }

        Spacer(Modifier.height(8.dp))

        if (resultado == null) CircularProgressIndicator()
        else Text(resultado ?: "")

        Spacer(Modifier.height(16.dp))
        HorizontalDivider()
        Spacer(Modifier.height(8.dp))

        LazyColumn(modifier = Modifier.fillMaxSize()) {
            items(edificios, key = { it.edificio.id }) { item ->
                Column(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "${item.edificio.nombre} (${item.edificio.categoria})",
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.weight(1f)
                        )
                        TextButton(onClick = { edificioAEditar = item.edificio }) { Text("Editar") }
                        TextButton(onClick = { viewModel.eliminarEdificio(item.edificio) }) { Text("Eliminar") }
                    }

                    item.ambientes.forEach { ambienteConNotas ->
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(start = 16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("• ${ambienteConNotas.ambiente.nombre}", modifier = Modifier.weight(1f))
                            TextButton(onClick = { ambienteAEditar = ambienteConNotas.ambiente }) { Text("Editar") }
                            TextButton(onClick = { viewModel.eliminarAmbiente(ambienteConNotas.ambiente) }) { Text("Eliminar") }
                            TextButton(onClick = { ambienteParaNota = ambienteConNotas.ambiente }) { Text("+ Nota") }
                        }
                        ambienteConNotas.notas.forEach { nota ->
                            Text(
                                text = "    - ${nota.texto}",
                                modifier = Modifier.padding(start = 32.dp)
                            )
                        }
                    }
                }
                HorizontalDivider()
            }
        }
    }

    edificioAEditar?.let { edificio ->
        var texto by remember(edificio.id) { mutableStateOf(edificio.nombre) }
        AlertDialog(
            onDismissRequest = { edificioAEditar = null },
            title = { Text("Editar edificio") },
            text = { OutlinedTextField(value = texto, onValueChange = { texto = it }) },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.actualizarEdificio(edificio.copy(nombre = texto))
                    edificioAEditar = null
                }) { Text("Guardar") }
            },
            dismissButton = { TextButton(onClick = { edificioAEditar = null }) { Text("Cancelar") } }
        )
    }

    ambienteAEditar?.let { ambiente ->
        var texto by remember(ambiente.id) { mutableStateOf(ambiente.nombre) }
        AlertDialog(
            onDismissRequest = { ambienteAEditar = null },
            title = { Text("Editar ambiente") },
            text = { OutlinedTextField(value = texto, onValueChange = { texto = it }) },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.actualizarAmbiente(ambiente.copy(nombre = texto))
                    ambienteAEditar = null
                }) { Text("Guardar") }
            },
            dismissButton = { TextButton(onClick = { ambienteAEditar = null }) { Text("Cancelar") } }
        )
    }

    ambienteParaNota?.let { ambiente ->
        var texto by remember(ambiente.id) { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { ambienteParaNota = null },
            title = { Text("Nueva nota para ${ambiente.nombre}") },
            text = { OutlinedTextField(value = texto, onValueChange = { texto = it }) },
            confirmButton = {
                TextButton(onClick = {
                    if (texto.isNotBlank()) {
                        viewModel.insertarNota(NotaEntity(ambienteId = ambiente.id, texto = texto))
                    }
                    ambienteParaNota = null
                }) { Text("Guardar") }
            },
            dismissButton = { TextButton(onClick = { ambienteParaNota = null }) { Text("Cancelar") } }
        )
    }
}

class EdificiosViewModel(private val dao: EdificioDao) : ViewModel() {

    private val _edificios = MutableStateFlow<List<EdificioConAmbientesYNotas>>(emptyList())
    val edificios: StateFlow<List<EdificioConAmbientesYNotas>> = _edificios.asStateFlow()

    init {
        viewModelScope.launch {
            if (dao.contarEdificios() == 0) {
                val idBib = dao.insertarEdificio(EdificioEntity(nombre = "Biblioteca Central", categoria = "Biblioteca"))
                val idIng = dao.insertarEdificio(EdificioEntity(nombre = "Facultad de Ingeniería", categoria = "Académico"))
                dao.insertarAmbiente(AmbienteEntity(edificioId = idBib.toInt(), nombre = "Sala de lectura"))
                dao.insertarAmbiente(AmbienteEntity(edificioId = idBib.toInt(), nombre = "Hemeroteca"))
                dao.insertarAmbiente(AmbienteEntity(edificioId = idIng.toInt(), nombre = "Laboratorio de cómputo"))
            }
            dao.observarEdificiosConAmbientesYNotas().collect { lista ->
                _edificios.value = lista
            }
        }
    }

    fun actualizarEdificio(edificio: EdificioEntity) = viewModelScope.launch { dao.actualizarEdificio(edificio) }
    fun eliminarEdificio(edificio: EdificioEntity) = viewModelScope.launch { dao.eliminarEdificio(edificio) }
    fun actualizarAmbiente(ambiente: AmbienteEntity) = viewModelScope.launch { dao.actualizarAmbiente(ambiente) }
    fun eliminarAmbiente(ambiente: AmbienteEntity) = viewModelScope.launch { dao.eliminarAmbiente(ambiente) }
    fun insertarNota(nota: NotaEntity) = viewModelScope.launch { dao.insertarNota(nota) }
}

class EdificiosViewModelFactory(private val dao: EdificioDao) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        @Suppress("UNCHECKED_CAST")
        return EdificiosViewModel(dao) as T
    }
}

class SincronizacionService : Service() {

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d("SYNC", "1. Servicio iniciado")

        val db = Room.databaseBuilder(applicationContext, AppDatabase::class.java, "app.db").build()
        val dao = db.edificioDao()

        CoroutineScope(Dispatchers.IO).launch {
            try {
                delay(3000)

                val hayError = Random.nextInt(1, 100) <= 25
                if (hayError) {
                    val intentError = Intent(ACCION_SYNC_ERROR).apply {
                        setPackage(packageName)
                        putExtra(EXTRA_MENSAJE_ERROR, "Falló la sincronización con el servidor. Reintente.")
                    }
                    sendBroadcast(intentError)
                    Log.d("SYNC", "2b. Aviso de ERROR enviado")
                    stopSelf()
                    return@launch
                }

                val edificios = dao.obtenerTodosConAmbientes()
                var actualizados = 0
                for (item in edificios) {
                    val cumple = item.ambientes.isNotEmpty() && item.edificio.categoria.isNotBlank()
                    if (cumple) {
                        val nuevoNombre = item.edificio.nombre.substringBefore(" [SYNC]") + " [SYNC]"
                        dao.actualizarEdificio(item.edificio.copy(nombre = nuevoNombre))
                        actualizados++
                    }
                }

                val resultado = Intent(ACCION_SYNC_LISTA).apply {
                    setPackage(packageName)
                    putExtra(EXTRA_EDIFICIOS, actualizados)
                }
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

@Entity(
    foreignKeys = [
        ForeignKey(
            entity = AmbienteEntity::class,
            parentColumns = ["id"],
            childColumns = ["ambienteId"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class NotaEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val ambienteId: Int,
    val texto: String
)

data class EdificioConAmbientes(
    @Embedded val edificio: EdificioEntity,
    @Relation(parentColumn = "id", entityColumn = "edificioId")
    val ambientes: List<AmbienteEntity>
)

data class AmbienteConNotas(
    @Embedded val ambiente: AmbienteEntity,
    @Relation(parentColumn = "id", entityColumn = "ambienteId")
    val notas: List<NotaEntity>
)

data class EdificioConAmbientesYNotas(
    @Embedded val edificio: EdificioEntity,
    @Relation(
        entity = AmbienteEntity::class,
        parentColumn = "id",
        entityColumn = "edificioId"
    )
    val ambientes: List<AmbienteConNotas>
)

@Dao
interface EdificioDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertarEdificio(edificio: EdificioEntity): Long

    @Update
    suspend fun actualizarEdificio(edificio: EdificioEntity)

    @Query("DELETE FROM EdificioEntity WHERE id = :id")
    suspend fun eliminarEdificioPorId(id: Int)

    suspend fun eliminarEdificio(edificio: EdificioEntity) = eliminarEdificioPorId(edificio.id)

    @Query("SELECT COUNT(*) FROM EdificioEntity")
    suspend fun contarEdificios(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertarAmbiente(ambiente: AmbienteEntity): Long

    @Update
    suspend fun actualizarAmbiente(ambiente: AmbienteEntity)

    @Query("DELETE FROM AmbienteEntity WHERE id = :id")
    suspend fun eliminarAmbientePorId(id: Int)

    suspend fun eliminarAmbiente(ambiente: AmbienteEntity) = eliminarAmbientePorId(ambiente.id)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertarNota(nota: NotaEntity): Long

    @Update
    suspend fun actualizarNota(nota: NotaEntity)

    @Query("DELETE FROM NotaEntity WHERE id = :id")
    suspend fun eliminarNotaPorId(id: Int)

    suspend fun eliminarNota(nota: NotaEntity) = eliminarNotaPorId(nota.id)

    @Transaction
    @Query("SELECT * FROM EdificioEntity")
    suspend fun obtenerTodosConAmbientes(): List<EdificioConAmbientes>

    @Transaction
    @Query("SELECT * FROM AmbienteEntity")
    suspend fun observarAmbientesConNotas(): List<AmbienteConNotas>

    @Transaction
    @Query("SELECT * FROM EdificioEntity")
    fun observarEdificiosConAmbientesYNotas(): Flow<List<EdificioConAmbientesYNotas>>
}

@Database(
    entities = [EdificioEntity::class, AmbienteEntity::class, NotaEntity::class],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun edificioDao(): EdificioDao
}