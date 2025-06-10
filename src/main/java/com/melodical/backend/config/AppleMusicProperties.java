package com.melodical.backend.config;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix="apple.music")
@Getter @Setter
public class AppleMusicProperties {
    private String teamId;
    private String keyId;
    private String privateKeyLocation;
    private long tokenValiditySeconds;
    private String storefrontId;
}