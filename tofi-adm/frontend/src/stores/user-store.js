import {defineStore} from "pinia";


export const useUserStore = defineStore("user", {
  state: () => {
    let ii = sessionStorage.getItem("userid") || 0;
    let nn = sessionStorage.getItem("username") || "";
    let tt = sessionStorage.getItem("target") || "";

    return {
      user: {
        id: ii !== 0 ? ii : 0,
        name: nn !== "" ? nn : "",
        target: tt !== "" ? tt : "",
      },
    };
  },

  getters: {
    getUserId: (state) => state.user.id,
    isSysAdmin: (state) => parseInt(state.user.id, 10) === 1,
    getUserName: (state) => state.user.name,
    getTarget: (state) =>
      state.user.target ? state.user.target.split(",") : "",
  },

  actions: {
    setUserName(name) {
      this.user.name = name;
    },

    setUserStore(data) {
      //console.info("setUserStore", data)
      if (JSON.stringify(data) !== "{}") {
        this.user.id = data.id;
        this.user.name = data.fullname;
        this.user.target = data.target;

        sessionStorage.setItem("userid", data.id.toString());
        sessionStorage.setItem("username", data.fullname);
        sessionStorage.setItem("target", data.target);
      } else {
        sessionStorage.removeItem("userid");
        sessionStorage.removeItem("username");
        sessionStorage.removeItem("target");
      }
    },
  },
});
