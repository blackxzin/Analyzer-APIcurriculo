package com.portfolio.cvanalyzer.resume;

import com.portfolio.cvanalyzer.common.exception.ResourceNotFoundException;
import com.portfolio.cvanalyzer.config.ResumeProperties;
import com.portfolio.cvanalyzer.resume.dto.ResumeResponse;
import com.portfolio.cvanalyzer.resume.dto.ResumeSummaryResponse;
import com.portfolio.cvanalyzer.resume.exception.InvalidUploadException;
import com.portfolio.cvanalyzer.resume.parser.ParsedResume;
import com.portfolio.cvanalyzer.resume.parser.ResumeParser;
import com.portfolio.cvanalyzer.resume.pdf.ExtractedPdf;
import com.portfolio.cvanalyzer.resume.pdf.PdfTextExtractor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.UUID;

/**
 * Regras de negocio do curriculo.
 *
 * @Transactional na classe com readOnly = true e o padrao seguro: metodo
 * que le nao abre transacao de escrita (o banco otimiza para leitura), e
 * so quem realmente grava sobrescreve com @Transactional simples.
 */
@Service
@Transactional(readOnly = true)
public class ResumeService {

    private static final Logger log = LoggerFactory.getLogger(ResumeService.class);

    private final ResumeRepository repository;
    private final PdfTextExtractor pdfTextExtractor;
    private final ResumeParser parser;
    private final ResumeMapper mapper;
    private final ResumeProperties properties;

    public ResumeService(ResumeRepository repository,
                         PdfTextExtractor pdfTextExtractor,
                         ResumeParser parser,
                         ResumeMapper mapper,
                         ResumeProperties properties) {
        this.repository = repository;
        this.pdfTextExtractor = pdfTextExtractor;
        this.parser = parser;
        this.mapper = mapper;
        this.properties = properties;
    }

    @Transactional
    public ResumeResponse upload(MultipartFile arquivo) {
        validarArquivo(arquivo);

        byte[] conteudo = lerBytes(arquivo);
        ExtractedPdf pdf = pdfTextExtractor.extract(conteudo);
        ParsedResume dados = parser.parse(pdf.text());

        Resume resume = Resume.create(
                nomeDeArquivoSeguro(arquivo.getOriginalFilename()),
                arquivo.getSize(),
                pdf.pageCount(),
                pdf.text());

        resume.applyExtractedData(dados.candidateName(), dados.email(), dados.phone());
        resume.replaceTechnologies(dados.technologies());
        for (SectionType tipo : SectionType.values()) {
            resume.replaceSection(tipo, dados.section(tipo));
        }

        Resume salvo = repository.save(resume);
        log.info("Curriculo {} processado: {} tecnologias, {} paginas",
                salvo.getId(), salvo.getTechnologies().size(), salvo.getPageCount());

        return mapper.toResponse(salvo);
    }

    public ResumeResponse findById(UUID id) {
        return mapper.toResponse(buscarOuFalhar(id));
    }

    public Page<ResumeSummaryResponse> findAll(Pageable pageable) {
        return repository.findAllByOrderByCreatedAtDesc(pageable).map(mapper::toSummary);
    }

    @Transactional
    public void delete(UUID id) {
        Resume resume = buscarOuFalhar(id);
        repository.delete(resume);
        log.info("Curriculo {} removido", id);
    }

    /**
     * Metodo compartilhado com o modulo de analise.
     * Devolve a ENTIDADE (nao o DTO) porque quem analisa precisa das
     * tecnologias carregadas, nao da representacao de API.
     */
    public Resume buscarOuFalhar(UUID id) {
        return repository.findWithDetailsById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Curriculo", id));
    }

    private void validarArquivo(MultipartFile arquivo) {
        if (arquivo == null || arquivo.isEmpty()) {
            throw new InvalidUploadException("Nenhum arquivo foi enviado.");
        }
        if (arquivo.getSize() > properties.maxFileSizeBytes()) {
            throw new InvalidUploadException(
                    "Arquivo de %d bytes excede o limite de %d bytes."
                            .formatted(arquivo.getSize(), properties.maxFileSizeBytes()));
        }
        String contentType = arquivo.getContentType();
        if (contentType == null || !properties.allowedContentTypes().contains(contentType)) {
            throw new InvalidUploadException(
                    "Tipo de arquivo nao suportado: %s. Envie um PDF.".formatted(contentType));
        }
    }

    private byte[] lerBytes(MultipartFile arquivo) {
        try {
            return arquivo.getBytes();
        } catch (IOException e) {
            log.error("Falha ao ler bytes do upload", e);
            throw new InvalidUploadException("Nao foi possivel ler o arquivo enviado.");
        }
    }

    /**
     * Blindagem contra path traversal.
     *
     * O nome do arquivo vem do cliente e pode ser "../../etc/passwd".
     * Mesmo sem gravar em disco hoje, guardar o valor cru seria uma bomba
     * armada para o dia em que alguem usar esse campo para montar um caminho.
     * Ficamos so com o nome final, sem diretorio.
     */
    private String nomeDeArquivoSeguro(String original) {
        if (original == null || original.isBlank()) {
            return "curriculo.pdf";
        }
        // Corta tudo ate a ultima barra, das DUAS convencoes. Path.getFileName()
        // sozinho nao serve: rodando em Linux, "..\\..\\evil.pdf" nao tem
        // separador nenhum e voltaria inteiro.
        String apenasNome = original.replaceAll("^.*[/\\\\]", "")
                .replaceAll("[\\p{Cntrl}]", "")
                .strip();
        if (apenasNome.isBlank() || apenasNome.equals(".") || apenasNome.equals("..")) {
            return "curriculo.pdf";
        }
        return apenasNome.length() > 255 ? apenasNome.substring(0, 255) : apenasNome;
    }
}
