package com.example.persistenciaroom_compose

import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.room.*
import com.example.persistenciaroom_compose.ui.theme.PersistenciaRoom_ComposeTheme
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

// ---------- ENTIDADES ----------
@Entity
data class EdificioEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val nombre: String,
    val categoria: String
)

@Entity(
    foreignKeys = [ForeignKey(
        entity = EdificioEntity::class,
        parentColumns = ["id"],
        childColumns = ["edificioId"],
        onDelete = ForeignKey.CASCADE
    )],
    indices = [Index("edificioId")]
)
data class AmbienteEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val edificioId: Int,
    val nombre: String
)

@Entity(
    foreignKeys = [ForeignKey(
        entity = AmbienteEntity::class,
        parentColumns = ["id"],
        childColumns = ["ambienteId"],
        onDelete = ForeignKey.CASCADE
    )],
    indices = [Index("ambienteId")]
)
data class NotaEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val ambienteId: Int,
    val texto: String
)

// ---------- RELACIONES ----------
// Consulta relacional 1 (del ejercicio resuelto)
data class EdificioConAmbientes(
    @Embedded val edificio: EdificioEntity,
    @Relation(parentColumn = "id", entityColumn = "edificioId")
    val ambientes: List<AmbienteEntity>
)

// Consulta relacional 2 (nueva)
data class AmbienteConNotas(
    @Embedded val ambiente: AmbienteEntity,
    @Relation(parentColumn = "id", entityColumn = "ambienteId")
    val notas: List<NotaEntity>
)

// ---------- DAO ----------
@Dao
interface EdificioDao {
    // Edificio
    @Insert suspend fun inserta rEdificio(edificio: EdificioEntity): Long
    @Update suspend fun actualizarEdificio(edificio: EdificioEntity)
    @Delete suspend fun eliminarEdificio(edificio: EdificioEntity)

    // Ambiente
    @Insert suspend fun insertarAmbiente(ambiente: AmbienteEntity): Long
    @Update suspend fun actualizarAmbiente(ambiente: AmbienteEntity)
    @Delete suspend fun eliminarAmbiente(ambiente: AmbienteEntity)

    // Nota
    @Insert suspend fun insertarNota(nota: NotaEntity): Long
    @Update suspend fun actualizarNota(nota: NotaEntity)
    @Delete suspend fun eliminarNota(nota: NotaEntity)

    @Query("SELECT * FROM EdificioEntity")
    suspend fun obtenerTodos(): List<EdificioEntity>

    // Consultas relacionales (Flow: la UI se actualiza sola al cambiar la BD)
    @Transaction
    @Query("SELECT * FROM EdificioEntity")
    fun observarEdificiosConAmbientes(): Flow<List<EdificioConAmbientes>>

    @Transaction
    @Query("SELECT * FROM AmbienteEntity")
    fun observarAmbientesConNotas(): Flow<List<AmbienteConNotas>>
}

// ---------- BASE DE DATOS ----------
@Database(
    entities = [EdificioEntity::class, AmbienteEntity::class, NotaEntity::class],
    version = 2
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun edificioDao(): EdificioDao
}

fun crearBaseDeDatos(context: Context): AppDatabase {
    return Room.databaseBuilder(context, AppDatabase::class.java, "app.db")
        .fallbackToDestructiveMigration(dropAllTables = true)
        .build()
}

// ---------- VIEWMODEL ----------
class EdificiosViewModel(private val dao: EdificioDao) : ViewModel() {

    val edificios: StateFlow<List<EdificioConAmbientes>> =
        dao.observarEdificiosConAmbientes()
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val notasPorAmbiente: StateFlow<Map<Int, List<NotaEntity>>> =
        dao.observarAmbientesConNotas()
            .map { lista -> lista.associate { it.ambiente.id to it.notas } }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyMap())

    init {
        viewModelScope.launch {
            if (dao.obtenerTodos().isEmpty()) {
                val id1 = dao.insertarEdificio(
                    EdificioEntity(nombre = "Biblioteca Central", categoria = "Servicios")
                ).toInt()
                val sala = dao.insertarAmbiente(
                    AmbienteEntity(edificioId = id1, nombre = "Sala de lectura")
                ).toInt()
                dao.insertarAmbiente(AmbienteEntity(edificioId = id1, nombre = "Hemeroteca"))
                dao.insertarNota(NotaEntity(ambienteId = sala, texto = "Se reserva los martes"))
                dao.insertarNota(NotaEntity(ambienteId = sala, texto = "Aire acondicionado malogrado"))

                val id2 = dao.insertarEdificio(
                    EdificioEntity(nombre = "Facultad de Ingeniería", categoria = "Académico")
                ).toInt()
                val lab = dao.insertarAmbiente(
                    AmbienteEntity(edificioId = id2, nombre = "Laboratorio de cómputo")
                ).toInt()
                dao.insertarNota(NotaEntity(ambienteId = lab, texto = "El proyector no funciona"))
            }
        }
    }

    fun actualizarEdificio(e: EdificioEntity, nuevoNombre: String) =
        viewModelScope.launch { dao.actualizarEdificio(e.copy(nombre = nuevoNombre)) }

    fun eliminarEdificio(e: EdificioEntity) =
        viewModelScope.launch { dao.eliminarEdificio(e) }

    fun actualizarAmbiente(a: AmbienteEntity, nuevoNombre: String) =
        viewModelScope.launch { dao.actualizarAmbiente(a.copy(nombre = nuevoNombre)) }

    fun eliminarAmbiente(a: AmbienteEntity) =
        viewModelScope.launch { dao.eliminarAmbiente(a) }

    fun agregarNota(ambienteId: Int, texto: String) =
        viewModelScope.launch { dao.insertarNota(NotaEntity(ambienteId = ambienteId, texto = texto)) }
}

