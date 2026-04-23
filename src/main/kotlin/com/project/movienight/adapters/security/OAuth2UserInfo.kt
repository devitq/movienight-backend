package com.project.movienight.adapters.security

interface OAuth2UserInfo {
    fun getProviderId(): String
    fun getEmail(): String
    fun getName(): String
    fun getProvider(): String
    fun getAttributes(): Map<String, Any>
}
