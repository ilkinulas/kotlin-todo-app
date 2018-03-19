package net.ilkinulas

import java.util.*

object Config {
    private val properties = Properties()
    fun init() {
        val stream = Config.javaClass.getResourceAsStream("/todo.properties")
        properties.load(stream)
    }

    fun get(key: String, defaultValue: String = ""): String {
        System.getProperty(key)?.let { return it }
        System.getenv(key)?.let { return it }
        return properties.getProperty(key, defaultValue)
    }
}