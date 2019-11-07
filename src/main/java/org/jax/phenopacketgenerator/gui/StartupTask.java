package org.jax.phenopacketgenerator.gui;

import javafx.concurrent.Task;
import org.jax.phenopacketgenerator.OptionalResources;
import org.jax.phenopacketgenerator.Utils;
import org.monarchinitiative.phenol.ontology.data.Ontology;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Properties;

/**
 * Initialization of the GUI resources is being done here. Information from {@link Properties} parsed from
 * <code>hpo-case-annotator.properties</code> are being read and following resources are initialized:
 * <ul>
 * <li>Human phenotype ontology OBO file</li>
 * </ul>
 * <p>
 * Changes made by user are stored for the next run in {@link org.jax.phenopacketgenerator.Main#stop()} method.
 *
 * @author <a href="mailto:daniel.danis@jax.org">Daniel Danis</a>
 * @version 0.0.1
 * @since 0.0
 */
public final class StartupTask extends Task<Void> {

    private static final Logger LOGGER = LoggerFactory.getLogger(StartupTask.class);

    private final OptionalResources optionalResources;

    private final Properties pgProperties;


    public StartupTask(OptionalResources optionalResources, Properties pgProperties) {
        this.optionalResources = optionalResources;
        this.pgProperties = pgProperties;
    }

    /**
     * Read {@link Properties} and initialize app resources in the {@link OptionalResources}:
     *
     * <ul>
     * <li>HPO ontology</li>
     * </ul>
     *
     * @return nothing
     * @throws Exception if an error occurs
     */
    @Override
    protected Void call() throws Exception {
        // HPO
        String ontologyPath = pgProperties.getProperty(OptionalResources.ONTOLOGY_PATH_PROPERTY);
        if (ontologyPath != null) {
            final Path hpOboPath = Paths.get(ontologyPath);
            if (hpOboPath.toFile().isFile()) {
                LOGGER.info("Loading HPO from file '{}'", hpOboPath);
                try (final InputStream is = Files.newInputStream(hpOboPath)) {
                    final Ontology ontology = Utils.deserializeOntology(is);
                    optionalResources.setOntology(ontology);
                } catch (IOException e) {
                    LOGGER.warn("Error loading HPO file ", e);
                }
            }
        } else {
            LOGGER.info("Skipping loading HPO file since the location is unset");
        }
        return null;
    }
}
