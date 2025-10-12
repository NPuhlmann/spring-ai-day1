package com.nicopuhlmann.springai.ragtoolsandagent.dto;

import java.util.List;

public record ReportPlan (
        String reportTitle,
        List<Kapitel> kapitel
)
{}
