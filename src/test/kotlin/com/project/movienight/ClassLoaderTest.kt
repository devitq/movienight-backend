package com.project.movienight

import org.junit.jupiter.api.Test
import kotlin.test.assertNotNull

class ClassLoaderTest {
    @Test
    fun `can load OAuth2ClientProperties class`() {
        val clazz =
            Class.forName(
                "org.springframework.boot.autoconfigure.security.oauth2.client.OAuth2ClientProperties",
            )
        assertNotNull(clazz)
        println("Successfully loaded: ${clazz.name}")
        println("ClassLoader: ${clazz.classLoader}")
    }
}
