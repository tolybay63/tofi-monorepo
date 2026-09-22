<template>
  <q-page class="q-pa-sm-sm bg-green-1">
    <q-tabs v-model="tab" class="text-teal">
      <div style="margin-left: 20px">
        {{ $t("role2") }}:
        <span style="color: black; margin-left: 10px">
          <strong>{{ infoRole() }}</strong>
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
      <q-tab-panel name="desc">
        <role-desc :role="role" />
      </q-tab-panel>

      <q-tab-panel name="permis">
        <role-permis />
      </q-tab-panel>
    </q-tab-panels>
  </q-page>
</template>

<script setup>
import { ref, onMounted } from "vue";
import { useRoute, useRouter } from "vue-router";
import { api } from "@/boot/axios";
import RoleDesc from "@/pages/roles/RoleDesc.vue";
import RolePermis from "@/pages/roles/RolePermis.vue";
import {notifyError} from "@/utils/jsutils.js";


const route = useRoute();
const router = useRouter();

const tab = ref("desc");
const roleId = ref(null);
const role = ref({});
const loading = ref(false);

const toBack = () => {
  router.push({
    name: "Roles",
    params: {
      role: roleId.value,
    },
  });
};

const infoRole = () => {
  return role.value?.name || "";
};

onMounted(() => {
  roleId.value = parseInt(route.params.role, 10);
  loading.value = true;
  api
    .post("", {
      method: "role/loadRec",
      params: [roleId.value],
    })
    .then((response) => {
      role.value = response.data.result.records[0] || {};
    })
    .catch((error) => {
      notifyError(error.message);
    })
    .finally(() => {
      loading.value = false;
    });
});
</script>

<style scoped></style>
