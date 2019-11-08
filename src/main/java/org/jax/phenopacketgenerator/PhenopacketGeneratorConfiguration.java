package org.jax.phenopacketgenerator;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Objects;
import java.util.Properties;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Configuration
public class PhenopacketGeneratorConfiguration {
    public static final String CONFIG_FILE_BASENAME = "phenopacket_generator.properties";
    private static final Logger LOGGER = LoggerFactory.getLogger(PhenopacketGeneratorConfiguration.class);

    @Bean
    public URL sciGraphUrl(Environment environment) throws MalformedURLException {
        return new URL(Objects.requireNonNull(environment.getProperty("scigraph.url")));
    }

    @Bean
    public ExecutorService executorService() {
        return Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());
    }

    @Bean
    public OptionalResources optionalResources() {
        return new OptionalResources();
    }

    @Bean
    public String phenopacketsVersion(Environment env) {
        return env.getProperty(Main.PG_PHENOPACKET_VERSION_PROP_KEY);
    }

    @Bean(name = "ecoVersion")
    public String ecoVersion(Environment env) {
        return env.getProperty("eco.version");
    }


    /**
     * Properties meant to store user configuration within the app's directory
     *
     * @param configFilePath path where the properties file is supposed to be present (it's ok if the file itself doesn't exist).
     * @return {@link Properties} with user configuration
     */
    @Bean
    public Properties pgProperties(Path configFilePath) {
        Properties properties = new Properties();
        if (configFilePath.toFile().isFile()) {
            try (final InputStream is = Files.newInputStream(configFilePath)) {
                properties.load(is);
            } catch (IOException e) {
                LOGGER.warn("Error during reading `{}`", configFilePath, e);
            }
        }
        return properties;
    }

    @Bean
    public Path configFilePath(Path appHomeDir) {
        return appHomeDir.resolve(CONFIG_FILE_BASENAME);
    }

    /**
     * Get path to application home directory, where PhenopacketGenerator stores global settings and files. The path depends
     * on underlying operating system.
     * <p>
     * A hidden <code>.phenopacketGenerator</code>  directory will be created in user's home dir in Linux and OSX.
     * <p>
     * App home directory for Windows' or unknown OS users will be the <code>PhenopacketGenerator</code> directory in their
     * home dir.
     *
     * @return {@link Path} to application home directory
     */
    @Bean
    public Path appHomeDir() throws IOException {
        String osName = System.getProperty("os.name").toLowerCase();
        // we want to have one resource directory for release version and another for snapshots

        Path homeDir = Paths.get(System.getProperty("user.home"));
        Path appHomeDir;
        if (osName.contains("nix") || osName.contains("nux") || osName.contains("aix")) { // Unix
            appHomeDir = homeDir.resolve(".phenopacketGenerator");
        } else if (osName.contains("win")) { // Windows
            appHomeDir = homeDir.resolve("PhenopacketGenerator");
        } else if (osName.contains("mac")) { // OsX
            appHomeDir = homeDir.resolve(".phenopacketGenerator");
        } else { // unknown platform
            appHomeDir = homeDir.resolve("phenopacketGenerator");
        }

        final File appHomeDirFile = appHomeDir.toFile();
        if (!appHomeDirFile.exists()) {
            LOGGER.debug("App home directory does not exist at {}", appHomeDirFile.getAbsolutePath());
            if (!appHomeDirFile.getParentFile().exists() && !appHomeDirFile.getParentFile().mkdirs()) {
                LOGGER.warn("Unable to create parent directory for app home at {}",
                        appHomeDirFile.getParentFile().getAbsolutePath());
                throw new IOException("Unable to create parent directory for app home at " +
                        appHomeDirFile.getParentFile().getAbsolutePath());
            } else {
                if (!appHomeDirFile.mkdir()) {
                    LOGGER.warn("Unable to create app home directory at {}", appHomeDir);
                    throw new IOException("Unable to create app home directory at " + appHomeDir);
                } else {
                    LOGGER.info("Created app home directory at {}", appHomeDir);
                }
            }
        }
        return appHomeDir;
    }
}
