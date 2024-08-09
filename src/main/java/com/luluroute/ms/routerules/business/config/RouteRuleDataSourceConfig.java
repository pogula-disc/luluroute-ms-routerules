package com.luluroute.ms.routerules.business.config;

import com.zaxxer.hikari.HikariDataSource;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.jdbc.DataSourceProperties;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.orm.jpa.EntityManagerFactoryBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.EnableTransactionManagement;

import javax.sql.DataSource;
import java.util.Objects;

@Configuration
@EnableTransactionManagement
@EnableJpaRepositories(entityManagerFactoryRef = "routeRuleManagerFactory", transactionManagerRef = "routeRuleTransactionManager", basePackages = {
        "com.luluroute.ms.routerules.business.repository" })
public class RouteRuleDataSourceConfig {

    @Primary
    @Bean
    @ConfigurationProperties("spring.datasource.routerule")
    public DataSourceProperties routeRuleDataSourceProperties() {
        return new DataSourceProperties();
    }

    @Primary
    @Bean
    @ConfigurationProperties("spring.datasource.routerule.configuration")
    public DataSource routeRuleDataSource() {
        return routeRuleDataSourceProperties().initializeDataSourceBuilder().type(HikariDataSource.class).build();
    }

    @Primary
    @Bean(name = "routeRuleManagerFactory")
    public LocalContainerEntityManagerFactoryBean routeRuleManagerFactory(EntityManagerFactoryBuilder builder) {
        return builder.dataSource(routeRuleDataSource()).packages("com.luluroute.ms.routerules.business.entity").build();
    }

    @Primary
    @Bean
    public PlatformTransactionManager routeRuleTransactionManager(
            final @Qualifier("routeRuleManagerFactory") LocalContainerEntityManagerFactoryBean routeRuleTransactionManager) {
        return new JpaTransactionManager(Objects.requireNonNull(routeRuleTransactionManager.getObject()));
    }
}
