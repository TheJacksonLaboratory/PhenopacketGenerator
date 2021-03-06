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
        /*
        This is the place where we deserialize HPO ontology if we know path to the OBO file.
        We need to make sure to set ontology property of `optionalResources` to null if loading fails.
        This way we ensure that GUI elements dependent on ontology presence (labels, buttons) stay disabled
        and that the user will be notified about the fact that the ontology is missing.
         */
        String ontologyPath = pgProperties.getProperty(OptionalResources.ONTOLOGY_PATH_PROPERTY);
        if (ontologyPath != null) {
            final Path hpOboPath = Paths.get(ontologyPath);
            if (hpOboPath.toFile().isFile()) {
                String msg = String.format("Loading HPO from file '%s'", hpOboPath);
                updateMessage(msg);
                LOGGER.info(msg);
                try (final InputStream is = Files.newInputStream(hpOboPath)) {
                    final Ontology ontology = Utils.deserializeOntology(is);
                    optionalResources.setOntology(ontology);
                    updateMessage("Ontology loaded");
                } catch (IOException e) {
                    updateMessage(String.format("Error loading HPO file : %s", e.getMessage()));
                    LOGGER.warn("Error loading HPO file: ", e);
                    optionalResources.setOntology(null);
                }
            } else {
                optionalResources.setOntology(null);
            }
        } else {
            String msg = "Need to set path to hp.obo file (See edit menu)";
            updateMessage(msg);
            LOGGER.info(msg);
            optionalResources.setOntology(null);
        }
        return null;
    }
}
