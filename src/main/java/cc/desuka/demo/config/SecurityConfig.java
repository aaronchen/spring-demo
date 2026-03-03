package cc.desuka.demo.config;

import cc.desuka.demo.model.Role;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            // ── Authorization rules ──────────────────────────────────────────
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/h2-console/**").permitAll()
                .requestMatchers("/webjars/**", "/css/**", "/js/**",
                                 "/bootstrap-icons/**", "/config.js").permitAll()
                .requestMatchers("/login", "/register").permitAll()
                .requestMatchers("/admin/**").hasRole(Role.ADMIN.name())
                // API admin-only mutations — GET stays open to all authenticated users
                .requestMatchers(HttpMethod.POST, "/api/tags").hasRole(Role.ADMIN.name())
                .requestMatchers(HttpMethod.DELETE, "/api/tags/**").hasRole(Role.ADMIN.name())
                .requestMatchers(HttpMethod.POST, "/api/users").hasRole(Role.ADMIN.name())
                .requestMatchers(HttpMethod.DELETE, "/api/users/**").hasRole(Role.ADMIN.name())
                .anyRequest().authenticated()
            )

            // ── Form login ───────────────────────────────────────────────────
            // Spring Security handles POST /login automatically.
            // The login form uses name="username" for the email field — Spring Security's
            // default parameter name; we do not need usernameParameter() override.
            .formLogin(form -> form
                .loginPage("/login")
                .defaultSuccessUrl("/", true)
                .failureUrl("/login?error")
                .permitAll()
            )

            // ── Logout ───────────────────────────────────────────────────────
            .logout(logout -> logout
                .logoutUrl("/logout")
                .logoutSuccessUrl("/login?logout")
                .invalidateHttpSession(true)
                .deleteCookies("JSESSIONID")
                .permitAll()
            )

            // ── CSRF ─────────────────────────────────────────────────────────
            // Enabled by default. Thymeleaf injects the token into <form th:action> tags.
            // HTMX standalone hx-post buttons get it via the htmx:configRequest listener in utils.js.
            // H2 console uses its own frame-based UI and cannot carry CSRF tokens — exempt it.
            .csrf(csrf -> csrf
                .ignoringRequestMatchers("/h2-console/**")
                // REST API clients don't use browser cookies/forms, so CSRF
                // doesn't apply. Web UI forms keep CSRF protection via Thymeleaf.
                .ignoringRequestMatchers("/api/**")
            )

            // ── Headers ──────────────────────────────────────────────────────
            // Spring Security sets X-Frame-Options: DENY by default, which breaks the H2 console
            // (it renders inside frames). sameOrigin() allows frames from the same origin only.
            .headers(headers -> headers
                .frameOptions(frame -> frame.sameOrigin())
            );

        return http.build();
    }
}
