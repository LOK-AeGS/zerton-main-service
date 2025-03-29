package com.zeroton.zeroSpring.config;

import com.mongodb.Block;
import com.mongodb.ConnectionString;
import com.mongodb.MongoClientSettings;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.data.mongodb.MongoDatabaseFactory;
import org.springframework.data.mongodb.config.AbstractMongoClientConfiguration;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.convert.*;
import org.springframework.data.mongodb.core.mapping.MongoMappingContext;

import java.util.Objects;
import java.util.concurrent.TimeUnit;

@Configuration
public class MongoConfig extends AbstractMongoClientConfiguration {
    @Autowired
    private Environment env;

    @NotNull
    public @Bean MongoClient mongoClient() {
        return com.mongodb.client.MongoClients.create(Objects.requireNonNull(env.getProperty("spring.datasource.mongodb.uri")));
    }
    @NotNull
    @Override
    protected String getDatabaseName() {
        return Objects.requireNonNull(env.getProperty("spring.datasource.mongodb.database"));
    }
    @NotNull
    @Bean
    public MappingMongoConverter mappingMongoConverter(@NotNull MongoDatabaseFactory factory, @NotNull MongoCustomConversions conversions, @NotNull MongoMappingContext context) {
        DbRefResolver dbRefResolver = new DefaultDbRefResolver(factory);
        MappingMongoConverter converter = new MappingMongoConverter(dbRefResolver, context);
        converter.setCustomConversions(conversions);
        converter.setTypeMapper(new DefaultMongoTypeMapper(null)); // _class 필드 비활성화
        return converter;
    }
    @NotNull
    @Bean
    public MongoTemplate mongoTemplate(@NotNull MongoDatabaseFactory factory, @NotNull MappingMongoConverter converter) {
        return new MongoTemplate(factory, converter);
    }

}
