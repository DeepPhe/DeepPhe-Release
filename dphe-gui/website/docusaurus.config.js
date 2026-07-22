// @ts-check
// Docusaurus configuration for the DeepPhe Desktop GUI documentation site.

import {themes as prismThemes} from 'prism-react-renderer';

/** @type {import('@docusaurus/types').Config} */
const config = {
  title: 'DeepPhe Desktop GUI',
  tagline: 'Use the DeepPhe desktop tools from one guided launcher',
  favicon: 'img/favicon.svg',

  url: 'https://deepphe.github.io',
  baseUrl: '/dphe-gui/',

  organizationName: 'DeepPhe',
  projectName: 'dphe-gui',
  deploymentBranch: 'gh-pages',

  onBrokenLinks: 'throw',

  markdown: {
    mermaid: true,
    hooks: {
      onBrokenMarkdownLinks: 'warn',
    },
  },
  themes: ['@docusaurus/theme-mermaid'],

  i18n: {
    defaultLocale: 'en',
    locales: ['en'],
  },

  presets: [
    [
      'classic',
      /** @type {import('@docusaurus/preset-classic').Options} */
      ({
        docs: {
          routeBasePath: '/',
          sidebarPath: './sidebars.js',
          editUrl:
            'https://github.com/DeepPhe/dphe-gui/tree/main/website/',
        },
        blog: false,
        theme: {
          customCss: './src/css/custom.css',
        },
      }),
    ],
  ],

  themeConfig:
    /** @type {import('@docusaurus/preset-classic').ThemeConfig} */
    ({
      colorMode: {
        respectPrefersColorScheme: true,
      },
      navbar: {
        title: 'Desktop GUI',
        logo: {
          alt: 'DeepPhe',
          src: 'img/logo.svg',
          srcDark: 'img/logo-dark.svg',
        },
        items: [
          {
            type: 'docSidebar',
            sidebarId: 'docsSidebar',
            position: 'left',
            label: 'Guide',
          },
          {
            href: 'https://github.com/DeepPhe/dphe-gui',
            label: 'GitHub',
            position: 'right',
          },
          {
            href: 'https://deepphe.github.io',
            label: 'DeepPhe Home',
            position: 'right',
          },
        ],
      },
      footer: {
        style: 'dark',
        logo: {
          alt: 'DeepPhe',
          src: 'img/logo-dark.svg',
          srcDark: 'img/logo-dark.svg',
          width: 180,
          height: 43,
        },
        links: [
          {
            title: 'Use the GUI',
            items: [
              {label: 'Overview', to: '/'},
              {label: 'Launch the GUI', to: '/getting-started/install-launch'},
              {label: 'Run a project', to: '/using-the-gui/run-workflow'},
            ],
          },
          {
            title: 'Reference',
            items: [
              {label: 'Paths and configuration', to: '/reference/paths-config'},
              {label: 'Troubleshooting', to: '/reference/troubleshooting'},
              {label: 'Publishing this site', to: '/reference/publishing'},
            ],
          },
          {
            title: 'DeepPhe',
            items: [
              {label: 'Homepage', href: 'https://deepphe.github.io'},
              {
                label: 'DeepPhe Release',
                href: 'https://github.com/DeepPhe/DeepPhe-Release',
              },
            ],
          },
        ],
        copyright: `Boston Children's Hospital · University of Pittsburgh. Built with Docusaurus.`,
      },
      prism: {
        theme: prismThemes.github,
        darkTheme: prismThemes.dracula,
        additionalLanguages: ['bash', 'batch', 'java', 'ini'],
      },
    }),
};

export default config;
