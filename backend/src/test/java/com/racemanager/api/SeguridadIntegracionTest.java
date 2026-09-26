package com.racemanager.api;

import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import com.jayway.jsonpath.JsonPath;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import com.racemanager.api.liga.Liga;
import com.racemanager.api.liga.LigaRepository;
import com.racemanager.api.usuario.Rol;
import com.racemanager.api.usuario.RolRepository;
import com.racemanager.api.usuario.Usuario;
import com.racemanager.api.usuario.UsuarioRepository;

/**
 * Pruebas de extremo a extremo de la capa de seguridad (criterios CA-01 y CA-02).
 * Usan H2 en memoria (perfil "test"); no requieren MySQL.
 */
@SpringBootTest
@ActiveProfiles("test")
class SeguridadIntegracionTest {

	/** Contraseña generada al azar para los usuarios ficticios de H2 (no se versiona ninguna). */
	private static final String PASSWORD = UUID.randomUUID().toString();

	@Autowired
	private WebApplicationContext contexto;
	@Autowired
	private UsuarioRepository usuarios;
	@Autowired
	private RolRepository roles;
	@Autowired
	private LigaRepository ligas;
	@Autowired
	private PasswordEncoder passwordEncoder;

	private MockMvc mvc;
	private Long ligaManagerA;
	private Long ligaManagerB;

	@BeforeEach
	void preparar() {
		mvc = MockMvcBuilders.webAppContextSetup(contexto).apply(springSecurity()).build();

		ligas.deleteAll();
		usuarios.deleteAll();
		roles.deleteAll();

		Rol manager = roles.save(new Rol(Rol.MANAGER, "Manager"));
		Rol piloto = roles.save(new Rol(Rol.PILOTO, "Piloto"));
		Rol admin = roles.save(new Rol(Rol.ADMIN, "Administrador"));
		Rol equipo = roles.save(new Rol(Rol.EQUIPO, "Equipo"));

		Usuario managerA = crearUsuario("manager.a@test.com", manager, true);
		Usuario managerB = crearUsuario("manager.b@test.com", manager, true);
		crearUsuario("piloto@test.com", piloto, true);
		crearUsuario("admin@test.com", admin, true);
		crearUsuario("equipo@test.com", equipo, true);
		crearUsuario("inactivo@test.com", manager, false);

		ligaManagerA = ligas.save(new Liga("Liga A", "2026", managerA.getId())).getId();
		ligaManagerB = ligas.save(new Liga("Liga B", "2026", managerB.getId())).getId();
	}

	// --- Endpoints públicos y rechazo de anónimos -------------------------------------

	@Test
	void healthEsPublico() throws Exception {
		mvc.perform(get("/api/health")).andExpect(status().isOk());
	}

	@Test
	void sinTokenResponde401() throws Exception {
		mvc.perform(get("/api/ligas")).andExpect(status().isUnauthorized());
		mvc.perform(get("/api/auth/me")).andExpect(status().isUnauthorized());
	}

	@Test
	void tokenInvalidoResponde401() throws Exception {
		mvc.perform(get("/api/ligas").header(HttpHeaders.AUTHORIZATION, "Bearer no.es.un.token"))
			.andExpect(status().isUnauthorized());
	}

	@Test
	void encabezadoAuthorizationSinBearerResponde401() throws Exception {
		mvc.perform(get("/api/ligas").header(HttpHeaders.AUTHORIZATION, "Basic YWRtaW46YWRtaW4="))
			.andExpect(status().isUnauthorized());
	}

	@Test
	void tokenFalsificadoConRolAdminYOtraClaveResponde401() throws Exception {
		String falsificado = Jwts.builder()
			.subject("1")
			.issuer("racemanager-api")
			.audience().add("racemanager-web").and()
			.expiration(Date.from(Instant.now().plusSeconds(600)))
			.claim("roles", List.of("ADMIN"))
			.signWith(Keys.hmacShaKeyFor((UUID.randomUUID() + "-" + UUID.randomUUID())
				.getBytes(StandardCharsets.UTF_8)), Jwts.SIG.HS256)
			.compact();

		mvc.perform(get("/api/ligas").header(HttpHeaders.AUTHORIZATION, "Bearer " + falsificado))
			.andExpect(status().isUnauthorized());
	}

	@Test
	void rutaNoDeclaradaNoEsAccesibleSinAutenticacion() throws Exception {
		mvc.perform(get("/actuator/env")).andExpect(status().isUnauthorized());
		mvc.perform(post("/api/ligas").contentType(MediaType.APPLICATION_JSON).content("{}"))
			.andExpect(status().isUnauthorized());
	}

