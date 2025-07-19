<template>
  <div class="body">
    <q-page class="flex flex-center">

      <div  style="height: 50%; width: 80%">

        <!-- for admin -->
        <div class="row q-my-lg justify-center">
            <!-- adm -->
            <q-btn
              rounded
              class="bg-orange text-white text-bold q-ma-md"
              style="height: 200px; width: 350px; font-size: 26px; opacity: 0.7"
              @click="fnAdm()"
            >
              <div class="column">
                <div>
                  <q-avatar icon="manage_accounts" color="green"></q-avatar>
                  {{ $t("appAdmName") }}
                </div>
              </div>
            </q-btn>
        </div>

        <!-- for analitic -->
        <div class="row q-my-lg justify-center">
            <!-- model-->
            <q-btn
              rounded
              class="bg-blue text-white text-bold q-ma-md"
              style="height: 200px; width: 350px; font-size: 26px; opacity: 0.7"
              @click="fnMdl()"
            >
              <div class="column">
                <div>
                  <q-avatar icon="recycling" color="orange"></q-avatar>
                  {{ $t("appModelName") }}
                </div>
              </div>
            </q-btn>

            <!-- data -->
            <q-btn
              rounded
              class="bg-blue text-white text-bold q-ma-md"
              style="height: 200px; width: 350px; font-size: 26px; opacity: 0.7"
              @click="fnData()"
            >
              <div class="column">
                <div>
                  <q-avatar icon="edit_note" color="orange"></q-avatar>
                  {{ $t("appDataName") }}
                </div>
              </div>
            </q-btn>

          <!-- cube -->
          <q-btn
            rounded
            class="bg-blue text-white text-bold q-ma-md"
            style="height: 200px; width: 350px; font-size: 26px; opacity: 0.7"
            @click="fnCube()"
          >
            <div class="column">
              <div>
                <q-avatar icon="view_in_ar" color="orange"></q-avatar>
                {{ $t("appCubeName") }}
              </div>
            </div>
          </q-btn>

        </div>

        <!-- for User-->
        <div class="row q-my-lg justify-center">
            <!-- User Data -->
            <q-btn
              rounded
              class="bg-green text-white text-bold q-ma-md"
              style="height: 200px; width: 350px; font-size: 24px; opacity: 0.7"
              @click="fnUserData()"
            >
              <div class="column">
                <div>
                  <q-avatar icon="contacts" color="blue"></q-avatar>
                  {{ $t("appUserDataName") }}
                </div>
              </div>
            </q-btn>

            <!-- NSI -->
            <q-btn
              rounded
              class="bg-green text-white text-bold q-ma-md"
              style="height: 200px; width: 350px; font-size: 26px; opacity: 0.7"
              @click="fnNSI()"
            >
              <div class="column">
                <div>
                  <q-avatar icon="menu_book" color="blue"></q-avatar>
                  {{ $t("appNSIName") }}
                </div>
              </div>
            </q-btn>

            <!-- Monitoring -->
            <q-btn
              rounded
              class="bg-green text-white text-bold q-ma-md"
              style="height: 200px; width: 350px; font-size: 26px; opacity: 0.7"
              @click="fnMonitoring()"
            >
              <div class="column">
                <div>
                  <q-avatar icon="set_meal" color="blue"></q-avatar>
                  {{ $t("appMonitoringName") }}
                </div>
              </div>
            </q-btn>

        </div>

      </div>

    </q-page>
  </div>
</template>

<script>
import {defineComponent} from "vue";
import {
  appAdmURL,
  appModelURL,
  appDataUserURL,
  appDataURL, appNSIURL, appMonitoringURL, appCubeURL
} from "boot/axios";

export default defineComponent({
  name: "IndexPage",

  methods: {
    fnAdm() {
      open(appAdmURL, "_self");
    },

    fnMdl() {
      open(appModelURL, "_self");
    },

    fnUserData() {
      open(appDataUserURL, "_self");
    },

    fnData() {
      open(appDataURL, "_self");
    },

    fnCube() {
      open(appCubeURL, "_self");
    },

    fnNSI() {
      open(appNSIURL, "_self");
    },

    fnMonitoring() {
      open(appMonitoringURL, "_self");
    },

  },
});
</script>

<style scoped>
.body {
  background-image: url("../assets/ms.png");
}

button:hover {
  transform: skew(-10deg);
  box-shadow: 0 5px 25px 0 rgba(0, 0, 0, 0.7);
}
</style>
