DeepPhe
=======

# Cancer Deep Phenotype Extraction (DeepPhe) Project

This repository is the home of the public releases of the DeepPhe Cancer Phenotype Extraction System. 

DeepPhe uses combines natural language processing (based on [Apache cTAKES](ctakes.apache.org)) with an exetensive domain information model to:
* extracts information from plaintext documents    
* summarizes information for Cancers and Tumors across mutiple documents  
*  writes results to a [Neo4j](https://neo4j.com/) database
*  visualize results at patient and cohort levels using our [DeepPhe Viz](https://github.com/DeepPhe/DeepPhe-Viz) tool. 

The system has been tested using documents from three cancer domains:
* Breast Cancer
* Ovarian Cancer
* Malignant Melanoma

There are two versions of DeepPhe:

* __DeepPhe Translational__: the full suite of tools, designed to support cohort discovery for cancer clinical research.  This is the version that must users will want to start with. Installation instructions are available on the [release page](https://github.com/DeepPhe/DeepPhe-Release/releases/tag/xn0.4.1).
* __DeepPhe-CR__: a web-service version of DeepPhe designed to support cancer registries.


## Licensing
DeepPhe is provided under an [Academic Software Use Agreement](LICENSE)  
Refer to that agreement for information about requesting the use of the Software for commercial purposes.

DeepPhe includes portions of the HemOnc.org ontology. Refer to [HemOnc.org](https://hemonc.org/wiki/Ontology) regarding the licensing of the HemOnc.org ontology.

DeepPhe includes portions of the [NCI Thesaurus](https://ncit.nci.nih.gov/ncitbrowser/) (NCIt).

Other licenses for your reference  
* [Apache cTAKES](https://ctakes.apache.org/license.html)&#8482;
* [Neo4j](https://neo4j.com/docs/license/)  

## Contact / Help
_Please drop us a note by posting to the [DeepPhe group]( https://groups.google.com/forum/#!forum/deepphe)_.

Metrics on downloads and usage could help us with funding future enhancements.

For questions, contact us via the [DeepPhe group]( https://groups.google.com/forum/#!forum/deepphe).
