---
title: Run the DeepPhe Workflow
---

Run the buttons from left to right for a complete local workflow.

## 1. Confirm Project Settings

Before starting a run, check:

- The Piper file exists and matches the pipeline you want.
- The corpus directory contains patient directories and readable documents.
- The OMOP database path points to the demographics JSON file.
- The output directory is writable and has enough disk space.

## 2. Run the NLP Summarizer

Click <span className="button-chip">NLP Summarizer</span>.

The GUI writes a small CLI parameter file for the selected project, then starts the DeepPhe NLP tool with the selected Piper file:

```bash
bin/runDeepPheGUI -p <PIPER_FILE> -c <PROJECT_NAME>.cli
```

The generated CLI file contains:

```ini
InputDirectory=<CORPUS_DIR>
OutputDirectory=<OUTPUT_DIR>
```

Progress and errors appear in the Desktop Activity Log. Detailed tool output is written to the DeepPhe log directory.

## 3. Run the Data Merge Tool

Click <span className="button-chip">Data Merge Tool</span>.

This step combines the NLP output with the OMOP demographics JSON and prepares data for visualization. The GUI passes three arguments to the merge tool:

```bash
runDbCreator <OUTPUT_DIR> <OMOP_DB> <OUTPUT_DIR>/vizDb/<PROJECT_NAME>
```

Wait for the activity log to report that the merge completed successfully before starting the visualizer.

## 4. Start the Visualization GUI

Click <span className="button-chip">Visualizer Startup</span>.

The GUI starts two local services:

| Service | URL | Purpose |
| --- | --- | --- |
| DeepPhe Data API | `http://127.0.0.1:3333` | Serves the visualization database. |
| DeepPhe Visualizer | `http://127.0.0.1:3334` | Browser-based cohort and patient interface. |

After both services pass their health checks, the GUI opens:

```text
http://127.0.0.1:3334
```

## 5. Shut Down the Visualizer

When the visualizer is running, the button label changes to <span className="button-chip">Visualizer Shutdown</span>. Click it before exiting if you want to stop the local Data API and visualizer immediately.

The GUI also tries to stop visualization services when the desktop app exits.

<div className="workflow-note">
The visualizer uses local ports only. It does not publish patient data to the public internet, but you should still follow your institution's rules for handling clinical text and derived data on the workstation.
</div>
