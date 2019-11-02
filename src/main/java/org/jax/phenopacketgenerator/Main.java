package org.jax.phenopacketgenerator;

import com.sun.javafx.css.StyleManager;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.fxml.JavaFXBuilderFactory;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import org.jax.phenopacketgenerator.gui.MainController;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ApplicationContext;

import java.util.Locale;
import java.util.ResourceBundle;

@SpringBootApplication
public class Main extends Application {
    /** Name of app, taken from application.properties. */
    static final String PG_NAME_KEY = "pg.name";

    static final String PG_VERSION_PROP_KEY = "pg.version";

    private ApplicationContext context;

    public static void main(String[] args) {
        launch(args);
    }


    @Override
    public void init() throws Exception {
        super.init();
    }

    @Override
    public void start(Stage window) throws Exception {
        Locale.setDefault(new Locale("en", "US"));
        context = new SpringApplicationBuilder(Main.class).run();

        // Apply CSS
        Application.setUserAgentStylesheet(Application.STYLESHEET_MODENA);
        StyleManager.getInstance().addUserAgentStylesheet("phenopacketgenerator.css");
        ResourceBundle resourceBundle = ResourceBundle.getBundle(Main.class.getName());

        Parent rootNode = FXMLLoader.load(MainController.class.getResource("main.fxml"), resourceBundle,
                new JavaFXBuilderFactory(), context::getBean);
        window.setTitle("Phenopacket Generator"); // todo -- set from porperties file
        window.setScene(new Scene(rootNode));
        window.show();
        /*
        window.getIcons().add(new Image(getClass().getResourceAsStream("/img/app-icon.png")));
        */

    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void stop() throws Exception {
        super.stop();
/*
        // save properties
        OptionalResources optionalResources = injector.getInstance(OptionalResources.class); // singleton
        Properties properties = injector.getInstance(Properties.class);

        if (optionalResources.getOntologyPath() != null) {
            properties.setProperty(OptionalResources.ONTOLOGY_PATH_PROPERTY,
                    optionalResources.getOntologyPath().getAbsolutePath());
        }
        if (optionalResources.getEntrezPath() != null) {
            properties.setProperty(OptionalResources.ENTREZ_GENE_PATH_PROPERTY,
                    optionalResources.getEntrezPath().getAbsolutePath());
        }
        if (optionalResources.getDiseaseCaseDir() != null) {
            properties.setProperty(OptionalResources.DISEASE_CASE_DIR_PROPERTY,
                    optionalResources.getDiseaseCaseDir().getAbsolutePath());
        }
        if (optionalResources.getBiocuratorId() != null) {
            properties.setProperty(OptionalResources.BIOCURATOR_ID_PROPERTY,
                    optionalResources.getBiocuratorId());
        }
        File where = injector.getInstance(Key.get(File.class, Names.named("propertiesFilePath")));
        properties.store(new FileWriter(where), "Hpo Case Annotator properties");
        LOGGER.info("Properties saved to {}", where.getAbsolutePath());

        where = injector.getInstance(Key.get(File.class, Names.named("refGenomePropertiesFilePath")));
        GenomeAssemblies assemblies = injector.getInstance(GenomeAssemblies.class); // singleton
        try (OutputStream os = new BufferedOutputStream(new FileOutputStream(where))) {
            GenomeAssembliesSerializer.serialize(assemblies, os);
            LOGGER.info("Reference genome data configuration saved to {}", where.getAbsolutePath());
        }

        ExecutorService executor = injector.getInstance(ExecutorService.class);
        executor.shutdown();
        */




    }


}
