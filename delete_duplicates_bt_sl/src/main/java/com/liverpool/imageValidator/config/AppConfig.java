package com.liverpool.imageValidator.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import lombok.Data;

@Data
@Component
@ConfigurationProperties(prefix = "config")
public class AppConfig {

    private String atgCataSchema;
    private String atgCoreSchema;
    private String iuoSchema;
    private int threads;
    private int batchSize;
    private int numberBatches;

    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(AppConfig.class);

    @javax.annotation.PostConstruct
    public void logProperties() {
        log.info("===== Config Loaded =====");
        log.info("atgCataSchema: {}", atgCataSchema);
        log.info("atgCoreSchema: {}", atgCoreSchema);
        log.info("iuoSchema: {}", iuoSchema);
        log.info("threads: {}", threads);
        log.info("batchSize: {}", batchSize);
        log.info("numberBatches: {}", numberBatches);
        log.info("=========================");
    }

}
