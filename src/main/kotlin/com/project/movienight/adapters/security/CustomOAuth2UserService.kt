package com.project.movienight.adapters.security

import com.project.movienight.application.ports.output.IdGenerator
import com.project.movienight.application.ports.output.UserRepositoryPort
import com.project.movienight.domain.model.User
import org.slf4j.LoggerFactory
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest
import org.springframework.security.oauth2.core.OAuth2AuthenticationException
import org.springframework.security.oauth2.core.user.OAuth2User
import org.springframework.stereotype.Service

@Service
class CustomOAuth2UserService(
    private val userRepository: UserRepositoryPort,
    private val idGenerator: IdGenerator,
) : DefaultOAuth2UserService() {

    companion object {
        private val log = LoggerFactory.getLogger(CustomOAuth2UserService::class.java)
    }

    override fun loadUser(userRequest: OAuth2UserRequest): OAuth2User {
        val oAuth2User = super.loadUser(userRequest)
        val registrationId = userRequest.clientRegistration.registrationId

        log.debug("Processing OAuth2 login for provider: {}", registrationId)

        return try {
            val userInfo = OAuth2UserInfoFactory.getOAuth2UserInfo(registrationId, oAuth2User)
            val user = findOrCreateUser(userInfo)
            UserPrincipal.create(user, oAuth2User.attributes)
        } catch (e: Exception) {
            log.error("OAuth2 authentication failed: ${e.message}", e)
            throw OAuth2AuthenticationException("Failed to process OAuth2 user data")
        }
    }

    private fun findOrCreateUser(userInfo: OAuth2UserInfo): User {
        // Сначала ищем по provider + provider_id (основной способ для OAuth2)
        val existingUser = userRepository.findByProviderAndProviderId(
            userInfo.getProvider(),
            userInfo.getProviderId()
        )

        return if (existingUser != null) {
            log.debug("User found by provider: {}", userInfo.getProvider())
            existingUser
        } else {
            // Проверяем нет ли пользователя с таким email (связывание аккаунтов)
            val userByEmail = userRepository.findByEmail(userInfo.getEmail())

            if (userByEmail != null) {
                // Пользователь существует, обновляем его OAuth2 данными
                log.debug("Linking OAuth2 account to existing user: {}", userInfo.getEmail())
                userRepository.saveWithOAuth2(userByEmail, userInfo.getProvider(), userInfo.getProviderId())
            } else {
                log.debug("Creating new user for provider: {}", userInfo.getProvider())
                val newUser = User(
                    id = idGenerator.generateId(),
                    name = userInfo.getName(),
                    email = userInfo.getEmail(),
                    password = "",
                    library = null,
                )
                userRepository.saveWithOAuth2(newUser, userInfo.getProvider(), userInfo.getProviderId())
            }
        }
    }
}
