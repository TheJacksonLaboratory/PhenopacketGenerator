package org.jax.phenopacketgenerator.gui;


import com.google.common.collect.ImmutableList;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.AnchorPane;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import org.jax.phenopacketgenerator.OptionalResources;
import org.jax.phenopacketgenerator.Utils;
import org.jax.phenopacketgenerator.model.PGException;
import org.jax.phenopacketgenerator.model.PgModel;
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
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.ExecutorService;
import java.util.function.Function;
import java.util.stream.Collectors;

@Component
public class MainController {

    private static final Logger LOGGER = LoggerFactory.getLogger(MainController.class);
    private static final String INVALID_STYLE = "-fx-border-color: red; -fx-border-width: 2px;";
    private static final String VALID_STYLE = "-fx-border-color: green; -fx-border-width: 2px;";
    private static final String EMPTY_STYLE = "";

    private final OptionalResources optionalResources;
    private final Properties pgProperties;
    private final ExecutorService executorService;
    private final URL scigraphMiningUrl;
    /**
     * valid assemblies for VCF file.
     */
    private final List<String> assemblies = ImmutableList.of("hg19", "hg38", "hg39");

    private final List<Integer> months = ImmutableList.of(0, 1,2,3,4,5,6,7,8,9,10,11);
    private final List<Integer> days;
    private final List<Integer> years;

    private final String EMPTY_STRING = "";

    /**
     * valid values for sex combobox
     */
    private final List<String> sexValues = ImmutableList.of("UNKOWN", "FEMALE", "MALE");
    private final ObservableList<PgOntologyClass> phenotypes = FXCollections.observableList(new ArrayList<>());
    private final String phenopacketsVersion;
    private final String ecoVersion;

    @FXML
    public AnchorPane contentPane;

   // @FXML
    //TextField ageTextfield;

    @FXML
    private ComboBox<String> genomeBuildComboBox;
    @FXML
    private ComboBox<Integer> yearsCombo, monthsCombo, daysCombo;
    @FXML
    private ComboBox<String> sexComboBox;

    private String vcfFileAbsolutePath = null;

    @FXML
    private Button hpoTextMiningButton;
    @FXML
    private Button exportPhenopacketButton;
    @FXML
    private Label phenotypeSummaryLabel;
    @FXML
    private Label vcfFileLabel;
    @FXML
    private TextField probandIdTextfield;
    @FXML
    private TextField phenopacketIdTextfield;
    @FXML
    private Label exportPhenopacketLabel;
    @FXML
    private Label statusLabel;


    @Autowired
    public MainController(OptionalResources optionalResources,
                          Properties pgProperties,
                          ExecutorService executorService,
                          URL scigraphMiningUrl,
                          String phenopacketsVersion,
                          String ecoVersion) {
        this.optionalResources = optionalResources;
        this.pgProperties = pgProperties;
        this.executorService = executorService;
        this.scigraphMiningUrl = scigraphMiningUrl;
        this.phenopacketsVersion = phenopacketsVersion;
        this.ecoVersion = ecoVersion;
        ImmutableList.Builder<Integer> builder = new ImmutableList.Builder<>();
        for (int i=0; i<31; i++) {
            builder.add(i);
        }
        this.days = builder.build();
        builder = new ImmutableList.Builder<>();
        for (int i=0; i<121; i++) {
            builder.add(i);
        }
        years = builder.build();
    }

 
    public void initialize() {
        // generate phenotype summary text
        phenotypes.addListener(makePhenotypeSummaryLabel(phenotypes, phenotypeSummaryLabel));
        genomeBuildComboBox.getItems().addAll(assemblies);
        sexComboBox.getItems().addAll(sexValues);
        sexComboBox.setValue("UNKNOWN");
        probandIdTextfield.setPromptText("ID for proband/patient");
        phenopacketIdTextfield.setPromptText("ID for Phenopacket");
        hpoTextMiningButton.disableProperty().bind(optionalResources.ontologyProperty().isNull());
        exportPhenopacketButton.disableProperty().bind(optionalResources.ontologyProperty().isNull());
        optionalResources.ontologyProperty().isNull().addListener((obs, oldValue, newValue) -> {
            if (newValue) {
                statusLabel.setText("Need to set path to hp.obo file (See edit menu)");
                statusLabel.setStyle(INVALID_STYLE);
            } else {
                statusLabel.setText("Ontology loaded");
                statusLabel.setStyle(VALID_STYLE);
            }
        });

        // run the initialization task on a separate thread
        StartupTask task = new StartupTask(optionalResources, pgProperties);
        statusLabel.textProperty().bind(task.messageProperty());
        daysCombo.getItems().addAll(days);
        daysCombo.setPromptText("Days");
        monthsCombo.getItems().addAll(months);
        monthsCombo.setPromptText("Months");
        yearsCombo.getItems().addAll(years);
        yearsCombo.setPromptText("Years");
        // we don't have to watch the task's status after completion
        task.setOnSucceeded(e -> statusLabel.textProperty().unbind());
        executorService.submit(task);
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
            phenotypeSummaryLabel.setText(String.join(", ", observedSummary, excludedSummary));
        };
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


