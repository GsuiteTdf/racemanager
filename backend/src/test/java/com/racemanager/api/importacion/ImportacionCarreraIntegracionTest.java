package com.racemanager.api.importacion;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.UUID;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import com.jayway.jsonpath.JsonPath;
import com.racemanager.api.carrera.Carrera;
import com.racemanager.api.carrera.CarreraRepository;
import com.racemanager.api.liga.Liga;
import com.racemanager.api.liga.LigaRepository;
import com.racemanager.api.usuario.Rol;
import com.racemanager.api.usuario.RolRepository;
import com.racemanager.api.usuario.Usuario;
import com.racemanager.api.usuario.UsuarioRepository;
import com.racemanager.api.auth.JwtService;
import com.racemanager.api.auth.UsuarioAutenticado;

@SpringBootTest
@ActiveProfiles("test")
class ImportacionCarreraIntegracionTest {
    private static final String PASSWORD = UUID.randomUUID().toString();
    @Autowired private WebApplicationContext contexto;
    @Autowired private CarreraRepository carreras;
    @Autowired private ImportacionCarreraRepository importaciones;
    @Autowired private LigaRepository ligas;
    @Autowired private UsuarioRepository usuarios;
    @Autowired private RolRepository roles;
    @Autowired private PasswordEncoder encoder;
    @Autowired private JwtService jwt;
    private MockMvc mvc;
    private Long ligaA;
    private Long ligaB;
    private Long carreraA;
    private Long carreraB;
    private String managerA;
    private String managerB;
    private String piloto;

    @BeforeEach
    void preparar() {
        mvc = MockMvcBuilders.webAppContextSetup(contexto).apply(springSecurity()).build();
        importaciones.deleteAll();
        carreras.deleteAll();
        ligas.deleteAll();
        usuarios.deleteAll();
        roles.deleteAll();
        Rol rolManager = roles.save(new Rol(Rol.MANAGER, "Manager"));
        Rol rolPiloto = roles.save(new Rol(Rol.PILOTO, "Piloto"));
        Usuario a = new Usuario("a@fixture.local", encoder.encode(PASSWORD), "Manager A");
        a.agregarRol(rolManager); a = usuarios.save(a);
        Usuario b = new Usuario("b@fixture.local", encoder.encode(PASSWORD), "Manager B");
        b.agregarRol(rolManager); b = usuarios.save(b);
        Usuario p = new Usuario("p@fixture.local", encoder.encode(PASSWORD), "Piloto");
        p.agregarRol(rolPiloto); p = usuarios.save(p);
        ligaA = ligas.save(new Liga("Liga A", "2026", a.getId())).getId();
        ligaB = ligas.save(new Liga("Liga B", "2026", b.getId())).getId();
        carreraA = carreras.save(new Carrera(ligaA, 1L, "Fecha 1", LocalDateTime.now())).getId();
        carreraB = carreras.save(new Carrera(ligaB, 1L, "Fecha 2", LocalDateTime.now())).getId();
        managerA = jwt.generar(new UsuarioAutenticado(a.getId(), a.getEmail(), a.codigosDeRol())).token();
        managerB = jwt.generar(new UsuarioAutenticado(b.getId(), b.getEmail(), b.codigosDeRol())).token();
        piloto = jwt.generar(new UsuarioAutenticado(p.getId(), p.getEmail(), p.codigosDeRol())).token();
    }

    @AfterEach
    void limpiar() {
        importaciones.deleteAll();
        carreras.deleteAll();
        ligas.deleteAll();
        usuarios.deleteAll();
        roles.deleteAll();
    }

    private byte[] muestra() throws IOException {
        return new ClassPathResource("ejemplos/ac-servidor-carrera.json").getContentAsByteArray();
    }
    private MockMultipartFile archivo(String nombre, byte[] datos) {
        return new MockMultipartFile("archivo", nombre, MediaType.APPLICATION_JSON_VALUE, datos);
    }
    private String url(Long liga, Long carrera) {
        return "/api/ligas/" + liga + "/carreras/" + carrera + "/importaciones";
    }
    private String bearer(String token) { return "Bearer " + token; }

