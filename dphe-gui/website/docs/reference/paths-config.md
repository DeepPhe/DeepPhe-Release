---
title: Paths and Configuration
---

The GUI derives most paths from `DEEPPHE_ROOT`. If `DEEPPHE_ROOT` is not set, the Java app uses the parent of the current working directory as its root.

## Important Directories

| Name | Default relative to `DEEPPHE_ROOT` | Purpose |
| --- | --- | --- |
| DeepPhe tools | `.DeepPhe` | NLP tool home. |
| Data Merge Tool | `.DeepPhe/tools/dphe-pipeline` | Converts NLP output and OMOP data for visualization. |
| Data API | `.DeepPhe/tools/dphe-data-api` | Serves the visualizer database. |
| Visualizer | `.DeepPhe/tools/deepphe-visualizer-v2` | Browser UI for cohort and patient review. |
| Logs | `.DeepPhe/logs` | Tool and service logs. |
| Projects | `.DeepPhe/resources/projects` | Project list, project files, and generated CLI parameter files. |

## Default Commands

| Tool | Default command |
| --- | --- |
| NLP Summarizer | `bin/runDeepPheGUI` |
| Data Merge Tool | `runDbCreator` |
| Data API | `dphe-data-api` |
| Visualizer | `start-viz` |

Relative commands are resolved under each tool's configured directory.

## Desktop Configuration File

You can pass one configuration file to the launcher:

```bash
./runDeepPheDesktop.sh /path/to/desktop-config.txt
```

Configuration files use `NAME=value` lines. Blank lines and lines beginning with `//` are ignored.

```ini
DpheDir=/opt/deepphe/.DeepPhe
VizDir=/opt/deepphe/.DeepPhe/tools/deepphe-visualizer-v2
ProjectsDir=/opt/deepphe/.DeepPhe/resources/projects
StartViz=start-viz
```

The GUI accepts several legacy aliases for each value. For example, the visualizer directory can be configured with `VizDir`, `VisDir`, `VizualizerDir`, or `VisualizerDir`.

## Project Files

Project files also use `NAME=value` lines, but they store the selected data paths for a run:

```ini
PROJECT_NAME=ExampleProject
PIPER_FILE=/path/to/DefaultDeepPhe.piper
CORPUS_DIR=/path/to/example_corpus
OMOP_DB=/path/to/patient_demographics.json
OUTPUT_DIR=/path/to/example_output
```

Project files are written when the app exits and when you switch projects.
