package org.jax.phenopacketgenerator.gui;


import javafx.fxml.FXML;
import javafx.scene.layout.StackPane;
import org.monarchinitiative.hpotextmining.gui.controller.HpoTextMining;
import org.springframework.stereotype.Component;

@Component
public class MainController {

    private final HpoTextMining mining;

    @FXML
    public StackPane miningbox;


    public MainController(HpoTextMining mining) {
        this.mining = mining;
    }

    public void initialize() throws Exception {
        miningbox.getChildren().add(mining.getMainParent());
    }
}
