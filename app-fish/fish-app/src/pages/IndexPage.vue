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

    <div class="q-pa-lg bg-elements">
      <img
        class="fish-phantom-vertical"
        alt="Fish logo"
        src="~assets/fish.png"
        style="width: 200px; height: 150px"
      />
    </div>

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
          <!-- User Data -->
          <q-btn
            rounded
            class="bg-blue text-white text-bold q-ma-md"
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

        </div>

        <div class="row q-ma-md justify-center">
          <!-- Nsi -->
          <q-btn
            rounded
            class="bg-green text-white text-bold q-ma-md"
            style="height: 200px; width: 350px; font-size: 26px; opacity: 0.7"
            @click="fnNsi()"
          >
            <div class="column">
              <div>
                <q-avatar icon="menu_book" color="orange"></q-avatar>
                <div>{{ $t("appNSIName") }}</div>
              </div>
            </div>
          </q-btn>

          <!-- Monitoring-->
          <q-btn
            rounded
            class="bg-green text-white text-bold q-ma-md"
            style="height: 200px; width: 350px; font-size: 26px; opacity: 0.7"
            @click="fnMonitoring()"
          >
            <div class="column">
              <div>
                <q-avatar icon="location_city" color="orange"></q-avatar>
                <div>{{$t("appMonitoringName")}}</div>
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
  appAdmURL, appModelURL, appDataUserURL,
  appDataURL, appNsiURL, appCubeURL, appMonitoringURL
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

    fnNsi() {
      open(appNsiURL, "_self");
    },

    fnMonitoring() {
      open(appMonitoringURL, "_self");
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

/*
.fish-phantom {
  !* Создаем маску: переход от прозрачного к черному *!
  !* Предполагаем, что голова слева, хвост справа *!
  -webkit-mask-image: linear-gradient(to left, black 33%, transparent 66%);
  mask-image: linear-gradient(to left, black 33%, transparent 66%);

  -webkit-mask-size: 300% 100%;
  mask-size: 300% 100%;

  !* Запускаем анимацию *!
  animation: dissolve-logic 6s ease-in-out infinite;
}

@keyframes dissolve-logic {
  0% {
    !* Рыба полностью видна *!
    -webkit-mask-position: 0% 0;
    mask-position: 0% 0;
  }
  40% {
    !* Исчезает: хвост уходит первым (сдвигаем прозрачную часть на рыбу) *!
    -webkit-mask-position: 70% 0;
    mask-position: 70% 0;
  }
  50%, 60% {
    !* Полностью растворилась (пауза в небытии) *!
    -webkit-mask-position: 100% 0;
    mask-position: 100% 0;
  }
  100% {
    !* Появляется: голова выходит первой из тумана *!
    -webkit-mask-position: 0% 0;
    mask-position: 0% 0;
  }
}
*/

.fish-phantom-vertical {
  /* Градиент сверху вниз: черная область (видимость) сверху */
  -webkit-mask-image: linear-gradient(to bottom, black 33%, transparent 66%);
  mask-image: linear-gradient(to bottom, black 33%, transparent 66%);

  /* Делаем маску высокой (100% ширина, 300% высота) */
  -webkit-mask-size: 100% 300%;
  mask-size: 100% 300%;

  animation: dissolve-vertical 6s ease-in-out infinite;
}

@keyframes dissolve-vertical {
  0% {
    /* Рыба полностью видна (черная часть маски наверху) */
    -webkit-mask-position: 0 0%;
    mask-position: 0 0%;
  }
  40% {
    /* Растворяется снизу вверх: сдвигаем прозрачную часть вверх */
    -webkit-mask-position: 0 70%;
    mask-position: 0 70%;
  }
  50%, 60% {
    /* Полное исчезновение */
    -webkit-mask-position: 0 100%;
    mask-position: 0 100%;
  }
  100% {
    /* Появляется сверху вниз: черная часть возвращается на место */
    -webkit-mask-position: 0 0%;
    mask-position: 0 0%;
  }
}



</style>
