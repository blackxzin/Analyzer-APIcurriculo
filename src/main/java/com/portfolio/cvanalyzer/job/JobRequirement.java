package com.portfolio.cvanalyzer.job;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * Um requisito da vaga.
 *
 * `mandatory` distingue obrigatorio de desejavel. Os dois contam para a
 * nota, mas com pesos diferentes — nao dominar um requisito obrigatorio
 * pesa mais do que nao dominar um diferencial.
 */
@Entity
@Table(name = "job_requirement")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class JobRequirement {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "job_id", nullable = false)
    private Job job;

    @Column(nullable = false, length = 80)
    private String requirement;

    @Column(nullable = false)
    private boolean mandatory;

    JobRequirement(Job job, String requirement, boolean mandatory) {
        this.job = job;
        this.requirement = requirement;
        this.mandatory = mandatory;
    }
}
