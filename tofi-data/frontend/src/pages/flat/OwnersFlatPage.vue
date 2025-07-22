<template>
  <owners-page :propName="'prop'" :isFlat="1"></owners-page>
</template>

<script>
import OwnersPage from "pages/main/OwnersPage.vue";

export default {
  name: "OwnersFlatPage",
  components: { OwnersPage },
};
</script>

<style scoped></style>
