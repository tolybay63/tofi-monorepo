<template>
  <q-page class="flex flex-center">

  <div>
    <q-page class="flex flex-center">
      <img
        class="balance"
        alt="TOFI logo"
        src="~assets/a.png"
        style="width: 100px; height: 100px"
      />
      <img
        alt="TOFI logo"
        src="~assets/b.png"
        style="width: 170px; height: 50px"
      />
    </q-page>
  </div>


  </q-page>
</template>

<script>
import { defineComponent } from 'vue'

export default defineComponent({
  name: 'IndexPage'
})
</script>

<style>

@keyframes sway {
  to {
    transform: rotate(30deg);
  }
}
.balance {
  transform-origin: center;
  transform: rotate(-30deg);
  animation: sway 2.2s infinite alternate ease-in-out;
}

@-webkit-keyframes bg-scrolling-reverse {
  200% {
    background-position: 50px 50px;
  }
}
@-moz-keyframes bg-scrolling-reverse {
  100% {
    background-position: 50px 50px;
  }
}
@-o-keyframes bg-scrolling-reverse {
  100% {
    background-position: 50px 50px;
  }
}
@keyframes bg-scrolling-reverse {
  100% {
    background-position: 50px 50px;
  }
}
@-webkit-keyframes bg-scrolling {
  0% {
    background-position: 50px 50px;
  }
}
@-moz-keyframes bg-scrolling {
  0% {
    background-position: 50px 50px;
  }
}
@-o-keyframes bg-scrolling {
  0% {
    background-position: 50px 50px;
  }
}
@keyframes bg-scrolling {
  0% {
    background-position: 50px 50px;
  }
}

</style>