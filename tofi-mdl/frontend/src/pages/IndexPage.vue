<template>
  <div class="body">
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
</template>

<script>
import {defineComponent} from "vue";

export default defineComponent({
  name: "IndexPage",
});
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

/* Main styles */
.body {
  width: 100%;
  height: 92%;

  color: #999;

  /* img size is 50x50 */
  background: url("data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAADIAAAAyCAIAAACRXR/mAAAACXBIWXMAAAsTAAALEwEAmpwYAAAAIGNIUk0AAHolAACAgwAA+f8AAIDpAAB1MAAA6mAAADqYAAAXb5JfxUYAAABnSURBVHja7M5RDYAwDEXRDgmvEocnlrQS2SwUFST9uEfBGWs9c97nbGtDcquqiKhOImLs/UpuzVzWEi1atGjRokWLFi1atGjRokWLFi1atGjRokWLFi1af7Ukz8xWp8z8AAAA//8DAJ4LoEAAlL1nAAAAAElFTkSuQmCC") repeat 0 0;
  -webkit-animation: bg-scrolling-reverse 0.92s infinite; /* Safari 4+ */
  -moz-animation: bg-scrolling-reverse 0.92s infinite; /* Fx 5+ */
  -o-animation: bg-scrolling-reverse 0.92s infinite; /* Opera 12+ */
  animation: bg-scrolling-reverse 0.92s infinite; /* IE 10+ */
  -webkit-animation-timing-function: linear;
  -moz-animation-timing-function: linear;
  -o-animation-timing-function: linear;
  animation-timing-function: linear;
}
</style>
