package com.kestrel.common.core.config.dynamodb;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.DynamoDbClientBuilder;

import java.net.URI;

/**
 * AWS SDK v2 DynamoDB and DynamoDB Enhanced Client configuration.
 * Automatically adapts between real AWS cloud credentials and local development / LocalStack endpoints.
 */
@Configuration
public class DynamoDbClientConfig {

    private static final Logger log = LoggerFactory.getLogger(DynamoDbClientConfig.class);

    @Value("${aws.region:${AWS_REGION:${AWS_DEFAULT_REGION:ap-south-1}}}")
    private String awsRegion;

    @Value("${aws.dynamodb.endpoint:${AWS_ENDPOINT_URL:${AWS_DYNAMODB_ENDPOINT:}}}")
    private String endpointUrl;

    @Value("${aws.accessKeyId:${AWS_ACCESS_KEY_ID:}}")
    private String accessKeyId;

    @Value("${aws.secretAccessKey:${AWS_SECRET_ACCESS_KEY:}}")
    private String secretAccessKey;

    @Bean
    @ConditionalOnMissingBean
    public DynamoDbClient dynamoDbClient() {
        log.info("Initializing AWS SDK v2 DynamoDbClient for region: {}", awsRegion);
        DynamoDbClientBuilder builder = DynamoDbClient.builder()
                .region(Region.of(awsRegion));

        if (endpointUrl != null && !endpointUrl.isBlank()) {
            log.info("Configuring DynamoDb endpoint override: {}", endpointUrl);
            builder.endpointOverride(URI.create(endpointUrl));

            String key = (accessKeyId != null && !accessKeyId.isBlank()) ? accessKeyId : "test";
            String secret = (secretAccessKey != null && !secretAccessKey.isBlank()) ? secretAccessKey : "test";
            builder.credentialsProvider(StaticCredentialsProvider.create(AwsBasicCredentials.create(key, secret)));
        } else {
            builder.credentialsProvider(DefaultCredentialsProvider.create());
        }

        return builder.build();
    }

    @Bean
    @ConditionalOnMissingBean
    public DynamoDbEnhancedClient dynamoDbEnhancedClient(DynamoDbClient dynamoDbClient) {
        log.info("Initializing DynamoDbEnhancedClient");
        return DynamoDbEnhancedClient.builder()
                .dynamoDbClient(dynamoDbClient)
                .build();
    }
}
