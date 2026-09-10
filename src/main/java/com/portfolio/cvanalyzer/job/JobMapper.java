package com.portfolio.cvanalyzer.job;

import com.portfolio.cvanalyzer.job.dto.JobResponse;
import com.portfolio.cvanalyzer.job.dto.JobSummaryResponse;
import org.springframework.stereotype.Component;

@Component
public class JobMapper {

    public JobResponse toResponse(Job job) {
        return new JobResponse(
                job.getId(),
                job.getTitle(),
                job.getCompany(),
                job.getSeniority(),
                job.getDescription(),
                job.mandatoryRequirements(),
                job.optionalRequirements(),
                job.getCreatedAt(),
                job.getUpdatedAt());
    }

    public JobSummaryResponse toSummary(Job job) {
        return new JobSummaryResponse(
                job.getId(),
                job.getTitle(),
                job.getCompany(),
                job.getSeniority(),
                job.getRequirements().size(),
                job.getCreatedAt());
    }
}
