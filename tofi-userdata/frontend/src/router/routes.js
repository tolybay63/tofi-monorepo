
const routes = [
  {
    path: '/',
    component: () => import('layouts/MainLayout.vue'),
    children: [
      {
        path: "",
        name: "home",
        component: () => import("pages/IndexPage.vue"),
      },
      {
        path: "/auth",
        name: "LoginPage",
        component: () => import("pages/auth/AuthPage.vue"),
      },
      {
        path: "/profile/:au",
        name: "ProfilePage",
        component: () => import("pages/auth/ProfilePage.vue"),
      },
      {
        path: "/myprofile",
        name: "MyProfilePage",
        component: () => import("pages/page/MyProfilePage.vue"),
      },
      {
        path: "/groupuser",
        name: "GroupUserPage",
        component: () => import("pages/page/GroupUserPage.vue"),
      },
      {
        path: "/accounts",
        name: "AccountsPage",
        component: () => import("pages/page/AccountsPage.vue"),
      },

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
