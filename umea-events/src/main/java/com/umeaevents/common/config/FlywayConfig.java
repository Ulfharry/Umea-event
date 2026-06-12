package com.umeaevents.common.config;

import org.flywaydb.core.Flyway;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.sql.DataSource;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@Configuration
public class FlywayConfig {

    @Bean
    public Flyway flyway(DataSource dataSource) {
        Flyway flyway = Flyway.configure()
                .dataSource(dataSource)
                .locations("classpath:db/migration")
                .encoding(java.nio.charset.StandardCharsets.UTF_8)
                .load();
        flyway.migrate();
        return flyway;
    }

    // Lägger till "flyway" som explicit beroende på entityManagerFactory,
    // vilket garanterar att migrationer körs innan Hibernate validerar schemat.
    // BeanFactoryPostProcessor körs efter att alla bean-definitioner registrerats.
    @Bean
    public static BeanFactoryPostProcessor flywayJpaDependency() {
        return (ConfigurableListableBeanFactory beanFactory) -> {
            if (beanFactory.containsBeanDefinition("entityManagerFactory")) {
                BeanDefinition bd = beanFactory.getBeanDefinition("entityManagerFactory");
                String[] existing = bd.getDependsOn();
                List<String> deps = existing != null ? new ArrayList<>(Arrays.asList(existing)) : new ArrayList<>();
                deps.add("flyway");
                bd.setDependsOn(deps.toArray(new String[0]));
            }
        };
    }
}
