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
import org.springframework.security.web.authentication.LoginUrlAuthenticationEntryPoint;

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
                .requestMatchers("/webjars/**", "/css/**", "/js/**",
                                 "/bootstrap-icons/**", "/config.js",
                                 "/favicon.svg").permitAll()
                .requestMatchers("/login", "/register").permitAll()
                .requestMatchers("/admin/**").hasRole(Role.ADMIN.name())
                // API admin-only mutations — GET stays open to all authenticated users
                .requestMatchers(HttpMethod.POST, "/api/tags").hasRole(Role.ADMIN.name())
                .requestMatchers(HttpMethod.DELETE, "/api/tags/**").hasRole(Role.ADMIN.name())
                .requestMatchers(HttpMethod.POST, "/api/users").hasRole(Role.ADMIN.name())
                .requestMatchers(HttpMethod.DELETE, "/api/users/**").hasRole(Role.ADMIN.name())
                .requestMatchers("/api/audit/**").hasRole(Role.ADMIN.name())
                .anyRequest().authenticated()
            )

            // ── HTMX-aware auth entry point ────────────────────────────────
            // When an unauthenticated HTMX request arrives (e.g. session expired
            // on a background tab), respond with HX-Redirect so HTMX does a full
            // page navigation to login instead of injecting the login page into
            // a modal or partial target.
            .exceptionHandling(ex -> ex
                .authenticationEntryPoint((request, response, authException) -> {
                    if ("true".equals(request.getHeader("HX-Request"))) {
                        response.setHeader("HX-Redirect", request.getContextPath() + "/login");
                        response.setStatus(200);
                    } else {
                        new LoginUrlAuthenticationEntryPoint("/login")
                                .commence(request, response, authException);
                    }
                })
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
            .csrf(csrf -> csrf
                // REST API clients don't use browser cookies/forms, so CSRF
                // doesn't apply. Web UI forms keep CSRF protection via Thymeleaf.
                .ignoringRequestMatchers("/api/**")
                // WebSocket handshake is a GET upgrade — CSRF tokens aren't sent.
                // The connection is already authenticated via the session cookie.
                .ignoringRequestMatchers("/ws")
            )

            // ── Headers ──────────────────────────────────────────────────────
            .headers(headers -> headers
                .frameOptions(frame -> frame.deny())
            );

        return http.build();
    }
}
