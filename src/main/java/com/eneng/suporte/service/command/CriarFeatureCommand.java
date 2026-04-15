package com.eneng.suporte.service.command;

public record CriarFeatureCommand(
        String title,
        String description,
        String businessValue,
        String targetVersion
) {
}