    @Test
    void managerCargaObtieneVistaYPersisteSinPublicar() throws Exception {
        String respuesta = mvc.perform(multipart(url(ligaA, carreraA))
                .file(archivo("resultado.json", muestra()))
                .header(HttpHeaders.AUTHORIZATION, bearer(managerA)))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.estado").value("PROCESADA"))
            .andExpect(jsonPath("$.resultado.fuente").value("AC_SERVER_JSON"))
            .andExpect(jsonPath("$.resultado.participantes.length()").value(2))
            .andReturn().getResponse().getContentAsString();
        Number id = JsonPath.read(respuesta, "$.id");
        assertThat(importaciones.count()).isEqualTo(1);
        assertThat(carreras.findById(carreraA).orElseThrow().getEstado())
            .isEqualTo(Carrera.Estado.PROGRAMADA);
        assertThat(importaciones.findById(id.longValue()).orElseThrow().getDetalleJson())
            .doesNotContain("ID_SIMULADO_A");
        mvc.perform(get(url(ligaA, carreraA) + "/" + id)
                .header(HttpHeaders.AUTHORIZATION, bearer(managerA)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.resultado.vueltasRegistradas").value(3));
    }

    @Test
    void duplicadoParaMismaCarreraDevuelve409() throws Exception {
        mvc.perform(multipart(url(ligaA, carreraA)).file(archivo("a.json", muestra()))
            .header(HttpHeaders.AUTHORIZATION, bearer(managerA))).andExpect(status().isCreated());
        mvc.perform(multipart(url(ligaA, carreraA)).file(archivo("copia.json", muestra()))
            .header(HttpHeaders.AUTHORIZATION, bearer(managerA))).andExpect(status().isConflict());
        assertThat(importaciones.count()).isEqualTo(1);
    }

    @Test
    void accesoAnonimoPilotoYManagerAjenoBloqueados() throws Exception {
        mvc.perform(multipart(url(ligaA, carreraA)).file(archivo("a.json", muestra())))
            .andExpect(status().isUnauthorized());
        mvc.perform(multipart(url(ligaA, carreraA)).file(archivo("a.json", muestra()))
            .header(HttpHeaders.AUTHORIZATION, bearer(piloto))).andExpect(status().isForbidden());
        mvc.perform(multipart(url(ligaA, carreraA)).file(archivo("a.json", muestra()))
            .header(HttpHeaders.AUTHORIZATION, bearer(managerB))).andExpect(status().isForbidden());
        mvc.perform(multipart(url(ligaA, carreraB)).file(archivo("a.json", muestra()))
            .header(HttpHeaders.AUTHORIZATION, bearer(managerA))).andExpect(status().isForbidden());
        assertThat(importaciones.count()).isZero();
    }

    @Test
    void rechazaArchivoVacioExtensionIncorrectaJsonMalformadoYOversize() throws Exception {
        mvc.perform(multipart(url(ligaA, carreraA)).file(archivo("a.json", new byte[0]))
            .header(HttpHeaders.AUTHORIZATION, bearer(managerA))).andExpect(status().isBadRequest());
        mvc.perform(multipart(url(ligaA, carreraA)).file(archivo("a.xml", muestra()))
            .header(HttpHeaders.AUTHORIZATION, bearer(managerA))).andExpect(status().isBadRequest());
        mvc.perform(multipart(url(ligaA, carreraA))
            .file(archivo("a.json", "{mal formado".getBytes(StandardCharsets.UTF_8)))
            .header(HttpHeaders.AUTHORIZATION, bearer(managerA))).andExpect(status().isBadRequest());
        mvc.perform(multipart(url(ligaA, carreraA))
            .file(archivo("grande.json", new byte[ImportacionCarreraService.MAX_BYTES + 1]))
            .header(HttpHeaders.AUTHORIZATION, bearer(managerA)))
            .andExpect(status().isBadRequest());
        assertThat(importaciones.count()).isZero();
    }

    @Test
    void noImportaEnCarreraPublicada() throws Exception {
        Carrera carrera = carreras.findById(carreraA).orElseThrow();
        carrera.setEstado(Carrera.Estado.PUBLICADA);
        carreras.saveAndFlush(carrera);
        mvc.perform(multipart(url(ligaA, carreraA)).file(archivo("a.json", muestra()))
            .header(HttpHeaders.AUTHORIZATION, bearer(managerA))).andExpect(status().isConflict());
        assertThat(importaciones.count()).isZero();
    }
}
