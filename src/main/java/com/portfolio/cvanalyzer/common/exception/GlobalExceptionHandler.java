package com.portfolio.cvanalyzer.common.exception;

import com.portfolio.cvanalyzer.common.dto.ApiError;
import com.portfolio.cvanalyzer.common.dto.ApiResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.util.List;

/**
 * Tratamento de erros em UM lugar so.
 *
 * @RestControllerAdvice intercepta excecoes de qualquer @RestController.
 * Beneficio: nenhum controller precisa de try/catch, e o formato de erro
 * fica identico em toda a API.
 *
 * Regra que seguimos aqui:
 *  - erro esperado (negocio/validacao) -> log em WARN, mensagem util ao cliente
 *  - erro inesperado                   -> log em ERROR com stack trace no servidor,
 *                                         mensagem GENERICA ao cliente
 * Nunca devolvemos e.getMessage() de excecao desconhecida: pode vazar
 * caminho de arquivo, SQL ou nome de tabela.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    /** Qualquer erro de negocio nosso: 404, 409, 422... o proprio erro diz o status. */
    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ApiResponse<Void>> handleBusiness(BusinessException ex) {
        log.warn("Erro de negocio [{}]: {}", ex.getCode(), ex.getMessage());
        return ResponseEntity
                .status(ex.getStatus())
                .body(ApiResponse.fail(ApiError.of(ex.getCode(), ex.getMessage())));
    }

    /** Falha de @Valid no corpo da requisicao. Devolve campo a campo. */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Void>> handleValidation(MethodArgumentNotValidException ex) {
        List<ApiError.FieldError> details = ex.getBindingResult()
                .getFieldErrors()
                .stream()
                .map(fe -> new ApiError.FieldError(fe.getField(), fe.getDefaultMessage()))
                .toList();

        log.warn("Falha de validacao: {}", details);

        ApiError error = new ApiError(
                "ERRO_DE_VALIDACAO",
                "Um ou mais campos sao invalidos.",
                details);

        return ResponseEntity.badRequest().body(ApiResponse.fail(error));
    }

    /**
     * URL que nao existe.
     *
     * Sem este handler o Spring lança NoResourceFoundException, ela cai no
     * handler generico de Exception e o cliente recebe 500 — mentira, o
     * servidor esta saudavel, quem errou foi a URL. Tratamos como 404.
     */
    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<ApiResponse<Void>> handleRotaInexistente(NoResourceFoundException ex) {
        log.warn("Rota inexistente: {}", ex.getResourcePath());
        return ResponseEntity
                .status(HttpStatus.NOT_FOUND)
                .body(ApiResponse.fail(ApiError.of(
                        "ROTA_NAO_ENCONTRADA",
                        "A rota solicitada nao existe: /" + ex.getResourcePath())));
    }

    /** Metodo HTTP errado na rota certa (POST onde so existe GET). */
    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ApiResponse<Void>> handleMetodoNaoSuportado(HttpRequestMethodNotSupportedException ex) {
        log.warn("Metodo nao suportado: {}", ex.getMessage());
        return ResponseEntity
                .status(HttpStatus.METHOD_NOT_ALLOWED)
                .body(ApiResponse.fail(ApiError.of(
                        "METODO_NAO_PERMITIDO",
                        "Metodo %s nao e permitido nesta rota.".formatted(ex.getMethod()))));
    }

    /** JSON malformado no corpo da requisicao. */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiResponse<Void>> handleCorpoInvalido(HttpMessageNotReadableException ex) {
        log.warn("Corpo da requisicao ilegivel: {}", ex.getMessage());
        return ResponseEntity
                .badRequest()
                .body(ApiResponse.fail(ApiError.of(
                        "CORPO_INVALIDO",
                        "O corpo da requisicao esta ausente ou nao e um JSON valido.")));
    }

    /** Parametro com tipo errado (ex.: /recurso/abc onde se espera um numero). */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ApiResponse<Void>> handleTipoInvalido(MethodArgumentTypeMismatchException ex) {
        log.warn("Parametro com tipo invalido: {}", ex.getName());
        return ResponseEntity
                .badRequest()
                .body(ApiResponse.fail(ApiError.of(
                        "PARAMETRO_INVALIDO",
                        "Valor invalido para o parametro '%s'.".formatted(ex.getName()))));
    }

    /** Rede de seguranca: nada escapa sem virar JSON no nosso formato. */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleUnexpected(Exception ex) {
        log.error("Erro inesperado", ex);
        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.fail(ApiError.of(
                        "ERRO_INTERNO",
                        "Erro interno no servidor. Tente novamente mais tarde.")));
    }
}
