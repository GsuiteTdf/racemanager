package com.racemanager.api.auth;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import javax.crypto.SecretKey;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

/**
 * Emite y valida tokens JWT firmados con HMAC-SHA256.
 *
 * <p>Reglas: secreto obligatorio de al menos 32 bytes (se lee de {@code JWT_SECRET}),
 * expiración corta, emisor y audiencia verificados y rechazo de tokens sin firma o alterados.
 */
@Service
public class JwtService {

	static final int LONGITUD_MINIMA_SECRETO = 32;
	/** Destinatario del token: evita aceptar JWT emitidos para otra aplicación con el mismo secreto. */
	static final String AUDIENCIA = "racemanager-web";
	private static final long TOLERANCIA_RELOJ_SEGUNDOS = 30;

	private final SecretKey clave;
	private final Duration expiracion;
	private final String emisor;

	public JwtService(
			@Value("${racemanager.jwt.secret:}") String secreto,
			@Value("${racemanager.jwt.expiracion-minutos:60}") long expiracionMinutos,
			@Value("${racemanager.jwt.emisor:racemanager-api}") String emisor) {
		if (secreto == null || secreto.getBytes(StandardCharsets.UTF_8).length < LONGITUD_MINIMA_SECRETO) {
			throw new IllegalStateException(
				"JWT_SECRET no está configurado o tiene menos de " + LONGITUD_MINIMA_SECRETO
					+ " caracteres. Definir la variable de entorno antes de iniciar la API.");
		}
		if (expiracionMinutos <= 0) {
			throw new IllegalStateException("racemanager.jwt.expiracion-minutos debe ser mayor a 0");
		}
		this.clave = Keys.hmacShaKeyFor(secreto.getBytes(StandardCharsets.UTF_8));
		this.expiracion = Duration.ofMinutes(expiracionMinutos);
		this.emisor = emisor;
	}

	public TokenEmitido generar(UsuarioAutenticado usuario) {
		Instant ahora = Instant.now();
		Instant vence = ahora.plus(expiracion);
		String token = Jwts.builder()
			.subject(String.valueOf(usuario.id()))
			.issuer(emisor)
			.audience().add(AUDIENCIA).and()
			.issuedAt(Date.from(ahora))
			.expiration(Date.from(vence))
			.claim("email", usuario.email())
			.claim("roles", List.copyOf(usuario.roles()))
			.signWith(clave, Jwts.SIG.HS256)
			.compact();
		return new TokenEmitido(token, vence);
	}

	/**
	 * Valida firma, expiración, emisor y audiencia.
	 *
	 * @throws JwtException si el token es inválido, está vencido o fue alterado
	 * @throws IllegalArgumentException si el contenido no tiene el formato esperado
	 */
	public UsuarioAutenticado validar(String token) {
		Claims claims = Jwts.parser()
			.verifyWith(clave)
			.requireIssuer(emisor)
			.requireAudience(AUDIENCIA)
			.clockSkewSeconds(TOLERANCIA_RELOJ_SEGUNDOS)
			.build()
			.parseSignedClaims(token)
			.getPayload();

		Long id = Long.valueOf(claims.getSubject());
		String email = claims.get("email", String.class);
		List<?> roles = claims.get("roles", List.class);
		Set<String> codigos = roles == null
			? Set.of()
			: roles.stream().map(String::valueOf).collect(Collectors.toUnmodifiableSet());
		return new UsuarioAutenticado(id, email, codigos);
	}

	public record TokenEmitido(String token, Instant expiraEn) {
	}
}
