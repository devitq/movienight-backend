package com.project.movienight.adapters.security.oauth2

interface OAuth2UserInfo {
    fun getProviderId(): String
    fun getEmail(): String
    fun getName(): String
    fun getProvider(): String
    fun getAttributes(): Map<String, Any>
}
