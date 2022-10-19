package me.vjgallego.pdfgenerator.configuration;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "templates")
public record ApplicationPaths(String path) {}

