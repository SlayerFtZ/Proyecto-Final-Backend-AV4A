package com.inventaria.api.summary.infrastructure;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SummaryIaJpaRepository extends JpaRepository<AiSummaryRecord, Integer> {

    List<AiSummaryRecord> findTop10ByUserIdOrderByFechaDesc(Integer userId);
}
