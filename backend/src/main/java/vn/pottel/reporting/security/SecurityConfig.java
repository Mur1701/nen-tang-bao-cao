package vn.pottel.reporting.security;

import jakarta.servlet.DispatcherType;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.context.annotation.Bean;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.web.access.intercept.AuthorizationFilter;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.csrf.HttpSessionCsrfTokenRepository;
import org.springframework.security.web.csrf.InvalidCsrfTokenException;
import org.springframework.security.web.csrf.MissingCsrfTokenException;
import org.springframework.security.web.csrf.CsrfTokenRequestAttributeHandler;
import vn.pottel.reporting.auth.AdminLoginThrottle;
import vn.pottel.reporting.auth.AdminSessionValidationFilter;
import vn.pottel.reporting.auth.BssAdminUserDetailsService;
import vn.pottel.reporting.auth.BssPasswordEncoder;

/** The report server is deny-by-default; Vercel URLs are NOT an auth boundary. */
@Configuration(proxyBeanMethods = false)
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    AuthenticationProvider adminProvider(BssAdminUserDetailsService users) {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider(users);
        provider.setPasswordEncoder(new BssPasswordEncoder());
        return provider;
    }

    @Bean
    AdminLoginThrottle adminLoginThrottle() { return new AdminLoginThrottle(); }

    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http, AuthenticationProvider adminProvider,
                          BssAdminUserDetailsService users, AdminLoginThrottle throttle,
                          @Value("${reporting.security.max-concurrent-requests:4}") int maxConcurrentReports)
                          throws Exception {
        http
          .authenticationProvider(adminProvider)
          .csrf(csrf -> csrf
                .csrfTokenRepository(new HttpSessionCsrfTokenRepository())
                .csrfTokenRequestHandler(new CsrfTokenRequestAttributeHandler()))
          .httpBasic(AbstractHttpConfigurer::disable)
          .requestCache(AbstractHttpConfigurer::disable)
          .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED)
                .sessionFixation(fix -> fix.changeSessionId()))
          .authorizeHttpRequests(auth -> auth
                // Spring Boot forwards failures to an ERROR dispatch. Keep original HTTP status.
                .dispatcherTypeMatchers(DispatcherType.ERROR).permitAll()
                .requestMatchers(HttpMethod.GET, "/api/health", "/api/auth/csrf").permitAll()
                .requestMatchers(HttpMethod.POST, "/api/auth/login").permitAll()
                .requestMatchers("/api/reports", "/api/reports/**", "/api/health/db", "/api/auth/me")
                    .hasRole("ADMIN")
                .requestMatchers(HttpMethod.POST, "/api/auth/logout").authenticated()
                .anyRequest().denyAll())
          .formLogin(form -> form
                .loginProcessingUrl("/api/auth/login")
                .usernameParameter("username")
                .passwordParameter("password")
                .successHandler((request, response, authentication) -> {
                    throttle.succeeded(request);
                    response.setStatus(200);
                    response.setContentType(MediaType.APPLICATION_JSON_VALUE);
                    response.getWriter().write("{\"ok\":true}");
                })
                .failureHandler((request, response, exception) -> {
                    throttle.fail(request);
                    response.setStatus(401);
                    response.setContentType(MediaType.APPLICATION_JSON_VALUE);
                    response.getWriter().write("{\"error\":\"Invalid admin credentials or unavailable account\"}");
                }))
          .logout(logout -> logout
                .logoutUrl("/api/auth/logout")
                .invalidateHttpSession(true)
                .clearAuthentication(true)
                .logoutSuccessHandler((request, response, authentication) -> response.setStatus(204)))
          .exceptionHandling(errors -> errors
                .authenticationEntryPoint((request, response, exception) -> {
                    response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                    response.setContentType(MediaType.APPLICATION_JSON_VALUE);
                    response.getWriter().write("{\"error\":\"Authentication required\"}");
                })
                .accessDeniedHandler((request, response, exception) -> {
                    response.setStatus(HttpServletResponse.SC_FORBIDDEN);
                    response.setContentType(MediaType.APPLICATION_JSON_VALUE);
                    String reason = exception instanceof MissingCsrfTokenException ? "CSRF_TOKEN_MISSING"
                            : exception instanceof InvalidCsrfTokenException ? "CSRF_TOKEN_INVALID"
                            : "ACCESS_DENIED";
                    response.getWriter().write("{\"error\":\"" + reason + "\"}");
                }));
        http.addFilterBefore(throttle, UsernamePasswordAuthenticationFilter.class);
        http.addFilterAfter(new AdminSessionValidationFilter(users), UsernamePasswordAuthenticationFilter.class);
        http.addFilterAfter(new ReportAdmissionFilter(maxConcurrentReports), AuthorizationFilter.class);
        return http.build();
    }
}
