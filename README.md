# PhenopacketGenerator
Generate a phenopacket for use with LIRICAL or Exomiser.


## Building PhenopacketGenerator

Most users should download the latest prebuilt executable from 
the [releases tab](https://github.com/TheJacksonLaboratory/PhenopacketGenerator/releases). Phenopacket
generator can also be built from source using maven as follows.

```aidl
$ git clone https://github.com/TheJacksonLaboratory/PhenopacketGenerator.git
$ cd PhenopacketGenerator
$ mvn package
```
This will build PhenopacketGenerator in the ``target/`` subdirectory.

## Running PhenopacketGenerator

On most systems, PhenopacketGenerator can be started with a double click. It can also be started from the command
line as follows.
```aidl
$ java -jar Phenopacket-Generator-0.0.3.jar 
```

To set up the executable, you will need to indicate the path to the version of the
[Human Phenotype Ontology](https://hpo.jax.org/app/) Ontology file (``hp.obo``). The current version
of this file can always be downloaded from the [HPO Download page](https://hpo.jax.org/app/download/ontology).

When Phenopacket Generator is started for the first time, it will indicate that the path to hp.obo needs to be set.

![Phenopacket Generator Start Screen](./img/startscreen.png?raw=true "Phenopacket Generator")


Use the File chooser dialog in the Edit menu to do so. Also set the biocurator ID (this id will be used to denote the
creator of the Phenopacket). Once the hp.obo path has been set, the ``Enter HPO terms`` and ``Export Phenopacket`` 
buttons will be activated. Enter the data as indicated. A separate dialog will appear once the ``Enter HPO Terms`` button 
is clicked that allows users to navigate the HPO hierarchy, use an autocomplete window, or use
text mining to enter HPO terms.

![HPO Text Mining](./img/hpo-textmining.png?raw=true "HPO Text Mining")

Once all data has been entered, click on ``Export Phenopacket`` to save the Phenopacket file to disk. If
any required data is missing or malformed, an error dialog will appear, and users will need to correct
the data before saving the file.

## Data Entry

The following fields can be entered.

* Sex (optional)
* Age (optional). Use one or more of the pull down menus to enter the age in years, months, or days. The
age information will be stored  using the  [ISO 8601](https://en.wikipedia.org/wiki/ISO_8601) format, 
e.g., P42Y for 42 years, P12Y2M3D for 12 years, 2 months, and 3 days
* Phenopacket ID (required). This ID cannot be empty but can be any user-defined string
* Proband ID (required). This ID cannot be empty but can be any user-defined string
* HPO terms. At least one term must be entered. Observed or excluded (negated) terms can be entered. There is no limit 
to the total number of HPO terms that can be entered.
* VCF file (optional). The path to a [VCF File](https://en.wikipedia.org/wiki/Variant_Call_Format) that is expected to
represent the results of NGS Gene Panel, Exome, or Genome sequencing on the proband. The file must have the suffix ``vcf`` or
``vcf.gz``
* Genome assembly (required if a VCF file is provided). The assembly of the VCF file.

## Phenopacket export
The [phenopacket-schema](https://phenopackets-schema.readthedocs.io/en/latest/) defines the phenotypic 
description of a patient/sample (for instance in the context of rare disease or cancer genomic diagnosis). 
It aims to provide sufficient and shareable information of the data outside of the EHR (Electronic Health Record)
with the aim of enabling capturing of sufficient structured data at the point of care by a clinician or clinical 
geneticist for sharing with other labs or computational analysis of the data in clinical or research environments.

PhenopacketGenerator currently is designed to generate a Phenopacket that represents the phenotypic features
of an individual with suspected Mendelian disease for whom genomic diagnostics is being performed. The
resulting phenopacket can be used as an in put file for programs such as [LIRICAL](https://github.com/TheJacksonLaboratory/LIRICAL).

Here is an example phenopacket for an individual with [Portal vein thrombosis](https://hpo.jax.org/app/browse/term/HP:0030242)
and [Splenomegaly](https://hpo.jax.org/app/browse/term/HP:0001744). The path to a VCF file (``/path/to/example.vcf``)
is indicated.



```aidl
{
  "id": "ID:1",
  "subject": {
    "id": "Patient A",
    "ageAtCollection": {
      "age": "P6Y5M"
    },
    "sex": "MALE"
  },
  "phenotypicFeatures": [{
    "type": {
      "id": "HP:0001744",
      "label": "Splenomegaly"
    },
    "evidence": [{
      "evidenceCode": {
        "id": "ECO:0000302",
        "label": "author statement used in manual assertion"
      }
    }]
  }, {
    "type": {
      "id": "HP:0030242",
      "label": "Portal vein thrombosis"
    },
    "evidence": [{
      "evidenceCode": {
        "id": "ECO:0000302",
        "label": "author statement used in manual assertion"
      }
    }]
  }],
  "htsFiles": [{
    "uri": "file://home/peter/data/lirical/SRR8906477.filtered.vcf",
    "htsFormat": "VCF",
    "genomeAssembly": "hg38"
  }],
  "metaData": {
    "created": "2019-11-10T15:47:06.750Z",
    "createdBy": "ExampleOrg:ExampleCurator",
    "resources": [{
      "id": "hp",
      "name": "human phenotype ontology",
      "url": "http://purl.obolibrary.org/obo/hp.owl",
      "version": "unknown HPO version",
      "namespacePrefix": "HP",
      "iriPrefix": "http://purl.obolibrary.org/obo/HP_"
    }, {
      "id": "eco",
      "name": "Evidence and Conclusion Ontology",
      "url": "http://purl.obolibrary.org/obo/eco.owl",
      "version": "2019-10-16",
      "namespacePrefix": "ECO",
      "iriPrefix": "http://purl.obolibrary.org/obo/ECO_"
    }],
    "phenopacketSchemaVersion": "1.0.0-RC3"
  }
}
```