const routes = [
  {
    path: '/',
    component: () => import('@/layouts/MainLayout.vue'),
    children: [
      {path: '', component: () => import('@/pages/IndexPage.vue') },
      {path: '/calc_stock', component: () => import('@/pages/calcstock/CalcStockPage.vue')},
      {path: '/calcA', component: () => import('@/pages/CalcA.vue')},
      {path: '/calcB', component: () => import('@/pages/CalcB.vue')},
    ],
  },

  {
    path: "/confirm-pws",
    component: () => import("@/pages/ConfirmPassword.vue")
  },


  // Always leave this as last one,
  // but you can also remove it
  {
    path: '/:catchAll(.*)*',
    component: () => import('@/pages/ErrorNotFound.vue'),
  },
]

export default routes
