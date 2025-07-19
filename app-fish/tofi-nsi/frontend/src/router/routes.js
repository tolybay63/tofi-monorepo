const routes = [
  {
    path: '/',
    component: () => import('layouts/MainLayout.vue'),
    children: [
      { path: '', component: () => import('pages/IndexPage.vue') },
      {path: '/fishing_tools', name: "FishingToolsPage", component: () => import('pages/fishing_tools/FishingToolsPage.vue')},
      {path: '/desc_techniques', name: "DescTechniquesPage", component: () => import('pages/desc_techniques/DescTechniquesPage.vue')},
      {path: '/regulatory_docs', name: "RegulatoryDocsPage", component: () => import('pages/regulatory_docs/RegulatoryDocsPage.vue')},
      {path: '/kato', name: "KatoPage", component: () => import('pages/kato/KatoPage.vue')},
      {path: '/struct_enterprise', name: "StructEnterprise", component: () => import('pages/struct_enterprise/StructEnterprise.vue')},

    ]
  },

  // Always leave this as last one,
  // but you can also remove it
  {
    path: '/:catchAll(.*)*',
    component: () => import('pages/ErrorNotFound.vue')
  }
]

export default routes
