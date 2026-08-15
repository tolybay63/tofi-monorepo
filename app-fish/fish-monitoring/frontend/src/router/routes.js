const routes = [
  {
    path: '/',
    component: () => import('layouts/MainLayout.vue'),
    children: [
      { path: '', component: () => import('pages/IndexPage.vue') },

      {
        path: '/reservoirs',
        name: 'ReservoirsPage',
        component: () => import('pages/reservoirs/ReservoirsPage.vue'),
      },

      {
        path: '/reservoirs2',
        name: 'ReservoirsPage2',
        component: () => import('pages/reservoirs/ReservoirsPage2.vue'),
      },


      {
        path: '/typesfish',
        name: 'TypesFishPage',
        component: () => import('pages/typesfish/TypesFishPage.vue'),
      },

/*      {
        path: '/fishGear',
        name: 'FishGearPage',
        component: () => import('pages/fishgear/FishGearPage.vue'),
      },*/

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
