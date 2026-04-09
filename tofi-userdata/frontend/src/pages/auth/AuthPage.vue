<template>
<q-page>

</q-page>

</template>

<script>

import LoginUser from "components/LoginUser.vue";
import {useUserStore} from "stores/user-store";
import {useRouter} from "vue-router";

const router = useRouter()

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
          setUserStore(r)
          router.push('/')

        });

    }
  },

  created() {
    this.login()
  }

}
</script>
