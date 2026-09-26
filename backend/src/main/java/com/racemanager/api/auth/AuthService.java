package com.racemanager.api.auth;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Optional;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.racemanager.api.auth.dto.LoginRequest;
import com.racemanager.api.auth.dto.LoginResponse;
import com.racemanager.api.usuario.Usuario;
import com.racemanager.api.usuario.UsuarioRepository;

/**
 * Inicio de sesión: valida email y contraseña (BCrypt) y emite un JWT.
 *
 * <p>Para no revelar qué emails existen, todos los rechazos (email inexistente,
 * contraseña incorrecta o usuario inactivo) devuelven el mismo error y la
 * comparación BCrypt se ejecuta siempre, incluso si el usuario no existe.
 */
@Service
public class AuthService {

	/** BCrypt solo considera los primeros 72 bytes; contraseñas más largas se rechazan. */
	static final int LONGITUD_MAXIMA_BCRYPT = 72;

	private final UsuarioRepository usuarioRepository;
	private final PasswordEncoder passwordEncoder;
	private final JwtService jwtService;
	private final String hashFicticio;

	public AuthService(UsuarioRepository usuarioRepository, PasswordEncoder passwordEncoder, JwtService jwtService) {
		this.usuarioRepository = usuarioRepository;
		this.passwordEncoder = passwordEncoder;
		this.jwtService = jwtService;
		this.hashFicticio = passwordEncoder.encode("hash-ficticio-para-igualar-tiempos");
	}

	@Transactional(readOnly = true)
	public LoginResponse login(LoginRequest solicitud) {
		if (solicitud.password().getBytes(StandardCharsets.UTF_8).length > LONGITUD_MAXIMA_BCRYPT) {
			passwordEncoder.matches("comparacion-para-igualar-tiempos", hashFicticio);
			throw new CredencialesInvalidasException();
		}
		Optional<Usuario> encontrado = usuarioRepository.findByEmailIgnoreCase(solicitud.email().trim());

		String hash = encontrado.map(Usuario::getPasswordHash).orElse(hashFicticio);
		boolean passwordCorrecta = passwordEncoder.matches(solicitud.password(), hash);

		if (encontrado.isEmpty() || !passwordCorrecta || !encontrado.get().isActivo()) {
			throw new CredencialesInvalidasException();
		}

		Usuario usuario = encontrado.get();
		UsuarioAutenticado identidad = new UsuarioAutenticado(usuario.getId(), usuario.getEmail(), usuario.codigosDeRol());
		JwtService.TokenEmitido emitido = jwtService.generar(identidad);

		return new LoginResponse(
			emitido.token(),
			"Bearer",
			emitido.expiraEn(),
			new LoginResponse.UsuarioResumen(
				usuario.getId(), usuario.getEmail(), usuario.getNombre(), List.copyOf(identidad.roles())));
	}
}
