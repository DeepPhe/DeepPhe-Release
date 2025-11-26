DeepPhe
=======

# Cancer Deep Phenotype Extraction (DeepPhe) Project

This repository is the home of the [**public source code** releases](https://github.com/DeepPhe/DeepPhe-Release/releases)
of the DeepPhe Cancer Phenotype Extraction System. 
For an installation GUI of the DeepPhe system, please download our **Installation Tool** 
for [Windows](https://github.com/DeepPhe/DeepPhe-Dist/releases/download/main/DeepPhe_windows.exe) 
or [Linux](https://github.com/DeepPhe/DeepPhe-Dist/releases/download/main/DeepPhe.dmg).


DeepPhe uses combines natural language processing (based on [Apache cTAKES](ctakes.apache.org)) with an extensive domain information model to:
* extract information from plaintext documents    
* summarize information for Cancers and Tumors across multiple documents
* visualize results at patient and cohort levels using our [DeepPhe Viz](https://github.com/DeepPhe/DeepPhe-Viz-v2) tool. 

The system has been tested using documents from five cancer domains:
* Breast Cancer
* Ovarian Cancer
* Malignant Melanoma
* Prostate Cancer
* Childhood brain cancer

There are two versions of DeepPhe:

* __DeepPhe-XN__: a full suite of tools, designed to support cohort discovery for cancer clinical research.
This is the version that most users will want to start with. 
Instructions for our installation tool are available on our [Wiki](https://github.com/DeepPhe/DeepPhe-Release/wiki) for [Windows](https://github.com/DeepPhe/DeepPhe-Release/wiki/Windows-Installation-Instructions), 
[MacOS](https://github.com/DeepPhe/DeepPhe-Release/wiki/Mac-Installation-Instructions),
or [Linux](https://github.com/DeepPhe/DeepPhe-Release/wiki/Linux-Installation-Instructions)
* __DeepPhe-CR__: a web-service version of DeepPhe designed to support cancer registries. 
Installation instructions are available in the [DeepPhe-CR release repository](https://github.com/DeepPhe/DeepPhe-CR-Release).


## Licensing
* DeepPhe is provided under an [Academic Software Use Agreement](LICENSE).  Refer to that agreement for information about requesting the use of the Software for commercial purposes.

* DeepPhe includes portions of the [HemOnc.org](https://hemonc.org/wiki/Ontology) ontology. 

* DeepPhe includes portions of the [NCI Thesaurus](https://ncit.nci.nih.gov/ncitbrowser/).

* DeepPhe includes portions of the [NLM UMLS](https://www.nlm.nih.gov/research/umls/index.html).

* DeepPhe uses [Apache cTAKES](https://github.com/apache/ctakes?tab=readme-ov-file#apache-ctakes).

* DeepPhe uses the [Neo4j](https://neo4j.com/) graph database platform.

## Contact / Help
If you'd like to drop us a note, or have technical questions, please post on [the issue tracker](https://github.com/DeepPhe/DeepPhe-Release/issues).

Metrics on downloads and usage could help us with funding future enhancements.

