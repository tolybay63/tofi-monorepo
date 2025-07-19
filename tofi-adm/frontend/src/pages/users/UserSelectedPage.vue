<template>
  <div class="q-pa-sm-sm bg-green-1">
    <q-tabs v-model="tab" class="text-teal">
      <div style="margin-left: 20px">
        {{ $t("user") }}:
        <span style="color: black; margin-left: 10px">
          <strong>{{ this.infoUser() }}</strong>
        </span>
      </div>

      <q-space />
      <q-btn
        dense
        round
        icon="arrow_back"
        color="secondary"
        class="q-mr-md"
        @click="toBack()"
      >
        <q-tooltip transition-show="rotate" transition-hide="rotate">
          {{ $t("back") }}
        </q-tooltip>
      </q-btn>
      <q-tab
        name="role"
        no-caps
        icon="manage_accounts"
        :label="$t('user_roles')"
        class="q-mr-md"
      />
      <q-tab
        name="permis"
        no-caps
        icon="task"
        :label="$t('user_privileges')"
        style="margin-right: 10px"
      />
    </q-tabs>

    <q-tab-panels v-model="tab" animated>
      <q-tab-panel
        name="role"
        style="height: calc(100vh - 200px); width: 100%"
      >
        <user-role :user="user" />
      </q-tab-panel>

      <q-tab-panel
        name="permis"
        style="height: calc(100vh - 200px); width: 100%"
      >
        <user-permis />
      </q-tab-panel>
    </q-tab-panels>
  </div>
</template>

<script>
import {ref} from "vue";
import {api, baseURL} from "boot/axios";
import {notifyError} from "src/utils/jsutils";
import UserRole from "pages/users/UserRole.vue";
import UserPermis from "pages/users/UserPermis.vue";

export default {
  name: "UserSelectedPage",
  components: { UserPermis, UserRole },

  data: function () {
    return {
      userGr_id: 0,
      user_id: 0,
      user: {},
    };
  },

  methods: {
    toBack() {
      this.$router.push({
        name: "Users",
        params: {
          user: this.user_id,
          userGr: this.userGr_id,
        },
      });
    },

    infoUser() {
      return this.user.fullName;
    },
  },

  mounted() {
    this.user_id = parseInt(this.$route.params.user, 10);
    this.userGr_id = parseInt(this.$route.params.userGr, 10);

    // load user
    this.loading = ref(true);
    api
      .post(baseURL, {
        method: "usr/loadUser",
        params: [this.user_id],
      })
      .then((response) => {
        this.user = response.data.result.records[0];
      })
      .catch((error) => {
        notifyError(error.message);
      })
      .finally(() => {
        this.loading = ref(false);
      });
    //
  },

  created() {
    //console.log("<<<<<  created")
    this.lang = localStorage.getItem("curLang");
    this.lang = this.lang === "en-US" ? "en" : this.lang;
  },

  computed: {},

  setup() {
    return {
      tab: ref("role"),
    };
  },
};
</script>

<style scoped></style>
