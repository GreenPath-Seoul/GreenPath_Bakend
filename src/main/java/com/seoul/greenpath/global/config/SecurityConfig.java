package com.seoul.greenpath.global.config;

import com.seoul.greenpath.global.jwt.JwtAuthenticationFilter;
import com.seoul.greenpath.global.jwt.JwtExceptionFilter;
import com.seoul.greenpath.global.jwt.JwtProvider;
import com.seoul.greenpath.global.oauth2.CustomOAuth2UserService;
import com.seoul.greenpath.global.oauth2.OAuth2FailureHandler;
import com.seoul.greenpath.global.oauth2.OAuth2SuccessHandler;
import com.seoul.greenpath.global.security.CustomUserDetailsService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

/**
 * Spring Security 핵심 설정
 *
 * - Stateless Session (JWT 기반)
 * - CSRF 비활성화 (REST API + JWT)
 * - CORS 설정 (프론트엔드 허용)
 * - JWT 필터 등록
 * - OAuth2 로그인 설정 (카카오/네이버)
 * - 인가 규칙 정의
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity // @PreAuthorize 사용 가능
@RequiredArgsConstructor
public class SecurityConfig {

        @org.springframework.beans.factory.annotation.Value("${cors.allowed-origins}")
        private List<String> allowedOrigins;

        private final JwtProvider jwtProvider;
        private final CustomUserDetailsService customUserDetailsService;
        private final CustomOAuth2UserService customOAuth2UserService;
        private final OAuth2SuccessHandler oAuth2SuccessHandler;
        private final OAuth2FailureHandler oAuth2FailureHandler;
        private final JwtExceptionFilter jwtExceptionFilter;

        @Bean
        public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
                http
                                // ── 기본 보안 설정 ────────────────────────────────
                                .csrf(AbstractHttpConfigurer::disable)
                                .httpBasic(AbstractHttpConfigurer::disable)
                                .formLogin(AbstractHttpConfigurer::disable)
                                .sessionManagement(session -> session
                                                .sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                                .cors(cors -> cors.configurationSource(corsConfigurationSource()))

                                // ── 인가 규칙 ────────────────────────────────────
                                .authorizeHttpRequests(auth -> auth
                                                // 인증 없이 허용
                                                .requestMatchers(
                                                                "/api/auth/sign-up",
                                                                "/api/auth/login",
                                                                "/api/auth/reissue",
                                                                "/oauth2/**",
                                                                "/login/oauth2/**",
                                                                "/h2-console/**",
                                                                "/actuator/health",
                                                                "/swagger-ui/**",
                                                                "/v3/api-docs/**")
                                                .permitAll()
                                                .anyRequest().authenticated())

                                // ── JWT 필터 등록 ─────────────────────────────────
                                .addFilterBefore(
                                                new JwtAuthenticationFilter(jwtProvider, customUserDetailsService),
                                                UsernamePasswordAuthenticationFilter.class)
                                .addFilterBefore(jwtExceptionFilter, JwtAuthenticationFilter.class) // 예외 필터 추가

                                // ── OAuth2 로그인 설정 ────────────────────────────
                                .oauth2Login(oauth2 -> oauth2
                                                .userInfoEndpoint(userInfo -> userInfo
                                                                .userService(customOAuth2UserService))
                                                .successHandler(oAuth2SuccessHandler)
                                                .failureHandler(oAuth2FailureHandler))

                                // ── 인증/인가 예외 처리 ───────────────────────────
                                .exceptionHandling(exception -> exception
                                                .authenticationEntryPoint((request, response, authException) -> {
                                                        response.setContentType("application/json;charset=UTF-8");
                                                        response.setStatus(401);
                                                        response.getWriter().write(
                                                                        "{\"status\":401,\"code\":\"UNAUTHORIZED\",\"message\":\"인증이 필요합니다.\"}");
                                                })
                                                .accessDeniedHandler((request, response, accessDeniedException) -> {
                                                        response.setContentType("application/json;charset=UTF-8");
                                                        response.setStatus(403);
                                                        response.getWriter().write(
                                                                        "{\"status\":403,\"code\":\"FORBIDDEN\",\"message\":\"접근 권한이 없습니다.\"}");
                                                }))

                                // H2 콘솔 iframe 허용 (개발 환경)
                                .headers(headers -> headers.frameOptions(frame -> frame.sameOrigin()));

                return http.build();
        }

        @Bean
        public PasswordEncoder passwordEncoder() {
                return new BCryptPasswordEncoder();
        }

        @Bean
        public AuthenticationManager authenticationManager(
                        AuthenticationConfiguration authenticationConfiguration) throws Exception {
                return authenticationConfiguration.getAuthenticationManager();
        }

        /**
         * CORS 설정
         * - 허용 Origin은 application.yml의 cors.allowed-origins로 관리
         */
        @Bean
        public CorsConfigurationSource corsConfigurationSource() {
                CorsConfiguration config = new CorsConfiguration();
                config.setAllowedOriginPatterns(allowedOrigins);
                config.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS"));
                config.setAllowedHeaders(List.of("*"));
                config.setExposedHeaders(List.of("Authorization"));
                config.setAllowCredentials(true);
                config.setMaxAge(3600L);

                UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
                source.registerCorsConfiguration("/**", config);
                return source;
        }
}