    private String getIso8601AgeString() {
        if (yearsCombo.getSelectionModel().isEmpty() &&
                monthsCombo.getSelectionModel().isEmpty() &&
                daysCombo.getSelectionModel().isEmpty()) {
            return null;
        }
        String age = "P";
        if (! yearsCombo.getSelectionModel().isEmpty()) {
            int y = yearsCombo.getValue();
            if (y > 0)
                age = String.format("%s%dY",age,y);
        }
        if (! monthsCombo.getSelectionModel().isEmpty()) {
            int m = monthsCombo.getValue();
            if (m > 0)
                age = String.format("%s%dM",age,m);
        }
        if (! daysCombo.getSelectionModel().isEmpty()) {
            int d = daysCombo.getValue();
            if (d > 0)
                age = String.format("%s%dD",age,d);
        }
        return age;
    }

  


    @FXML
    void exportPhenopacket() {
        PgModel pgmodel = new PgModel(phenotypes);
        if (vcfFileAbsolutePath != null) {
            pgmodel.setVcfPath(vcfFileAbsolutePath);
            String assembly = genomeBuildComboBox.getValue() == null ? "hg19" : genomeBuildComboBox.getValue();
            pgmodel.setGenomeAssembly(assembly);
        }
        pgmodel.setBiocurator(pgProperties.getProperty(OptionalResources.BIOCURATOR_ID_PROPERTY, EMPTY_STRING));
        String id = probandIdTextfield.getText();
        String ppacketid = phenopacketIdTextfield.getText();
        pgmodel.setProbandId(id);
        pgmodel.setPhenopacketId(ppacketid);
        String hpoVersion = optionalResources.getOntology().getMetaInfo().getOrDefault("version", "unknown HPO version");
        pgmodel.setHpoVersion(hpoVersion);
        pgmodel.setEcoVersion(ecoVersion);
        pgmodel.setPhenopacketVersion(this.phenopacketsVersion);
        pgmodel.setIsoAge(getIso8601AgeString());
        pgmodel.setSex(sexComboBox.getValue());
        try {
            pgmodel.qc();
        } catch (PGException e) {
            PopUps.showException("Exception", "Error in phenopacket creation", e.getLocalizedMessage(), e);
            return;
        }

        FileChooser chooser = new FileChooser();
        chooser.setTitle("Export as Phenopacket (JSON) file");
        String probandId = this.probandIdTextfield.getText();
        if (probandId == null || probandId.isEmpty()) {
            PopUps.showInfoMessage("Enter a proband ID before saving Phenopacket", "Error");
            return;
        }
        String suggestedFileName = String.format("%s-phenopacket.json",probandId);
        chooser.setInitialFileName(suggestedFileName);
        File f = chooser.showSaveDialog(exportPhenopacketButton.getScene().getWindow());
        if (f == null) {
            PopUps.showInfoMessage("Could not retrieve path to save phenopacket", "Warning");
            return;
        }
        PhenopacketExporter exporter = new PhenopacketExporter(pgmodel);
        exporter.export(f);
        String abspath = f.getAbsolutePath();
        int L = abspath.length();
        String message;
        if (L < 85) {
            message = String.format("Wrote to %s", abspath);
        } else {
            message = String.format("Wrote to %s...%s",
                    abspath.substring(0, 30),
                    abspath.substring(L - 30));
        }
        this.exportPhenopacketLabel.setText(message);
    }

