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
        path: '/piscesreservoirs',
        name: 'PiscesReservoirsTabs',
        component: () => import('pages/piscesreservoirs/PiscesReservoirsTabs.vue'),
      },
      {
        path: '/typesfish',
        name: 'TypesFishPage',
        component: () => import('pages/typesfish/TypesFishPage.vue'),
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
      {
        path: '/samplingstations',
        name: 'StationAreaTabs',
        component: () => import('pages/samplingstations/StationAreaTabs.vue'),
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
