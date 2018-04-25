package net.ilkinulas

import com.zaxxer.hikari.HikariDataSource
import junit.framework.TestCase.assertEquals
import org.jetbrains.exposed.sql.transactions.transaction
import org.junit.Before
import org.junit.ClassRule
import org.junit.Test
import org.testcontainers.containers.BindMode

class JdbcWithMysqlContainerTest {

    companion object {
        const val dbUser = "todouser"
        const val dbPassword = "todopass"

        @ClassRule
        @JvmField
        val database = KMysqlContainer("mysql:5.7.21")
                .withDatabaseName("tododb")
                .withUsername(dbUser)
                .withPassword(dbPassword)
                .withClasspathResourceMapping("init-scripts", "/docker-entrypoint-initdb.d", BindMode.READ_ONLY)

    }

    lateinit var dao: TodoJdbcDao

    @Before
    fun setup() {
        val ds = HikariDataSource()
        ds.driverClassName = "com.mysql.cj.jdbc.Driver"
        ds.jdbcUrl = database.jdbcUrl
        println("JDBC URL = ${database.jdbcUrl}")
        ds.username = dbUser
        ds.password = dbPassword
        ds.maximumPoolSize = 1

        dao = TodoJdbcDao(ds)
    }

    @Test
    fun test_insert_and_select_all() {
        transaction {
            val numRows = dao.selectAll().size
            for (i in 1..10) {
                dao.insert("todo-$i")
            }
            assertEquals(numRows + 10, dao.selectAll().size)
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