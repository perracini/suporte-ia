package com.eneng.suporte.service.command;

import com.eneng.suporte.domain.model.Severity;

public record CriarBugCommand(
        String title,
        String description,
        String stepsToReproduce,
        String affectedVersion,
        Severity severity
) {
}
