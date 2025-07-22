<template>
  <div class="body">
    <q-page class="flex flex-center">

      <div style="height: 50%; width: 80%">

        <div class="row q-ma-md justify-center">
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


        <div class="row q-ma-md justify-center">
            <!-- model -->
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
                {{ $t("appCubesName") }}
              </div>
            </div>
          </q-btn>


        </div>

        <div class="row q-ma-md justify-center">
            <!-- User Data -->
            <q-btn
              rounded
              class="bg-green text-white text-bold q-ma-md"
              style="height: 200px; width: 350px; font-size: 24px; opacity: 0.7"
              @click="fnUserData()"
            >
              <div class="column">
                <div>
                  <q-avatar icon="contacts" color="orange"></q-avatar>
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
                  <q-avatar icon="menu_book" color="orange"></q-avatar>
                  {{ $t("appNSIName") }}
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
  appDataURL, appNSIURL, appCubeURL
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

    fnNSI() {
      open(appNSIURL, "_self");
    },

    fnCube() {
      open(appCubeURL, "_self");
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
