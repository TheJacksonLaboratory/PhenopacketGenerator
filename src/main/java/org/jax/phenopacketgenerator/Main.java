package org.jax.phenopacketgenerator;

import com.sun.javafx.css.StyleManager;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.fxml.JavaFXBuilderFactory;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import org.jax.phenopacketgenerator.gui.MainController;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ConfigurableApplicationContext;

import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;
import java.util.Properties;
import java.util.ResourceBundle;

@SpringBootApplication
public class Main extends Application {
    /**
     * Name of app, taken from application.properties.
     */
    public static final String PG_NAME_KEY = "pg.name";

    public static final String PG_VERSION_PROP_KEY = "pg.version";

    public static final String PG_PHENOPACKET_VERSION_PROP_KEY = "pg.phenopacket.version";

    private ConfigurableApplicationContext context;

    public static void main(String[] args) {
        launch(args);
    }


    @Override
    public void init() throws Exception {
        super.init();
        // export app's version into System properties
        try (InputStream is = getClass().getResourceAsStream("/application.properties")) {
            Properties properties = new Properties();
            properties.load(is);

            // pg.name
            String name = properties.getProperty(PG_NAME_KEY, "Phenopacket Generator");
            System.setProperty(PG_NAME_KEY, name);

            // pg.version
            String version = properties.getProperty(PG_VERSION_PROP_KEY, "unknown version");
            System.setProperty(PG_VERSION_PROP_KEY, version);

            // pg.phenopacket.version
            String ppVersion = properties.getProperty(PG_PHENOPACKET_VERSION_PROP_KEY, "N/A");
            System.setProperty(PG_PHENOPACKET_VERSION_PROP_KEY, ppVersion);
        }
    }

    @Override
    public void start(Stage window) throws Exception {
        Locale.setDefault(new Locale("en", "US"));
        context = SpringApplication.run(Main.class);

        // Apply CSS
        Application.setUserAgentStylesheet(Application.STYLESHEET_MODENA);
        StyleManager.getInstance().addUserAgentStylesheet("phenopacketgenerator.css");
        ResourceBundle resourceBundle = ResourceBundle.getBundle(Main.class.getName());

        Parent rootNode = FXMLLoader.load(MainController.class.getResource("main.fxml"), resourceBundle,
                new JavaFXBuilderFactory(), context::getBean);
        window.setTitle("Phenopacket Generator"); // todo -- set from properties file
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
        // save properties
        final Properties pgProperties = context.getBean("pgProperties", Properties.class);
        final Path configFilePath = context.getBean("configFilePath", Path.class);
        try (OutputStream os = Files.newOutputStream(configFilePath)) {
            pgProperties.store(os, "Phenopacket generator properties");
        }
        // close the context
        context.close();
    }


}
