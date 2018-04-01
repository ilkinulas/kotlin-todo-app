package net.ilkinulas

import com.zaxxer.hikari.HikariDataSource
import junit.framework.TestCase
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.deleteAll
import org.jetbrains.exposed.sql.transactions.transaction
import org.junit.Before
import org.junit.BeforeClass
import org.junit.ClassRule
import org.junit.Test
import org.testcontainers.containers.BindMode
import org.testcontainers.containers.GenericContainer
import org.testcontainers.containers.MySQLContainer
import javax.sql.DataSource

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


        lateinit var dataSource: DataSource

        @BeforeClass
        @JvmStatic
        fun createDataSource() {
            val ds = HikariDataSource()
            ds.driverClassName = "com.mysql.cj.jdbc.Driver"
            ds.jdbcUrl = database.jdbcUrl
            ds.username = dbUser
            ds.password = dbPassword
            ds.maximumPoolSize = 1
            val connection = ds.connection
            connection.close()
            dataSource = ds
            Database.connect(dataSource)
        }
    }

    lateinit var dao: TodoJdbcDao

    @Before
    fun setup() {
        dao = TodoJdbcDao(dataSource)
    }

    @Test
    fun test_insert_and_select_all() {
        transaction {
            val numRows = dao.selectAll().size
            for (i in 1..10) {
                dao.insert("todo-$i")
            }
            TestCase.assertEquals(numRows + 10, dao.selectAll().size)
        }
    }

    @Test
    fun test_select_by_id() {
        transaction {
            val todoText = "Don't forget to call Dad."
            val id = dao.insert(todoText)
            val todo = dao.select(id)
            TestCase.assertEquals(todoText, todo.text)
            TestCase.assertEquals(id, todo.id)
        }
    }

    @Test
    fun test_update() {
        transaction {
            val todoText = "Don't forget to call Dad."
            val id = dao.insert(todoText)
            var todo = dao.select(id)
            TestCase.assertEquals(todoText, todo.text)
            TestCase.assertEquals(id, todo.id)
            TestCase.assertEquals(false, todo.done)

            dao.update(id, "Don't forget to call Mom.", true)
            todo = dao.select(id)
            TestCase.assertEquals(todoText.replace("Dad", "Mom"), todo.text)
            TestCase.assertEquals(id, todo.id)
            TestCase.assertEquals(true, todo.done)
        }
    }
}