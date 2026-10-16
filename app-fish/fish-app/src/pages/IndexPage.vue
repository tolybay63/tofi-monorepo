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

    <!-- Основной контент страницы -->
    <!-- flex no-wrap обеспечивает жесткое разделение на колонки без переносов и скролла -->
    <q-page class="relative-position flex no-wrap overflow-hidden" style="height: 100%;">

      <!-- ЛЕВАЯ ЧАСТЬ: Крупный заголовок -->
      <div class="col-4 flex items-center q-pl-xl" style="min-width: 400px; z-index: 2;">
        <h1 class="text-weight-bolder text-blue q-ma-none" style="font-size: clamp(2.5rem, 3.5vw, 4rem); line-height: 1.2; letter-spacing: -0.5px;">
          Система<br>оценки<br>запасов и<br>расчета ПДУ
        </h1>
      </div>

      <!-- ПРАВАЯ ЧАСТЬ: Ссылки сверху и диагональная лесенка -->
      <div class="col flex column relative-position" style="z-index: 2;">

        <!-- Ссылки "Администрирование" и "Моделирование" (жестко в правом верхнем углу) -->
        <div class="absolute-top-right q-pa-xl column q-gutter-y-md">
          <a href="#" @click.prevent="fnAdm()" class="row items-center no-wrap custom-link">
            <q-icon name="manage_accounts" color="red" size="28px" class="q-mr-sm" />
            <span class="text-h6 text-weight-bold link-text text-red-6">{{ $t("appAdmName") }}</span>
          </a>

          <a href="#" @click.prevent="fnMdl()" class="row items-center no-wrap custom-link">
            <q-icon name="recycling" color="blue" size="28px" class="q-mr-sm" />
            <span class="text-h6 text-weight-bold link-text text-blue-8">{{ $t("appModelName") }}</span>
          </a>
        </div>

        <!-- Центральный блок с лесенкой сервисов -->
        <!-- Заменили flex-center на column justify-center — это прижимает блок к левому краю и центрирует по вертикали -->
        <div class="column justify-center full-height">
          <div class="column q-gutter-y-lg w-full">

            <!-- 1. НСИ -->
            <div
              class="service-card shadow-3 cursor-pointer relative-position"
              style="margin-left: 10%;"
              v-ripple
              @click="fnNSI()"
            >
              <div class="row items-center justify-center no-wrap full-height q-px-md">
                <q-avatar size="48px" color="orange-4" text-color="white" icon="menu_book" class="q-mr-md absolute-left q-ml-md" style="top: 50%; transform: translateY(-50%);" />
                <div class="text-white text-h6 text-weight-bold text-uppercase no-wrap text-center" style="letter-spacing: 0.5px;">
                  {{ $t("appNsiName") }}
                </div>
              </div>
            </div>

            <!-- 2. Мониторинг водоемов -->
            <div
              class="service-card shadow-3 cursor-pointer relative-position"
              style="margin-left: 20%;"
              v-ripple
              @click="fnMonitoring()"
            >
              <div class="row items-center justify-center no-wrap full-height q-px-md">
                <q-avatar size="48px" color="orange-4" text-color="white" icon="location_city" class="q-mr-md absolute-left q-ml-md" style="top: 50%; transform: translateY(-50%);" />
                <div class="text-white text-h6 text-weight-bold text-uppercase no-wrap text-center" style="letter-spacing: 0.5px;">
                  {{ $t("appMonitoringName") }}
                </div>
              </div>
            </div>

            <!-- 3. Оценка запасов и ПДУ (Калькулятор) -->
            <div
              class="service-card shadow-3 cursor-pointer relative-position"
              style="margin-left: 30%;"
              v-ripple
              @click="fnCalc()"
            >
              <div class="row items-center justify-center no-wrap full-height q-px-md">
                <q-avatar size="48px" color="orange-4" text-color="white" icon="functions" class="q-mr-md absolute-left q-ml-md" style="top: 50%; transform: translateY(-50%);" />
                <div class="text-white text-h6 text-weight-bold text-uppercase no-wrap text-center" style="letter-spacing: 0.5px;">
                  {{ $t("appCalcName") }}
                </div>
              </div>
            </div>

            <!-- 4. Анализ данных -->
            <div
              class="service-card shadow-3 cursor-pointer relative-position"
              style="margin-left: 40%;"
              v-ripple
              @click="fnAnalyse()"
            >
              <div class="row items-center justify-center no-wrap full-height q-px-md">
                <q-avatar size="48px" color="orange-4" text-color="white" icon="biotech" class="q-mr-md absolute-left q-ml-md" style="top: 50%; transform: translateY(-50%);" />
                <div class="text-white text-h6 text-weight-bold text-uppercase no-wrap text-center" style="letter-spacing: 0.5px;">
                  {{ $t("appAnalyseName") }}
                </div>
              </div>
            </div>

          </div>
        </div>

      </div>
    </q-page>
  </div>
</template>

<script setup>
import {appAdmURL, appCalcURL, appModelURL, appMonitoringURL, appNsiURL} from "@/boot/axios";
import {Notify} from "quasar";

function fnAdm() {
  window.open(appAdmURL, "_self");
}

function fnMdl() {
  window.open(appModelURL, "_self");
}

function fnNSI() {
  window.open(appNsiURL, "_self");
}

function fnMonitoring() {
  window.open(appMonitoringURL, "_self");
}

function fnCalc() {
  window.open(appCalcURL, "_self");
}

function fnAnalyse() {
  Notify.create({
    type: 'negative',
    message: "На стадии реализации",
    position: 'top',
    timeout: 5000,
    actions: [{ icon: 'close', color: 'white' }]
  });
}
</script>

<style scoped>
/* Ваши оригинальные стили */
.body {
  position: relative;
  width: 100%;
  height: 90%;
  background: #f0f2f5;
  overflow: hidden;
}

.bg-elements {
  position: absolute;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  z-index: 1;
  pointer-events: none;
}

.bg-item {
  position: absolute;
  color: rgba(25, 118, 210, 0.1);
  font-weight: bold;
  font-family: 'Arial Black', sans-serif;
  user-select: none;
  animation: float 15s infinite linear;
  display: block;
}

.circle {
  width: 100px;
  height: 100px;
  background: rgba(25, 118, 210, 0.1);
  border: 1px none rgba(0, 0, 0, 0.03);
  border-radius: 50%;
}

.square {
  width: 80px;
  height: 80px;
  background: rgba(25, 118, 210, 0.1);
  border: 1px solid rgba(0, 0, 0, 0.03);
  transform: rotate(45deg);
}

.small {
  width: 40px;
  height: 40px;
}

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

/* Настройки одинаковых зеленых карточек */
.service-card {
  background-color: #81c784;
  border-radius: 12px;
  height: 90px;
  width: 650px; /* Жесткая ширина карточек */
  transition: transform 0.3s cubic-bezier(0.25, 0.8, 0.25, 1), box-shadow 0.3s ease;
  opacity: 0.95;
}

.service-card:hover {
  transform: translateY(-4px) scale(1.02);
  box-shadow: 0 12px 24px rgba(129, 199, 132, 0.5);
  background-color: #78bd7b;
  opacity: 1;
}

.custom-link {
  text-decoration: none;
  opacity: 0.9;
  transition: opacity 0.2s ease, transform 0.2s ease;
}

.custom-link:hover {
  opacity: 1;
  transform: translateX(-5px);
}

.link-text {
  text-decoration: underline;
  text-underline-offset: 4px;
}
</style>
