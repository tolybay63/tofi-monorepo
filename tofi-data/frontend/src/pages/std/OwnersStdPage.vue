<template>
  <owners-page :propName="'prop'" :isFlat="0"></owners-page>
</template>

<script>
import OwnersPage from "pages/main/OwnersPage.vue";

export default {
  name: "OwnersStdPage",
  components: { OwnersPage },
};
</script>

<style scoped></style>
