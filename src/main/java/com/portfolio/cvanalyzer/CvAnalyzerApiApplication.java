package com.portfolio.cvanalyzer;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

/**
 * Ponto de entrada da aplicacao.
 *
 * @SpringBootApplication liga tres coisas:
 *  - @Configuration        : esta classe pode declarar beans
 *  - @EnableAutoConfiguration: Spring configura sozinho o que achar no classpath
 *  - @ComponentScan        : varre este pacote e subpacotes procurando
 *                            @Controller, @Service, @Repository, @Component
 *
 * Por isso todo codigo da aplicacao precisa ficar DENTRO de com.portfolio.cvanalyzer.
 */
@SpringBootApplication
@ConfigurationPropertiesScan   // habilita as classes @ConfigurationProperties do projeto
public class CvAnalyzerApiApplication {

    public static void main(String[] args) {
        SpringApplication.run(CvAnalyzerApiApplication.class, args);
    }
}
