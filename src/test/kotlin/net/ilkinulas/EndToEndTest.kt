package net.ilkinulas

import junit.framework.TestCase.*
import org.junit.Before
import org.junit.ClassRule
import org.junit.Test
import org.junit.rules.RuleChain
import org.openqa.selenium.By
import org.openqa.selenium.Keys
import org.openqa.selenium.NoSuchElementException
import org.openqa.selenium.WebDriver
import org.openqa.selenium.remote.DesiredCapabilities
import org.openqa.selenium.remote.RemoteWebDriver
import org.openqa.selenium.support.ui.ExpectedCondition
import org.openqa.selenium.support.ui.ExpectedConditions
import org.openqa.selenium.support.ui.FluentWait
import org.openqa.selenium.support.ui.WebDriverWait
import org.testcontainers.containers.BrowserWebDriverContainer
import org.testcontainers.containers.Network
import java.io.File
import java.time.Duration

class BrowserContainer : BrowserWebDriverContainer<BrowserContainer>()

class EndToEndTest {

    companion object {

        val network = Network.newNetwork()

        val database = KMysqlContainer("mysql:5.7.21")
                .withNetwork(network)
                .withNetworkAliases("database")
                .withDatabaseName("tododb")
                .withUsername("todouser")
                .withPassword("todopass")

        val todoapp = KGenericContainer("ilkinulas/todoapp:1.0")
                .withNetwork(network)
                .withNetworkAliases("todoapp")
                .withExposedPorts(9000)
                .withEnv("DB_URL", "jdbc:mysql://database:3306/tododb?nullNamePatternMatchesAll=true&useSSL=false")

        val browser = BrowserContainer()
                .withNetwork(network)
                .withNetworkAliases("browser")
                .withDesiredCapabilities(DesiredCapabilities.chrome())
                .withRecordingMode(BrowserWebDriverContainer.VncRecordingMode.RECORD_ALL, File("./out/"))


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
    }

    @Test
    fun test_add_item() {
        val input = webDriver.findElement(By.id("new_item"))
        val todoList = listOf("Call mom", "Read more books.", "Be kind to people.")
        todoList.forEach {
            input.sendKeys(it)
            input.sendKeys(Keys.RETURN)
        }
        webDriver.findElements(By.xpath("//ul/li")).forEach {
            println(it.text)
        }

        todoList.forEach {
            assertTrue(isTodoVisible(webDriver, it))
        }
    }

    @Test
    fun test_delete_item() {
        val willBeDeletedText = "This item will be deleted."

        val input = webDriver.findElement(By.id("new_item"))
        input.sendKeys(willBeDeletedText)
        input.sendKeys(Keys.RETURN)

        assertTrue(isTodoVisible(webDriver, willBeDeletedText))

        val checkbox = webDriver.findElements(By.tagName("input")).last()
        checkbox.click()
        val deleteLink = webDriver.findElementsByClassName("delete_link").last()
        deleteLink.click()

        assertFalse(isTodoVisible(webDriver, willBeDeletedText))
    }

    private fun isTodoVisible(webDriver: RemoteWebDriver, todo: String) =
            webDriver.findElements(By.xpath("//ul/li")).find {
                it.text == todo
            } != null

}
