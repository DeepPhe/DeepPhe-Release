DeepPhe
=======

# Cancer Deep Phenotype Extraction (DeepPhe) Project

This repository is the home of the public releases of the DeepPhe Cancer Phenotype Extraction System. 

DeepPhe uses combines natural language processing (based on [Apache cTAKES](ctakes.apache.org)) with an exetensive domain information model to:
* extract information from plaintext documents    
* summarize information for Cancers and Tumors across mutiple documents  
*  write results to a [Neo4j](https://neo4j.com/) database
*  visualize results at patient and cohort levels using our [DeepPhe Viz](https://github.com/DeepPhe/DeepPhe-Viz) tool. 

The system has been tested using documents from three cancer domains:
* Breast Cancer
* Ovarian Cancer
* Malignant Melanoma

There are two versions of DeepPhe:

* __DeepPhe-XN__: the full suite of tools, designed to support cohort discovery for cancer clinical research.  This is the version that most users will want to start with. Installation instructions are available on the [Windows Installation](https://github.com/DeepPhe/DeepPhe-Release/wiki/Windows-Installation-Instructions) or [Mac Installation](https://github.com/DeepPhe/DeepPhe-Release/wiki/Mac-Linux-Installation-Instructions)
* __DeepPhe-CR__: a web-service version of DeepPhe designed to support cancer registries. Requires Docker for [installation](https://github.com/DeepPhe/DeepPhe-Release/tree/v0.1.0-cr).


## Licensing
* DeepPhe is provided under an [Academic Software Use Agreement](LICENSE).  Refer to that agreement for information about requesting the use of the Software for commercial purposes.

* DeepPhe includes portions of the [HemOnc.org](https://hemonc.org/wiki/Ontology) ontology. 

* DeepPhe includes portions of the [NCI Thesaurus](https://ncit.nci.nih.gov/ncitbrowser/) (NCIt).

* DeepPhe uses the [Apache cTAKES](https://ctakes.apache.org/license.html) (click for license) library.

* DeepPhe writes output to a [Neo4j](https://neo4j.com/docs/license/) (click for license) database.

## Contact / Help
If you'd like to drop us a note, or have technical questions, please post on [the issue tracker](https://github.com/DeepPhe/DeepPhe-Release/issues).

Metrics on downloads and usage could help us with funding future enhancements.

