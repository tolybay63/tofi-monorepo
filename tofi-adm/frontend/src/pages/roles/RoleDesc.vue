<template>
  <q-card class="bg-amber-1">
    <q-card-section>
      <div class="row">
        <q-item> {{ $t("fldName") }}:</q-item>
        <q-separator></q-separator>
        <q-item>
          <strong> {{ role?.name }} </strong>
        </q-item>
      </div>

      <div class="row">
        <q-item> {{ $t("fldFullName") }}:</q-item>
        <q-separator></q-separator>
        <q-item>
          <strong> {{ role?.fullName }} </strong>
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

<script setup>
import { ref, onMounted } from "vue";
import { useRoute } from "vue-router";
import { api } from "@/boot/axios";
import {notifyError} from "@/utils/jsutils.js";


const props = defineProps({
  role: {
    type: Object,
    default: () => ({}),
  },
});

const route = useRoute();
const permis = ref("");
const loading = ref(false);

onMounted(() => {
  const roleId = route.params.role;
  loading.value = true;
  api
    .post("", {
      method: "role/getRolePermis",
      params: [roleId],
    })
    .then((response) => {
      permis.value = response.data.result;
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
