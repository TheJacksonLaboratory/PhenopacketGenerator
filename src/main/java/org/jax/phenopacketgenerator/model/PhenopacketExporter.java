package org.jax.phenopacketgenerator.model;

import com.google.protobuf.Timestamp;
import com.google.protobuf.util.JsonFormat;
import org.jax.phenopacketgenerator.gui.PopUps;
import org.phenopackets.schema.v1.Phenopacket;
import org.phenopackets.schema.v1.core.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

public class PhenopacketExporter {

    private static final Logger LOGGER = LoggerFactory.getLogger(PhenopacketExporter.class);
    private static final JsonFormat.Printer PRINTER = JsonFormat.printer();


    // source https://bioportal.bioontology.org/ontologies/ECO/?p=classes&conceptid=http%3A%2F%2Fpurl.obolibrary.org%2Fobo%2FECO_0000033&jump_to_nav=true
    private static final OntologyClass TRACEABLE_AUTHOR_STATEMENT = ontologyClass("ECO:0000033", "author statement supported by traceable reference");
    private final static String UNITIALIZED = "Uninitialized";

    private final List<PgOntologyClass> phenotypes;
    private final boolean hasVcf;
    private final String vcfPath;
    private final String genomeAssembly;
    private final String probandId;
    private final String phenopacketId;
    private final String biocuratorId;
    private final String phenopacketVersion;
    private final String ecoVersion;
    private final String hpoVersion;

    private final String sex;
    private final String age;




    public PhenopacketExporter(PgModel model) {
        this.phenotypes = model.getPhenotypes();
        this.hasVcf = model.hasVcf();
        if (model.hasVcf()) {
            this.vcfPath = model.getVcfPath();
            this.genomeAssembly = model.getGenomeAssembly();
        } else {
            this.vcfPath = null;
            this.genomeAssembly = null;
        }
        this.probandId = model.getProbandId();
        this.phenopacketId = model.getPhenopacketId();
        this.biocuratorId = model.getBiocurator();
        this.phenopacketVersion = model.getPhenopacketVersion();
        this.ecoVersion = model.getEcoVersion();
        this.hpoVersion = model.getHpoVersion();
        if (model.hasSexData()) {
            this.sex = model.getSex();
        } else {
            this.sex = UNITIALIZED;
        }
        if (model.hasAgeData()) {
            this.age = model.getIsoAge();
        } else {
            this.age = UNITIALIZED;
        }
    }

    /**
     * Make sure the file name ends with ".json"
     * @param f The file name returned by the user from the File chooser dialog
     * @return The corresponding path (with .json appended if necessary)
     */
    private Path getCanonicalPath(File f) {
        String abspath = f.getAbsolutePath();
        if (abspath.toLowerCase().endsWith("json")) {
            return f.toPath();
        }
        abspath = abspath + ".json";
        File f2 = new File(abspath);
        return f2.toPath();
    }


    /**
     * Create a phenopackets OntologyClass object
     * @param id, e.g., HP:0001234
     * @param label e.g., Abnormal X morphology
     * @return corresponding OntologyClass
     */
    private static OntologyClass ontologyClass(String id, String label) {
        return OntologyClass.newBuilder()
                .setId(id)
                .setLabel(label)
                .build();
    }


    private static Function<PgOntologyClass, PhenotypicFeature> hcaPhenotypeToPhenopacketPhenotype() {
        return oc -> PhenotypicFeature.newBuilder()
                .setType(ontologyClass(oc.getId(), oc.getLabel()))
                .setNegated(oc.getNotObserved())
                .addEvidence(Evidence.newBuilder()
                        .setEvidenceCode(TRACEABLE_AUTHOR_STATEMENT)
                      //  .setReference(ExternalReference.newBuilder()
                        //        .setId("ID:todo") // TODO
                          //      .setDescription("Phenopacket created with PhenopacketGenerator")
                           //     .build())
                        .build())
                .build();
    }

    public void export(File fileToWriteTo) {
        Phenopacket packet = encode();
        Path mypath = getCanonicalPath(fileToWriteTo);
        try (BufferedWriter writer = Files.newBufferedWriter(mypath)) {
            LOGGER.trace("Writing phenopacket to '{}'", mypath.toFile().getAbsolutePath());
            String jsonString = PRINTER.print(packet);
            writer.write(jsonString);
        } catch (IOException e) {
            LOGGER.warn("Error occurred during phenopacket export", e);
            PopUps.showException("Error", "Error occurred during phenopacket export", e.getMessage(), e);
        }
    }


    private Individual subject() {
        Individual.Builder builder = Individual.newBuilder() .setId(probandId);
        if (! age.equals(UNITIALIZED)) {
            builder.setAgeAtCollection(Age.newBuilder().setAge(age).build());
        }
        if (! sex.equals(UNITIALIZED)) {
            if (sex.equals("MALE")) {
                builder.setSex(Sex.MALE);
            } else if (sex.equals("FEMALE")) {
                builder.setSex(Sex.FEMALE);
            }
        }
        return builder.build();
    }


    private String getVcfUri() {
        if (vcfPath.startsWith("file")){
            return vcfPath;
        } else if (this.vcfPath.startsWith("//")) {
            return String.format("file:%s",this.vcfPath);
        } else if (this.vcfPath.startsWith("/")) {
            return String.format("file:/%s",this.vcfPath);
        } else {
            File f = new File(vcfPath);
            return String.format("file://%s",f.getAbsolutePath());
        }
    }


    private Phenopacket encode() {
        Phenopacket.Builder builder = Phenopacket.newBuilder()
                .setId(phenopacketId)
                // proband
                .setSubject(subject())
                // phenotype (HPO) terms
                .addAllPhenotypicFeatures(phenotypes.stream()
                        .map(hcaPhenotypeToPhenopacketPhenotype())
                        .collect(Collectors.toList()));
        if (this.vcfPath != null) {
            HtsFile hts = HtsFile.newBuilder()
                    .setHtsFormat(HtsFile.HtsFormat.VCF)
                    .setGenomeAssembly(this.genomeAssembly)
                    .setUri(getVcfUri())
                    .build();
            builder.addHtsFiles(hts);
        }
        builder.setMetaData(metadata());
        return builder.build();
    }


    private MetaData metadata() {

        long millis = System.currentTimeMillis();
        Timestamp timestamp = Timestamp.newBuilder().setSeconds(millis / 1000)
                .setNanos((int) ((millis % 1000) * 1000000)).build();

        MetaData metaData = MetaData.newBuilder()
                .addResources(Resource.newBuilder()
                        .setId("hp")
                        .setName("human phenotype ontology")
                        .setNamespacePrefix("HP")
                        .setIriPrefix("http://purl.obolibrary.org/obo/HP_")
                        .setUrl("http://purl.obolibrary.org/obo/hp.owl")
                        .setVersion(this.hpoVersion)
                        .build())
                .addResources(Resource.newBuilder()
                        .setId("eco")
                        .setName("Evidence and Conclusion Ontology")
                        .setNamespacePrefix("ECO")
                        .setIriPrefix("http://purl.obolibrary.org/obo/ECO_")
                        .setUrl("http://purl.obolibrary.org/obo/eco.owl")
                        .setVersion(this.ecoVersion)
                        .build())
                .setCreatedBy(this.biocuratorId)
                .setCreated(timestamp)
                .setPhenopacketSchemaVersion(this.phenopacketVersion)
                .build();
        return metaData;
    }

}
