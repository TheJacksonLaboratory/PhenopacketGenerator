package org.jax.phenopacketgenerator;

import org.monarchinitiative.hpotextmining.gui.controller.HpoTextMining;
import org.monarchinitiative.phenol.io.OntologyLoader;
import org.monarchinitiative.phenol.ontology.data.Ontology;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;

import java.io.*;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Objects;
import java.util.Properties;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Configuration
public class PhenopacketGeneratorConfiguration {
    private static final Logger LOGGER = LoggerFactory.getLogger(PhenopacketGeneratorConfiguration.class);

    private static final String CONFIG_FILE_BASENAME = "phenopacketgen.config";

    @Bean
    public HpoTextMining hpoTextMining(URL sciGraphUrl, Ontology ontology) throws IOException {
        return HpoTextMining.builder()
                .withSciGraphUrl(sciGraphUrl)
                .withOntology(ontology)
                .build();
    }

    @Bean
    public URL sciGraphUrl(Environment environment) throws MalformedURLException {
        return new URL(Objects.requireNonNull(environment.getProperty("scigraph.url")));
    }

    @Bean
    public Ontology ontology(Path hpOboPath) {
        return OntologyLoader.loadOntology(hpOboPath.toFile());
    }

    @Bean
    public Path hpOboPath(Environment environment) {
        return Paths.get(Objects.requireNonNull(environment.getProperty("hp.obo.path")));
    }

    @Bean
    public Path configFilePath(File appHomeDir) {
        String abs = appHomeDir.getAbsolutePath();
        String path = String.format("%s%s%s", abs, File.separator, CONFIG_FILE_BASENAME );
        return  Paths.get(path);
    }

    @Bean
    public ExecutorService executorService() {
       return Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());
    }

    @Bean
    public OptionalResources optionalResources() {
        return new OptionalResources();
    }

    @Bean(name = "phenopacketsVersion")
    public String phenopacketsVersion(Environment env) {
        return env.getProperty("phenopacket.version");
    }

    @Bean(name = "ecoVersion")
    public String ecoVersion(Environment env) {
        return env.getProperty("eco.version");
    }



    @Bean
    public Properties properties() {
        Properties properties = new Properties();
        try {
            String configpath = String.format("%s%s%s",appHomeDir(), File.separator, CONFIG_FILE_BASENAME );
            BufferedReader reader = new BufferedReader(new FileReader(configpath));
            properties.load(reader);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return properties;
    }




    /**
     * Get path to application home directory, where HpoCaseAnnotator stores global settings and files. The path depends
     * on underlying operating system.
     * <p>
     * A hidden <code>.hpo-case-annotator</code>  directory will be created in user's home dir in Linux and OSX.
     * <p>
     * App home directory for Windows' or unknown OS users will be the <code>HpoCaseAnnotator</code> directory in their
     * home dir.
     *
     * @return {@link File} with path to application home directory
     */
    @Bean
    public File appHomeDir() throws IOException {
        String osName = System.getProperty("os.name").toLowerCase();
        // we want to have one resource directory for release version and another for snapshots


        File appHomeDir;
        if (osName.contains("nix") || osName.contains("nux") || osName.contains("aix")) { // Unix
            appHomeDir = new File(System.getProperty("user.home") + File.separator + ".phenopacketGenerator" );
        } else if (osName.contains("win")) { // Windows
            appHomeDir = new File(System.getProperty("user.home") + File.separator + "PhenopacketGenerator" );
        } else if (osName.contains("mac")) { // OsX
            appHomeDir = new File(System.getProperty("user.home") + File.separator + ".phenopacketGenerator");
        } else { // unknown platform
            appHomeDir = new File(System.getProperty("user.home") + File.separator + "phenopacketGenerator" );
        }

        if (!appHomeDir.exists()) {
            LOGGER.debug("App home directory does not exist at {}", appHomeDir.getAbsolutePath());
            if (!appHomeDir.getParentFile().exists() && !appHomeDir.getParentFile().mkdirs()) {
                LOGGER.warn("Unable to create parent directory for app home at {}",
                        appHomeDir.getParentFile().getAbsolutePath());
                throw new IOException("Unable to create parent directory for app home at " +
                        appHomeDir.getParentFile().getAbsolutePath());
            } else {
                if (!appHomeDir.mkdir()) {
                    LOGGER.warn("Unable to create app home directory at {}", appHomeDir.getAbsolutePath());
                    throw new IOException("Unable to create app home directory at " + appHomeDir.getAbsolutePath());
                } else {
                    LOGGER.info("Created app home directory at {}", appHomeDir.getAbsolutePath());
                }
            }
        }
        String configpath = String.format("%s%s%s",appHomeDir, File.separator, "phenopacketgen.config" );
        File config = new File(configpath);
        if (! config.exists()) {
            try { // initialize the file
                BufferedWriter writer = new BufferedWriter(new FileWriter(config));
                writer.write("#Phenopacket Generator Configuration\n");
                writer.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return appHomeDir;
    }
}
