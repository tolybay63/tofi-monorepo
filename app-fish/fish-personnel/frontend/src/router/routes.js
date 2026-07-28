const routes = [
  {
    path: '/',
    component: () => import('../layouts/MainLayout.vue'),
    children: [
      { path: '', component: () => import('../pages/IndexPage.vue') },
      { path: '/struct_enterprise', name: "StructEnterprisePage", component: () => import('../pages/struct_enterprise/StructEnterprise.vue')},
      { path: '/struct_enterprise2', name: "StructEnterprisePage2", component: () => import('../pages/struct_enterprise/StructEnterprise2.vue')},
      { path: '/personnel', name: "PersonnelPage", component: () => import('../pages/personnel/PesonnelPage.vue')},

    ],
  },

  {
    path: "/confirm-pws",
    component: () => import("../pages/ConfirmPassword.vue")
  },


  // Always leave this as last one,
  // but you can also remove it
  {
    path: '/:catchAll(.*)*',
    component: () => import('../pages/ErrorNotFound.vue'),
  },
]

export default routes
