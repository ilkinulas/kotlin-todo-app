package net.ilkinulas

import com.zaxxer.hikari.HikariDataSource
import junit.framework.TestCase.assertEquals
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.deleteAll
import org.jetbrains.exposed.sql.transactions.transaction
import org.junit.Before
import org.junit.ClassRule
import org.junit.Test
import javax.sql.DataSource

const val SERVICE_PORT = 3306
const val SERVICE_NAME = "database_1"

class JdbcWithDockerComposeTest {
    companion object {
        @ClassRule
        @JvmField
        val dockerCompose = KDockerComposeContainer("docker-compose.yml")
                .withExposedService(SERVICE_NAME, SERVICE_PORT)
    }

    lateinit var dataSource: DataSource

    private fun createDataSource(maxConnections: Int = 1): DataSource {
        val ds = HikariDataSource()
        val host = dockerCompose.getServiceHost(SERVICE_NAME, SERVICE_PORT)
        val port = dockerCompose.getServicePort(SERVICE_NAME, SERVICE_PORT)

        ds.driverClassName = "com.mysql.cj.jdbc.Driver"
        ds.jdbcUrl = "jdbc:mysql://$host:$port/tododb"
        ds.username = "todouser"
        ds.password = "todopass"
        ds.maximumPoolSize = maxConnections
        val connection = ds.connection
        connection.close()
        return ds
    }

    lateinit var dao: TodoJdbcDao

    @Before
    fun setup() {
        dataSource = createDataSource()
        Database.connect(dataSource)
        transaction {
            SchemaUtils.create(Todos)
            Todos.deleteAll()
        }
        dao = TodoJdbcDao(dataSource)
    }

    @Test
    fun test_insert_and_select_all() {
        transaction {
            for (i in 1..10) {
                dao.insert("todo-$i")
            }
            assertEquals(10, dao.selectAll().size)
        }
    }

    @Test
    fun test_select_by_id() {
        transaction {
            val todoText = "Don't forget to call Dad."
            val id = dao.insert(todoText)
            val todo = dao.select(id)
            assertEquals(todoText, todo.text)
            assertEquals(id, todo.id)
        }
    }

    @Test
    fun test_update() {
        transaction {
            val todoText = "Don't forget to call Dad."
            val id = dao.insert(todoText)
            var todo = dao.select(id)
            assertEquals(todoText, todo.text)
            assertEquals(id, todo.id)
            assertEquals(false, todo.done)

            dao.update(id, "Don't forget to call Mom.", true)
            todo = dao.select(id)
            assertEquals(todoText.replace("Dad", "Mom"), todo.text)
            assertEquals(id, todo.id)
            assertEquals(true, todo.done)
        }
    }
}
