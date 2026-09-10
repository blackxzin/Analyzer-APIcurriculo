package com.portfolio.cvanalyzer.support;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Anotacao composta para os testes de integracao.
 *
 * Em vez de repetir @SpringBootTest + @AutoConfigureMockMvc + configuracao
 * do container em cada classe, concentramos tudo aqui. Mudar a configuracao
 * dos testes passa a ser mudar um arquivo, nao dez (DRY).
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@SpringBootTest
@AutoConfigureMockMvc
@Import(PostgresContainerConfig.class)
public @interface IntegrationTest {
}
