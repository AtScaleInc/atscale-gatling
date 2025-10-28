package executors;

import com.atscale.java.utils.AwsSecretsManager;
import com.atscale.java.utils.PropertiesManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.Map;
import java.util.HashMap;

public class AdditionalPropertiesLoader {
    private static final Logger LOGGER = LoggerFactory.getLogger(AdditionalPropertiesLoader.class);

    public enum SecretsManagerType {
        AWS("AWS");

        private final String value;

        SecretsManagerType(String value) {
            this.value = value;
        }

        public String getValue() {
            return value;
        }
    }

    @SuppressWarnings("all")
    protected Map<String, String> fetchAdditionalProperties(SecretsManagerType type) {
        if(type == SecretsManagerType.AWS) {
            return fetchSecretsFromAws();
        }
        LOGGER.warn("Unsupported Secrets Manager type: {}. No additional properties loaded.", type.getValue());
        return new HashMap<>();
    }

    private Map<String, String> fetchSecretsFromAws() {
        String regionProperty = "aws.region";
        String secretsKeyProperty = "aws.secrets-key";
        if(PropertiesManager.hasProperty(regionProperty) && PropertiesManager.hasProperty(secretsKeyProperty)) {
            LOGGER.info("Loading additional properties from AWS Secrets Manager.");
            String region = PropertiesManager.getCustomProperty(regionProperty);
            String secretsKey = PropertiesManager.getCustomProperty(secretsKeyProperty);
            return new AwsSecretsManager().loadSecrets(region, secretsKey);
        }
        LOGGER.warn("AWS region or secrets-key property not found. Skipping loading additional properties from AWS Secrets Manager.");
        return new HashMap<>();
    }
}
