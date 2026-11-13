package ar.jornadas.data

import androidx.room.*
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import retrofit2.http.GET
import kotlinx.coroutines.flow.Flow

data class FichaRemota(val id: String, val title: String, val track: String, val day: Int,
    val time: String, val room: String, val speaker: String, val summary: String)

interface ProgramaApi {
    @GET("programa") suspend fun consultar(): List<FichaRemota>
}

@Entity(tableName = "charlas")
data class Charla(@PrimaryKey val id: String, val titulo: String, val tema: String, val dia: Int,
    val hora: String, val sala: String, val ponente: String, val resumen: String,
    val favorita: Boolean = false)

/**
 * Pedido 41: una sola fila (id = 0) que indica "hay una edición válida descargada".
 * Hace falta porque una edición exitosa puede tener cero charlas (v4): sin esta fila,
 * una tabla `charlas` vacía no distingue "nunca descargué" de "descargué un programa vacío".
 */
@Entity(tableName = "edicion")
data class Edicion(@PrimaryKey val id: Int = 0, val descargadaEn: Long)

@Dao
abstract class CarteleraDao {
    @Query("SELECT * FROM charlas ORDER BY dia, hora, sala")
    abstract fun observar(): Flow<List<Charla>>

    @Query("SELECT * FROM edicion WHERE id = 0")
    abstract fun observarEdicion(): Flow<Edicion?>

    @Query("UPDATE charlas SET favorita = NOT favorita WHERE id = :id")
    abstract suspend fun alternar(id: String)

    @Query("SELECT id FROM charlas WHERE favorita = 1")
    protected abstract suspend fun idsFavoritas(): List<String>

    @Query("DELETE FROM charlas")
    protected abstract suspend fun borrarCharlas()

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    protected abstract suspend fun insertar(fichas: List<Charla>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    protected abstract suspend fun registrar(edicion: Edicion)

    /**
     * Reemplaza el programa por la instantánea recibida, en una única transacción
     * (si algo falla, no queda un programa a medias y Room notifica una sola vez).
     * La organización publica instantáneas completas: lo que no viene, se retira.
     *
     * Pedido 42: las marcas se leen recién acá, cuando ya llegó la respuesta y dentro de la
     * transacción, así que incluyen lo que el usuario marcó o desmarcó durante la petición.
     * - Charla vigente (mismo id): conserva su marca aunque cambie título o sala.
     * - Charla retirada: se borra con su marca; si vuelve en otra edición, vuelve sin marcar.
     */
    @Transaction
    open suspend fun aplicarEdicion(fichas: List<Charla>, momento: Long) {
        val marcadas = idsFavoritas().toSet()
        borrarCharlas()
        insertar(fichas.map { it.copy(favorita = it.id in marcadas) })
        registrar(Edicion(descargadaEn = momento))
    }
}

@Database(entities = [Charla::class, Edicion::class], version = 2, exportSchema = false)
abstract class Archivo : RoomDatabase() {
    abstract fun cartelera(): CarteleraDao

    companion object {
        /**
         * v1 (versión 1.2 instalada) → v2: agrega la tabla `edicion` sin tocar `charlas`,
         * así los usuarios conservan charlas y marcas. Si ya tenían charlas guardadas,
         * quedan registrados como "con edición previa" (pueden usar la app sin señal).
         */
        val MIGRACION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("CREATE TABLE IF NOT EXISTS `edicion` (`id` INTEGER NOT NULL, " +
                    "`descargadaEn` INTEGER NOT NULL, PRIMARY KEY(`id`))")
                db.execSQL("INSERT INTO edicion (id, descargadaEn) " +
                    "SELECT 0, 0 WHERE EXISTS (SELECT 1 FROM charlas)")
            }
        }
    }
}
