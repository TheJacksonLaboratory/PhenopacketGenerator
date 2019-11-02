package org.jax.phenopacketgenerator.model;

import java.util.List;

/**
 * A POJO class that contains all of the data we need to export a Phenopacket
 */
public class PgModel {

    private final static String UNITIALIZED = "Uninitialized";

    private String hpoVersion = UNITIALIZED;
    private String ecoVersion = UNITIALIZED;
    private final List<PgOntologyClass> phenotypes;
    private String vcfPath = null;
    private String genomeAssembly;
    private String biocurator = UNITIALIZED;
    private String probandId;
    private String phenopacketId;
    private String phenopacketVersion = UNITIALIZED;
    private String isoAge = UNITIALIZED;
    private String sex = UNITIALIZED;

    public PgModel(List<PgOntologyClass> phenotypes) {
        this.phenotypes = phenotypes;
    }

    public String getIsoAge() {
        return isoAge;
    }

    public void setIsoAge(String isoAge)  throws IllegalArgumentException {
        String s = isoAge;
        if (! s.startsWith("P")) {
            throw new IllegalArgumentException("Age string must begin with P");
        }
        s = s.substring(1);
        if (! Character.isDigit(s.charAt(0)) ) {
            throw new IllegalArgumentException("Malformed age string (expecting numberfollowing initial P): "+ isoAge);
        }
        // TODO use regex
        this.isoAge = isoAge;
    }

    public boolean hasSexData() {
        return ! sex.equals(UNITIALIZED);
    }

    public boolean hasAgeData() {
        return ! isoAge.equals(UNITIALIZED);
    }

    public String getSex() {
        return sex;
    }

    public void setSex(String sex) {
        this.sex = sex;
    }



    public boolean hasVcf() {
        return this.vcfPath != null;
    }

    public String getHpoVersion() {
        return hpoVersion;
    }

    public String getPhenopacketVersion() {
        return phenopacketVersion;
    }

    public void setPhenopacketVersion(String phenopacketVersion) {
        this.phenopacketVersion = phenopacketVersion;
    }

    public void setHpoVersion(String hpoVersion) {
        this.hpoVersion = hpoVersion;
    }

    public String getEcoVersion() {
        return ecoVersion;
    }

    public void setEcoVersion(String ecoVersion) {
        this.ecoVersion = ecoVersion;
    }

    public List<PgOntologyClass> getPhenotypes() {
        return phenotypes;
    }

    public String getVcfPath() {
        return vcfPath;
    }

    public void setVcfPath(String vcfPath) {
        this.vcfPath = vcfPath;
    }

    public String getGenomeAssembly() {
        return genomeAssembly;
    }

    public void setGenomeAssembly(String genomeAssembly) {
        this.genomeAssembly = genomeAssembly;
    }

    public String getBiocurator() {
        return biocurator;
    }

    public void setBiocurator(String biocurator) {
        this.biocurator = biocurator;
    }

    public String getProbandId() {
        return probandId;
    }

    public void setProbandId(String probandId) {
        this.probandId = probandId;
    }

    public String getPhenopacketId() {
        return phenopacketId;
    }

    public void setPhenopacketId(String phenopacketId) {
        this.phenopacketId = phenopacketId;
    }





}
