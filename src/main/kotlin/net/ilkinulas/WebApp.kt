package net.ilkinulas

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import org.jetbrains.exposed.sql.transactions.transaction
import org.slf4j.LoggerFactory
import spark.Spark.*


val logger = LoggerFactory.getLogger("todo")
val port = 9000
val mapper by lazy { jacksonObjectMapper() }

fun main(args: Array<String>) {
    Config.init()

    val ds = setupDatabase()
    val todoDao: TodoDao = TodoJdbcDao(ds)

    port(port)
    staticFileLocation("/public/")
    //externalStaticFileLocation("src/main/resources/public") //used for development

    logger.info("Todo App is ready, listening on port $port")
    path("/todo/") {
        get("") { _, _ ->
            transaction {
                mapper.writeValueAsString(todoDao.selectAll())
            }
        }

        post("") { req, _ ->
            val todoText = req.body()
            if (todoText.isNullOrEmpty()) badRequest("Todo text must be non empty.")
            transaction {
                val todoId = todoDao.insert(todoText)
                val todo = todoDao.select(todoId)
                mapper.writeValueAsString(todo)
            }
        }

        put(":id") { req, _ ->
            val updateReq = mapper.readTree(req.body())
            transaction {
                val todoId = req.params("id").toInt()
                val updated = todoDao.update(
                        todoId,
                        updateReq.get("text").asText(),
                        updateReq.get("done").asBoolean())
                val todo = todoDao.select(todoId)
                if (updated == 1) mapper.writeValueAsString(todo)
                else serverError("Failed to update todo")
            }
        }

        delete(":id") { req, _ ->
            transaction {
                val deleted = todoDao.delete(req.params("id").toInt())
                if (deleted == 1) "ok"
                else serverError("Failed to delete todo ${req.params("id")}")
            }
        }
    }
}

fun badRequest(reason: String) = halt(400, reason)
fun serverError(reason: String) = halt(500, reason)