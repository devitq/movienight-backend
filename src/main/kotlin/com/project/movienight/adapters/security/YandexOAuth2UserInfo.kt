package com.project.movienight.adapters.security.oauth2

@Suppress("UNCHECKED_CAST")
class YandexOAuth2UserInfo(
    private val attributes: Map<String, Any>
) : OAuth2UserInfo {

    override fun getProviderId(): String = attributes["id"]?.toString() ?: ""

    override fun getEmail(): String {
        val emails = attributes["emails"] as? List<Map<String, String>>
        return emails?.firstOrNull()?.get("value") ?: ""
    }

    override fun getName(): String = attributes["display_name"] as? String ?: ""

    override fun getProvider(): String = "yandex"

    override fun getAttributes(): Map<String, Any> = attributes
}
