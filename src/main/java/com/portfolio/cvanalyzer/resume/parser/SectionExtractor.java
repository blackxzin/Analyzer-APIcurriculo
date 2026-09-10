package com.portfolio.cvanalyzer.resume.parser;

import com.portfolio.cvanalyzer.resume.SectionType;
import com.portfolio.cvanalyzer.technology.TextNormalizer;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * Fatia o curriculo em secoes (formacao, experiencia, cursos, certificacoes,
 * idiomas).
 *
 * Como funciona:
 *  1. varre linha a linha procurando cabecalhos conhecidos;
 *  2. tudo que vem depois de um cabecalho pertence aquela secao, ate o
 *     proximo cabecalho;
 *  3. linhas quebradas pelo PDF sao remontadas: linha nova so comeca em
 *     marcador (-, •) ou depois de linha em branco.
 *
 * O passo 3 importa porque o PDF quebra um paragrafo em varias linhas
 * fisicas; sem remontar, uma unica experiencia viraria cinco itens picados.
 */
@Component
public class SectionExtractor {

    private static final int MAX_CARACTERES_CABECALHO = 60;
    private static final int MAX_ITENS_POR_SECAO = 40;
    private static final int MIN_CARACTERES_ITEM = 3;

    private static final Pattern MARCADOR_DE_LISTA =
            Pattern.compile("^\\s*(?:[-*•▪●·–—>]+|\\d{1,2}[.)])\\s+");

    /**
     * Cabecalhos que abrem uma secao que nos interessa.
     * Ordem importa: o mais especifico precisa vir antes ("formacao
     * academica" antes de "formacao"), senao o generico casa primeiro.
     */
    private static final List<Map.Entry<String, SectionType>> CABECALHOS = List.of(
            Map.entry("experiencia profissional", SectionType.EXPERIENCE),
            Map.entry("experiencias profissionais", SectionType.EXPERIENCE),
            Map.entry("historico profissional", SectionType.EXPERIENCE),
            Map.entry("atuacao profissional", SectionType.EXPERIENCE),
            Map.entry("vivencia profissional", SectionType.EXPERIENCE),
            Map.entry("experiencia", SectionType.EXPERIENCE),
            Map.entry("experiencias", SectionType.EXPERIENCE),
            Map.entry("professional experience", SectionType.EXPERIENCE),
            Map.entry("work experience", SectionType.EXPERIENCE),
            Map.entry("experience", SectionType.EXPERIENCE),

            Map.entry("formacao academica", SectionType.EDUCATION),
            Map.entry("formacao complementar", SectionType.COURSE),
            Map.entry("formacao", SectionType.EDUCATION),
            Map.entry("escolaridade", SectionType.EDUCATION),
            Map.entry("educacao", SectionType.EDUCATION),
            Map.entry("education", SectionType.EDUCATION),
            Map.entry("academic background", SectionType.EDUCATION),

            Map.entry("cursos complementares", SectionType.COURSE),
            Map.entry("cursos e capacitacoes", SectionType.COURSE),
            Map.entry("cursos", SectionType.COURSE),
            Map.entry("capacitacoes", SectionType.COURSE),
            Map.entry("courses", SectionType.COURSE),

            Map.entry("certificacoes", SectionType.CERTIFICATION),
            Map.entry("certificados", SectionType.CERTIFICATION),
            Map.entry("certifications", SectionType.CERTIFICATION),
            Map.entry("licenses and certifications", SectionType.CERTIFICATION),

            Map.entry("idiomas", SectionType.LANGUAGE),
            Map.entry("linguas", SectionType.LANGUAGE),
            Map.entry("languages", SectionType.LANGUAGE)
    );

    /**
     * Cabecalhos de secoes que NAO guardamos, mas que precisam ser
     * reconhecidos: eles encerram a secao anterior. Sem esta lista, tudo que
     * viesse depois de "Experiencia" — incluindo "Projetos" e "Contato" —
     * seria gravado como experiencia.
     */
    private static final List<String> CABECALHOS_NEUTROS = List.of(
            "objetivo", "objetivos", "resumo", "resumo profissional", "perfil",
            "perfil profissional", "sobre mim", "sobre", "apresentacao",
            "habilidades", "competencias", "competencias tecnicas",
            "conhecimentos", "conhecimentos tecnicos", "tecnologias",
            "hard skills", "soft skills", "skills", "summary", "profile",
            "objective", "about", "projetos", "projects", "portfolio",
            "contato", "contatos", "dados pessoais", "informacoes pessoais",
            "voluntariado", "premios", "publicacoes", "referencias"
    );

