---
title: Publishing This Site
---

The docs site lives under `website/` and is configured for GitHub Pages at:

```text
https://deepphe.github.io/dphe-gui/
```

The important Docusaurus settings are:

```js
url: 'https://deepphe.github.io',
baseUrl: '/dphe-gui/',
organizationName: 'DeepPhe',
projectName: 'dphe-gui',
```

## Run Locally

```bash
cd website
npm install
npm run start
```

## Build Locally

```bash
cd website
npm run build
```

The production output is written to:

```text
website/build/
```

## GitHub Pages Workflow

The repository includes:

```text
.github/workflows/deploy-docs.yml
```

On pushes to `main` that change the docs site, the workflow:

1. Installs Node.js.
2. Runs `npm ci` in `website/`.
3. Runs `npm run build`.
4. Uploads `website/build` as a GitHub Pages artifact.
5. Deploys the artifact with `actions/deploy-pages`.

In the GitHub repository settings, Pages should be configured to deploy from GitHub Actions.
