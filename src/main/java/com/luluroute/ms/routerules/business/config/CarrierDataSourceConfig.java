package com.luluroute.ms.routerules.business.config;

import com.zaxxer.hikari.HikariDataSource;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.jdbc.DataSourceProperties;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.orm.jpa.EntityManagerFactoryBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.EnableTransactionManagement;

import java.util.Objects;

@Configuration
@EnableTransactionManagement
@EnableJpaRepositories(entityManagerFactoryRef = "carrierManagerFactory", transactionManagerRef = "carrierTransactionManager", basePackages = {
        "com.luluroute.ms.routerules.business.carrier.repository" })
public class CarrierDataSourceConfig {

    @Bean
    @ConfigurationProperties("spring.datasource.carrier")
    public DataSourceProperties carrierDataSourceProperties() {
        return new DataSourceProperties();
    }

    @Bean
    @ConfigurationProperties("spring.datasource.carrier.hikari")
    public HikariDataSource carrierDataSource() {
        return carrierDataSourceProperties().initializeDataSourceBuilder().type(HikariDataSource.class).build();
    }

    @Bean(name = "carrierManagerFactory")
    public LocalContainerEntityManagerFactoryBean carrierManagerFactory(EntityManagerFactoryBuilder builder) {
        return builder.dataSource(carrierDataSource()).packages("com.luluroute.ms.routerules.business.carrier.entity").build();
    }

    @Bean
    public PlatformTransactionManager carrierTransactionManager(
            final @Qualifier("carrierManagerFactory") LocalContainerEntityManagerFactoryBean carrierTransactionManager) {
        return new JpaTransactionManager(Objects.requireNonNull(carrierTransactionManager.getObject()));
    }
}
