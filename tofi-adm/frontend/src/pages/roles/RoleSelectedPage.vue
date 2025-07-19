<template>
  <q-page class="q-pa-sm-sm bg-green-1">
    <q-tabs v-model="tab" class="text-teal">
      <div style="margin-left: 20px">
        {{ $t("role2") }}:
        <span style="color: black; margin-left: 10px">
          <strong>{{ this.infoRole() }}</strong>
        </span>
      </div>

      <q-space />
      <q-btn dense round icon="arrow_back" color="secondary" @click="toBack()">
        <q-tooltip transition-show="rotate" transition-hide="rotate">
          {{ $t("back") }}
        </q-tooltip>
      </q-btn>
      <q-tab name="desc" no-caps icon="pin" :label="$t('description')" />

      <q-tab
        name="permis"
        no-caps
        icon="task"
        :label="$t('role_privileges')"
        style="margin-right: 10px"
      />
    </q-tabs>

    <q-tab-panels v-model="tab" animated>
      <q-tab-panel name="desc" style="height: calc(100vh - 190px); width: 100%">
        <role-desc :role="role" />
      </q-tab-panel>

      <q-tab-panel
        name="permis"
        style="height: calc(100vh - 190px); width: 100%"
      >
        <role-permis />
      </q-tab-panel>
    </q-tab-panels>
  </q-page>
</template>

<script>
import {ref} from "vue";
import {api, baseURL} from "boot/axios";
import RoleDesc from "pages/roles/RoleDesc.vue";
import RolePermis from "pages/roles/RolePermis.vue";
import {notifyError} from "src/utils/jsutils";

export default {
  name: "RoleSelectedPage",
  components: { RoleDesc, RolePermis },

  data: function () {
    return {
      role_id: null,
      role: {},
    };
  },

  methods: {
    toBack() {
      this.$router.push({
        name: "Roles",
        params: {
          role: this.role_id,
        },
      });
    },

    infoRole() {
      return this.role.name;
    },
  },

  mounted() {
    this.role_id = parseInt(this.$route.params.role, 10);

    // load role
    this.loading = ref(true);
    api
      .post(baseURL, {
        method: "role/loadRec",
        params: [this.role_id],
      })
      .then((response) => {
        this.role = response.data.result.records[0];
      })
      .catch((error) => {
        notifyError(error.message);
      })
      .finally(() => {
        this.loading = ref(false);
      });
    //
  },

  computed: {},

  created() {
    //console.log("<<<<<  created")
    this.lang = localStorage.getItem("curLang");
    this.lang = this.lang === "en-US" ? "en" : this.lang;
  },

  setup() {
    return {
      tab: ref("desc"),
    };
  },
};
</script>

<style scoped></style>