	// --- Login ------------------------------------------------------------------------

	@Test
	void loginCorrectoDevuelveTokenUtilizable() throws Exception {
		String token = login("manager.a@test.com", PASSWORD);

		mvc.perform(get("/api/auth/me").header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.email").value("manager.a@test.com"))
			.andExpect(jsonPath("$.roles[0]").value("MANAGER"));
	}

	@Test
	void loginRechazaPasswordIncorrectaEmailInexistenteYUsuarioInactivoConElMismoMensaje() throws Exception {
		for (String[] credenciales : new String[][] {
				{ "manager.a@test.com", "incorrecta" },
				{ "no.existe@test.com", PASSWORD },
				{ "inactivo@test.com", PASSWORD } }) {
			mvc.perform(post("/api/auth/login")
					.contentType(MediaType.APPLICATION_JSON)
					.content(json(credenciales[0], credenciales[1])))
				.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.error").value("Credenciales inválidas"))
				.andExpect(jsonPath("$.token").doesNotExist());
		}
	}

	@Test
	void passwordMayorA72BytesSeRechazaSinError500() throws Exception {
		mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
				.content(json("manager.a@test.com", PASSWORD + "x".repeat(80))))
			.andExpect(status().isUnauthorized());
	}

	@Test
	void loginConDatosMalFormadosResponde400() throws Exception {
		mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
				.content("{\"email\":\"no-es-email\",\"password\":\"\"}"))
			.andExpect(status().isBadRequest());
	}

	// --- Roles y aislamiento entre ligas (CA-02) -------------------------------------

	@Test
	void managerSoloListaSusLigas() throws Exception {
		String token = login("manager.a@test.com", PASSWORD);

		mvc.perform(get("/api/ligas").header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.length()").value(1))
			.andExpect(jsonPath("$[0].nombre").value("Liga A"));
	}

	@Test
	void managerAccedeASuLigaPeroNoALaAjena() throws Exception {
		String token = login("manager.a@test.com", PASSWORD);

		mvc.perform(get("/api/ligas/" + ligaManagerA).header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
			.andExpect(status().isOk());
		mvc.perform(get("/api/ligas/" + ligaManagerB).header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
			.andExpect(status().isForbidden());
	}

	@Test
	void ligaInexistenteSeTrataComoAjena() throws Exception {
		String token = login("manager.a@test.com", PASSWORD);

		mvc.perform(get("/api/ligas/999999").header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
			.andExpect(status().isForbidden());
	}

	@Test
	void pilotoNoAccedeALaGestionDeLigas() throws Exception {
		String token = login("piloto@test.com", PASSWORD);

		mvc.perform(get("/api/ligas").header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
			.andExpect(status().isForbidden());
		mvc.perform(get("/api/ligas/" + ligaManagerA).header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
			.andExpect(status().isForbidden());
	}

	@Test
	void equipoNoAccedeALaGestionDeLigas() throws Exception {
		String token = login("equipo@test.com", PASSWORD);

		mvc.perform(get("/api/ligas").header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
			.andExpect(status().isForbidden());
		mvc.perform(get("/api/ligas/" + ligaManagerA).header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
			.andExpect(status().isForbidden());
	}

	@Test
	void adminAccedeAUnaLigaDeCualquierManager() throws Exception {
		String token = login("admin@test.com", PASSWORD);

		mvc.perform(get("/api/ligas/" + ligaManagerB).header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.nombre").value("Liga B"));
	}

	@Test
	void adminVeTodasLasLigas() throws Exception {
		String token = login("admin@test.com", PASSWORD);

		mvc.perform(get("/api/ligas").header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.length()").value(2));
	}

	// --- Utilidades -------------------------------------------------------------------

	private Usuario crearUsuario(String email, Rol rol, boolean activo) {
		Usuario usuario = new Usuario(email, passwordEncoder.encode(PASSWORD), email);
		usuario.agregarRol(rol);
		usuario.setActivo(activo);
		return usuarios.save(usuario);
	}

	private String login(String email, String password) throws Exception {
		String respuesta = mvc.perform(post("/api/auth/login")
				.contentType(MediaType.APPLICATION_JSON)
				.content(json(email, password)))
			.andExpect(status().isOk())
			.andReturn().getResponse().getContentAsString();
		return JsonPath.read(respuesta, "$.token");
	}

	private static String json(String email, String password) {
		return "{\"email\":\"" + email + "\",\"password\":\"" + password + "\"}";
	}
}
