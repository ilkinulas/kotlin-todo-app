package net.ilkinulas

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import kotliquery.*
import org.joda.time.DateTime
import org.slf4j.LoggerFactory
import spark.Spark.*

val logger = LoggerFactory.getLogger("todo")
val port = 9000

fun main(args: Array<String>) {
    Config.init()
    setupDatabase()
    port(port)
    staticFileLocation("/public/")
    //externalStaticFileLocation("src/main/resources/public") //used for development

    logger.info("Todo App is ready, listening on port $port")
    path("/todo/") {
        get("") { _, _ ->
            //TODO pagination
            jacksonObjectMapper().writeValueAsString(Todo.all())
        }

        post("") { req, _ ->
            val text = req.body()
            if (text.isNullOrEmpty()) badRequest("Todo text must be non empty.")
            val todoId = Todo(text = text).insert()
            if (todoId == null) serverError("Can not save todo.")
            else jacksonObjectMapper().writeValueAsString(Todo.get(todoId))
        }

        put(":id") { req, _ ->
            val updateReq = jacksonObjectMapper().readTree(req.body())

            val updated = Todo.update(
                    req.params("id").toLong(),
                    updateReq.get("text").asText(),
                    updateReq.get("done").asBoolean())
            if (updated == 1) jacksonObjectMapper().writeValueAsString(Todo.get(req.params("id").toLong()))
            else serverError("Failed to update todo")
        }

        delete(":id") { req, _ ->
            val deleted = using(sessionOf(HikariCP.dataSource())) { session ->
                session.run(queryOf("DELETE FROM TODOS WHERE ID=?", req.params("id").toLong()).asUpdate)
            }

            if (deleted == 1) "ok"
            else serverError("Failed to delete todo ${req.params("id")}")
        }
    }
}

fun badRequest(reason: String) = halt(400, reason)
fun serverError(reason: String) = halt(500, reason)

data class Todo(val id: Long = 0, val text: String, val done: Boolean = false, val dateCreated: DateTime = DateTime.now()) {
    companion object {
        fun all() = using(sessionOf(HikariCP.dataSource())) { con ->
            con.run(queryOf("SELECT * FROM TODOS").map(rowToTodo).asList)
        }

        fun get(id: Long) = using(sessionOf(HikariCP.dataSource())) { con ->
            con.run(queryOf("SELECT * FROM TODOS WHERE ID=?", id).map(rowToTodo).asSingle)
        }

        fun delete(id: Long) = using(sessionOf(HikariCP.dataSource())) { con ->
            con.run(queryOf("DELETE FROM FROM TODOS WHERE ID=?", id).asUpdate)
        }

        fun update(id: Long, text: String, done: Boolean) = using(sessionOf(HikariCP.dataSource())) { con ->
            con.run(queryOf("UPDATE TODOS SET TEXT=?, DONE=? WHERE ID=?", text, done, id).asUpdate)
        }
    }
}

fun setupDatabase() {
    HikariCP.default(Config.get("jdbc.url"), Config.get("jdbc.user"), Config.get("jdbc.password"))
    if (Config.isInMemoryDb()) {
        using(sessionOf(HikariCP.dataSource())) {
            it.run(queryOf(Config.readFromClassPath("/sql/todo-h2.sql")).asExecute)
        }
    }
    addTestData()
}

fun Todo.insert() = using(sessionOf(HikariCP.dataSource())) { con ->
    con.run(queryOf("INSERT INTO TODOS (TEXT) VALUES (?)", this.text).asUpdateAndReturnGeneratedKey)
}


val rowToTodo: (Row) -> Todo = { row -> Todo(row.long("ID"), row.string("TEXT"), row.boolean("DONE"), row.jodaDateTime("DATE_CREATED")) }

fun addTestData() {
    Todo(text = "Prepare presentation for JavaDay 2018.").insert()
    Todo(text = "Favor object composition over class inheritance.").insert()
    Todo(text = "Automate your tests.").insert()
}