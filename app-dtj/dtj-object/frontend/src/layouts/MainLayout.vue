<template>
  <q-layout view="hHh lpR fFf">
    <q-header elevated>
      <q-toolbar>

        <!-- Menu Toggle -->
<!--
        <q-btn
          flat
          dense
          round
          icon="menu"
          @click="toggleLeftDrawer"
          aria-label="Menu"
        >
        </q-btn>
-->

        <!-- App Title -->
        <q-toolbar-title class="text-center">
          DTJ Service - Object (api testing)
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

    <!-- Navigation Drawer -->
<!--

    <q-drawer
      :width="250"
      v-model="leftDrawerOpen"
      show-if-above
      bordered
      elevated
    >
      <div class="q-pa-sm">
        <q-list>
          <q-item

            v-for="link in essentialLinks"
            :key="link['title']"
            class="q-table&#45;&#45;bordered bg-blue-1"
            clickable
            tag="a"
            :to="link['link']"
            active-class="text-bold text-blue"
          >
            <q-item-section v-if="link['icon']" avatar>
              <q-icon :name="link.icon" size="32px"/>
            </q-item-section>

            <q-item-section>
              <q-item-label>{{ $t(link.title) }}</q-item-label>
              <q-item-label caption>{{ link.info }}</q-item-label>
            </q-item-section>
          </q-item>
        </q-list>
      </div>
    </q-drawer>

-->
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
  },

  created() {
  },
});
</script>

<style></style>
