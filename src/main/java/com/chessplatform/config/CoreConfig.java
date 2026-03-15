package com.chessplatform.config;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.CorsFilter;

import java.util.List;

/**
 * CorsConfig — allows the frontend HTML file (opened from local filesystem
 * or any origin) to call the Spring Boot API at localhost:1111.
 *
 * HOW TO ADD THIS TO YOUR BACKEND:
 *   1. Copy this file to:
 *      src/main/java/com/chessplatform/config/CorsConfig.java
 *   2. Restart IntelliJ / the Spring Boot app.
 *   3. Done — the frontend will now work.
 */
@Configuration
public class CoreConfig {

    @Bean
    public CorsFilter corsFilter() {
        CorsConfiguration config = new CorsConfiguration();

        // Allow specific production origins and local development
        config.setAllowedOriginPatterns(List.of(
            "http://localhost:3000",
            "https://chess-forge-eight.vercel.app",
            "https://*.vercel.app"
        ));

        // Allow all standard HTTP methods
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));

        // Allow all headers including Authorization
        config.setAllowedHeaders(List.of("*"));

        // Expose Authorization header in responses
        config.setExposedHeaders(List.of("Authorization"));

        // Allow credentials (cookies / auth headers)
        config.setAllowCredentials(true); 

        // Cache preflight for 1 hour
        config.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);

        return new CorsFilter(source);
    }
}
