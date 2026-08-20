const routes = [
  {
    path: '/',
    component: () => import('layouts/MainLayout.vue'),
    children: [
      { path: '', component: () => import('pages/IndexPage.vue') },

      {
        path: '/reservoirs',
        name: 'FilialsPage',
        component: () => import('pages/reservoirs/Filials.vue'),
      },
      {
        path: '/reservoirs/:filial',
        name: 'ReservoirFilialPage',
        component: () => import('pages/reservoirs/ReservoirsFilialPage.vue'),
      },

      { path: '/samplingstations', name: 'SamplingStationsPage', component: () => import('../pages/samplingstations/SamplingStationsPage.vue')},


      {
        path: '/typesfish',
        name: 'TypesFishPage',
        component: () => import('pages/typesfish/TypesFishPage.vue'),
      },

      {
        path: '/piscesreservoirs',
        name: 'PiscesReservoirsTabs',
        component: () => import('pages/piscesreservoirs/PiscesReservoirsPage.vue'),
      },

      {
        path: '/fishing',
        name: 'FishingPage',
        component: () => import('pages/fishing/FishingPage.vue'),
      },

      { path: '/fill', name: 'TestPage', component: () => import('pages/test/TestPage.vue') },
      { path: '/chart1', component: () => import('pages/charts/Chart1Page.vue') },
      { path: '/chart7', component: () => import('pages/charts/Chart7Page.vue') },
    ],
  },


  {
    path: "/confirm-pws",
    component: () => import("pages/ConfirmPassword.vue")
  },

  // Always leave this as last one,
  // but you can also remove it
  {
    path: '/:catchAll(.*)*',
    component: () => import('pages/ErrorNotFound.vue'),
  },
]

export default routes
