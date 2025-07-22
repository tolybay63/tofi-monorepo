<template>
  <owners-page :propName="'multiProp'" :isFlat="0"></owners-page>
</template>

<script>
import OwnersPage from "pages/main/OwnersPage.vue";

export default {
  name: "OwnersMultiPage",
  components: { OwnersPage },
};
</script>

<style scoped></style>
