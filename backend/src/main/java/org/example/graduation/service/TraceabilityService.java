package org.example.graduation.service;

import java.util.Map;

public interface TraceabilityService {

    Map<String, Object> traceMaterialBatch(String batchNo);

    Map<String, Object> traceFinishedBatch(String batchNo);
}