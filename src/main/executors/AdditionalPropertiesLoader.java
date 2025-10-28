package executors;

import com.atscale.java.utils.AwsSecretsManager;
import com.atscale.java.utils.PropertiesManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.Map;
import java.util.HashMap;

public class AdditionalPropertiesLoader {
    private static final Logger LOGGER = LoggerFactory.getLogger(AdditionalPropertiesLoader.class);

    protected Map<String, String> fetchAdditionalProperties() {
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
