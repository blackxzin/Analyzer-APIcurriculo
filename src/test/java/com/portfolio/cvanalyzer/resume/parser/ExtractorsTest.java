package com.portfolio.cvanalyzer.resume.parser;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Testes dos extratores de contato e nome.
 *
 * @Nested agrupa por extrator: o relatorio de teste fica legivel e cada
 * grupo conta sua propria historia.
 */
class ExtractorsTest {

    @Nested
    @DisplayName("EmailExtractor")
    class Email {

        private final EmailExtractor extractor = new EmailExtractor();

        @Test
        @DisplayName("encontra o e-mail no meio do texto")
        void encontraEmail() {
            assertThat(extractor.extract("Contato: maria.souza@email.com | (11) 91234-5678"))
                    .contains("maria.souza@email.com");
        }

        @Test
        @DisplayName("normaliza para minusculas")
        void normalizaCaixa() {
            assertThat(extractor.extract("MARIA@EMAIL.COM")).contains("maria@email.com");
        }

        @Test
        @DisplayName("devolve vazio quando nao ha e-mail")
        void semEmail() {
            assertThat(extractor.extract("Nenhum contato aqui")).isEmpty();
            assertThat(extractor.extract(null)).isEmpty();
        }
    }

    @Nested
    @DisplayName("PhoneExtractor")
    class Telefone {

        private final PhoneExtractor extractor = new PhoneExtractor();

        @ParameterizedTest
        @DisplayName("reconhece os formatos brasileiros usuais")
        @ValueSource(strings = {
                "(11) 98765-4321",
                "11 98765-4321",
                "+55 11 98765-4321",
                "11987654321",
                "(11) 3456-7890"
        })
        void reconheceFormatos(String telefone) {
            assertThat(extractor.extract("Telefone: " + telefone)).isPresent();
        }

        @Test
        @DisplayName("NAO confunde intervalo de anos com telefone")
        void ignoraAnos() {
            assertThat(extractor.extract("Experiencia de 2020 a 2024")).isEmpty();
        }

        @Test
        @DisplayName("devolve vazio quando nao ha telefone")
        void semTelefone() {
            assertThat(extractor.extract("Sem numero de contato")).isEmpty();
        }
    }

    @Nested
    @DisplayName("NameExtractor")
    class Nome {

        private final NameExtractor extractor = new NameExtractor();

        @Test
        @DisplayName("pega o nome da primeira linha util")
        void pegaNome() {
            String curriculo = """
                    Maria Souza Lima
                    maria@email.com
                    Desenvolvedora
                    """;
            assertThat(extractor.extract(curriculo)).contains("Maria Souza Lima");
        }

        @Test
        @DisplayName("aceita conectivos como 'de' e 'da'")
        void aceitaConectivos() {
            assertThat(extractor.extract("Joao Pedro de Almeida\njoao@email.com"))
                    .contains("Joao Pedro de Almeida");
        }

        @Test
        @DisplayName("ignora o titulo 'CURRICULO' e pega o nome logo abaixo")
        void ignoraTitulo() {
            assertThat(extractor.extract("CURRICULO\nAna Beatriz Costa\nana@email.com"))
                    .contains("Ana Beatriz Costa");
        }

        @Test
        @DisplayName("ignora linha com e-mail, numero ou link")
        void ignoraLinhaDeContato() {
            assertThat(extractor.extract("contato@email.com\nRua 25 de Marco\nhttp://site.com"))
                    .isEmpty();
        }

        @Test
        @DisplayName("devolve vazio quando nada parece nome")
        void semNome() {
            assertThat(extractor.extract("1234567890")).isEmpty();
            assertThat(extractor.extract("")).isEmpty();
        }
    }
}
