package com.kestrel.common.core.config.dynamodb;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * Resolves production DynamoDB table names matching the AWS SAM template schema
 * with stage suffix (${baseName}_${stage}), or environment variable overrides.
 */
@Component
public class DynamoDbTableNameResolver {

    private final String stage;

    public DynamoDbTableNameResolver(@Value("${kestrel.stage:${STAGE:prod}}") String stage) {
        this.stage = (stage != null && !stage.isBlank()) ? stage : "prod";
    }

    /**
     * Resolves the full DynamoDB table name.
     * Checks if an explicit environment variable or property exists, else formats as baseName_stage.
     */
    public String resolve(String baseName, String envVarKey) {
        if (envVarKey != null && !envVarKey.isBlank()) {
            String envVal = System.getenv(envVarKey);
            if (envVal != null && !envVal.isBlank()) {
                return envVal;
            }
            String propVal = System.getProperty(envVarKey);
            if (propVal != null && !propVal.isBlank()) {
                return propVal;
            }
        }
        return baseName + "_" + stage;
    }

    public String resolve(String baseName) {
        return resolve(baseName, null);
    }

    public String getStage() {
        return stage;
    }
}
