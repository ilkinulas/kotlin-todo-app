package net.ilkinulas

import org.testcontainers.containers.BrowserWebDriverContainer
import org.testcontainers.containers.DockerComposeContainer
import org.testcontainers.containers.GenericContainer
import org.testcontainers.containers.MySQLContainer
import java.io.File


class KMysqlContainer(dockerImage: String) : MySQLContainer<KMysqlContainer>(dockerImage)

class KGenericContainer(dockerImage: String) : GenericContainer<KGenericContainer>(dockerImage)

class KDockerComposeContainer(vararg composeFiles: String) : DockerComposeContainer<KDockerComposeContainer>(composeFiles.map { File(it) })

class BrowserContainer : BrowserWebDriverContainer<BrowserContainer>("selenium/standalone-chrome-debug:3.11.0")