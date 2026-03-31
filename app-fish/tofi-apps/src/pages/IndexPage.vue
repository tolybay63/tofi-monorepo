<template>
  <div class="body">

    <!-- Декоративные плавающие элементы фона -->
    <div class="bg-elements">
      <span class="bg-item text">{{$t("tofi")}}</span>
      <span class="bg-item circle"></span>
      <span class="bg-item text">{{$t("tofi")}}</span>
      <span class="bg-item square"></span>
      <span class="bg-item text">{{$t("tofi")}}</span>
      <span class="bg-item circle small"></span>
    </div>

    <q-page class="flex flex-center">

        <div class="row q-my-lg justify-center">

            <q-btn
              round
              class="bg-blue text-white text-bold q-ma-md"
              style="height: 350px; width: 350px; font-size: 26px; opacity: 0.7"
              @click="fnFish()"
            >
              <div class="column">
                <div>
                  <q-avatar icon="set_meal" color="black"></q-avatar>
                  {{ $t("appFishName") }}
                </div>
              </div>
            </q-btn>

            <q-btn
              round
              class="bg-orange text-white text-bold q-ma-md"
              style="height: 350px; width: 350px; font-size: 26px; opacity: 0.7"
              @click="fnDtj()"
            >
              <div class="column">
                <div>
                  <q-avatar icon="train" color="black" size="256"></q-avatar>
                  {{ $t("appDtjName") }}
                </div>
              </div>
            </q-btn>

        </div>


    </q-page>
  </div>
</template>

<script>
import {defineComponent} from "vue";
import {appFishURL, appDtjURL} from "boot/axios";

export default defineComponent({
  name: "IndexPage",

  methods: {

    fnFish() {
      open(appFishURL, "_self");
    },

    fnDtj() {
      open(appDtjURL, "_self");
    },

  },
});
</script>

<style scoped>
.body {
  position: relative;
  width: 100%;
  height: 90%;
  background: #f0f2f5; /* Чистый светлый фон #fdfdfd */
  overflow: hidden;
}

.content-wrapper {
  position: relative;
  z-index: 2; /* Логотипы поверх фона */
}

/* Контейнер для элементов фона */
.bg-elements {
  position: absolute;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  z-index: 1;
  pointer-events: none;
}

/* Общие стили для плавающих объектов */
.bg-item {
  position: absolute;
  color: rgba(25, 118, 210, 0.1); /* Цвет текста TOFI 0, 0, 0, 0.05 */
  font-weight: bold;
  font-family: 'Arial Black', sans-serif;
  user-select: none;
  animation: float 15s infinite linear;
  display: block; /* Гарантируем отображение */
}

/* Стили для фигур — добавим легкую заливку, чтобы их было видно */
.circle {
  width: 100px;
  height: 100px;
  background: rgba(25, 118, 210, 0.1); /* Легкая заливка 0, 0, 0, 0.03 */
  border: 1px none rgba(0, 0, 0, 0.03); /* Тонкий контур 0, 0, 0, 0.05 */
  border-radius: 50%;
}

.square {
  width: 80px;
  height: 80px;
  background: rgba(25, 118, 210, 0.1); /* Легкая заливка */
  border: 1px solid rgba(0, 0, 0, 0.03);
  transform: rotate(45deg);
}

.small {
  width: 40px;
  height: 40px;
}

/* Разные фигуры */
.circle {
  width: 100px;
  height: 100px;
  border: 1px solid rgba(0, 0, 0, 0.03);
  border-radius: 50%;
}

.square {
  width: 60px;
  height: 60px;
  border: 1px solid rgba(0, 0, 0, 0.03);
  transform: rotate(45deg);
}

/* Расположение и индивидуальная анимация */
.bg-item:nth-child(1) { top: 10%; left: 10%; font-size: 80px; animation-duration: 20s; }
.bg-item:nth-child(2) { top: 70%; left: 15%; animation-duration: 25s; }
.bg-item:nth-child(3) { top: 40%; left: 75%; font-size: 120px; animation-duration: 18s; animation-direction: reverse; }
.bg-item:nth-child(4) { top: 15%; left: 80%; animation-duration: 22s; }
.bg-item:nth-child(5) { top: 80%; left: 75%; font-size: 60px; animation-duration: 30s; }
.bg-item:nth-child(6) { top: 50%; left: 5%; width: 40px; height: 40px; }

@keyframes float {
  0% { transform: translate(0, 0) rotate(0deg); }
  33% { transform: translate(30px, 50px) rotate(10deg); }
  66% { transform: translate(-20px, 20px) rotate(-10deg); }
  100% { transform: translate(0, 0) rotate(0deg); }
}

/* Анимация логотипа */
@keyframes sway {
  to { transform: rotate(30deg); }
}

.balance {
  transform-origin: center;
  transform: rotate(-30deg);
  animation: sway 2.2s infinite alternate ease-in-out;
}
</style>
