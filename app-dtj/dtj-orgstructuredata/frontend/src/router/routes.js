const routes = [
  {
    path: '/',
    component: () => import('layouts/MainLayout.vue'),
    children: [
      { path: '', component: () => import('pages/IndexPage.vue') },
      {path: '/types', name: "TestTypPage", component: () => import('pages/test/TestTypPage.vue')},
      {path: '/objects', name: "TestObjPage", component: () => import('pages/test/TestObjPage.vue')},


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
