package com.piggy.piggybackend.global.config;

import com.piggy.piggybackend.auth.service.CustomOAuth2UserService;
import com.piggy.piggybackend.auth.service.OAuth2FailureHandler;
import com.piggy.piggybackend.auth.service.OAuth2SuccessHandler;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

@Configuration
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final CustomOAuth2UserService customOAuth2UserService;
    private final OAuth2SuccessHandler oAuth2SuccessHandler;
    private final OAuth2FailureHandler oAuth2FailureHandler;

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {

        http
                .csrf(csrf -> csrf.disable())
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .httpBasic(httpBasic -> httpBasic.disable())
                .formLogin(form -> form.disable())

                .sessionManagement(session ->
                        session.sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED)
                )


                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(HttpMethod.GET, "/api/community/**").permitAll()
                        .requestMatchers(
                                "/users/sign-up",
                                "/users/login",
                                "/oauth2/**",
                                "/users/check-email",
                                "/users/check-nickname",
                                "/error",
                                "/auth/refresh",
                                "/auth/logout",
                                "/v3/api-docs/**",
                                "/swagger-ui/**",
                                "/swagger-ui.html",
                                "/users/password/**",
                                "/password/reset",
                                "/users/password/reset-request",
                                "/api/exchange/**",
                                "/api/v1/shopping/**",
                                "/api/exchange/simulate",
                                "/api/v1/ocr/**",
                                "/food/**",
                                "/api/chatbot/**"
                        ).permitAll()
                        .anyRequest().authenticated()
                )

                .oauth2Login(oauth -> oauth
                        .userInfoEndpoint(user ->
                                user.userService(customOAuth2UserService)
                        )
                        .successHandler(oAuth2SuccessHandler)
                        .failureHandler(oAuth2FailureHandler)
                )

                .addFilterBefore(
                        jwtAuthenticationFilter,
                        UsernamePasswordAuthenticationFilter.class
                );

        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();

        // 1. 접속을 허용할 프론트엔드 주소를 명시적으로 등록합니다.
        configuration.setAllowedOriginPatterns(List.of(
                "https://piggys.mooo.com", // 실제 배포된 프론트엔드 도메인
                "http://localhost:3000",     // 로컬 React 개발용
                "http://localhost:8081",
                "http://localhost:8080",
                "http://localhost:5173"
        ));

        // 2. 허용할 HTTP 메소드를 지정합니다.
        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));

        // 3. 허용할 HTTP 헤더를 지정합니다. ("*"로 모든 헤더 허용)
        configuration.setAllowedHeaders(List.of(
                "*"
        ));
        configuration.setExposedHeaders(List.of("Authorization"));

        // 4. 특정 도메인을 명시했으므로 'true'로 설정하여 JWT 토큰을 주고받을 수 있게 합니다.
        configuration.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration); // 모든 경로에 대해 위 설정 적용

        return source;
    }
}
