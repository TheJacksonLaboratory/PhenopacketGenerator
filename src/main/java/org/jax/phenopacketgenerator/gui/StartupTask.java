package org.jax.phenopacketgenerator.gui;

import javafx.concurrent.Task;
import org.jax.phenopacketgenerator.OptionalResources;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.Properties;

/**
 * Initialization of the GUI resources is being done here. Information from {@link Properties} parsed from
 * <code>hpo-case-annotator.properties</code> are being read and following resources are initialized:
 * <ul>
 * <li>Path to reference genome directory</li>
 * <li>Human phenotype ontology OBO file</li>
 * <li>Entrez gene file</li>
 * <li>Curated files directory</li>
 * <li>Biocurator ID</li>
 * <li>OMIM tab file</li>
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

    private final Properties properties;



    public StartupTask(OptionalResources optionalResources, Properties properties) {
        this.optionalResources = optionalResources;
        this.properties = properties;

    }


    /**
     * Read {@link Properties} and initialize app resources in the {@link OptionalResources}:
     *
     * <ul>
     * <li>HPO ontology</li>
     * <li>Entrez gene file</li>
     * <li>Curated files directory</li>
     * <li>Biocurator ID, and </li>
     * <li>OMIM file</li>
     * </ul>
     *
     * @return nothing
     * @throws Exception if an error occurs
     */
    @Override
    protected Void call() throws Exception {
        // Curated files directory
        // Biocurator ID
        optionalResources.setBiocuratorId(properties.getProperty(OptionalResources.BIOCURATOR_ID_PROPERTY, ""));
        // HPO
        String ontologyPath = properties.getProperty(OptionalResources.ONTOLOGY_PATH_PROPERTY);
        if (ontologyPath != null && new File(ontologyPath).isFile()) {
            File ontologyFile = new File(ontologyPath);
            LOGGER.info("Loading HPO from file '{}'", ontologyFile.getAbsolutePath());
            optionalResources.setOntology(OptionalResources.deserializeOntology(ontologyFile));
            optionalResources.setOntologyPath(ontologyFile);
        } else {
            LOGGER.info("Skipping loading HPO file since the location is unset");
        }

        LOGGER.info("Done");
        return null;
    }
}
