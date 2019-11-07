package org.jax.phenopacketgenerator.gui;


import com.google.common.collect.ImmutableList;
import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.StackPane;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import org.jax.phenopacketgenerator.OptionalResources;
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

import java.io.*;
import java.net.URL;
import java.nio.file.Path;
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
    private static final String EMPTY_STYLE = "";
    private final HpoTextMining mining;
    private final OptionalResources optionalResources;
    private final Properties properties;
    private final ExecutorService executorService;
    private final URL scigraphMiningUrl;
    /**
     * valid assemblies for VCF file.
     */
    private final List<String> assemblies = ImmutableList.of("hg19", "hg38", "hg39");
    /**
     * valid values for sex combobox
     */
    private final List<String> sexValues = ImmutableList.of("UNKOWN", "FEMALE", "MALE");
    private final ObservableList<PgOntologyClass> phenotypes = FXCollections.observableList(new ArrayList<>());
    private final String phenopacketsVersion;
    private final String ecoVersion;
    private String biocuratorID;
    private final StringProperty probandId;
    private final StringProperty phenopacketId;
    private final StringProperty isoAge;
    private final File homeDirectory;
    private final Path configFilePath;
    @FXML
    public StackPane miningbox;
    @FXML
    private ComboBox<String> genomeBuildComboBox;
    @FXML
    private ComboBox<String> sexComboBox;
    @FXML
    TextField ageTextfield;
    private String vcfFileAbsolutePath = null;
    private String hpoAbsolutePath = null;
    private Ontology ontology = null;
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
    public MainController(HpoTextMining mining,
                          OptionalResources optionalResources,
                          Properties properties,
                          ExecutorService executorService,
                          URL scigraphMiningUrl,
                          String phenopacketsVersion,
                          String ecoVersion,
                          File appHomeDir,
                          Path configFilePath) {
        this.mining = mining;
        this.optionalResources = optionalResources;
        this.properties = properties;
        this.executorService = executorService;
        this.scigraphMiningUrl = scigraphMiningUrl;
        this.probandId = new SimpleStringProperty(this, "probandId", null);
        this.phenopacketId = new SimpleStringProperty(this, "phenopacketId", null);
        this.isoAge = new SimpleStringProperty(this, "isoAge", null);
        this.phenopacketsVersion = phenopacketsVersion;
        this.ecoVersion = ecoVersion;
        this.homeDirectory = appHomeDir;
        this.configFilePath = configFilePath;
        setHpoFromPropertiesIfPossible();
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

    public void initialize() {
        //miningbox.getChildren().add(mining.getMainParent());
        StartupTask task = new StartupTask(optionalResources, properties);
        executorService.submit(task);
        // generate phenotype summary text
        phenotypes.addListener(makePhenotypeSummaryLabel(phenotypes, phenotypeSummaryLabel));
        genomeBuildComboBox.getItems().addAll(assemblies);
        sexComboBox.getItems().addAll(sexValues);
        sexComboBox.setValue("UNKNOWN");
        probandId.bind(probandIdTextfield.textProperty());
        phenopacketId.bind(phenopacketIdTextfield.textProperty());
        isoAge.bindBidirectional(ageTextfield.textProperty());
        probandIdTextfield.setPromptText("ID for proband/patient");
        phenopacketIdTextfield.setPromptText("ID for Phenopacket");
        ageTextfield.setPromptText("PxxYyyMzzD");
        Tooltip agett = new Tooltip("Enter Age is ISO TODO format, e.g., P42Y for 42 years, P12Y2M3D for 12 years, 2 months, and 3 days");
        ageTextfield.setTooltip(agett);
        this.hpoTextMiningButton.disableProperty().bind(optionalResources.ontologyProperty().isNull());
        this.exportPhenopacketButton.disableProperty().bind(optionalResources.ontologyProperty().isNull());
        optionalResources.ontologyProperty().addListener((a, oldval, newval) -> {
            if (newval == null) {
                setHpoStatus(false);
            } else {
                setHpoStatus(true);
            }
        });
        if (optionalResources.ontologyProperty().isNull().get()) {
            setHpoStatus(false);
        }
    }

    private void setHpoFromPropertiesIfPossible() {
        Properties properties = new Properties();
        if (ontology != null) return;
        try {
            BufferedReader br = new BufferedReader(new FileReader(this.configFilePath.toFile()));
            properties.load(br);
            br.close();
            if (properties.getProperty("hp.obo.path") != null) {
                this.hpoAbsolutePath = properties.getProperty("hp.obo.path");
                optionalResources.setOntologyPath(new File(this.hpoAbsolutePath));
                optionalResources.initializeOntologyFromFile();
                this.ontology = optionalResources.getOntology();
                LOGGER.info("Imported hpo file from {}", this.hpoAbsolutePath);
                setHpoStatus(true);
            }
            if (properties.getProperty("biocurator.id") != null) {
                this.optionalResources.setBiocuratorId(properties.getProperty("biocurator.id"));
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @FXML
    void exportPhenopacket() {
        PgModel pgmodel = new PgModel(this.phenotypes);
        if (this.vcfFileAbsolutePath != null) {
            pgmodel.setVcfPath(this.vcfFileAbsolutePath);
            String assembly = genomeBuildComboBox.getValue() == null ? "hg19" : genomeBuildComboBox.getValue();
            pgmodel.setGenomeAssembly(assembly);
        }
        pgmodel.setBiocurator(this.optionalResources.getBiocuratorId());
        String id = probandId.getValue();//.isEmpty() ? "n/a" : probandId.getValue();
        String ppacketid = phenopacketId.getValue();//.isEmpty() ? "n/a" : phenopacketId.getValue();
        pgmodel.setProbandId(id);
        pgmodel.setPhenopacketId(ppacketid);
        String hpoVersion = this.optionalResources.getOntology().getMetaInfo().getOrDefault("version", "unknown HPO version");
        pgmodel.setHpoVersion(hpoVersion);
        pgmodel.setEcoVersion(this.ecoVersion);
        pgmodel.setPhenopacketVersion(this.phenopacketsVersion);
        pgmodel.setIsoAge(isoAge.getValue());
        pgmodel.setSex(this.sexComboBox.getValue());
        try {
            pgmodel.qc();
        } catch (PGException e) {
            PopUps.showException("Exception", "Error in phenopacket creation", e.getLocalizedMessage(), e);
            return;
        }

        FileChooser chooser = new FileChooser();
        chooser.setTitle("Export as Phenopacket (JSON) file");
        File f = chooser.showSaveDialog(null);
        if (f == null) {
            PopUps.showInfoMessage("Could not retrieve path to save phenopacket", "Warning");
            return;
        }
        PhenopacketExporter exporter = new PhenopacketExporter(pgmodel);
        System.out.println("[TODO] Save complete Phenopacket to this file: " + f.getAbsolutePath());

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
    public void aboutWindow(ActionEvent e) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Phenopacket Generator");
        alert.setHeaderText("Phenopacket Generator");
        String s = "Creating Phenopackets for Genomic Diagnostics.";
        alert.setContentText(s);
        alert.showAndWait();
        e.consume();
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
    private void setHpoStatus(boolean downloaded) {
        if (downloaded) {
            final String message = String.format("  Ontology file: %s", this.hpoAbsolutePath);
            Platform.runLater(() -> {
                this.statusLabel.setText(message);
                this.statusLabel.setStyle(EMPTY_STYLE);
            });
        } else {
            this.statusLabel.setText("  Need to download hp.obo (see Edit menu)");
            this.statusLabel.setStyle(INVALID_STYLE);
        }
    }

    @FXML
    void setPathToHpoObo() {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Set path to hp.obo file");
        FileChooser.ExtensionFilter extOBO = new FileChooser.ExtensionFilter("OBO file (*.obo)", "*.obo");
        chooser.getExtensionFilters().add(extOBO);
        File f = chooser.showOpenDialog(null);
        if (f == null) {
            LOGGER.error("Unable to obtain path to OBO file");
            PopUps.showInfoMessage("Unable to obtain path to OBO file", "Error");
            return;
        }
        this.hpoAbsolutePath = f.getAbsolutePath();
        this.optionalResources.setOntologyPath(new File(this.hpoAbsolutePath));
        setHpoStatus(true);
        writeHpoToPropertiesFile();
    }

    private void writeHpoToPropertiesFile() {
        try {
            BufferedWriter writer = new BufferedWriter(new FileWriter(this.configFilePath.toFile()));
            writer.write("hp.obo.path:" + this.hpoAbsolutePath);
            writer.close();
            LOGGER.info("Wrote hp.obo path to {}", this.hpoAbsolutePath);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @FXML
    void setPathToVcfFile() {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Set path to VCF file");
        FileChooser.ExtensionFilter extFilterVCF = new FileChooser.ExtensionFilter("Variant Call Format file file (*.vcf)", "*.vcf");
        FileChooser.ExtensionFilter extFilterVCFGZ = new FileChooser.ExtensionFilter("Compressed Variant Call Format file file (*.vcf.gz)", "*.vcf.gz");

        chooser.getExtensionFilters().addAll(extFilterVCF, extFilterVCFGZ);
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
            String firstpart = vcfFileAbsolutePath.substring(0, 40);
            String lastPart = vcfFileAbsolutePath.substring(L - 40);
            displayString = String.format("%s.......%s", firstpart, lastPart);
        }
        this.vcfFileLabel.setText(displayString);
    }

    @FXML
    void exitMenuItemAction() {
        Platform.exit();
        System.exit(0);
    }

}
