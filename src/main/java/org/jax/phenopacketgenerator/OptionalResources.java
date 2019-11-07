package org.jax.phenopacketgenerator;

import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import org.monarchinitiative.phenol.io.OntologyLoader;
import org.monarchinitiative.phenol.ontology.data.Ontology;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.util.Properties;


/**
 * This class is a POJO holding paths to HRMD resources. Created by Daniel Danis on 7/16/17.
 */
public final class OptionalResources {
    private static final Logger LOGGER = LoggerFactory.getLogger(OptionalResources.class);
    /**
     * Use this name to save HP.obo file on the local filesystem.
     */
    public static final String DEFAULT_HPO_FILE_NAME = "hp.obo";


    public static final String BIOCURATOR_ID_PROPERTY = "biocurator.id";

    public static final String ONTOLOGY_PATH_PROPERTY = "hp.obo.path";



    // default value does not harm here
    private final StringProperty biocuratorId = new SimpleStringProperty(this, "biocuratorId", "");

    private final ObjectProperty<Ontology> ontology = new SimpleObjectProperty<>(this, "ontology");

    private File ontologyPath;


    public OptionalResources() {
    }

    public static Ontology deserializeOntology(File ontologyPath) throws IOException {
        // this might not be the best place for ontology deserialization, but it works for now
        try (InputStream is = Files.newInputStream(ontologyPath.toPath())) {
            return deserializeOntology(is);
        }
    }

    public static Ontology deserializeOntology(InputStream is) {
        return OntologyLoader.loadOntology(is);
    }

    public File getOntologyPath() {
        return ontologyPath;
    }


    public void setOntologyPath(File ontologyPath) {
        this.ontologyPath = ontologyPath;
        writeOntologyPathToProperties();
        if (ontology.isNull().get()) {
            try {
                initializeOntologyFromFile();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }


    private void writeOntologyPathToProperties() {
        Properties properties;
    }

    public String getBiocuratorId() {
        return biocuratorId.get();
    }


    public void setBiocuratorId(String biocuratorId) {
        this.biocuratorId.set(biocuratorId);
    }


    public StringProperty biocuratorIdProperty() {
        return biocuratorId;
    }


    public Ontology getOntology() {
        return ontology.get();
    }


    public void setOntology(Ontology ontology) {
        this.ontology.set(ontology);
    }


    public ObjectProperty<Ontology> ontologyProperty() {
        return ontology;
    }

    public void initializeOntologyFromFile() throws IOException {
        Ontology o = deserializeOntology(this.ontologyPath);
        setOntology(o);
    }


}