class EdificiosViewModelFactory(private val dao: EdificioDao) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        @Suppress("UNCHECKED_CAST")
        return EdificiosViewModel(dao) as T
    }
}

// ---------- UI ----------
@Composable
fun DialogoTexto(
    titulo: String,
    valorInicial: String,
    onConfirmar: (String) -> Unit,
    onCancelar: () -> Unit
) {
    var texto by remember { mutableStateOf(valorInicial) }
    AlertDialog(
        onDismissRequest = onCancelar,
        title = { Text(titulo) },
        text = { OutlinedTextField(value = texto, onValueChange = { texto = it }) },
        confirmButton = {
            TextButton(onClick = { if (texto.isNotBlank()) onConfirmar(texto) }) { Text("Guardar") }
        },
        dismissButton = { TextButton(onClick = onCancelar) { Text("Cancelar") } }
    )
}

@Composable
fun ListaEdificiosConAmbientes(viewModel: EdificiosViewModel) {
    val edificios by viewModel.edificios.collectAsState()
    val notas by viewModel.notasPorAmbiente.collectAsState()

    var edificioEditando by remember { mutableStateOf<EdificioEntity?>(null) }
    var ambienteEditando by remember { mutableStateOf<AmbienteEntity?>(null) }
    var ambienteNota by remember { mutableStateOf<AmbienteEntity?>(null) }

    LazyColumn(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        items(edificios, key = { it.edificio.id }) { registro ->
            Column(modifier = Modifier.padding(vertical = 8.dp)) {
                Text(
                    text = registro.edificio.nombre,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                )
                Row {
                    TextButton(onClick = { edificioEditando = registro.edificio }) { Text("Editar") }
                    TextButton(onClick = { viewModel.eliminarEdificio(registro.edificio) }) { Text("Eliminar") }
                }
                registro.ambientes.forEach { ambiente ->
                    Column(modifier = Modifier.padding(start = 16.dp)) {
                        Text(text = "- ${ambiente.nombre}", fontSize = 14.sp)
                        Row {
                            TextButton(onClick = { ambienteEditando = ambiente }) { Text("Editar") }
                            TextButton(onClick = { viewModel.eliminarAmbiente(ambiente) }) { Text("Eliminar") }
                            TextButton(onClick = { ambienteNota = ambiente }) { Text("+ Nota") }
                        }
                        notas[ambiente.id].orEmpty().forEach { nota ->
                            Text(
                                text = "• ${nota.texto}",
                                fontSize = 12.sp,
                                modifier = Modifier.padding(start = 16.dp)
                            )
                        }
                    }
                }
            }
        }
    }

    edificioEditando?.let { e ->
        DialogoTexto(
            titulo = "Editar edificio",
            valorInicial = e.nombre,
            onConfirmar = { viewModel.actualizarEdificio(e, it); edificioEditando = null },
            onCancelar = { edificioEditando = null }
        )
    }
    ambienteEditando?.let { a ->
        DialogoTexto(
            titulo = "Editar ambiente",
            valorInicial = a.nombre,
            onConfirmar = { viewModel.actualizarAmbiente(a, it); ambienteEditando = null },
            onCancelar = { ambienteEditando = null }
        )
    }
    ambienteNota?.let { a ->
        DialogoTexto(
            titulo = "Nueva nota para ${a.nombre}",
            valorInicial = "",
            onConfirmar = { viewModel.agregarNota(a.id, it); ambienteNota = null },
            onCancelar = { ambienteNota = null }
        )
    }
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val database = crearBaseDeDatos(applicationContext)
        val dao = database.edificioDao()
        val factory = EdificiosViewModelFactory(dao)

        setContent {
            PersistenciaRoom_ComposeTheme {
                val viewModel: EdificiosViewModel = viewModel(factory = factory)
                Surface(modifier = Modifier.fillMaxSize()) {
                    ListaEdificiosConAmbientes(viewModel)
                }
            }
        }
    }
}