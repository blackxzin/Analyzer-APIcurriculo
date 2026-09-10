package com.portfolio.cvanalyzer.resume;

import com.portfolio.cvanalyzer.resume.dto.ResumeResponse;
import com.portfolio.cvanalyzer.resume.dto.ResumeSummaryResponse;
import org.springframework.stereotype.Component;

/**
 * Converte Entity -> DTO.
 *
 * Ficar num componente proprio evita que essa conversao se espalhe pelo
 * service e pelo controller (e depois divirja entre eles). Um lugar so
 * decide o que vira resposta.
 */
@Component
public class ResumeMapper {

    public ResumeResponse toResponse(Resume resume) {
        return new ResumeResponse(
                resume.getId(),
                resume.getCandidateName(),
                resume.getEmail(),
                resume.getPhone(),
                resume.getFileName(),
                resume.getFileSizeBytes(),
                resume.getPageCount(),
                resume.getTechnologies(),
                resume.itemsOf(SectionType.EDUCATION),
                resume.itemsOf(SectionType.EXPERIENCE),
                resume.itemsOf(SectionType.COURSE),
                resume.itemsOf(SectionType.CERTIFICATION),
                resume.itemsOf(SectionType.LANGUAGE),
                resume.getCreatedAt());
    }

    public ResumeSummaryResponse toSummary(Resume resume) {
        return new ResumeSummaryResponse(
                resume.getId(),
                resume.getCandidateName(),
                resume.getEmail(),
                resume.getFileName(),
                resume.getPageCount(),
                resume.getCreatedAt());
    }
}
