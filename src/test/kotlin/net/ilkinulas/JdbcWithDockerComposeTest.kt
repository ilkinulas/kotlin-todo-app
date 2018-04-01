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
import org.junit.rules.RuleChain
import javax.sql.DataSource

const val SERVICE_PORT = 3306
const val SERVICE_NAME = "database_1"

class JdbcWithDockerComposeTest {
    companion object {

        val database = KDockerComposeContainer("docker-compose.yml")
                .withExposedService(SERVICE_NAME, SERVICE_PORT)
                .withLocalCompose(true) //Docker-compose version 3 is not supported by TestContainers.
                .withPull(false) //TODO remove this before pushing.

        val databaseHealthCheck = HealthCheckRule(fun() = databaseHealthCheck(), 50, 100)

        lateinit var dataSource: DataSource

        @ClassRule
        @JvmField
        val ruleChain = RuleChain.outerRule(database).around(databaseHealthCheck)

        fun createDataSource(maxConnections: Int = 1): DataSource {
            val ds = HikariDataSource()
            val host = database.getServiceHost(SERVICE_NAME, SERVICE_PORT)
            val port = database.getServicePort(SERVICE_NAME, SERVICE_PORT)

            ds.driverClassName = "com.mysql.cj.jdbc.Driver"
            ds.jdbcUrl = "jdbc:mysql://$host:$port/tododb?nullNamePatternMatchesAll=true"
            ds.username = "todouser"
            ds.password = "todopass"
            ds.maximumPoolSize = maxConnections
            val connection = ds.connection
            connection.close()
            return ds
        }

        fun databaseHealthCheck() =
                try {
                    dataSource = createDataSource()
                    SuccessOrFailure.success()
                } catch (e: Exception) {
                    SuccessOrFailure.fail(e.message ?: "Failed to create DataSource")
                }
    }

    lateinit var dao: TodoJdbcDao

    @Before
    fun setup() {
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



