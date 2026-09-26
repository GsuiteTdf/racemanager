package com.racemanager.api;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;

/**
 * Regla de arquitectura: todo endpoint propio debe declarar {@code @PreAuthorize},
 * salvo los explícitamente públicos o de identidad. Evita que un endpoint nuevo quede
 * accesible para cualquier usuario autenticado (por ejemplo, un PILOTO) por olvido.
 */
@SpringBootTest
@ActiveProfiles("test")
class EndpointsProtegidosTest {

	/** Endpoints que intencionalmente no llevan @PreAuthorize ("Clase#metodo"). */
	private static final Set<String> EXCEPCIONES = Set.of(
		"HealthController#health",   // público
		"AuthController#login",      // público
		"AuthController#me");        // cualquier usuario autenticado consulta su propia identidad

	@Autowired
	@Qualifier("requestMappingHandlerMapping")
	private RequestMappingHandlerMapping mapeos;

	@Test
	void todoEndpointDeNegocioDeclaraPreAuthorize() {
		List<String> sinProteccion = mapeos.getHandlerMethods().values().stream()
			.filter(h -> h.getBeanType().getPackageName().startsWith("com.racemanager"))
			.filter(h -> !EXCEPCIONES.contains(nombre(h)))
			.filter(h -> !tienePreAuthorize(h))
			.map(EndpointsProtegidosTest::nombre)
			.toList();

		assertThat(sinProteccion)
			.as("Endpoints sin @PreAuthorize (agregar la regla o justificar la excepción)")
			.isEmpty();
	}

	private static boolean tienePreAuthorize(HandlerMethod h) {
		return h.hasMethodAnnotation(PreAuthorize.class)
			|| AnnotatedElementUtils.hasAnnotation(h.getBeanType(), PreAuthorize.class);
	}

	private static String nombre(HandlerMethod h) {
		return h.getBeanType().getSimpleName() + "#" + h.getMethod().getName();
	}
}
