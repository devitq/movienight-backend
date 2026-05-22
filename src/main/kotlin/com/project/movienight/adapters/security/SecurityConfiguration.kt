package com.project.movienight.adapters.security

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity
import org.springframework.security.web.SecurityFilterChain

@Configuration
@EnableWebSecurity
class SecurityConfiguration(
    private val customOAuth2UserService: CustomOAuth2UserService,
) {
    @Bean
    fun securityFilterChain(http: HttpSecurity): SecurityFilterChain {
        http
            .oauth2Login { oauth2 ->
                oauth2
                    .userInfoEndpoint { userInfo ->
                        userInfo.userService(customOAuth2UserService)
                    }.defaultSuccessUrl("/api/users/me", true)
            }.authorizeHttpRequests { auth ->
                auth
                    .requestMatchers("/", "/login/**", "/oauth2/**", "/h2-console/**", "/actuator/health")
                    .permitAll()
                    .requestMatchers("/api/v1/docs/**", "/api/v1/swagger-ui/**", "/swagger-ui/**")
                    .permitAll()
                    .requestMatchers(
                        "/api/integrations/jellyfin/events",
                        "/api/integrations/jellyfin/sync",
                        "/api/integrations/jellyfin/sync-state",
                    ).permitAll()
                    .requestMatchers("/api/users/me")
                    .authenticated()
                    .requestMatchers("/api/**")
                    .authenticated()
                    .anyRequest()
                    .authenticated()
            }.headers { headers ->
                headers.frameOptions { frameOptions ->
                    frameOptions.sameOrigin()
                }
            }.csrf { csrf ->
                csrf.disable()
            }

        return http.build()
    }
}
