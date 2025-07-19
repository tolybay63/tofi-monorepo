const routes = [
  {

    path: "/",
    component: () => import("layouts/MainLayout.vue"),
    children: [
      {
        path: "",
        name: "Home",
        component: () => import("pages/IndexPage.vue"),
      },
      {
        path: "/roles/:role",
        name: "Roles",
        component: () => import("pages/roles/RolePage.vue"),
      },
      {
        path: "/roles/:role",
        name: "RoleSelected",
        component: () => import("pages/roles/RoleSelectedPage.vue"),
      },
      {
        path: "/users/:userGr/:user",
        name: "Users",
        component: () => import("pages/users/UserPage.vue"),
      },
      {
        path: "/users/:userGr/:user",
        name: "UserSelected",
        component: () => import("pages/users/UserSelectedPage.vue"),
      },
      {
        path: "/permis",
        name: "Permis",
        component: () => import("pages/permis/PermisPage.vue"),
      },
    ],


  },

  // Always leave this as last one,
  // but you can also remove it
  {
    path: '/:catchAll(.*)*',
    component: () => import('pages/ErrorNotFound.vue')
  }
]

export default routes
