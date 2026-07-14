# DeepPhe Desktop GUI Docs

This Docusaurus site documents how to use the DeepPhe Desktop GUI. It is configured as a GitHub Pages project site at:

https://deepphe.github.io/dphe-gui/

## Local development

```bash
npm install
npm run start
```

## Production build

```bash
npm run build
```

## Publishing

The repository workflow in `.github/workflows/deploy-docs.yml` builds this directory and deploys `website/build` to GitHub Pages when changes land on `main`.
