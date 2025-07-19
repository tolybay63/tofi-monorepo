<template>
  <q-card class="bg-amber-1">
    <q-card-section>
      <div class="row">
        <q-item> {{ $t("fldName") }}:</q-item>
        <q-separator></q-separator>
        <q-item>
          <strong> {{ role.name }} </strong>
        </q-item>
      </div>

      <div class="row">
        <q-item> {{ $t("fldFullName") }}:</q-item>
        <q-separator></q-separator>
        <q-item>
          <strong> {{ role.fullName }} </strong>
        </q-item>
      </div>

      <div class="row">
        <q-item> {{ $t("role_privileges") }}:</q-item>
        <q-separator></q-separator>
        <q-item>
          <strong> {{ permis }} </strong>
        </q-item>
      </div>
    </q-card-section>
  </q-card>
</template>

<script>
import {ref} from "vue";
import {api, baseURL} from "boot/axios";
import {notifyError} from "src/utils/jsutils";

export default {
  name: "RoleDesc",
  props: ["role"],

  data() {
    return {
      permis: "",
    };
  },

  mounted() {
    let role_id = this.$route.params.role;
    this.loading = ref(true);
    api
      .post(baseURL, {
        method: "role/getRolePermis",
        params: [role_id],
      })
      .then((response) => {
        this.permis = response.data.result;
      })
      .catch((error) => {
        notifyError(error.message);
      })
      .finally(() => {
        this.loading = ref(false);
      });

    //this.fetchData(requestParam);
  },

  created() {
    //console.log("created!", this.role);
  },
};
</script>

<style scoped></style>
