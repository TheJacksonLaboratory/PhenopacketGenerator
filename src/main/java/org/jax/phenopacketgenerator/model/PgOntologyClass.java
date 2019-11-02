package org.jax.phenopacketgenerator.model;

/**
 * POJO based on the protobuf model of HpoCaseAnnotator
 * <pre>
 *
 // A class (aka term, concept) in an ontology (borrowed from phenopackets)
 message OntologyClass {
 // a CURIE-style identifier e.g. HP:0100024, MP:0001284, UBERON:0001690.
 // This is the primary key for the ontology class
 string id = 1;
 // class label, aka name. E.g. "Abnormality of cardiovascular system"
 string label = 2;
 // the phenotype is either observed (false, default) or not (true)
 bool not_observed = 3;
 }

 * </pre>
 */
public class PgOntologyClass {
    private final String id;
    private final String label;
    private final boolean not_observed;

    public String getId() {
        return id;
    }

    public String getLabel() {
        return label;
    }

    public boolean getNotObserved() {
        return not_observed;
    }

    private PgOntologyClass(String id, String label, boolean not_obs) {
        this.id = id;
        this.label = label;
        this.not_observed = not_obs;
    }

    public static PgOntologyClass.Builder newBuilder() {
        return new PgOntologyClass.Builder();
    }



    public static class Builder {
        private String id;
        private String label;
        private boolean not_observed = false;

        public Builder setId(String id) {
            this.id = id;
            return this;
        }

        public Builder setLabel(String label) {
            this.label = label;
            return this;
        }

        public Builder setNotObserved(boolean not) {
            not_observed = not;
            return this;
        }

        public PgOntologyClass build() {
            return new PgOntologyClass(id,label,not_observed);
        }

    }
}
