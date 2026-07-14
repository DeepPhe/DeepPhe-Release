// @ts-check
// Sidebar for the DeepPhe Desktop GUI guide.

/** @type {import('@docusaurus/plugin-content-docs').SidebarsConfig} */
const sidebars = {
  docsSidebar: [
    'intro',
    {
      type: 'category',
      label: 'Getting Started',
      collapsed: false,
      items: ['getting-started/install-launch'],
    },
    {
      type: 'category',
      label: 'Using the GUI',
      collapsed: false,
      items: [
        'using-the-gui/project-setup',
        'using-the-gui/run-workflow',
        'using-the-gui/visualizer',
      ],
    },
    {
      type: 'category',
      label: 'Reference',
      collapsed: false,
      items: [
        'reference/paths-config',
        'reference/troubleshooting',
        'reference/publishing',
      ],
    },
  ],
};

export default sidebars;
