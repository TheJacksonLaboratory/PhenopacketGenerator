package org.jax.phenopacketgenerator.gui;


import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.StackPane;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import org.jax.phenopacketgenerator.OptionalResources;
import org.jax.phenopacketgenerator.model.PgOntologyClass;
import org.jax.phenopacketgenerator.model.PhenopacketExporter;
import org.monarchinitiative.hpotextmining.gui.controller.HpoTextMining;
import org.monarchinitiative.hpotextmining.gui.controller.Main;
import org.monarchinitiative.phenol.ontology.data.Ontology;
import org.monarchinitiative.phenol.ontology.data.Term;
import org.monarchinitiative.phenol.ontology.data.TermId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.function.Function;
import java.util.stream.Collectors;

@Component
public class MainController {

    private static final Logger LOGGER = LoggerFactory.getLogger(MainController.class);

    private final HpoTextMining mining;

    private final OptionalResources optionalResources;

    private final Properties properties;

    private final ExecutorService executorService;

    private String vcfFileAbsolutePath = null;

    private final URL scigraphMiningUrl;

    private final Ontology ontology;

    private final ObservableList<PgOntologyClass> phenotypes = FXCollections.observableList(new ArrayList<>());
    @Autowired
    private String phenopacketsVersion = "todo";

    @FXML
    public StackPane miningbox;

    @FXML
    private Button hpoTextMiningButton;

    @FXML
    private Label phenotypeSummaryLabel;

    @FXML
    private Label vcfFileLabel;

    @Autowired
    public MainController(HpoTextMining mining, OptionalResources optionalResources,
                          Properties properties, ExecutorService executorService,
                          Ontology ontology, URL scigraphMiningUrl ) {
        this.mining = mining;
        this.optionalResources = optionalResources;
        this.properties = properties;
        this.executorService = executorService;
        this.scigraphMiningUrl = scigraphMiningUrl;
        this.ontology = ontology;
    }

    public void initialize() {
        //miningbox.getChildren().add(mining.getMainParent());
        StartupTask task = new StartupTask(optionalResources, properties);
        executorService.submit(task);
        // generate phenotype summary text
        phenotypes.addListener(makePhenotypeSummaryLabel(phenotypes, phenotypeSummaryLabel));

    }


    @FXML
    void exportPhenopacket() {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Export as Phenopacket (JSON) file");
        File f = chooser.showSaveDialog(null);
        if (f==null) {
            PopUps.showInfoMessage("Could not retrieve path to save phenopacket","Warning");
            return;
        }
        PhenopacketExporter exporter = new PhenopacketExporter(this.phenotypes,this.vcfFileAbsolutePath);
        System.out.println("[TODO] Save complete Phenopacket to this file: " + f.getAbsolutePath());
        String biocurator = this.optionalResources.getBiocuratorId();
        String id = "TODO";
        exporter.export(id, biocurator, phenopacketsVersion, f);
    }


    /**
     * @return change listener for Phenotypes observable list that updates the {@code phenotypeSummaryLabel} with observed/excluded
     * phenotype term count
     */
    private static ListChangeListener<PgOntologyClass> makePhenotypeSummaryLabel(List<PgOntologyClass> phenotypes, Label phenotypeSummaryLabel) {
        return c -> {
            int nObserved = 0, nExcluded = 0;
            for (PgOntologyClass phenotype : phenotypes) {
                if (phenotype.getNotObserved()) {
                    nExcluded++;
                } else {
                    nObserved++;
                }
            }
            String observedSummary = (nObserved == 1) ? "1 observed term" : String.format("%d observed terms", nObserved);
            String excludedSummary = (nExcluded == 1) ? "1 excluded term" : String.format("%d excluded terms", nExcluded);

            phenotypeSummaryLabel.setText(String.join("\n", observedSummary, excludedSummary));
        };
    }


