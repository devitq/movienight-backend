package com.project.movienight.adapters.security.oauth2

class GoogleOAuth2UserInfo(
    private val attributes: Map<String, Any>
) : OAuth2UserInfo {

    override fun getProviderId(): String = attributes["sub"] as String

    override fun getEmail(): String = attributes["email"] as String

    override fun getName(): String = attributes["name"] as String

    override fun getProvider(): String = "google"

    override fun getAttributes(): Map<String, Any> = attributes
}
