package com.aicode.repository;

import com.aicode.model.AnalysisRecord;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

/**
 * In-memory repository for analysis records.
 *
 * Stores up to 1000 records (LRU eviction).
 * To persist to a database, replace this with a JpaRepository
 * and add spring-data-jpa + a datasource to pom.xml.
 */
@Repository
public class AnalysisRecordRepository {

    private static final int MAX_RECORDS = 1000;

    private final Map<Long, AnalysisRecord> store = new ConcurrentHashMap<>();
    private final AtomicLong idSeq = new AtomicLong(1);

    public AnalysisRecord save(AnalysisRecord record) {
        if (record.getId() == null) record.setId(idSeq.getAndIncrement());
        if (store.size() >= MAX_RECORDS) evictOldest();
        store.put(record.getId(), record);
        return record;
    }

    public Optional<AnalysisRecord> findById(Long id) {
        return Optional.ofNullable(store.get(id));
    }

    public List<AnalysisRecord> findAll() {
        return new ArrayList<>(store.values());
    }

    public List<AnalysisRecord> findByFilePathOrderByAnalyzedAtDesc(String filePath) {
        return store.values().stream()
                .filter(r -> filePath.equals(r.getFilePath()))
                .sorted(Comparator.comparing(AnalysisRecord::getAnalyzedAt,
                        Comparator.nullsLast(Comparator.reverseOrder())))
                .collect(Collectors.toList());
    }

    public List<AnalysisRecord> findByRepositoryNameOrderByAnalyzedAtDesc(String repoName) {
        return store.values().stream()
                .filter(r -> repoName.equals(r.getRepositoryName()))
                .sorted(Comparator.comparing(AnalysisRecord::getAnalyzedAt,
                        Comparator.nullsLast(Comparator.reverseOrder())))
                .collect(Collectors.toList());
    }

    public List<AnalysisRecord> findByRepositoryNameAndAnalyzedAtBetween(
            String repoName, LocalDateTime start, LocalDateTime end) {
        return store.values().stream()
                .filter(r -> repoName.equals(r.getRepositoryName())
                          && r.getAnalyzedAt() != null
                          && !r.getAnalyzedAt().isBefore(start)
                          && !r.getAnalyzedAt().isAfter(end))
                .sorted(Comparator.comparing(AnalysisRecord::getAnalyzedAt).reversed())
                .collect(Collectors.toList());
    }

    public long count() { return store.size(); }

    private void evictOldest() {
        store.values().stream()
             .filter(r -> r.getAnalyzedAt() != null)
             .min(Comparator.comparing(AnalysisRecord::getAnalyzedAt))
             .ifPresent(r -> store.remove(r.getId()));
    }
}
