package com.feex.mealplannersystem.service;

import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

@Service
public class AsyncJobTrackerService {

    private final Map<String, String> jobStatuses = new ConcurrentHashMap<>();
    private final Map<String, Object> jobResults = new ConcurrentHashMap<>();

    public <T> String startJob(Supplier<T> task) {
        String jobId = UUID.randomUUID().toString();
        jobStatuses.put(jobId, "PROCESSING");

        CompletableFuture.supplyAsync(task).whenComplete((result, ex) -> {
            if (ex != null) {
                failJob(jobId, ex.getMessage() != null ? ex.getMessage() : ex.toString());
            } else {
                completeJob(jobId, result);
            }
        });

        return jobId;
    }

    private void completeJob(String jobId, Object result) {
        jobResults.put(jobId, result);
        jobStatuses.put(jobId, "COMPLETED");
    }

    private void failJob(String jobId, String error) {
        jobResults.put(jobId, error);
        jobStatuses.put(jobId, "ERROR");
    }

    public Map<String, Object> getJobStatus(String jobId) {
        String status = jobStatuses.getOrDefault(jobId, "NOT_FOUND");
        
        if ("COMPLETED".equals(status)) {
            Object result = jobResults.remove(jobId);
            jobStatuses.remove(jobId);
            return Map.of("status", "COMPLETED", "result", result);
        } else if ("ERROR".equals(status)) {
            Object error = jobResults.remove(jobId);
            jobStatuses.remove(jobId);
            return Map.of("status", "ERROR", "error", error);
        }
        
        return Map.of("status", status);
    }
}
