package com.project.movienight.adapters.security

import com.project.movienight.application.ports.input.security.OAuth2UserInfo

class VkOAuth2UserInfo(
    private val attributes: Map<String, Any>,
) : OAuth2UserInfo {
    override fun getProviderId(): String =
        (attributes["response"] as? List<*>)
            ?.firstOrNull()
            ?.let { it as? Map<*, *> }
            ?.get("id")
            ?.toString() ?: ""

    override fun getEmail(): String = attributes["email"]?.toString() ?: ""

    override fun getName(): String {
        val response = attributes["response"] as? List<*>
        val first = response?.firstOrNull() as? Map<*, *>
        val firstName = first?.get("first_name")?.toString() ?: ""
        val lastName = first?.get("last_name")?.toString() ?: ""
        return "$firstName $lastName".trim()
    }

    override fun getProvider(): String = "vk"

    override fun getAttributes(): Map<String, Any> = attributes
}
