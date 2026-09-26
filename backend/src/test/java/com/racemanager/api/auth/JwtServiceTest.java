package com.racemanager.api.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

class JwtServiceTest {

	/** Secretos aleatorios por ejecución: no se versiona ninguna clave de firma. */
	private static final String SECRETO = secretoAleatorio();
	private static final String EMISOR = "racemanager-api";

	private final JwtService servicio = new JwtService(SECRETO, 15, EMISOR);
	private final UsuarioAutenticado manager = new UsuarioAutenticado(7L, "manager@liga.test", Set.of("MANAGER"));

	@Test
	void generaYValidaUnTokenConservandoIdentidadYRoles() {
		String token = servicio.generar(manager).token();

		UsuarioAutenticado leido = servicio.validar(token);

		assertThat(leido.id()).isEqualTo(7L);
		assertThat(leido.email()).isEqualTo("manager@liga.test");
		assertThat(leido.roles()).containsExactly("MANAGER");
	}

	@Test
	void rechazaTokenAlterado() {
		String token = servicio.generar(manager).token();
		String alterado = token.substring(0, token.length() - 2) + (token.endsWith("A") ? "BB" : "AA");

		assertThatThrownBy(() -> servicio.validar(alterado)).isInstanceOf(JwtException.class);
	}

	@Test
	void rechazaTokenFirmadoConOtroSecreto() {
		JwtService otro = new JwtService(secretoAleatorio(), 15, EMISOR);
		String token = otro.generar(manager).token();

		assertThatThrownBy(() -> servicio.validar(token)).isInstanceOf(JwtException.class);
	}

	@Test
	void rechazaTokenVencido() {
		Instant hace2Horas = Instant.now().minusSeconds(7200);
		String vencido = Jwts.builder()
			.subject("7")
			.issuer(EMISOR)
			.audience().add(JwtService.AUDIENCIA).and()
			.issuedAt(Date.from(hace2Horas))
			.expiration(Date.from(hace2Horas.plusSeconds(60)))
			.claim("roles", List.of("MANAGER"))
			.signWith(Keys.hmacShaKeyFor(SECRETO.getBytes(StandardCharsets.UTF_8)), Jwts.SIG.HS256)
			.compact();

		assertThatThrownBy(() -> servicio.validar(vencido)).isInstanceOf(JwtException.class);
	}

	@Test
	void rechazaTokenDeOtroEmisor() {
		JwtService otroEmisor = new JwtService(SECRETO, 15, "otra-aplicacion");
		String token = otroEmisor.generar(manager).token();

		assertThatThrownBy(() -> servicio.validar(token)).isInstanceOf(JwtException.class);
	}

	@Test
	void rechazaTokenSinFirma() {
		String sinFirma = Jwts.builder().subject("7").issuer(EMISOR)
			.claim("roles", List.of("ADMIN")).compact();

		assertThatThrownBy(() -> servicio.validar(sinFirma)).isInstanceOf(JwtException.class);
	}

	@Test
	void rechazaTokenConOtraAudiencia() {
		String token = Jwts.builder()
			.subject("7")
			.issuer(EMISOR)
			.audience().add("otra-aplicacion").and()
			.expiration(Date.from(Instant.now().plusSeconds(600)))
			.claim("roles", List.of("ADMIN"))
			.signWith(Keys.hmacShaKeyFor(SECRETO.getBytes(StandardCharsets.UTF_8)), Jwts.SIG.HS256)
			.compact();

		assertThatThrownBy(() -> servicio.validar(token)).isInstanceOf(JwtException.class);
	}

	@Test
	void noArrancaSinSecretoOConSecretoCorto() {
		assertThatThrownBy(() -> new JwtService("", 15, EMISOR)).isInstanceOf(IllegalStateException.class);
		assertThatThrownBy(() -> new JwtService("corto", 15, EMISOR)).isInstanceOf(IllegalStateException.class);
	}

	private static String secretoAleatorio() {
		return UUID.randomUUID() + "-" + UUID.randomUUID();
	}
}
