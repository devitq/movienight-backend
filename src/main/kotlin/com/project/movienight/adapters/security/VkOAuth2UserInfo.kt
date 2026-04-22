package com.project.movienight.adapters.security.oauth2

@Suppress("UNCHECKED_CAST")
class VkOAuth2UserInfo(
    private val attributes: Map<String, Any>
) : OAuth2UserInfo {

    override fun getProviderId(): String {
        val response = attributes["response"] as? List<Map<String, Any>>
        return response?.firstOrNull()?.get("id")?.toString() ?: ""
    }

    override fun getEmail(): String = attributes["email"] as? String ?: ""

    override fun getName(): String {
        val response = attributes["response"] as? List<Map<String, Any>>
        val first = response?.firstOrNull()
        val firstName = first?.get("first_name") as? String ?: ""
        val lastName = first?.get("last_name") as? String ?: ""
        return "$firstName $lastName".trim()
    }

    override fun getProvider(): String = "vk"

    override fun getAttributes(): Map<String, Any> = attributes
}
