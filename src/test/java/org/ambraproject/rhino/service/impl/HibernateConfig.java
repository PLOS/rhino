package org.ambraproject.rhino.service.impl;

import org.hibernate.SessionFactory;
import org.hibernate.dialect.HSQLDialect;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.core.io.Resource;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseBuilder;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseType;
import org.springframework.orm.hibernate3.HibernateTransactionManager;
import org.springframework.orm.hibernate3.LocalSessionFactoryBean;

import javax.sql.DataSource;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

/**
 * Created by jkrzemien on 7/23/14.
 */

public class HibernateConfig {

    static {
        System.setProperty("SYSTEM_OBJECT_ID_PREFIX", "info:doi/0.0/");
    }

    @Autowired
    private ApplicationContext context;

    private void setAmbraMappings(LocalSessionFactoryBean sessionFactoryBean) throws IOException {
        final String mappingLocation = "classpath:org/ambraproject/models/*.hbm.xml";
        Resource[] mappingLocations = context.getResources(mappingLocation);
        if (mappingLocations.length == 0) {
            throw new IllegalStateException("Config error: No Ambra data models found");
        }
        List<Resource> finalResources = new ArrayList<>(mappingLocations.length);
        for (Resource resource : mappingLocations) {
            if (!"Article.hbm.xml".equals(resource.getFilename())) {
                finalResources.add(resource);
            }
        }
        finalResources.add(context.getResource("classpath:ambra/configuration/Article.hbm.xml"));
        sessionFactoryBean.setMappingLocations(finalResources.toArray(new Resource[finalResources.size()]));
    }

    @Bean
    public DataSource dataSource() {
        return new EmbeddedDatabaseBuilder()
                .setType(EmbeddedDatabaseType.HSQL)
                .setName("int-tests")
                        //.addScript("classpath:schema.sql")
                        //.addScript("classpath:test-data.sql")
                .build();
    }

    @Bean
    public HibernateTransactionManager transactionManager(SessionFactory sessionFactory) {
        HibernateTransactionManager manager = new HibernateTransactionManager();
        manager.setSessionFactory(sessionFactory);
        return manager;
    }

    @Bean
    public LocalSessionFactoryBean sessionFactory(DataSource hibernateDataSource) throws IOException {
        LocalSessionFactoryBean localSessionFactory = new LocalSessionFactoryBean();
        localSessionFactory.setDataSource(hibernateDataSource);
        setAmbraMappings(localSessionFactory);

        Properties hibernateProperties = new Properties();
        hibernateProperties.setProperty("hibernate.dialect", HSQLDialect.class.getName());
        hibernateProperties.setProperty("hibernate.show_sql", Boolean.TRUE.toString());
        hibernateProperties.setProperty("hibernate.format_sql", Boolean.TRUE.toString());
        hibernateProperties.setProperty("hibernate.hbm2ddl.auto", "create");
        localSessionFactory.setHibernateProperties(hibernateProperties);

        return localSessionFactory;
    }
}
