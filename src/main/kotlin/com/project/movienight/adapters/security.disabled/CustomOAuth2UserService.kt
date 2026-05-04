package com.project.movienight.adapters.security

import com.project.movienight.adapters.persistence.entity.toDomain
import com.project.movienight.adapters.persistence.entity.toEntity
import com.project.movienight.application.ports.input.security.OAuth2UserInfo
import com.project.movienight.application.ports.output.IdGenerator
import com.project.movienight.application.ports.output.UserRepositoryPort
import com.project.movienight.domain.model.AuthProvider
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
        } catch (e: IllegalArgumentException) {
            log.error("OAuth2 authentication failed: ${e.message}", e)
            throw OAuth2AuthenticationException("Failed to process OAuth2 user data")
        } catch (e: OAuth2AuthenticationException) {
            log.error("OAuth2 authentication failed: ${e.message}", e)
            throw e
        }
    }

    private fun findOrCreateUser(userInfo: OAuth2UserInfo): User {
        val provider = AuthProvider.valueOf(userInfo.getProvider().uppercase())

        val existingUser =
            userRepository.findByProviderAndProviderId(
                provider,
                userInfo.getProviderId(),
            )

        return if (existingUser != null) {
            log.debug("User found by provider: {}", userInfo.getProvider())
            existingUser
        } else {
            val userByEmail = userRepository.findByEmail(userInfo.getEmail())

            if (userByEmail != null) {
                log.debug("Linking OAuth2 account to existing user: {}", userInfo.getEmail())
                val entity =
                    userByEmail.toEntity(
                        provider = provider,
                        providerId = userInfo.getProviderId(),
                    )
                userRepository.save(entity.toDomain())
            } else {
                log.debug("Creating new user for provider: {}", userInfo.getProvider())
                val newUser =
                    User(
                        id = idGenerator.generateId(),
                        name = userInfo.getName(),
                        email = userInfo.getEmail(),
                        library = null,
                    )
                val entity =
                    newUser.toEntity(
                        provider = provider,
                        providerId = userInfo.getProviderId(),
                    )
                userRepository.save(entity.toDomain())
            }
        }
    }
}