    public Map<SectionType, List<String>> extract(String texto) {
        Map<SectionType, List<String>> resultado = new EnumMap<>(SectionType.class);
        if (texto == null || texto.isBlank()) {
            return resultado;
        }

        List<String> linhas = texto.lines().map(String::strip).toList();

        SectionType secaoAtual = null;
        List<String> buffer = new ArrayList<>();
        boolean iniciarNovoItem = true;

        for (String linha : linhas) {
            SectionType cabecalho = classificarCabecalho(linha);
            boolean ehNeutro = ehCabecalhoNeutro(linha);

            if (cabecalho != null || ehNeutro) {
                gravar(resultado, secaoAtual, buffer);
                buffer = new ArrayList<>();
                secaoAtual = cabecalho;   // null quando o cabecalho e neutro
                iniciarNovoItem = true;
                continue;
            }

            if (secaoAtual == null) {
                continue;
            }

            if (linha.isEmpty()) {
                iniciarNovoItem = true;
                continue;
            }

            String conteudo = MARCADOR_DE_LISTA.matcher(linha).replaceFirst("").strip();
            if (conteudo.isEmpty()) {
                continue;
            }

            boolean temMarcador = MARCADOR_DE_LISTA.matcher(linha).find();
            if (iniciarNovoItem || temMarcador || buffer.isEmpty()) {
                buffer.add(conteudo);
                iniciarNovoItem = false;
            } else {
                // Continuacao da linha anterior: remonta o paragrafo.
                int ultimo = buffer.size() - 1;
                buffer.set(ultimo, buffer.get(ultimo) + " " + conteudo);
            }
        }

        gravar(resultado, secaoAtual, buffer);
        return resultado;
    }

    private void gravar(Map<SectionType, List<String>> resultado,
                        SectionType secao,
                        List<String> buffer) {
        if (secao == null || buffer.isEmpty()) {
            return;
        }
        List<String> itens = buffer.stream()
                .map(String::strip)
                .filter(item -> item.length() >= MIN_CARACTERES_ITEM)
                .limit(MAX_ITENS_POR_SECAO)
                .toList();

        if (itens.isEmpty()) {
            return;
        }
        // merge: um curriculo pode ter "Cursos" e "Formacao complementar",
        // que caem na mesma secao e devem se somar, nao se sobrescrever.
        resultado.merge(secao, new ArrayList<>(itens), (antigos, novos) -> {
            List<String> juntos = new ArrayList<>(antigos);
            novos.stream().filter(item -> !juntos.contains(item)).forEach(juntos::add);
            return juntos.stream().limit(MAX_ITENS_POR_SECAO).toList();
        });
    }

    private SectionType classificarCabecalho(String linha) {
        String normalizada = normalizarCabecalho(linha);
        if (normalizada.isEmpty() || normalizada.length() > MAX_CARACTERES_CABECALHO) {
            return null;
        }
        return CABECALHOS.stream()
                .filter(entrada -> entrada.getKey().equals(normalizada))
                .map(Map.Entry::getValue)
                .findFirst()
                .orElse(null);
    }

    private boolean ehCabecalhoNeutro(String linha) {
        String normalizada = normalizarCabecalho(linha);
        return !normalizada.isEmpty()
                && normalizada.length() <= MAX_CARACTERES_CABECALHO
                && CABECALHOS_NEUTROS.contains(normalizada);
    }

    /**
     * Cabecalho em PDF vem sujo: "EXPERIÊNCIA PROFISSIONAL:", "— Idiomas —",
     * "F O R M A Ç Ã O". Limpamos pontuacao das bordas antes de comparar.
     */
    private String normalizarCabecalho(String linha) {
        String limpa = linha.replaceAll("^[^\\p{L}]+", "")
                .replaceAll("[^\\p{L}]+$", "");
        return TextNormalizer.normalize(limpa);
    }
}
