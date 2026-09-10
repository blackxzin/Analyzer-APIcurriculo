package com.portfolio.cvanalyzer.auth;

import com.portfolio.cvanalyzer.support.IntegrationTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.nio.charset.StandardCharsets;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Cadastro, login e as regras de acesso, contra a aplicacao inteira.
 *
 * A tabela de usuarios e limpa antes de cada teste: varias regras aqui
 * dependem do estado do cadastro (o primeiro usuario vira ADMIN), e teste
 * que depende da ordem de execucao quebra sozinho no dia em que alguem
 * inserir um teste novo no meio.
 */
@IntegrationTest
class AuthIntegrationTest {

    private static final Pattern TOKEN_NO_JSON = Pattern.compile("\"token\"\\s*:\\s*\"([^\"]+)\"");

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private AppUserRepository repository;

    @BeforeEach
    void limparUsuarios() {
        repository.deleteAll();
    }

    private String cadastrar(String nome, String email, String senha) throws Exception {
        String resposta = mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"nome": "%s", "email": "%s", "senha": "%s"}
                                """.formatted(nome, email, senha)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);

        Matcher matcher = TOKEN_NO_JSON.matcher(resposta);
        assertThat(matcher.find()).as("o cadastro deve devolver um token").isTrue();
        return matcher.group(1);
    }

    private static String bearer(String token) {
        return "Bearer " + token;
    }

    @Test
    @DisplayName("primeiro usuario cadastrado vira ADMIN e o segundo, RECRUTADOR")
    void primeiroUsuarioViraAdmin() throws Exception {
        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"nome": "Ana Souza", "email": "ana@empresa.com", "senha": "senha-forte-1"}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.tipo").value("Bearer"))
                .andExpect(jsonPath("$.data.expiraEmSegundos").value(3600))
                .andExpect(jsonPath("$.data.usuario.papel").value("ADMIN"))
                .andExpect(jsonPath("$.data.usuario.email").value("ana@empresa.com"));

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"nome": "Bruno Lima", "email": "BRUNO@empresa.com", "senha": "senha-forte-2"}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.usuario.papel").value("RECRUTADOR"))
                // e-mail e guardado em minusculas: "BRUNO@" e "bruno@" sao a mesma pessoa
                .andExpect(jsonPath("$.data.usuario.email").value("bruno@empresa.com"));
    }

    @Test
    @DisplayName("nenhuma resposta de autenticacao devolve o hash da senha")
    void nuncaVazaSenha() throws Exception {
        String resposta = mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"nome": "Ana", "email": "ana@empresa.com", "senha": "senha-forte-1"}
                                """))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);

        assertThat(resposta).doesNotContain("senha", "$2a$", "passwordHash");
    }

    @Test
    @DisplayName("e-mail repetido devolve 409")
    void emailRepetido() throws Exception {
        cadastrar("Ana Souza", "ana@empresa.com", "senha-forte-1");

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"nome": "Outra Ana", "email": "Ana@Empresa.com", "senha": "senha-forte-9"}
                                """))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("EMAIL_JA_CADASTRADO"));
    }

    @Test
    @DisplayName("senha curta demais e barrada na validacao")
    void senhaCurta() throws Exception {
        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"nome": "Ana", "email": "ana@empresa.com", "senha": "123"}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("ERRO_DE_VALIDACAO"))
                .andExpect(jsonPath("$.error.details[0].field").value("senha"));
    }

    @Test
    @DisplayName("login com a senha certa devolve token, e com a errada devolve 401")
    void login() throws Exception {
        cadastrar("Ana Souza", "ana@empresa.com", "senha-forte-1");

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email": "ana@empresa.com", "senha": "senha-forte-1"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.token").isNotEmpty());

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email": "ana@empresa.com", "senha": "senha-errada-1"}
                                """))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error.code").value("CREDENCIAIS_INVALIDAS"));
    }

    @Test
    @DisplayName("login de e-mail inexistente responde igual a senha errada")
    void loginDeEmailInexistente() throws Exception {
        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email": "ninguem@empresa.com", "senha": "senha-forte-1"}
                                """))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error.code").value("CREDENCIAIS_INVALIDAS"))
                .andExpect(jsonPath("$.error.message").value("E-mail ou senha invalidos."));
    }

    @Test
    @DisplayName("/me devolve o dono do token")
    void meDevolveODono() throws Exception {
        String token = cadastrar("Ana Souza", "ana@empresa.com", "senha-forte-1");

        mockMvc.perform(get("/api/v1/auth/me").header(HttpHeaders.AUTHORIZATION, bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.email").value("ana@empresa.com"))
                .andExpect(jsonPath("$.data.papel").value("ADMIN"));
    }

    @Test
    @DisplayName("rota protegida sem token responde 401 no envelope da API")
    void rotaProtegidaSemToken() throws Exception {
        mockMvc.perform(get("/api/v1/jobs"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("NAO_AUTENTICADO"));
    }

    @Test
    @DisplayName("token invalido nao autentica")
    void tokenInvalido() throws Exception {
        mockMvc.perform(get("/api/v1/jobs").header(HttpHeaders.AUTHORIZATION, bearer("token.falso.aqui")))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error.code").value("NAO_AUTENTICADO"));
    }

    @Test
    @DisplayName("rota publica continua aberta sem token")
    void rotaPublicaSegueAberta() throws Exception {
        mockMvc.perform(get("/api/v1/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    @DisplayName("apenas ADMIN remove recurso: RECRUTADOR recebe 403")
    void remocaoExigeAdmin() throws Exception {
        cadastrar("Ana Souza", "ana@empresa.com", "senha-forte-1");            // vira ADMIN
        String recrutador = cadastrar("Bruno Lima", "bruno@empresa.com", "senha-forte-2");

        mockMvc.perform(delete("/api/v1/jobs/{id}", UUID.randomUUID())
                        .header(HttpHeaders.AUTHORIZATION, bearer(recrutador)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error.code").value("ACESSO_NEGADO"));
    }

    @Test
    @DisplayName("ADMIN passa pela regra de remocao e chega ao controller")
    void adminPassaPelaRegraDeRemocao() throws Exception {
        String admin = cadastrar("Ana Souza", "ana@empresa.com", "senha-forte-1");

        // 404, e nao 403: a autorizacao permitiu e quem respondeu foi a regra
        // de negocio, que nao achou a vaga.
        mockMvc.perform(delete("/api/v1/jobs/{id}", UUID.randomUUID())
                        .header(HttpHeaders.AUTHORIZATION, bearer(admin)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error.code").value("RECURSO_NAO_ENCONTRADO"));
    }
}
