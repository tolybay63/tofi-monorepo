<template>
  <q-layout view="hHh lpR fFf">
    <q-header elevated>
      <q-toolbar>

        <!-- App Title -->
        <q-toolbar-title class="text-center">
          DTJ Service - Personnel
        </q-toolbar-title>

      </q-toolbar>
    </q-header>

    <!-- Footer -->
    <q-footer reveal elevated>
      <q-toolbar>
        <q-toolbar-title class="text-center">
          <q-icon class="q-pa-sm">
            <img src="../assets/factor.png" alt="Company Logo"/>
          </q-icon>
          KSI Factor
        </q-toolbar-title>
      </q-toolbar>
    </q-footer>

    <!-- Main Content -->
    <q-page-container>
      <router-view/>
    </q-page-container>
  </q-layout>
</template>

<script>
import {defineComponent} from "vue";


export default defineComponent({
  name: "MainLayout",

  setup() {
    return {
    };
  },

  created() {
  },
});
</script>

<style></style>
