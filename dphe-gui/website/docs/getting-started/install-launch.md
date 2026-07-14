---
title: Install and Launch
---

The GUI is normally launched from a DeepPhe desktop installation. The launcher starts a Java Swing application named `org.healthnlp.deepphe.gui.DpheDesktop`.

## Requirements

- A complete DeepPhe installation with the `.DeepPhe` tool directory.
- Java 8 or newer. Installed desktop bundles usually provide their own Java runtime.
- Writable project, log, and output directories.
- Ports `3333` and `3334` available when using the visualizer.

## Launch from an Installed DeepPhe App

Open the DeepPhe Desktop application from the operating system shortcut or application launcher. The GUI opens with a welcome message and a project panel at the top.

The default example project expects sample data under:

```text
~/DeepPheDocs/
```

Installer-created examples usually include:

```text
~/DeepPheDocs/example_corpus/
~/DeepPheDocs/example_omop/patient_demographics.json
~/DeepPheDocs/example_output/
```

## Launch from the Repository or a Distribution Folder

On macOS or Linux:

```bash
./runDeepPheDesktop.sh
```

On Windows:

```batch
runDeepPheDesktop.bat
```

The scripts derive `DEEPPHE_ROOT` from the script location when it is not already set. They also derive `DEEPPHE_HOME` as:

```text
%DEEPPHE_ROOT%\.DeepPhe
```

or, on macOS and Linux:

```text
$DEEPPHE_ROOT/.DeepPhe
```

Set `JAVA_HOME` before launching if you are running from scripts on a machine without a bundled Java runtime.

## Optional Desktop Configuration File

The Java entry point accepts one optional argument: a desktop configuration file.

```bash
./runDeepPheDesktop.sh /path/to/desktop-config.txt
```

Use that file only when you need to override installation paths or tool commands. See [Paths and Configuration](../reference/paths-config.md).
