const routes = [
  {
    path: '/',
    component: () => import('../layouts/MainLayout.vue'),
    children: [
      { path: '', component: () => import('../pages/IndexPage.vue') },
      { path: '/struct_enterprise', name: "PesonnelPage", component: () => import('../pages/struct_enterprise/StructEnterprise.vue')},
      { path: '/personnel', name: "StructEnterprisePage", component: () => import('../pages/personnel/PesonnelPage.vue')},

    ],
  },

  // Always leave this as last one,
  // but you can also remove it
  {
    path: '/:catchAll(.*)*',
    component: () => import('../pages/ErrorNotFound.vue'),
  },
]

export default routes
