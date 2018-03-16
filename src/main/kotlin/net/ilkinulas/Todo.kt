package net.ilkinulas

import com.zaxxer.hikari.HikariDataSource
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.transaction
import org.joda.time.DateTime
import javax.sql.DataSource

data class Todo(val id: Int = 0, val text: String, val done: Boolean = false, val dateCreated: DateTime = DateTime.now())

interface TodoDao {
    fun selectAll(): List<Todo>
    fun select(id: Int): Todo
    fun insert(todoText: String): Int
    fun update(id: Int, todoText: String, done: Boolean): Int
    fun delete(id: Int): Int
}

object Todos : Table() {
    val id = integer("id").autoIncrement().primaryKey()
    val text = varchar("text", 256)
    val done = bool("done").default(false)
    val dateCreated = date("date_created").default(DateTime.now())
}

class TodoJdbcDao(val dataSource: DataSource) : TodoDao {

    init {
        Database.connect(this.dataSource)
    }

    override fun selectAll() = Todos.selectAll().map { rowToTodo(it) }

    override fun select(id: Int) = Todos.select { (Todos.id eq id) }.map { rowToTodo(it) }.first()

    override fun insert(todoText: String) = Todos.insert { it[Todos.text] = todoText } get Todos.id ?: 0

    override fun update(id: Int, todoText: String, done: Boolean) =
            Todos.update({ Todos.id eq id }) {
                it[Todos.text] = todoText
                it[Todos.done] = done
            }

    override fun delete(id: Int) = Todos.deleteWhere { Todos.id eq id }

    private fun rowToTodo(it: ResultRow) =
            Todo(it[Todos.id], it[Todos.text], it[Todos.done], it[Todos.dateCreated])
}

fun addTestData() {
    Todos.insert { it[text] = "Prepare presentation for JavaDay 2018." }
    Todos.insert { it[text] = "Favor object composition over class inheritance." }
    Todos.insert { it[text] = "Automate your tests." }
}

fun createDataSource(): DataSource {
    val ds = HikariDataSource()

    val dbUrl = Config.get("DB_URL")
    ds.driverClassName = when {
        dbUrl.contains(":h2:") -> "org.h2.Driver"
        dbUrl.contains(":mysql:") -> "com.mysql.cj.jdbc.Driver"
        else -> throw RuntimeException("Unrecognized db url $dbUrl")
    }
    ds.jdbcUrl = dbUrl
    ds.username = Config.get("DB_USER")
    ds.password = Config.get("DB_PASSWORD")

    for (i in (1..5)) {
        try {
            val con = ds.connection
            con.close()
            return ds
        } catch (e: Exception) {
            Thread.sleep(3000)
        }
    }
    throw RuntimeException("Database setup failed.")
}

fun setupDatabase(): DataSource {
    val ds = createDataSource()
    Database.connect(ds)
    transaction {
        SchemaUtils.create(Todos)
        addTestData()
    }
    return ds
}
