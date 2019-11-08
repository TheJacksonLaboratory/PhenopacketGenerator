package org.jax.phenopacketgenerator;

import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import org.monarchinitiative.phenol.ontology.data.Ontology;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * This class is a POJO for resources that are necessary for full functionality of Phenopacket generator but might not
 * be available during startup.
 */
public final class OptionalResources {
    /**
     * Use this name to save HP.obo file on the local filesystem.
     */
    public static final String DEFAULT_HPO_FILE_NAME = "hp.obo";
    public static final String BIOCURATOR_ID_PROPERTY = "biocurator.id";
    public static final String ONTOLOGY_PATH_PROPERTY = "hp.obo.path";
    private static final Logger LOGGER = LoggerFactory.getLogger(OptionalResources.class);

    // default value does not harm here
    private final ObjectProperty<Ontology> ontology = new SimpleObjectProperty<>(this, "ontology");

    public Ontology getOntology() {
        return ontology.get();
    }


    public void setOntology(Ontology ontology) {
        this.ontology.set(ontology);
    }


    public ObjectProperty<Ontology> ontologyProperty() {
        return ontology;
    }


}
