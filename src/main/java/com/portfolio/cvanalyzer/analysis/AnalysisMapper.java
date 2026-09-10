package com.portfolio.cvanalyzer.analysis;

import com.portfolio.cvanalyzer.analysis.dto.AnalysisResponse;
import com.portfolio.cvanalyzer.analysis.dto.AnalysisSummaryResponse;
import org.springframework.stereotype.Component;

@Component
public class AnalysisMapper {

    public AnalysisResponse toResponse(Analysis analysis) {
        return new AnalysisResponse(
                analysis.getId(),
                analysis.getResume().getId(),
                analysis.getJob().getId(),
                analysis.getResume().getCandidateName(),
                analysis.getJob().getTitle(),
                analysis.getCompatibility(),
                analysis.getMatchedRequirements(),
                analysis.getMissingRequirements(),
                analysis.getRecommendations(),
                analysis.getTotalRequirements(),
                analysis.getCreatedAt());
    }

    public AnalysisSummaryResponse toSummary(Analysis analysis) {
        return new AnalysisSummaryResponse(
                analysis.getId(),
                analysis.getResume().getId(),
                analysis.getResume().getCandidateName(),
                analysis.getJob().getId(),
                analysis.getJob().getTitle(),
                analysis.getJob().getCompany(),
                analysis.getCompatibility(),
                analysis.getMatchedCount(),
                analysis.getTotalRequirements(),
                analysis.getCreatedAt());
    }
}
