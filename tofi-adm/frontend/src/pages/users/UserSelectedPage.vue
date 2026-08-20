<template>
  <div class="q-pa-sm-sm bg-green-1">
    <q-tabs v-model="tab" class="text-teal">
      <div style="margin-left: 20px">
        {{ $t("user") }}:
        <span style="color: black; margin-left: 10px">
          <strong>{{ infoUser() }}</strong>
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
        <user-role :user="user_id" />
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

<script setup>
import {onMounted, ref} from "vue";
import {useRoute, useRouter} from "vue-router";
import {api} from "@/boot/axios";
import {notifyError} from "../../utils/jsutils";
import UserRole from "@/pages/users/UserRole.vue";
import UserPermis from "@/pages/users/UserPermis.vue";

const route = useRoute();
const router = useRouter();

const tab = ref("role");
const userGr_id = ref(0);
const user_id = ref(0);
const user = ref({});
const loading = ref(false);

const toBack = () => {
  router.push({
    name: "Users",
    params: {
      user: user_id.value,
      userGr: userGr_id.value,
    },
  });
};

const infoUser = () => {
  return user.value?.fullName || "";
};

onMounted(() => {
  user_id.value = parseInt(route.params.user, 10);
  userGr_id.value = parseInt(route.params.userGr, 10);

  loading.value = true;
  api
    .post("", {
      method: "usr/loadUser",
      params: [user_id.value],
    })
    .then((response) => {
      user.value = response.data.result.records[0];
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
