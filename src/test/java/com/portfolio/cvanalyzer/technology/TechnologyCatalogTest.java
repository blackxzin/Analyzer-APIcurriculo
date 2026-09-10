package com.portfolio.cvanalyzer.technology;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.assertj.core.api.Assertions.assertThat;

class TechnologyCatalogTest {

    private static TechnologyCatalog catalogo;

    @BeforeAll
    static void setUp() {
        catalogo = new TechnologyCatalog();
        catalogo.carregar();
    }

    @Test
    @DisplayName("reconhece tecnologias escritas de formas diferentes")
    void reconheceVariacoes() {
        String texto = "Experiencia com Java 21, springboot, PostgreSQL e Docker.";
        assertThat(catalogo.extractFrom(texto))
                .contains("Java", "Spring Boot", "PostgreSQL", "Docker");
    }

    @Test
    @DisplayName("NAO confunde Java com JavaScript")
    void naoConfundeJavaComJavaScript() {
        assertThat(catalogo.extractFrom("Desenvolvedor JavaScript"))
                .contains("JavaScript")
                .doesNotContain("Java");
    }

    @Test
    @DisplayName("reconhece tecnologias com simbolo no nome")
    void reconheceSimbolos() {
        assertThat(catalogo.extractFrom("Stack: C#, C++ e .NET"))
                .contains("C#", "C++", ".NET");
    }

    @Test
    @DisplayName("ignora acentos e diferenca de maiusculas")
    void ignoraAcentoECaixa() {
        assertThat(catalogo.extractFrom("Atuei com MICROSSERVIÇOS e integração contínua"))
                .contains("Microservices", "CI/CD");
    }

    @Test
    @DisplayName("aceita quebra de linha no meio do termo, comum em PDF")
    void aceitaQuebraDeLinha() {
        assertThat(catalogo.extractFrom("Framework:\nSpring\nBoot")).contains("Spring Boot");
    }

    @Test
    @DisplayName("nao repete a mesma tecnologia citada varias vezes")
    void naoRepete() {
        assertThat(catalogo.extractFrom("java, Java, JAVA, jdk"))
                .filteredOn(t -> t.equals("Java"))
                .hasSize(1);
    }

    @ParameterizedTest
    @DisplayName("canonicaliza termos digitados na mao")
    @CsvSource({
            "springboot,   Spring Boot",
            "  POSTGRES  , PostgreSQL",
            "k8s,          Kubernetes",
            "node,         Node.js",
            "js,           JavaScript"
    })
    void canonicalizaTermos(String entrada, String esperado) {
        assertThat(catalogo.canonicalize(entrada)).isEqualTo(esperado);
    }

    @Test
    @DisplayName("termo desconhecido volta como veio, apenas aparado")
    void termoDesconhecido() {
        assertThat(catalogo.canonicalize("  Cobol Mainframe  ")).isEqualTo("Cobol Mainframe");
    }

    @Test
    @DisplayName("texto vazio nao quebra")
    void textoVazio() {
        assertThat(catalogo.extractFrom("")).isEmpty();
        assertThat(catalogo.extractFrom(null)).isEmpty();
    }
}
