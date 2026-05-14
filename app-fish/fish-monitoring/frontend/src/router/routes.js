const routes = [
  {
    path: '/',
    component: () => import('layouts/MainLayout.vue'),
    children: [
      { path: '', component: () => import('pages/IndexPage.vue') },
      {path: '/struct_enterprise', name: "StructEnterprise", component: () => import('pages/struct_enterprise/StructEnterprise.vue')},
      {path: '/kato', name: "KatoPage", component: () => import('pages/kato/KatoPage.vue')},
      {
        path: '/reservoirs',
        name: 'ReservoirsPage',
        component: () => import('pages/reservoirs/ReservoirsPage.vue'),
      },

      {
        path: '/samplingstations',
        name: 'SamplingStationsPage',
        component: () => import('pages/samplingstations/SamplingStationsPage.vue'),
        //component: () => import('pages/samplingstations/StationAreaTabs.vue'),
      },

      {
        path: '/typesfish',
        name: 'TypesFishPage',
        component: () => import('pages/typesfish/TypesFishPage.vue'),
      },

      {
        path: '/fishGear',
        name: 'FishGearPage',
        component: () => import('pages/fishgear/FishGearPage.vue'),
      },

      {
        path: '/piscesreservoirs',
        name: 'PiscesReservoirsTabs',
        component: () => import('pages/piscesreservoirs/PiscesReservoirsPage.vue'),
      },

      {
        path: '/sampling',
        name: 'SamplingPageTabs',
        component: () => import('pages/sampling/SamplingPageTabs.vue'),
      },
      {
        path: '/samplingresults',
        name: 'SamplingResPageTabs',
        component: () => import('pages/samplingresults/SamplingResPageTabs.vue'),
      },

      { path: '/test', name: 'TestPage', component: () => import('pages/test/TestPage.vue') },
    ],
  },

  // Always leave this as last one,
  // but you can also remove it
  {
    path: '/:catchAll(.*)*',
    component: () => import('pages/ErrorNotFound.vue'),
  },
]

export default routes
