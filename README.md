# CV Analyzer API

[![CI](https://github.com/blackxzin/Analyzer-APIcurriculo/actions/workflows/ci.yml/badge.svg)](https://github.com/blackxzin/Analyzer-APIcurriculo/actions/workflows/ci.yml)

API REST que recebe um currículo em PDF, extrai e estrutura suas informações,
compara o candidato com uma descrição de vaga e devolve uma pontuação de
compatibilidade com recomendações práticas.

## Stack

| Item | Versão |
|---|---|
| Java | 21 |
| Spring Boot | 4.1.1 |
| Maven | via wrapper (`./mvnw`) |
| PostgreSQL | 17 (Docker) |
| Flyway | 12.4 (migrations versionadas) |
| Spring Security | 7 (autenticação stateless com JWT) |
| JJWT | 0.13.0 (assinatura e leitura dos tokens) |
| Apache PDFBox | 3.0.8 (extração de texto e geração do relatório) |
| OpenAPI / Swagger | springdoc 3.1.1 |
| Testes | JUnit 5, Mockito, AssertJ, Testcontainers, JaCoCo |

## Pré-requisitos

- JDK 21
- Docker + Docker Compose

Maven não precisa estar instalado — o wrapper `./mvnw` baixa a versão correta.

## Como rodar

Antes de subir, crie o `.env` — a aplicação **não sobe sem `JWT_SECRET`**:

```bash
cp .env.example .env
sed -i "s|^JWT_SECRET=.*|JWT_SECRET=$(openssl rand -base64 48)|" .env
```

### Opção A — banco em Docker, aplicação local (recomendado no dia a dia)

```bash
docker compose up -d

set -a; source .env; set +a    # exporta as variáveis do .env para a sessão
./mvnw spring-boot:run
```

### Opção B — tudo em Docker

```bash
docker compose --profile full up -d --build
```

Para parar:

```bash
docker compose --profile full down          # para tudo
docker compose --profile full down -v       # para tudo e apaga os dados
```

| Recurso | URL |
|---|---|
| Swagger UI | http://localhost:8080/swagger-ui.html |
| OpenAPI JSON | http://localhost:8080/v3/api-docs |
| Health da aplicação | http://localhost:8080/api/v1/health |
| Health do Actuator | http://localhost:8080/actuator/health |

## Endpoints

A coluna **Acesso** diz o que a rota exige: `público`, `autenticado` (qualquer
usuário com token válido) ou `ADMIN`.

| Método | Rota | Descrição | Acesso |
|---|---|---|---|
| `POST` | `/api/v1/auth/register` | Cadastra um usuário e devolve o token | público |
| `POST` | `/api/v1/auth/login` | Autentica e devolve o token | público |
| `GET` | `/api/v1/auth/me` | Dados do dono do token | autenticado |
| `POST` | `/api/v1/resumes` | Envia um PDF (`multipart/form-data`, campo `file`) e extrai os dados | autenticado |
| `GET` | `/api/v1/resumes/{id}` | Busca um currículo | autenticado |
| `GET` | `/api/v1/resumes` | Lista currículos (paginado) | autenticado |
| `DELETE` | `/api/v1/resumes/{id}` | Remove o currículo e suas análises | ADMIN |
| `POST` | `/api/v1/jobs` | Cadastra uma vaga | autenticado |
| `GET` | `/api/v1/jobs/{id}` | Busca uma vaga | autenticado |
| `GET` | `/api/v1/jobs` | Lista vagas (paginado) | autenticado |
| `PUT` | `/api/v1/jobs/{id}` | Atualiza uma vaga | autenticado |
| `DELETE` | `/api/v1/jobs/{id}` | Remove a vaga e suas análises | ADMIN |
| `POST` | `/api/v1/analyses` | Compara currículo × vaga e gera a pontuação | autenticado |
| `GET` | `/api/v1/analyses/{id}` | Busca uma análise | autenticado |
| `GET` | `/api/v1/analyses/{id}/relatorio` | Baixa o relatório da análise em PDF | autenticado |
| `GET` | `/api/v1/analyses` | Histórico, com filtro por `curriculoId` e/ou `vagaId` | autenticado |
| `GET` | `/api/v1/health` | Verificação de disponibilidade | público |

## Autenticação

A API é **stateless**: não existe sessão no servidor. Quem se cadastra ou faz
login recebe um **JWT** assinado (HS256, validade de 1 hora por padrão) e o
envia em toda requisição:

```
Authorization: Bearer <token>
```

```bash
# Cadastro — o PRIMEIRO usuário cadastrado vira ADMIN; os demais, RECRUTADOR
TOKEN=$(curl -s -X POST http://localhost:8080/api/v1/auth/register \
  -H 'Content-Type: application/json' \
  -d '{"nome":"Ana Souza","email":"ana@empresa.com","senha":"senha-forte-123"}' \
  | python3 -c "import json,sys;print(json.load(sys.stdin)['data']['token'])")

# Login (mesma resposta do cadastro)
curl -s -X POST http://localhost:8080/api/v1/auth/login \
  -H 'Content-Type: application/json' \
  -d '{"email":"ana@empresa.com","senha":"senha-forte-123"}'

# Quem sou eu
curl -s http://localhost:8080/api/v1/auth/me -H "Authorization: Bearer $TOKEN"
```

| Papel | Pode |
|---|---|
| `RECRUTADOR` | Tudo, menos remover currículo ou vaga |
| `ADMIN` | Tudo, incluindo as remoções (que apagam o histórico em cascata) |

Por que o primeiro cadastro vira ADMIN: sem isso o sistema subiria sem ninguém
capaz de administrar, e a saída seria um usuário de senha fixa no código ou em
uma migration — exatamente o tipo de credencial que vaza. Promover outra pessoa
a ADMIN é operação de banco, feita de propósito fora da API.

No Swagger UI, use o botão **Authorize** e cole o token uma vez.

## Exemplo de uso

Com o `$TOKEN` da seção anterior em mãos:

```bash
# 1. Enviar o currículo
CV=$(curl -s -X POST http://localhost:8080/api/v1/resumes \
  -H "Authorization: Bearer $TOKEN" \
  -F "file=@curriculo.pdf;type=application/pdf" \
  | python3 -c "import json,sys;print(json.load(sys.stdin)['data']['id'])")

# 2. Cadastrar a vaga
VAGA=$(curl -s -X POST http://localhost:8080/api/v1/jobs \
  -H "Authorization: Bearer $TOKEN" \
  -H 'Content-Type: application/json' -d '{
    "titulo": "Desenvolvedor Java Backend Pleno",
    "empresa": "Acme Tecnologia",
    "senioridade": "PLENO",
    "descricao": "Vaga backend para produto de larga escala.",
    "requisitosObrigatorios": ["Java", "SQL", "Git", "springboot", "docker"],
    "requisitosDesejaveis": ["Kubernetes", "JUnit", "Linux"]
  }' | python3 -c "import json,sys;print(json.load(sys.stdin)['data']['id'])")

# 3. Analisar
ANALISE=$(curl -s -X POST http://localhost:8080/api/v1/analyses \
  -H "Authorization: Bearer $TOKEN" \
  -H 'Content-Type: application/json' \
  -d "{\"curriculoId\":\"$CV\",\"vagaId\":\"$VAGA\"}" \
  | python3 -c "import json,sys;print(json.load(sys.stdin)['data']['id'])")

# 4. Baixar o relatório em PDF
curl -s -OJ http://localhost:8080/api/v1/analyses/$ANALISE/relatorio \
  -H "Authorization: Bearer $TOKEN"
```

Resposta:

```json
{
  "success": true,
  "data": {
    "candidato": "Carlos Eduardo Ferreira",
    "vaga": "Desenvolvedor Java Backend Pleno",
    "compatibilidade": 61,
    "pontosFortes": ["Java", "SQL", "Git", "JUnit", "Linux"],
    "requisitosAusentes": ["Spring Boot", "Docker", "Kubernetes"],
    "recomendacoes": [
      "Sua compatibilidade com esta vaga e de 61%. Voce ja atende boa parte dos requisitos; cobrir os 3 itens abaixo deixaria seu perfil bem mais competitivo.",
      "Estude Spring Boot construindo uma API REST completa com camadas controller, service e repository",
      "Crie um Dockerfile e um docker-compose para subir um projeto seu com banco de dados",
      "Faca o deploy de uma aplicacao sua em um cluster local (kind ou minikube)"
    ],
    "totalRequisitos": 8,
    "analisadoEm": "2026-09-10T04:02:12.508700Z"
  },
  "timestamp": "2026-09-10T04:02:12.521000631Z"
}
```

## Como funciona a pontuação

Cada requisito da vaga tem um peso:

| Tipo de requisito | Peso |
|---|---|
| Obrigatório | 3 |
| Desejável | 1 |

```
compatibilidade = 100 × (soma dos pesos atendidos) ÷ (soma de todos os pesos)
```

Peso, e não contagem simples: um candidato que domina cinco diferenciais mas
não sabe o requisito obrigatório da vaga não está 83% compatível — está longe.
O peso 3× faz o obrigatório dominar a nota, como em uma triagem real.

Antes de comparar, currículo e vaga passam pelo **mesmo catálogo de
tecnologias** (`src/main/resources/technologies.csv`), que reconhece variações:
`springboot`, `Spring Boot` e `spring   boot` viram todos `Spring Boot`. Sem
isso a comparação seria injusta e a nota, errada.

## Testes

```bash
./mvnw test      # roda todos os testes
./mvnw verify    # testes + relatório de cobertura
```

Relatório: `target/site/jacoco/index.html`.

**Estado atual: 100 testes, 0 falhas, 95,7% de cobertura de instruções.**

O `verify` também aplica um **gate de 80%**: abaixo disso o build falha, em vez
de o relatório envelhecer sem ninguém notar.

| Tipo | O que cobre |
|---|---|
| Unitário | catálogo de tecnologias, extratores, motor de pontuação, motor de recomendações, leitor de PDF |
| Camada web | controllers isolados com o service mockado |
| Integração | fluxo completo com PostgreSQL real via Testcontainers, incluindo cadastro, login e as regras de acesso |

Os testes de integração usam Testcontainers — **Docker precisa estar rodando**.
Os PDFs de teste são gerados em tempo de execução pelo PDFBox, então não há
binário versionado no repositório.

## Configuração

```bash
cp .env.example .env    # ajuste os valores; .env é ignorado pelo git
```

| Variável | Default | Descrição |
|---|---|---|
| `DB_URL` | `jdbc:postgresql://localhost:5433/cvanalyzer` | URL JDBC |
| `DB_USERNAME` | `cvanalyzer` | Usuário do banco |
| `DB_PASSWORD` | `cvanalyzer` | Senha do banco |
| `DB_PORT` | `5433` | Porta exposta pelo container |
| `SERVER_PORT` | `8080` | Porta HTTP da API |
| `API_PORT` | `8080` | Porta da API quando roda em Docker |
| `JWT_SECRET` | **sem default** | Segredo de assinatura do token, mínimo de 32 caracteres |
| `JWT_EXPIRATION` | `1h` | Validade do token (formato de duração do Spring: `30m`, `2h`) |

> A porta 5433 é usada porque a 5432 costuma já estar ocupada por outro
> PostgreSQL na máquina.

Nenhuma senha fica no código: tudo vem de variável de ambiente, com defaults
apenas para desenvolvimento local.

`JWT_SECRET` é a única variável **sem default**, de propósito: a aplicação se
recusa a subir sem ela, com mensagem clara. Um segredo de desenvolvimento
embutido no `application.yml` é como a maioria dos projetos acaba assinando
token de produção com um segredo público.

## Arquitetura

```
HTTP  →  Controller  →  Service  →  Repository  →  PostgreSQL
             │             │            │
            DTO      regra de negócio  Entity
```

Organização **por feature**, não por tipo de classe:

```
com.portfolio.cvanalyzer
├── config/                  # beans de infraestrutura (OpenAPI, Clock, limites)
├── common/
│   ├── dto/                 # ApiResponse, ApiError, PageResponse
│   └── exception/           # exceções base + handler global
├── security/                # filtro JWT, regras de acesso, respostas 401/403
├── auth/                    # usuário, cadastro, login
│   ├── dto/  exception/
│   └── AppUser, AuthService, AuthController, AppUserRepository
├── report/                  # geração do relatório da análise em PDF
├── technology/              # catálogo compartilhado de tecnologias
├── resume/                  # currículo
│   ├── pdf/                 # extração de texto do PDF
│   ├── parser/              # nome, e-mail, telefone, seções
│   ├── dto/  exception/
│   └── Resume, ResumeService, ResumeController, ResumeRepository
├── job/                     # vaga e seus requisitos
├── analysis/                # comparação e histórico
│   └── engine/              # pontuação e recomendações
└── health/
```

### Decisões e o porquê

| Decisão | Motivo |
|---|---|
| DTO separado de Entity | O contrato da API não muda quando o schema muda, e nenhum campo interno (como o texto bruto do PDF) vaza |
| Envelope `ApiResponse` | Sucesso e erro têm sempre o mesmo formato |
| `@RestControllerAdvice` global | Nenhum controller precisa de `try/catch`; erro inesperado loga no servidor e devolve mensagem genérica |
| Injeção por construtor | Dependências `final`, testáveis sem subir o Spring |
| `records` para DTOs | Imutáveis por construção |
| Entidades sem `@Setter` público | JPA impede imutabilidade total; construção só por método de fábrica |
| Flyway em vez de `ddl-auto: update` | Schema vira código versionado e revisável; Hibernate roda em `validate` e falha na subida se divergir |
| `@Enumerated(STRING)` | O padrão `ORDINAL` grava índices; reordenar o enum corromperia dados já gravados |
| `open-in-view: false` | Evita consultas ao banco durante a serialização JSON |
| `@EntityGraph` / `join fetch` | Evita o problema N+1 nas listagens |
| Resultado da análise congelado | Se a vaga mudar amanhã, o histórico continua contando a verdade daquele dia |
| Testcontainers em vez de H2 | H2 aceita SQL que o Postgres recusa; testar no mesmo banco de produção elimina o bug "passou no teste, quebrou em produção" |
| Catálogo em `.csv`, não em código | Ampliar o vocabulário de tecnologias não exige recompilar |
| JWT stateless em vez de sessão | Nenhum estado no servidor: dá para rodar várias instâncias atrás de um balanceador sem sessão compartilhada. O preço é o token continuar válido até expirar, por isso a validade é curta |
| BCrypt para as senhas | Hash lento de propósito e com salt embutido: rainbow table não serve, e GPU não testa bilhões de candidatos por segundo |
| Negar por padrão (`anyRequest().authenticated()`) | Rota nova nasce protegida; esquecer de listar não abre buraco, no máximo quebra o teste |
| Remoção restrita a ADMIN | É a única operação irreversível da API (apaga o histórico em cascata) |
| Erros 401/403 no mesmo envelope | Sem isso o cliente lidaria com dois formatos de erro, e logo nos dois que todo cliente precisa tratar |
| Relatório com as fontes padrão do PDF | Sem `.ttf` no repositório, PDF de poucos KB e legível em qualquer leitor |
| Docker multi-stage + usuário não-root | Imagem final sem Maven nem código-fonte, e sem rodar como root |

### Segurança já tratada

- Nome do arquivo enviado é sanitizado contra *path traversal* (`../../etc/passwd.pdf` vira `passwd.pdf`).
- Tipo do arquivo é validado pelos **bytes** (`%PDF-`), não só pelo `Content-Type`, que o cliente controla.
- Limites de tamanho e de páginas configuráveis, aplicados antes do processamento.
- Tamanho de página das listagens limitado a 100 — `?tamanho=999999` não derruba a aplicação.
- Stack trace nunca sai na resposta HTTP.
- Nenhum segredo no código — e a aplicação não sobe com `JWT_SECRET` ausente ou fraco.
- Senha guardada só como hash BCrypt; nenhuma resposta da API devolve o hash.
- Login com e-mail inexistente responde igual ao de senha errada, e no mesmo
  tempo (a verificação roda contra um hash descartável): a API não vira uma
  ferramenta de descobrir quais e-mails estão cadastrados.
- Rota desconhecida responde 401 para quem não tem token — 404 ali contaria a
  um anônimo quais rotas existem.
- E-mail único garantido por índice no banco, não só por verificação na
  aplicação: duas requisições simultâneas passariam as duas pela verificação.

## CI

Todo push e todo pull request rodam `.github/workflows/ci.yml`: build, suíte
completa contra PostgreSQL real (Testcontainers, com Docker já disponível no
runner) e o gate de cobertura. O relatório do JaCoCo fica anexado como artefato
da execução, e o job tem `timeout-minutes` para um teste travado não segurar o
runner por horas.

## Roadmap

- [x] Estrutura base, envelope de resposta, tratamento global de erros
- [x] Upload de PDF e extração de texto
- [x] Extração de nome, e-mail, telefone, formação, experiências, tecnologias, cursos, certificações e idiomas
- [x] Persistência em PostgreSQL com migrations Flyway
- [x] CRUD de vagas com requisitos obrigatórios e desejáveis
- [x] Motor de comparação e pontuação de compatibilidade
- [x] Geração de recomendações
- [x] Histórico de análises com filtros e paginação
- [x] Documentação OpenAPI / Swagger
- [x] Dockerfile e Docker Compose
- [x] Cobertura de testes acima de 80%
- [x] Autenticação e autorização (Spring Security + JWT)
- [x] Exportação do relatório de análise em PDF
- [x] Pipeline de CI (GitHub Actions)
- [ ] Camada de IA para enriquecer a análise semântica do currículo