    @FXML
    private void hpoTextMiningButtonAction() {
        String conversationTitle = "HPO text mining analysis";
        if (ontology == null) {
            PopUps.showInfoMessage("Need to set location to hp.obo ontology file first!","Error");
            return;
        }
        try {
            HpoTextMining hpoTextMining = HpoTextMining.builder()
                    .withSciGraphUrl(scigraphMiningUrl)
                    .withOntology(ontology)
                    .withExecutorService(executorService)
                    .withPhenotypeTerms(phenotypes.stream()
                            .map(ontologyClassToPhenotypeTerm(optionalResources.getOntology()))
                            .collect(Collectors.toSet()))
                    .build();

            Stage stage = new Stage();
            stage.initOwner(hpoTextMiningButton.getParent().getScene().getWindow());
            stage.setTitle(conversationTitle);
            stage.setScene(new Scene(hpoTextMining.getMainParent()));
            stage.showAndWait();

            phenotypes.clear();
            phenotypes.addAll(hpoTextMining.getApprovedTerms().stream()
                    .map(phenotypeTermToOntologyClass())
                    .collect(Collectors.toSet()));
        } catch (IOException e) {
            LOGGER.warn("Error occurred during text mining", e);
            PopUps.showException(conversationTitle, "Error occurred during text mining", e.getMessage(), e);
        } catch (StringIndexOutOfBoundsException sioe) {
            LOGGER.warn("Error occurred during text mining", sioe);
            PopUps.showException(conversationTitle, "Error: StringIndexOutOfBoundsException", sioe.getMessage(), sioe);
        }
    }

    /**
     * @return {@link Function} for mapping {@link org.monarchinitiative.hpotextmining.gui.controller.Main.PhenotypeTerm} to
     * {@link PgOntologyClass}
     */
    private static Function<Main.PhenotypeTerm, PgOntologyClass> phenotypeTermToOntologyClass() {
        return pt -> PgOntologyClass.newBuilder()
                .setId(pt.getTerm().getId().getValue())
                .setLabel(pt.getTerm().getName())
                .setNotObserved(!pt.isPresent())
                .build();
    }

    /**
     * Runs after user clicks Settings/Set biocurator MenuItem and asks user to provide the ID.
     */
    @FXML
    void setBiocuratorMenuItemClicked(ActionEvent event) {
        String biocurator = PopUps.getStringFromUser("Biocurator ID",
                "e.g. HPO:rrabbit", "Enter your biocurator ID:");
        if (biocurator != null) {
            optionalResources.setBiocuratorId(biocurator);
            PopUps.showInfoMessage(String.format("Biocurator ID set to \n\"%s\"",
                    biocurator), "Success");
        } else {
            PopUps.showInfoMessage("Biocurator ID not set.",
                    "Information");
        }
        event.consume();
    }

    @FXML
    void setPathToVcfFile() {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Set path to VCF file");
        FileChooser.ExtensionFilter extFilterVCF = new FileChooser.ExtensionFilter("Variant Call Format file file (*.vcf)", "*.vcf");
        FileChooser.ExtensionFilter extFilterVCFGZ = new FileChooser.ExtensionFilter("Compressed Variant Call Format file file (*.vcf.gz)", "*.vcf.gz");

        chooser.getExtensionFilters().addAll(extFilterVCF,extFilterVCFGZ);
        File f = chooser.showOpenDialog(null);
        if (f == null) {
            LOGGER.error("Unable to obtain path to VCF file");
            PopUps.showInfoMessage("Unable to obtain path to VCF file", "Error");
            return;
        }
        this.vcfFileAbsolutePath = f.getAbsolutePath();
        String displayString;
        if (vcfFileAbsolutePath.length() < 100) {
            displayString = vcfFileAbsolutePath;
        } else {
            int L = vcfFileAbsolutePath.length();
            String firstpart = vcfFileAbsolutePath.substring(0,40);
            String lastPart = vcfFileAbsolutePath.substring(L-40);
            displayString = String.format("%s.......%s",firstpart,lastPart);
        }
        this.vcfFileLabel.setText(displayString);
    }

    @FXML
    void exitMenuItemAction() {
        Platform.exit();
    }

    /**
     * @param ontology {@link Ontology} needed for mapping
     * @return {@link Function} mapping {@link PgOntologyClass} to {@link Main.PhenotypeTerm} instance
     */
    private static Function<PgOntologyClass, Main.PhenotypeTerm> ontologyClassToPhenotypeTerm(Ontology ontology) {
        return oc -> {
            TermId id = TermId.of(oc.getId());
            Term term = ontology.getTermMap().get(id);
            return new Main.PhenotypeTerm(term, !oc.getNotObserved());
        };
    }

}
