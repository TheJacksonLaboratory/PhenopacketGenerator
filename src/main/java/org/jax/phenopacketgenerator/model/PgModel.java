package org.jax.phenopacketgenerator.model;

import com.google.common.collect.ImmutableList;

import java.util.List;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * A POJO class that contains all of the data we need to export a Phenopacket
 */
public class PgModel {

    private final static String EMPTY_STRING = "";

    private String hpoVersion = EMPTY_STRING;
    private String ecoVersion = EMPTY_STRING;
    private final TreeSet<PgOntologyClass> phenotypes;
    private String vcfPath = null;
    private String genomeAssembly;
    private String biocurator = EMPTY_STRING;
    private String probandId = EMPTY_STRING;
    private String phenopacketId = EMPTY_STRING;
    private String phenopacketVersion = EMPTY_STRING;
    private String isoAge = EMPTY_STRING;
    private String sex = EMPTY_STRING;

    private final String iso8601 = "^P(?=\\d|T\\d)(?:(\\d+)Y)?(?:(\\d+)M)?(?:(\\d+)([DW]))?";
    private final Pattern pattern = Pattern.compile(iso8601);

    public PgModel(List<PgOntologyClass> phenotypes) {
        this.phenotypes = new TreeSet<>(phenotypes);
    }

    public String getIsoAge() {
        return isoAge;
    }

    public void setIsoAge(String isoAge) {
        this.isoAge = isoAge;
    }

    public boolean hasSexData() {
        return ! sex.equals(EMPTY_STRING);
    }

    public boolean hasAgeData() {
        return isoAge != null && ! isoAge.equals(EMPTY_STRING);
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
        ImmutableList.Builder<PgOntologyClass> builder = new ImmutableList.Builder<>();
        for (PgOntologyClass pgoc : this.phenotypes) {
            builder.add(pgoc);
        }
        return builder.build();
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


    public void qc() throws PGException {
        if (this.phenopacketId.equals(EMPTY_STRING) || phenopacketId.isEmpty()) {
            throw new PGException("Phenopacket ID is not initialized");
        } else {
            System.out.println("phenopacket id is "+phenopacketId);
        }
        if (this.probandId.equals(EMPTY_STRING) || probandId.isEmpty()) {
            throw new PGException("Proband ID is not initialized");
        }
        if (this.isoAge != null && !this.isoAge.equals(EMPTY_STRING) && !this.isoAge.isEmpty()) {
            Matcher m = pattern.matcher(isoAge);
            if (! m.find()) {
                throw new PGException("Invalid age string: " + isoAge);
            }
        }
        if (this.phenotypes.isEmpty()) {
            throw new PGException("At least one phenotype term required!");
        }
        if (biocurator == null || biocurator.equals(EMPTY_STRING) || biocurator.isEmpty()) {
            throw new PGException("Biocurator ID not unitialized (use Edit menu)");
        }
    }


}
