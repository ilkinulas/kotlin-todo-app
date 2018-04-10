package net.ilkinulas

import junit.framework.TestCase.*
import org.junit.Before
import org.junit.ClassRule
import org.junit.Test
import org.junit.rules.RuleChain
import org.openqa.selenium.By
import org.openqa.selenium.Keys
import org.openqa.selenium.NoSuchElementException
import org.openqa.selenium.remote.DesiredCapabilities
import org.openqa.selenium.remote.RemoteWebDriver
import org.openqa.selenium.support.ui.ExpectedConditions
import org.openqa.selenium.support.ui.FluentWait
import org.openqa.selenium.support.ui.Wait
import org.testcontainers.containers.BrowserWebDriverContainer
import org.testcontainers.containers.Network
import java.io.File
import java.time.Duration

class EndToEndTest {

    companion object {

        val network = Network.newNetwork()

        val database = KMysqlContainer("mysql:5.7.21")
                .withNetwork(network)
                .withNetworkAliases("dockerCompose")
                .withDatabaseName("tododb")
                .withUsername("todouser")
                .withPassword("todopass")

        val todoapp = KGenericContainer("ilkinulas/todoapp:1.0")
                .withNetwork(network)
                .withNetworkAliases("todoapp")
                .withExposedPorts(9000)
                .withEnv("DB_URL", "jdbc:mysql://dockerCompose:3306/tododb")

        val browser = BrowserContainer()
                .withNetwork(network)
                .withNetworkAliases("browser")
                .withDesiredCapabilities(DesiredCapabilities.chrome())
                .withRecordingMode(BrowserWebDriverContainer.VncRecordingMode.RECORD_ALL, File("./out"))


        @ClassRule
        @JvmField
        val ruleChain = RuleChain.outerRule(network).around(database).around(todoapp).around(browser)
    }

    lateinit var webDriver: RemoteWebDriver

    @Before
    fun setup() {
        webDriver = browser.webDriver
        val url = "http://todoapp:9000"
        webDriver.get(url)
        insertTodo("Place holder")
        waitForNumTodosMoreThan(0, webDriver)
    }

    @Test
    fun test_add_item() {
        val todoCount = numberOfVisibleTodos(webDriver)
        val todoList = listOf("Call mom", "Read more books.", "Be kind to people.")
        todoList.forEach {
           insertTodo(it)
        }
        webDriver.findElements(By.xpath("//ul/li")).forEach {
            println(it.text)
        }

        waitForNumTodosMoreThan(todoCount, webDriver)

        todoList.forEach {
            assertTrue(isTodoVisible(webDriver, it))
        }

        assertEquals(todoCount + 3, numberOfVisibleTodos(webDriver))
    }

    @Test
    fun test_delete_item() {
        val todoCount = numberOfVisibleTodos(webDriver)
        val willBeDeletedText = "This item will be deleted."

        insertTodo(willBeDeletedText)

        waitForNumTodosMoreThan(todoCount, webDriver)

        assertTrue(isTodoVisible(webDriver, willBeDeletedText))

        val checkbox = webDriver.findElements(By.tagName("input")).last()
        checkbox.click()
        val deleteLink = webDriver.findElementsByClassName("delete_link").last()
        deleteLink.click()


        waitForNumTodosLessThan(todoCount + 1, webDriver)
        assertFalse(isTodoVisible(webDriver, willBeDeletedText))
    }

    private fun insertTodo(willBeDeletedText: String) {
        val input = webDriver.findElement(By.id("new_item"))
        input.sendKeys(willBeDeletedText)
        input.sendKeys(Keys.RETURN)
    }

    @Test
    fun test_all_done() {
        val todos = webDriver.findElements(By.xpath("//input[@type='checkbox']"))
        todos.forEach {
            it.click()
        }

        FluentWait(webDriver).withTimeout(Duration.ofSeconds(5))
                .pollingEvery(Duration.ofMillis(10))
                .ignoring(NoSuchElementException::class.java)
                .until(ExpectedConditions.numberOfElementsToBe(By.xpath("//ul/li/a"), todos.size))

        val deleteLinks = webDriver.findElements(By.xpath("//ul/li/a"))
        for (i in (deleteLinks.size - 1).downTo(0)) {
            deleteLinks[i].click()
        }

        waitForNumTodosLessThan(1, webDriver)

        FluentWait(webDriver).withTimeout(Duration.ofSeconds(5))
                .pollingEvery(Duration.ofMillis(10))
                .ignoring(NoSuchElementException::class.java)
                .until(ExpectedConditions.visibilityOfElementLocated(By.id("all_done_title")))
    }

    private fun isTodoVisible(webDriver: RemoteWebDriver, todo: String) =
            webDriver.findElements(By.xpath("//ul/li")).find {
                it.text == todo
            } != null

    private fun numberOfVisibleTodos(webDriver: RemoteWebDriver) = webDriver.findElements(By.xpath("//ul/li")).size

    private fun waitForNumTodosMoreThan(count: Int, webDriver: RemoteWebDriver) {
        FluentWait(webDriver).withTimeout(Duration.ofSeconds(5))
                .pollingEvery(Duration.ofMillis(10))
                .ignoring(NoSuchElementException::class.java)
                .until(ExpectedConditions.numberOfElementsToBeMoreThan(By.xpath("//ul/li"), count))
    }

    private fun waitForNumTodosLessThan(count: Int, webDriver: RemoteWebDriver) {
        FluentWait(webDriver).withTimeout(Duration.ofSeconds(5))
                .pollingEvery(Duration.ofMillis(10))
                .ignoring(NoSuchElementException::class.java)
                .until(ExpectedConditions.numberOfElementsToBeLessThan(By.xpath("//ul/li"), count))
    }

    fun isElementPresent(locator: By, webDriver: RemoteWebDriver): Boolean {
        return try {
            webDriver.findElement(locator)
            true
        } catch (e: NoSuchElementException) {
            false
        }

    }
}