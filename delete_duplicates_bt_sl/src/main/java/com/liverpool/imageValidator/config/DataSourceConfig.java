package com.liverpool.imageValidator.config;

import javax.sql.DataSource;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.jdbc.DataSourceBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.jdbc.core.JdbcTemplate;
import com.zaxxer.hikari.HikariDataSource;

@Configuration
public class DataSourceConfig {
    @Bean(name = "iuoDataSource")
    @ConfigurationProperties(prefix = "datasource.primary")
    public DataSource iuoDataSource() {
        return DataSourceBuilder.create()
                .type(HikariDataSource.class)
                .build();
    }

    @Bean(name = "atgDataSource")
    @ConfigurationProperties(prefix = "datasource.secondary")
    public DataSource atgDataSource() {
        return DataSourceBuilder.create()
                .type(HikariDataSource.class)
                .build();
    }

    @Bean(name = "iuoJdbcTemplate")
    public JdbcTemplate iuoJdbcTemplate(@Qualifier("iuoDataSource") DataSource ds) {
        return new JdbcTemplate(ds);
    }

    @Bean(name = "atgJdbcTemplate")
    public JdbcTemplate atgJdbcTemplate(@Qualifier("atgDataSource") DataSource ds) {
        return new JdbcTemplate(ds);
    }
}
