<template>
  <q-page padding>
    <!-- content -->
  </q-page>

  <q-inner-loading
    :showing="loading"
  />
</template>

<script>
import {useUserStore} from "stores/user-store";
import ProfileUser from "components/ProfileUser.vue";
import {storeToRefs} from "pinia";
import {ref} from "vue";
import {baseURL} from "boot/axios.js";
import {notifyError} from "src/utils/jsutils.js";

export default {
  name: 'ProfilePage',

  data() {
    return {
      au: 0,
      loading: ref(false)

    }
  },

  methods: {
    createProfile() {
      this.$q
          .dialog({
            component: ProfileUser,
            componentProps: {
              userId: this.au,
              // ...
            },
          })
          .onOk((r) => {
            this.$router["push"]("/myprofile")
          })
        .onCancel(()=> {
          this.$router["push"]("/")
        })
    },

  },

  mounted() {
    this.au = parseInt(this.$route.params.au, 10);
    //
    if (this.au===1) {
      this.$router["push"]("/accounts")
    }
    //
    const store = useUserStore()
    const {setUserName} = store;

    this.loading = ref(true);
    this.$axios
      .post(baseURL, {
        method: "data/existsProfile",
        params: [this.au],
      })
      .then(
        (response) => {
          if (!response.data.result.exists)
            this.createProfile()
          else {
            setUserName(response.data.result.name)
            this.$router.push("/myprofile")

          }

        },
        (error) => {
          let msgs = error.response.data.error.message.split("@")
          let m1 = this.$t(`${msgs[0]}`)
          let m2 = (msgs.length>1) ? " ["+msgs[1]+"]" : ""
          let msg = m1 + m2
          notifyError(msg);
        }
      )
      .finally(() => {
        this.loading = ref(false);
      });


  },


  created() {
  }

}
</script>
