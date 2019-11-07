package org.jax.phenopacketgenerator;

import org.monarchinitiative.phenol.io.OntologyLoader;
import org.monarchinitiative.phenol.ontology.data.Ontology;

import java.io.InputStream;

public class Utils {

    private Utils() {
        // private no-op
    }

    public static Ontology deserializeOntology(InputStream is) {
        return OntologyLoader.loadOntology(is);
    }
}
