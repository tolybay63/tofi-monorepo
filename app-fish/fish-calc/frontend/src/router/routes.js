const routes = [
  {
    path: '/',
    component: () => import('@/layouts/MainLayout.vue'),
    children: [
      {path: '', component: () => import('@/pages/IndexPage.vue') },

      {path: '/calc_determ',
        name: 'CalcStockDeterm',
        component: () => import('@/pages/calcstock/CalcStockDeterm.vue')
      },

      {path: '/calc_bayes',
        name: 'CalcStockBayes',
        component: () => import('@/pages/calcstock/CalcStockBayes.vue')
      },

      {path: '/calculation_determ/:id/:title',
        name: 'CalculationDeterm',
        component: () => import('@/pages/calcstock/CalculationDeterm.vue')
      },
      {path: '/calculation_bayes/:id/:title',
        name: 'CalculationBayes',
        component: () => import('@/pages/calcstock/CalculationBayes.vue')
      },


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
