package utils;

import com.atscale.java.utils.PropertiesManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Properties;

/**
 * Utility class for loading properties from configuration files.
 * Supports layered property lookup: system properties → profile properties → PropertiesManager.
 * Properties files are loaded from the filesystem first, falling back to classpath if not found.
 */
public class ProfilePropertyLoader {
    private static final Logger LOGGER = LoggerFactory.getLogger(ProfilePropertyLoader.class);

    private final Properties profileProperties;
    private final String propertiesFileName;

    /**
     * Creates a ProfilePropertyLoader that loads properties from the specified file.
     * The file is first searched on the filesystem, then on the classpath.
     *
     * @param propertiesFileName the properties file name to load
     */
    public ProfilePropertyLoader(String propertiesFileName) {
        this.propertiesFileName = propertiesFileName;
        this.profileProperties = loadProperties(propertiesFileName);
        LOGGER.info("Loaded properties from: {}", propertiesFileName);
    }

    public HashMap<String, String> getProfileProperties() {
        HashMap<String, String> map = new HashMap<>();
        for (String key : profileProperties.stringPropertyNames()) {
            map.put(key, profileProperties.getProperty(key));
        }
        return map;
    }

    /**
     * Gets the properties file name.
     */
    public String getPropertiesFileName() {
        return propertiesFileName;
    }

    /**
     * Gets a property value with layered lookup:
     * 1. System properties (-D flags)
     * 2. Profile properties file
     * 3. Main properties file (systems.properties via PropertiesManager)
     *
     * @param key the property key
     * @return the property value
     * @throws RuntimeException if the property is not found in any source
     */
    public String getProperty(String key) {
        // System property takes precedence (for -D flags)
        String value = System.getProperty(key);
        if (value != null && !value.trim().isEmpty()) {
            return value.trim();
        }
        // Check profile properties file
        if (profileProperties != null && profileProperties.containsKey(key)) {
            return profileProperties.getProperty(key).trim();
        }
        // Fall back to main properties file
        if (PropertiesManager.hasProperty(key)) {
            return PropertiesManager.getCustomProperty(key);
        }
        throw new RuntimeException("Required property not found: " + key);
    }

    /**
     * Loads properties from the specified file.
     * First checks the filesystem, then falls back to the classpath.
     */
    private Properties loadProperties(String propsFile) {
        Properties props = new Properties();

        // First, try to load from filesystem
        File file = new File(propsFile);
        if (file.exists() && file.isFile()) {
            try (InputStream input = new FileInputStream(file)) {
                props.load(input);
                LOGGER.debug("Loaded properties from filesystem: {}", file.getAbsolutePath());
                return props;
            } catch (IOException e) {
                throw new RuntimeException("Failed to load properties file from filesystem: " + propsFile, e);
            }
        }

        // Fall back to classpath
        try (InputStream input = getClass().getClassLoader().getResourceAsStream(propsFile)) {
            if (input == null) {
                throw new RuntimeException("Properties file not found on filesystem or classpath: " + propsFile);
            }
            props.load(input);
            LOGGER.debug("Loaded properties from classpath: {}", propsFile);
        } catch (IOException e) {
            throw new RuntimeException("Failed to load properties file from classpath: " + propsFile, e);
        }
        return props;
    }
}
