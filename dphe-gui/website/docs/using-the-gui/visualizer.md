---
title: Visualization GUI
---

The Visualization GUI button is a startup and shutdown toggle for the local browser-based visualizer.

## Startup Sequence

When you click <span className="button-chip">Visualizer Startup</span>, the desktop app:

1. Checks that the visualizer database exists.
2. Checks that ports `3333` and `3334` are available on `127.0.0.1`.
3. Starts the DeepPhe Data API on port `3333`.
4. Waits for `http://127.0.0.1:3333/openapi.json` to respond.
5. Starts the DeepPhe Visualizer on port `3334`.
6. Waits for `http://127.0.0.1:3334/healthz` to respond.
7. Opens `http://127.0.0.1:3334` in your browser.

If any check fails, the activity log explains what stopped startup.

## Runtime Settings

The GUI sets these environment variables for the visualizer services:

| Variable | Value |
| --- | --- |
| `PORT` for the Data API | `3333` |
| `PORT` for the visualizer | `3334` |
| `DEEPPHE_API_LOCATION` | `http://127.0.0.1:3333` |
| `PIPER_ACTIVE_FILE` | The selected Piper file name. |
| `PIPER_FILES_DIR` | The installed Piper directory when available, otherwise the selected Piper file's directory. |

## Browser Behavior

The desktop app opens your default browser after the local services are ready. If the browser does not open, manually visit:

```text
http://127.0.0.1:3334
```

## Shutdown Behavior

Click <span className="button-chip">Visualizer Shutdown</span> to stop services in order:

1. DeepPhe Visualizer.
2. DeepPhe Data API.

The activity log shows service exit codes and any shutdown warnings.
