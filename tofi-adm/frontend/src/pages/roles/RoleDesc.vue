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

      <q-inner-loading :showing="loading" color="secondary" />

    </q-card-section>
  </q-card>
</template>

<script>
import {api, } from "boot/axios";
import {notifyError} from "src/utils/jsutils";

export default {
  name: "RoleDesc",
  props: ["role"],

  data() {
    return {
      permis: "",
      loading: false,
    };
  },

  mounted() {
    let role_id = this.$route["params"].role;
    this.loading = true;
    api
      .post("", {
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
        this.loading = false;
      });

    //this.fetchData(requestParam);
  },

  created() {
    //console.log("created!", this.role);
  },
};
</script>

<style scoped></style>
