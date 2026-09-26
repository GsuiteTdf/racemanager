package com.racemanager.api.importacion;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;
import tools.jackson.databind.json.JsonMapper;

class AcServerJsonParserTest {
    private final AcServerJsonParser parser = new AcServerJsonParser(JsonMapper.builder().build());

    private byte[] muestra() throws IOException {
        return new ClassPathResource("ejemplos/ac-servidor-carrera.json").getContentAsByteArray();
    }

    @Test
    void reconoceJsonNativoYCreaVistaNormalizada() throws IOException {
        ResultadoParseado r = parser.parsear(muestra());
        assertThat(r.fuente()).isEqualTo("AC_SERVER_JSON");
        assertThat(r.circuito()).isEqualTo("monza");
        assertThat(r.tipoSesion()).isEqualTo("RACE");
        assertThat(r.vueltasProgramadas()).isEqualTo(10);
        assertThat(r.vueltasRegistradas()).isEqualTo(3);
        assertThat(r.eventosRegistrados()).isEqualTo(1);
        assertThat(r.participantes()).hasSize(2);
        assertThat(r.participantes().get(0).ordenEnArchivo()).isEqualTo(1);
        assertThat(r.participantes().get(0).carId()).isEqualTo(9);
        assertThat(r.participantes().get(0).vueltas()).hasSize(2);
        assertThat(r.participantes().get(0).mejorVueltaMs()).isEqualTo(98907L);
        assertThat(r.participantes().get(0).vueltas().get(0).sectoresMs())
            .containsExactly(38437L, 34564L, 31356L);
    }

    @Test
    void noPropagaIdentificadoresExternosNiEventosCrudos() throws IOException {
        ResultadoParseado r = parser.parsear(muestra());
        String json = JsonMapper.builder().build().writeValueAsString(r);
        assertThat(json).doesNotContain("ID_SIMULADO_A", "ID_SIMULADO_B", "COLLISION_WITH_ENV");
    }

    @Test
    void aceptaEventsNullYAdvierte() throws IOException {
        String json = new String(muestra(), StandardCharsets.UTF_8)
            .replace("\"Events\": [{\"Type\": \"COLLISION_WITH_ENV\", \"CarId\": 4}]", "\"Events\": null");
        ResultadoParseado r = parser.parsear(json.getBytes(StandardCharsets.UTF_8));
        assertThat(r.eventosRegistrados()).isZero();
        assertThat(r.advertencias()).anyMatch(x -> x.contains("no incluye eventos"));
    }

    @Test
    void rechazaFormatoDeCompetizioneYRacceExplorer() {
        for (String entrada : new String[] {
            "{\"sessionType\":\"R\",\"sessionResult\":{}}",
            "{\"session_info\":{\"track\":\"monza\"},\"driver_statistics\":{}}"
        }) {
            assertThatThrownBy(() -> parser.parsear(entrada.getBytes(StandardCharsets.UTF_8)))
                .isInstanceOf(ArchivoImportacionInvalidoException.class);
        }
    }

    @Test
    void rechazaJsonMalformadoYCarIdInconsistente() throws IOException {
        assertThatThrownBy(() -> parser.parsear("{oops".getBytes(StandardCharsets.UTF_8)))
            .isInstanceOf(ArchivoImportacionInvalidoException.class);
        String json = new String(muestra(), StandardCharsets.UTF_8)
            .replace("\"CarId\": 9, \"CarModel\"", "\"CarId\": 99, \"CarModel\"");
        assertThatThrownBy(() -> parser.parsear(json.getBytes(StandardCharsets.UTF_8)))
            .isInstanceOf(ArchivoImportacionInvalidoException.class)
            .hasMessageContaining("CarId");
    }

    @Test
    void noAdmiteVueltasConTiempoNegativo() throws IOException {
        String json = new String(muestra(), StandardCharsets.UTF_8)
            .replace("\"LapTime\": 104357", "\"LapTime\": -1");
        assertThatThrownBy(() -> parser.parsear(json.getBytes(StandardCharsets.UTF_8)))
            .isInstanceOf(ArchivoImportacionInvalidoException.class);
    }

    @Test
    void procesaMuestraHistoricaDeMonzaConIdentidadesFicticias() throws IOException {
        byte[] muestra = new ClassPathResource(
            "ejemplos/ac-servidor-monza-historico-anonimizado.json").getContentAsByteArray();
        ResultadoParseado resultado = parser.parsear(muestra);
        assertThat(resultado.circuito()).isEqualTo("monza");
        assertThat(resultado.participantes()).hasSize(17);
        assertThat(resultado.vueltasRegistradas()).isEqualTo(82);
        assertThat(resultado.eventosRegistrados()).isEqualTo(129);
        assertThat(resultado.participantes().get(0).nombre()).startsWith("Piloto de prueba");
        assertThat(JsonMapper.builder().build().writeValueAsString(resultado))
            .doesNotContain("IDENTIFICADOR_NO_REAL");
    }
}
