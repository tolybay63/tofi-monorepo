<template>
<q-page>

</q-page>

</template>

<script>

import LoginUser from "components/LoginUser.vue";
import {useUserStore} from "stores/user-store";
import {notifyError} from "src/utils/jsutils";
import {baseURL} from "boot/axios";

export default {
  name: 'LoginPage',

  data() {
    return {
    };
  },

  methods: {

    login() {
      const store = useUserStore();
      const { setUserStore } = store;

      this.$q
        .dialog({
          component: LoginUser,
          componentProps: {
            // ...
          },
        })
        .onOk((r) => {

          this.$axios
            .post(baseURL, {
              method: "data/getCurUserInfo",
              params: [],
            })
            .then(
              (response) => {
                console.info("userId", response.data.result["id"])
                let au = response.data.result["id"]
                setUserStore(response.data.result)

                if (au===1) {
                  this.$router["push"]("/accounts")
                } else {
                  this.$router["push"]({
                    name: "ProfilePage",
                    params: {
                      au: response.data.result["id"],
                    },
                  })
                }

              },
              (error) => {
                //console.log("error", error);
                setUserStore({});
                notifyError(error.response.data.error.message);
                this.$router["push"]("/auth");
              }
            )
            .finally(() => {
              //this.$router["push"]("/profile");
              //location.reload()
            });
        });

    }
  },

  created() {
    this.login()
  }

}
</script>
