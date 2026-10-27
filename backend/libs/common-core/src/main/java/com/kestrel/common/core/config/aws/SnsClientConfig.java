package com.kestrel.common.core.config.aws;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.sns.SnsClient;
import software.amazon.awssdk.services.sns.SnsClientBuilder;

import java.net.URI;

@Configuration
public class SnsClientConfig {

    private static final Logger log = LoggerFactory.getLogger(SnsClientConfig.class);

    @Value("${aws.region:${AWS_REGION:${AWS_DEFAULT_REGION:ap-south-1}}}")
    private String awsRegion;

    @Value("${aws.sns.endpoint:${AWS_ENDPOINT_URL:${AWS_SNS_ENDPOINT:}}}")
    private String endpointUrl;

    @Value("${aws.accessKeyId:${AWS_ACCESS_KEY_ID:}}")
    private String accessKeyId;

    @Value("${aws.secretAccessKey:${AWS_SECRET_ACCESS_KEY:}}")
    private String secretAccessKey;

    @Bean
    @ConditionalOnMissingBean
    public SnsClient snsClient() {
        log.info("Initializing AWS SDK v2 SnsClient for region: {}", awsRegion);
        SnsClientBuilder builder = SnsClient.builder()
                .region(Region.of(awsRegion));

        if (endpointUrl != null && !endpointUrl.isBlank()) {
            log.info("Configuring SNS endpoint override: {}", endpointUrl);
            builder.endpointOverride(URI.create(endpointUrl));

            String key = (accessKeyId != null && !accessKeyId.isBlank()) ? accessKeyId : "test";
            String secret = (secretAccessKey != null && !secretAccessKey.isBlank()) ? secretAccessKey : "test";
            builder.credentialsProvider(StaticCredentialsProvider.create(AwsBasicCredentials.create(key, secret)));
        } else {
            builder.credentialsProvider(DefaultCredentialsProvider.create());
        }

        return builder.build();
    }
}
