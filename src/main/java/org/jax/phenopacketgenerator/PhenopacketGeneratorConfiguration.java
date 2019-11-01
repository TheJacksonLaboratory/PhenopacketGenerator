package org.jax.phenopacketgenerator;

import org.monarchinitiative.hpotextmining.gui.controller.HpoTextMining;
import org.monarchinitiative.phenol.io.OntologyLoader;
import org.monarchinitiative.phenol.ontology.data.Ontology;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Objects;

@Configuration
public class PhenopacketGeneratorConfiguration {


    @Bean
    public HpoTextMining hpoTextMining(URL sciGraphUrl, Ontology ontology) throws IOException {
        return HpoTextMining.builder()
                .withSciGraphUrl(sciGraphUrl)
                .withOntology(ontology)
                .build();
    }

    @Bean
    public URL sciGraphUrl(Environment environment) throws MalformedURLException {
        return new URL(Objects.requireNonNull(environment.getProperty("scigraph.url")));
    }

    @Bean
    public Ontology ontology(Path hpOboPath) {
        return OntologyLoader.loadOntology(hpOboPath.toFile());
    }

    @Bean
    public Path hpOboPath(Environment environment) {
        return Paths.get(Objects.requireNonNull(environment.getProperty("hp.obo.path")));
    }
}