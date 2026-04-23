package com.project.movienight.adapters.security

import com.project.movienight.application.ports.input.security.OAuth2UserInfo

class YandexOAuth2UserInfo(
    private val attributes: Map<String, Any>
) : OAuth2UserInfo {

    override fun getProviderId(): String = attributes["id"]?.toString() ?: ""

    override fun getEmail(): String {
        return (attributes["emails"] as? List<*>)
            ?.firstOrNull()
            ?.let { it as? Map<*, *> }
            ?.get("value")
            ?.toString() ?: ""
    }

    override fun getName(): String = attributes["display_name"]?.toString() ?: ""

    override fun getProvider(): String = "yandex"

    override fun getAttributes(): Map<String, Any> = attributes
}
