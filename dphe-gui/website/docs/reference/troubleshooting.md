---
title: Troubleshooting
---

Use the Desktop Activity Log first. It records the command being started, readiness checks, exit codes, and common path errors.

## The GUI Opens, But a Tool Button Fails

Check the project table:

- The Piper file must exist.
- The corpus directory must exist and contain patient document directories.
- The OMOP database must be a readable JSON file.
- The output directory must be writable.

Also check that the configured tool directory exists under `.DeepPhe/tools/`.

## "Visualizer Database Not Found"

Run <span className="button-chip">Data Merge Tool</span> before starting the visualizer.

The visualizer startup check looks for:

```text
<docs-directory>/visualizer_database/deepphe.sqlite3
```

The docs directory is usually the parent of the selected output directory. If the selected output directory is named `json`, the GUI uses the grandparent directory.

Examples:

| Output directory | Database expected at |
| --- | --- |
| `~/DeepPheDocs/example_output` | `~/DeepPheDocs/visualizer_database/deepphe.sqlite3` |
| `~/DeepPheDocs/run1/json` | `~/DeepPheDocs/visualizer_database/deepphe.sqlite3` |

If the file is missing after a merge, review the Data Merge Tool log in `.DeepPhe/logs`.

## Port 3333 or 3334 Is Already in Use

The visualizer needs:

```text
127.0.0.1:3333
127.0.0.1:3334
```

Close any previous DeepPhe visualizer session, stop the process using the port, or restart the workstation.

## Java Is Not Found

Installed DeepPhe desktop bundles should launch with their bundled Java runtime. If you are running the scripts manually, set `JAVA_HOME` to a Java 8 or newer installation before launching.

On macOS or Linux:

```bash
export JAVA_HOME=/path/to/jdk
./runDeepPheDesktop.sh
```

On Windows:

```batch
set JAVA_HOME=C:\Path\To\JDK
runDeepPheDesktop.bat
```

## NLP Output Is Empty or Unexpected

Confirm that:

- The corpus directory points at patient directories, not an individual note file.
- The selected Piper file is the intended pipeline.
- The input documents are readable plain text.
- The output directory does not contain stale results from a different project.

For a clean rerun, move or archive the previous output directory before starting the NLP step.

## The Browser Does Not Open

If the activity log says the visualizer started successfully, open this URL manually:

```text
http://127.0.0.1:3334
```

If the page does not load, check the Data API and visualizer logs under `.DeepPhe/logs`.
