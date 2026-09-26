package com.racemanager.api.config;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import com.racemanager.api.auth.JwtAuthenticationFilter;
import com.racemanager.api.auth.JwtService;

import jakarta.servlet.http.HttpServletResponse;

/**
 * Seguridad de la API.
 *
 * <ul>
 *   <li>Autenticación sin estado con JWT ({@code Authorization: Bearer ...}).</li>
 *   <li>Públicos: {@code GET /api/health} y {@code POST /api/auth/login}.</li>
 *   <li>Resto de {@code /api/**}: requiere token; los permisos por rol y por liga
 *       se declaran en cada endpoint con {@code @PreAuthorize}.</li>
 *   <li>Cualquier otra ruta: denegada por defecto.</li>
 * </ul>
 *
 * <p>CSRF se desactiva porque el token viaja en un encabezado, no en cookies.
 * Si en el futuro el JWT se guardara en una cookie, habría que reactivarlo.
 */
@Configuration
@EnableMethodSecurity
public class SecurityConfig {

	@Bean
	SecurityFilterChain securityFilterChain(HttpSecurity http, JwtService jwtService) throws Exception {
		http
			.csrf(AbstractHttpConfigurer::disable)
			.cors(Customizer.withDefaults())
			.httpBasic(AbstractHttpConfigurer::disable)
			.formLogin(AbstractHttpConfigurer::disable)
			.logout(AbstractHttpConfigurer::disable)
			.sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
			.authorizeHttpRequests(auth -> auth
				.requestMatchers(HttpMethod.GET, "/api/health").permitAll()
				.requestMatchers(HttpMethod.POST, "/api/auth/login").permitAll()
				.requestMatchers("/error").permitAll()
				.requestMatchers("/api/**").authenticated()
				.anyRequest().denyAll())
			.exceptionHandling(errores -> errores
				.authenticationEntryPoint((request, response, ex) ->
					escribirError(response, HttpServletResponse.SC_UNAUTHORIZED, "No autenticado"))
				.accessDeniedHandler((request, response, ex) ->
					escribirError(response, HttpServletResponse.SC_FORBIDDEN, "Acceso denegado")))
			.addFilterBefore(new JwtAuthenticationFilter(jwtService), UsernamePasswordAuthenticationFilter.class);

		return http.build();
	}

	@Bean
	PasswordEncoder passwordEncoder() {
		return new BCryptPasswordEncoder();
	}

	/**
	 * La autenticación se resuelve en AuthService + JWT. Declarar este bean evita que
	 * Spring Boot cree el usuario en memoria por defecto (con contraseña impresa en el log).
	 */
	@Bean
	UserDetailsService userDetailsService() {
		return username -> {
			throw new UsernameNotFoundException("La autenticación se realiza únicamente mediante JWT");
		};
	}

	@Bean
	CorsConfigurationSource corsConfigurationSource(
			@Value("${racemanager.cors.origenes-permitidos:}") List<String> origenesPermitidos) {
		CorsConfiguration config = new CorsConfiguration();
		config.setAllowedOrigins(origenesPermitidos.stream().map(String::trim).filter(o -> !o.isEmpty()).toList());
		config.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
		config.setAllowedHeaders(List.of(HttpHeaders.AUTHORIZATION, HttpHeaders.CONTENT_TYPE));
		config.setAllowCredentials(false);
		config.setMaxAge(3600L);

		UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
		source.registerCorsConfiguration("/api/**", config);
		return source;
	}

	private static void escribirError(HttpServletResponse response, int estado, String mensaje) throws IOException {
		response.setStatus(estado);
		response.setContentType(MediaType.APPLICATION_JSON_VALUE);
		response.setCharacterEncoding(StandardCharsets.UTF_8.name());
		response.getWriter().write("{\"error\":\"" + mensaje + "\"}");
	}
}
