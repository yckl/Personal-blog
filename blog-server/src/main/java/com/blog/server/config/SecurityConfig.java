package com.blog.server.config;

import com.blog.server.security.JwtAuthenticationFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
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

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .cors(org.springframework.security.config.Customizer.withDefaults())
            .csrf(AbstractHttpConfigurer::disable)
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                // Allow all CORS preflight requests
                .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                // Admin auth endpoints (public)
                .requestMatchers(
                    "/api/admin/auth/login",
                    "/api/admin/auth/verify-2fa",
                    "/api/admin/auth/refresh"
                ).permitAll()
                // Public endpoints
                .requestMatchers(HttpMethod.GET, "/api/public/**").permitAll()
                .requestMatchers(HttpMethod.POST, "/api/public/article/*/like").permitAll()
                .requestMatchers(HttpMethod.POST, "/api/public/article/*/favorite").permitAll()
                .requestMatchers(HttpMethod.POST, "/api/public/article/*/share").permitAll()
                .requestMatchers(HttpMethod.GET, "/files/**").permitAll()
                .requestMatchers("/api/subscribers/subscribe").permitAll()
                .requestMatchers(HttpMethod.POST, "/api/comments").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/comments/**").permitAll()
                .requestMatchers(HttpMethod.POST, "/api/comments/*/like").permitAll()
                .requestMatchers(HttpMethod.POST, "/api/visits").permitAll()
                .requestMatchers(HttpMethod.POST, "/api/visits/*/stay").permitAll()
                .requestMatchers(HttpMethod.POST, "/api/articles/*/like").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/articles", "/api/articles/**").permitAll()
                // SEO endpoints
                .requestMatchers("/sitemap.xml", "/robots.txt").permitAll()
                .requestMatchers("/api/redirect").permitAll()
                .requestMatchers("/api/site/seo").permitAll()
                // Subscription
                .requestMatchers("/api/subscribe", "/api/subscribe/**").permitAll()
                .requestMatchers("/api/unsubscribe").permitAll()
                // Search
                .requestMatchers("/api/search/**").permitAll()
                // Newsletter tracking
                .requestMatchers("/api/newsletter/track/**").permitAll()
                // Member auth + plans (public)
                .requestMatchers("/api/member/register", "/api/member/login").permitAll()
                .requestMatchers("/api/member/access-check").permitAll()
                .requestMatchers("/api/membership/plans").permitAll()
                // Member authenticated endpoints (require ROLE_MEMBER)
                .requestMatchers("/api/member/*/profile", "/api/member/*/orders").hasRole("MEMBER")
                .requestMatchers("/api/member/payment/**").hasRole("MEMBER")
                .requestMatchers("/api/membership/upgrade").hasRole("MEMBER")
                // Payment webhooks + public payment methods
                .requestMatchers("/api/payment/webhook/**").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/payment/methods").permitAll()
                // Digital products (public store)
                .requestMatchers(HttpMethod.GET, "/api/products", "/api/products/**").permitAll()
                // RSS
                .requestMatchers("/rss.xml").permitAll()
                // Push notifications (subscribe/unsubscribe)
                .requestMatchers("/api/push/subscribe", "/api/push/unsubscribe").permitAll()
                // A/B testing (assignment + conversion)
                .requestMatchers("/api/ab/**").permitAll()
                // Multi-author public endpoints
                .requestMatchers(HttpMethod.GET, "/api/public/articles/*/authors", "/api/public/authors/*/articles").permitAll()
                // Friend links
                .requestMatchers(HttpMethod.GET, "/api/public/links").permitAll()
                // Actuator (health check only — other endpoints require auth)
                .requestMatchers("/actuator/health").permitAll()
                // Swagger / OpenAPI
                .requestMatchers("/swagger-ui/**", "/swagger-ui.html", "/v3/api-docs/**").permitAll()
                // Everything else requires authentication
                .anyRequest().authenticated()
            )
            .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration authConfig) throws Exception {
        return authConfig.getAuthenticationManager();
    }
}
