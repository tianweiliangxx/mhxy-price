package com.mhxy.price;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "mimo")
public record MimoProperties(String apiKey, String baseUrl, String model) {}
