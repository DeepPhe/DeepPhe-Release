---
id: intro
slug: /
title: DeepPhe Desktop GUI Guide
sidebar_label: Overview
---

import useBaseUrl from '@docusaurus/useBaseUrl';

The DeepPhe Desktop GUI is the launcher for the local DeepPhe workflow. Use it to choose a project, point DeepPhe at a text corpus and OMOP demographics file, run the NLP summarizer, merge the output into a visualization database, and open the cohort visualizer.

```mermaid
flowchart LR
  corpus["Patient document corpus"] --> settings["Project settings"]
  omop["OMOP demographics JSON"] --> settings
  piper["Piper pipeline"] --> settings
  settings --> nlp["NLP Summarizer"]
  nlp --> output["DeepPhe NLP output"]
  output --> merge["Data Merge Tool"]
  omop --> merge
  merge --> db["Visualizer database"]
  db --> viz["Visualization GUI"]
```

## The Main Buttons

<div className="tool-grid">
  <div className="tool-card">
    <img src={useBaseUrl('/img/app-icons/NLP_3_100.png')} alt="NLP Summarizer icon" />
    <strong>NLP Summarizer</strong>
    Runs the selected Piper pipeline over the selected corpus and writes DeepPhe output to the project output directory.
  </div>
  <div className="tool-card">
    <img src={useBaseUrl('/img/app-icons/ETL_3_100.png')} alt="Data Merge Tool icon" />
    <strong>Data Merge Tool</strong>
    Combines NLP output and OMOP demographics into the database used by the visualization services.
  </div>
  <div className="tool-card">
    <img src={useBaseUrl('/img/app-icons/Viz_4_100.png')} alt="Visualization GUI icon" />
    <strong>Visualization GUI</strong>
    Starts the local Data API and visualizer, then opens the visualizer in your browser.
  </div>
  <div className="tool-card">
    <img src={useBaseUrl('/img/app-icons/Wiki_1_80.png')} alt="Wiki icon" />
    <strong>Wiki</strong>
    Opens DeepPhe manuals and release documentation.
  </div>
  <div className="tool-card">
    <img src={useBaseUrl('/img/app-icons/Website_1_80.png')} alt="Website icon" />
    <strong>Web Site</strong>
    Opens the public DeepPhe site.
  </div>
</div>

## Typical Session

1. Launch the DeepPhe Desktop GUI.
2. Select or create a project.
3. Confirm the Piper file, corpus directory, OMOP database JSON, and output directory.
4. Click <span className="button-chip">NLP Summarizer</span>.
5. Click <span className="button-chip">Data Merge Tool</span>.
6. Click <span className="button-chip">Visualizer Startup</span> to open the browser-based visualizer.
7. Click <span className="button-chip">Visualizer Shutdown</span> when finished.

The activity log at the bottom of the window shows commands, startup progress, errors, and the location of tool logs.
