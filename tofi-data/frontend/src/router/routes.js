const routes = [
  {
    path: "/",
    component: () => import("layouts/MainLayout.vue"),
    children: [
      { path: "", component: () => import("pages/IndexPage.vue") },
      {
        path: "/std",
        name: "OwnersStdPage",
        component: () => import("pages/std/OwnersStdPage.vue"),
      },
      {
        path: "/std/:owner/:isObj/:typORrel",
        name: "StdInputPage",
        component: () => import("pages/std/DataStd.vue"),
      },
      {
        path: "/multi",
        name: "OwnersMultiPage",
        component: () => import("pages/multi/OwnersMultiPage.vue"),
      },

      {
        path: "/multi/:owner/:isObj/:typORrel",
        name: "MultiInputPage",
        component: () => import("pages/multi/DataMulti.vue"),
      },

      {
        path: "/flat",
        name: "OwnersFlatPage",
        component: () => import("pages/flat/OwnersFlatPage.vue"),
      },

/*
      {
        path: "/dataFlat/:owner/:isObj/:typORrel",
        name: "FlatInputPage",
        component: () => import("pages/flat/DataFlat.vue"),
      },
*/

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