    /**
     * Show the about message
     */
    @FXML
    public void aboutWindow() {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Phenopacket Generator");
        alert.setHeaderText("Phenopacket Generator");
        String s = "Creating Phenopackets for Genomic Diagnostics.";
        alert.setContentText(s);
        alert.showAndWait();
    }

    @FXML
    private void hpoTextMiningButtonAction() {
        String conversationTitle = "HPO text mining analysis";
        Ontology ontology = this.optionalResources.getOntology();
        if (ontology == null) {
            PopUps.showInfoMessage("Need to set location to hp.obo ontology file first!", "Error");
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
     * Runs after user clicks Settings/Set biocurator MenuItem and asks user to provide the ID.
     */
    @FXML
    void setBiocuratorMenuItemClicked() {
        String defaultId = pgProperties.getProperty(OptionalResources.BIOCURATOR_ID_PROPERTY, "e.g. HPO:rrabbit");
        String biocurator = PopUps.getStringFromUser("Biocurator ID",
                defaultId, "Enter your biocurator ID:");
        if (biocurator != null) {
            pgProperties.setProperty(OptionalResources.BIOCURATOR_ID_PROPERTY, biocurator);
            PopUps.showInfoMessage(String.format("Biocurator ID set to \n\"%s\"",
                    biocurator), "Success");
        } else {
            PopUps.showInfoMessage("Biocurator ID not set.",
                    "Information");
        }
    }


    @FXML
    void setPathToHpoObo() {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Set path to hp.obo file");
        FileChooser.ExtensionFilter extOBO = new FileChooser.ExtensionFilter("OBO file (*.obo)", "*.obo");
        chooser.getExtensionFilters().add(extOBO);
        File f = chooser.showOpenDialog(contentPane.getScene().getWindow());
        if (f == null) {
            LOGGER.error("Unable to obtain path to OBO file");
            PopUps.showInfoMessage("Unable to obtain path to OBO file", "Error");
            return;
        }
        final Path hpoPath = Paths.get(f.getAbsolutePath());
        statusLabel.setText("Loading ontology...");
        executorService.submit(() -> {
            /*
            Deserialize ontology and set it to optional resources. This should trigger enabling hpoTextMiningButton
            that was disabled.
             */
            try (InputStream is = Files.newInputStream(hpoPath)) {
                Ontology ontology = Utils.deserializeOntology(is);
                optionalResources.setOntology(ontology);
                // only store path to ontology if parsing went well
                pgProperties.setProperty(OptionalResources.ONTOLOGY_PATH_PROPERTY, hpoPath.toFile().getAbsolutePath());
                Platform.runLater(() -> { statusLabel.setText("Ontology loaded"); statusLabel.setStyle(VALID_STYLE); } );
            } catch (IOException e) {
                LOGGER.warn("Error parsing OBO file at `{}`", hpoPath, e);
            }
        });
    }

    @FXML
    void setPathToVcfFile() {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Set path to VCF file");
        FileChooser.ExtensionFilter extFilterVCF = new FileChooser.ExtensionFilter("Variant Call Format file file (*.vcf)", "*.vcf");
        FileChooser.ExtensionFilter extFilterVCFGZ = new FileChooser.ExtensionFilter("Compressed Variant Call Format file file (*.vcf.gz)", "*.vcf.gz");

        chooser.getExtensionFilters().addAll(extFilterVCF, extFilterVCFGZ);
        File f = chooser.showOpenDialog(contentPane.getScene().getWindow());
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
            String firstpart = vcfFileAbsolutePath.substring(0, 40);
            String lastPart = vcfFileAbsolutePath.substring(L - 40);
            displayString = String.format("%s.......%s", firstpart, lastPart);
        }
        this.vcfFileLabel.setText(displayString);
    }

    @FXML
    void exitMenuItemAction() {
        Platform.exit();
    }

}
