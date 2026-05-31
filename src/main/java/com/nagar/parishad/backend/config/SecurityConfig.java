package com.nagar.parishad.backend.config;

import com.nagar.parishad.backend.service.UserDetailsServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;
import java.util.List;

import static org.springframework.security.web.util.matcher.AntPathRequestMatcher.antMatcher;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    @Autowired
    UserDetailsServiceImpl userDetailsService;

    @Autowired
    private AuthEntryPointJwt unauthorizedHandler;

    @Bean
    public AuthTokenFilter authenticationJwtTokenFilter() {
        return new AuthTokenFilter();
    }

    @Bean
    public DaoAuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider authProvider = new DaoAuthenticationProvider();

        authProvider.setUserDetailsService(userDetailsService);
        authProvider.setPasswordEncoder(passwordEncoder());

        return authProvider;
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration authConfig) throws Exception {
        return authConfig.getAuthenticationManager();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http.csrf(csrf -> csrf.disable())
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .exceptionHandling(exception -> exception.authenticationEntryPoint(unauthorizedHandler))
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(antMatcher("/api/auth/**")).permitAll()
                        .requestMatchers(antMatcher("/api/owner/**")).hasRole("OWNER")
                        .requestMatchers(antMatcher("/api/admin/bot-sim/**"))
                        .hasAnyRole("ADMIN", "OWNER", "CHIEF_OFFICER")
                        .requestMatchers(antMatcher("/api/admin/chatbot/**"))
                        .hasAnyRole("ADMIN", "OWNER", "CHIEF_OFFICER")
                        .requestMatchers(antMatcher("/api/admin/complaints/**"))
                        .hasAnyRole("ADMIN", "OWNER", "DEPARTMENT_HEAD", "STAFF")
                        .requestMatchers(antMatcher("/api/admin/**")).hasAnyRole("ADMIN", "OWNER")
                        .requestMatchers(antMatcher("/api/dept/**")).hasRole("DEPARTMENT_HEAD")
                        .requestMatchers(antMatcher("/api/staff/**")).hasRole("STAFF")
                        .requestMatchers(antMatcher("/api/test/**")).permitAll() // For testing
                        .requestMatchers(antMatcher("/api/public/**")).permitAll() // Chatbot Public APIs
                        .requestMatchers(antMatcher("/uploads/**")).permitAll() // Allow image loading without auth
                                                                                // header
                        .anyRequest().authenticated());

        http.authenticationProvider(authenticationProvider());

        http.addFilterBefore(authenticationJwtTokenFilter(), UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOriginPatterns(Arrays.asList("*"));
        // configuration.setAllowedOrigins(Arrays.asList(
        // "http://localhost:4200", "http://localhost:8100", "http://localhost:8080",
        // "capacitor://localhost", "http://localhost",
        // "http://loknagar.in", "https://loknagar.in",
        // "http://www.loknagar.in", "https://www.loknagar.in"));
        // configuration.setAllowedOriginPatterns(Arrays.asList("*"));
        configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS", "PATCH", "HEAD"));
        configuration.setAllowedHeaders(Arrays.asList("*")); // Allow ALL headers
        configuration.setExposedHeaders(Arrays.asList("Authorization", "Content-Type", "Content-Disposition"));
        configuration.setAllowCredentials(true);
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}
