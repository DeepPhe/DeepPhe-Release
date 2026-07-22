---
title: Set Up a Project
---

A project is a saved set of paths for one DeepPhe run. The project selector is editable: choose an existing project from the list, or type a new project name to create a new saved configuration.

Project files are saved under:

```text
<DEEPPHE_ROOT>/.DeepPhe/resources/projects/
```

The project list is saved in:

```text
<DEEPPHE_ROOT>/.DeepPhe/resources/projects/ProjectList.txt
```

## Project Fields

| Field | What to select | Default |
| --- | --- | --- |
| Piper File | A `.piper` file defining the NLP pipeline. | `<DEEPPHE_ROOT>/.DeepPhe/resources/pipeline/DefaultDeepPhe.piper` |
| Corpus Directory | A directory containing patient folders with document text files. | `~/DeepPheDocs/example_corpus` |
| OMOP Database | A JSON demographics file in the expected OMOP format. | `~/DeepPheDocs/example_omop/patient_demographics.json` |
| Output Directory | Where NLP output and downstream data products are written. | `~/DeepPheDocs/example_output` |

You can type or paste paths directly into the Value column. You can also click the Explorer column to open a file chooser. The file chooser restricts Piper selections to `.piper` files and OMOP selections to `.json` files.

## How Project Saving Works

The GUI saves the active project when you switch to another project and when the application exits. New project names are moved to the top of the project list after selection.

Project files are simple `NAME=value` text files. A saved project looks like:

```ini
PROJECT_NAME=ExampleProject
PIPER_FILE=/path/to/DefaultDeepPhe.piper
CORPUS_DIR=/path/to/corpus
OMOP_DB=/path/to/patient_demographics.json
OUTPUT_DIR=/path/to/output
```

## Piper Files

[Piper files](https://github.com/apache/ctakes/wiki/Piper-Files) contain commands and parameters used to configure a pipeline.
The DefaultDeepPhe.piper file contains the default DeepPhe Translational (DeepPhe-XN) pipeline that can be used to generate example output.
You can modify the pipeline to cater DeepPhe processing and output to a particular cancer type, cancer attribute, document type, etc.
DeepPhe is meant to be highly modifiable, and using the default pipeline without modification may not provide the results
that can be achieved by making even minor changes to the default workflow or settings.


## Corpus Directory

The corpus directory should contain one directory per patient, with that patient's text documents inside the patient directory.

```text
example_corpus/
  Patient_001/
    note_001.txt
    note_002.txt
  Patient_002/
    note_001.txt
```

## OMOP Database

The OMOP Database is expected to follow the [Common Data Model (CDM)](https://www.ohdsi.org/data-standardization/) 
and have tables similar to what is outlined in the [official schema](https://ohdsi.github.io/CommonDataModel/cdm54.html). 
Please note that your institution's data repository and/or your view of that repository may not follow the OMOP CDM standard.

## Output Directory

Use stable, writable output directories for real runs. The NLP step and data merge step both write files under the selected output directory.
