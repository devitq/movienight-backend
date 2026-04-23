package com.project.movienight.adapters.security

import com.project.movienight.domain.model.User
import org.springframework.security.core.GrantedAuthority
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.core.userdetails.UserDetails
import org.springframework.security.oauth2.core.user.OAuth2User
import java.util.UUID

class UserPrincipal(
    private val user: User,
    private val attributes: Map<String, Any>? = null,
) : OAuth2User, UserDetails {

    fun getId(): UUID = user.id

    override fun getName(): String = user.name

    override fun getAttributes(): Map<String, Any> = attributes ?: emptyMap()

    override fun getAuthorities(): Collection<GrantedAuthority> {
        return listOf(SimpleGrantedAuthority("ROLE_USER"))
    }

    override fun getPassword(): String = user.password

    override fun getUsername(): String = user.email

    override fun isAccountNonExpired(): Boolean = true

    override fun isAccountNonLocked(): Boolean = true

    override fun isCredentialsNonExpired(): Boolean = true

    override fun isEnabled(): Boolean = true

    companion object {
        fun create(user: User, attributes: Map<String, Any>? = null): UserPrincipal {
            return UserPrincipal(user, attributes)
        }
    }
}
