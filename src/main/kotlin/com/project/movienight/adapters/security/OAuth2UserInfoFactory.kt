package com.project.movienight.adapters.security

import com.project.movienight.application.ports.input.security.OAuth2UserInfo
import org.springframework.security.oauth2.core.OAuth2AuthenticationException
import org.springframework.security.oauth2.core.user.OAuth2User

object OAuth2UserInfoFactory {

    fun getOAuth2UserInfo(registrationId: String, user: OAuth2User): OAuth2UserInfo {
        val attributes = user.attributes

        return when (registrationId.lowercase()) {
            "google" -> GoogleOAuth2UserInfo(attributes)
            "yandex" -> YandexOAuth2UserInfo(attributes)
            "vk" -> VkOAuth2UserInfo(attributes)
            else -> throw OAuth2AuthenticationException("Unknown provider: $registrationId")
        }
    }
}
