package net.ilkinulas

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.zaxxer.hikari.HikariDataSource
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.transaction
import org.joda.time.DateTime
import org.slf4j.LoggerFactory
import spark.Spark.*
import javax.sql.DataSource


val logger = LoggerFactory.getLogger("todo")
val port = 9000
val mapper by lazy { jacksonObjectMapper() }

fun main(args: Array<String>) {
    Config.init()
    setupDatabase()

    port(port)
    staticFileLocation("/public/")
    //externalStaticFileLocation("src/main/resources/public") //used for development

    logger.info("Todo App is ready, listening on port $port")
    path("/todo/") {
        get("") { _, _ ->
            transaction {
                val todos = Todos.selectAll().map { rowToTodo(it) }
                mapper.writeValueAsString(todos)
            }
        }

        post("") { req, _ ->
            val todoText = req.body()
            if (todoText.isNullOrEmpty()) badRequest("Todo text must be non empty.")
            transaction {
                val todoId = Todos.insert {
                    it[text] = todoText
                } get Todos.id ?: 0
                val todo = Todos.select { (Todos.id eq todoId) }.map { rowToTodo(it) }.first()
                mapper.writeValueAsString(todo)
            }
        }

        put(":id") { req, _ ->
            val updateReq = mapper.readTree(req.body())
            transaction {
                val updated = Todos.update({ Todos.id eq req.params("id").toInt() }) {
                    it[text] = updateReq.get("text").asText()
                    it[done] = updateReq.get("done").asBoolean()
                }
                val todo = Todos.select { Todos.id eq req.params("id").toInt() }.map { rowToTodo(it) }
                if (updated == 1) mapper.writeValueAsString(todo)
                else serverError("Failed to update todo")
            }
        }

        delete(":id") { req, _ ->
            transaction {
                val deleted = Todos.deleteWhere { Todos.id eq req.params("id").toInt() }
                if (deleted == 1) "ok"
                else serverError("Failed to delete todo ${req.params("id")}")
            }
        }
    }
}

private fun rowToTodo(it: ResultRow) =
        Todo(it[Todos.id], it[Todos.text], it[Todos.done], it[Todos.dateCreated])

fun badRequest(reason: String) = halt(400, reason)
fun serverError(reason: String) = halt(500, reason)

object Todos : Table() {
    val id = integer("id").autoIncrement().primaryKey()
    val text = varchar("text", 256)
    val done = bool("done").default(false)
    val dateCreated = date("date_created").default(DateTime.now())
}

data class Todo(val id: Int = 0, val text: String, val done: Boolean = false, val dateCreated: DateTime = DateTime.now())

fun setupDatabase() {
    val ds = createDataSource()
    Database.connect(ds)
    transaction {
        SchemaUtils.create(Todos)
        addTestData()
    }
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

fun addTestData() {
    Todos.insert { it[text] = "Prepare presentation for JavaDay 2018." }
    Todos.insert { it[text] = "Favor object composition over class inheritance." }
    Todos.insert { it[text] = "Automate your tests." }
}