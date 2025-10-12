package com.nicopuhlmann.springai.ragtoolsandagent.dto;

import java.util.List;

public record OrchestratorResponse(
        String analysis,
        boolean needsFullReport,
        List<WorkerTask> tasks
){}
