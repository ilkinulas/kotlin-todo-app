package net.ilkinulas

import java.util.*

object Config {
    private val properties = Properties()

    fun get(key: String, defaultValue: String = ""): String = properties.getProperty(key, defaultValue)

    fun isInMemoryDb() = get("jdbc.url").contains(":h2:")

    fun init() {
        val stream = Config.javaClass.getResourceAsStream("/todo.properties")
        properties.load(stream)
    }

    fun readFromClassPath(file: String): String {
        val stream = Config.javaClass.getResourceAsStream(file)
        return stream.bufferedReader().use { it.readLines().joinToString("\n") }
    }
}