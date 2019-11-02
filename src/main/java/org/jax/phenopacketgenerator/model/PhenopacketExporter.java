package org.jax.phenopacketgenerator.model;

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
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

public class PhenopacketExporter {

    private static final Logger LOGGER = LoggerFactory.getLogger(PhenopacketExporter.class);
    private static final JsonFormat.Printer PRINTER = JsonFormat.printer();


    // source https://bioportal.bioontology.org/ontologies/ECO/?p=classes&conceptid=http%3A%2F%2Fpurl.obolibrary.org%2Fobo%2FECO_0000033&jump_to_nav=true
    private static final OntologyClass TRACEABLE_AUTHOR_STATEMENT = ontologyClass("ECO:0000033", "author statement supported by traceable reference");


    private final List<PgOntologyClass> phenotypes;
    private final String vcfPath;



    public PhenopacketExporter(List<PgOntologyClass> phenotypes, String vcfPath) {
        this.phenotypes = phenotypes;
        this.vcfPath = vcfPath;
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
                        .setReference(ExternalReference.newBuilder()
                                .setId(String.format("ID:todo")) // TODO
                                .setDescription("Phenopacket created with PhenopacketGenerator")
                                .build())
                        .build())
                .build();
    }

    public void export(String id, String biocuratorId,
                       String phenopacketVersion,
                       File fileToWriteTo) {
        Phenopacket packet = encode(id,  biocuratorId, phenopacketVersion);
        try (BufferedWriter writer = Files.newBufferedWriter(fileToWriteTo.toPath())) {
            LOGGER.trace("Writing phenopacket to '{}'", fileToWriteTo.getAbsolutePath());
            String jsonString = PRINTER.print(packet);
            writer.write(jsonString);
        } catch (IOException e) {
            LOGGER.warn("Error occurred during phenopacket export", e);
            PopUps.showException("Error", "Error occurred during phenopacket export", e.getMessage(), e);
        }
    }

    private Phenopacket encode(String id, String biocuratorId,
                              String phenopacketVersion) {
        String probandId = id;

       // String metadata = data.getMetadata();
        Phenopacket.Builder builder = Phenopacket.newBuilder()
                .setId(probandId)
                // proband and the publication data
                .setSubject(Individual.newBuilder()
                        .setId(probandId)
                        //.setAgeAtCollection(Age.newBuilder().setAge(data.getFamilyInfo().getAge()).build())
                       // .setSex(hcaSexToPhenopacketSex(data.getFamilyInfo().getSex()))
                        //.setTaxonomy(HOMO_SAPIENS)
                        .build())
                // phenotype (HPO) terms
                .addAllPhenotypicFeatures(phenotypes.stream()
                        .map(hcaPhenotypeToPhenopacketPhenotype())
                        .collect(Collectors.toList()))
                // metadata - Biocurator ID, ontologies used
                .setMetaData(MetaData.newBuilder()
                       // .setCreatedBy(data.getSoftwareVersion())
                        .setSubmittedBy(biocuratorId)
                        .setPhenopacketSchemaVersion(phenopacketVersion)
                       // .addAllResources(RESOURCES)
                      //  .addExternalReferences(ExternalReference.newBuilder()
                       //         .setId(String.format("PMID:%s", data.getPublication().getPmid()))
                         //       .setDescription(data.getPublication().getTitle())
                       //         .build())
                        .build())
                ;
        if (this.vcfPath != null) {
            HtsFile hts = HtsFile.newBuilder()
                    .setHtsFormat(HtsFile.HtsFormat.VCF)
                    .setGenomeAssembly("TODO")
                    .setUri(this.vcfPath)
                    .build();
            builder.addHtsFiles(hts);
        }
        return builder.build();
    }

}
