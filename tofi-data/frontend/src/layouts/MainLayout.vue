<template>

  <q-layout view="hHh lpR fFf">
    <q-header elevated>
      <q-toolbar>

        <!--Main App -->
        <q-btn
          class="q-mr-md"
          rounded
          color="primary"
          dense
          icon="grid_view"
          @click="mainApp()"
        >
          <q-tooltip transition-show="rotate" transition-hide="rotate">
            {{ $t("appName") }}
          </q-tooltip>
        </q-btn>

        <q-toolbar-title class="text-center"
          >{{ $t("appDataName") }}
        </q-toolbar-title>

        <!--Home -->
        <q-btn
          class="q-pa-md-sm"
          rounded
          color="primary"
          dense
          icon="home"
          @click="this.$router.push('/')"
        >
          <q-tooltip transition-show="rotate" transition-hide="rotate">
            {{ $t("mainPage") }}
          </q-tooltip>
        </q-btn>

        <!-- login user-->
        <div class="q-pa-md q-gutter-sm">
          <q-btn
            class="q-pr-sm-sm"
            rounded
            color="primary"
            dense
            icon="account_circle"
            @click="loginOnOff()"
            no-caps
          >
            <q-tooltip
              transition-show="rotate"
              transition-hide="rotate"
              v-if="getUserName === ''"
              >{{ $t("logIn") }}
            </q-tooltip>
            <q-tooltip
              transition-show="rotate"
              transition-hide="rotate"
              v-if="getUserName !== ''"
              >{{ $t("logOut") }}
            </q-tooltip>

            <div
              itemid="USERID"
              v-if="getUserName !== ''"
              style="margin-left: 5px"
            >
              {{ getUserName }}

            </div>
            <q-badge rounded color="primary" align="middle">
              <q-icon :name="nameIcon()" color="white" />
            </q-badge>
          </q-btn>
        </div>

        <!-- Текущий язык-->
        <SetLocale></SetLocale>
      </q-toolbar>

      <div v-if="getUserName !== ''" class="row">

        <div class="q-mx-sm q-mt-sm text-white">{{ $t("app") }}:</div>
        <q-select
          v-model="dataBase" :model-value="dataBase" bg-color="blue-4"
          :options="dataBaseOpt" dense map-options style="width: 330px; "
          option-label="name" option-value="id" filled
          @update:model-value="fnSelectDataBase"
        />

        <q-select
          v-model="dataType" :model-value="dataType" bg-color="blue-4"
          :options="dataTypeOpt" dense map-options style="width: 250px"
          option-label="text" option-value="id" class="q-mx-lg q-mb-lg" filled
          @update:model-value="fnSelectDataType"
        />

      </div>

    </q-header>

    <q-footer reveal elevated>
      <q-toolbar>
        <q-toolbar-title class="flex flex-center">
          <q-icon class="q-pa-sm">
            <img src="../assets/factor.png" alt="Logo" />
          </q-icon>
          {{ $t("company") }}
        </q-toolbar-title>
      </q-toolbar>
    </q-footer>

    <q-page-container>
      <router-view />
    </q-page-container>
  </q-layout>
</template>

<script>
import {defineComponent, ref} from "vue";
import LoginUser from "components/LoginUser.vue";
import SetLocale from "components/SetLocale.vue";
import {api, authURL, baseURL, urlMainApp} from "boot/axios";
import {notifyError } from "src/utils/jsutils";
import {useUserStore} from "stores/user-store";
import {useParamsStore} from "stores/params-store";
import {storeToRefs} from "pinia";

const store = useUserStore()
const { isSysAdmin, getUserName, getTarget } = storeToRefs(store)
const { setUserName, setUserStore } = store
const storeParams = useParamsStore()
const { setStoreParams } = storeParams


export default defineComponent({
  name: "MainLayout",
  components: { SetLocale },

  data() {
    return {
      getUserName,
      dataType: {id: "std", text: this.$t("dataStd")},
      dataTypeOpt: this.getDataTyp(),
      dataBase: null,
      dataBaseOpt: [],
      loading: ref(false),
      params: {
        dataType: "std",
        dataBase: 0,
        model: ""
      }
    }
  },

  methods: {

    getDataTyp() {
      return [
        {id: "std", text: this.$t("dataStd")},
        {id: "multi", text: this.$t("dataMulti")},
        {id: "flat", text: this.$t("dataFlat")},
      ]
    },

/*    fnRefresh() {
      this.$router.push("/")
      setTimeout(()=> {
        this.toMainPage()
      }, 100)
    },*/

    fnSelectDataType(val) {
      this.$router.push("/")
      this.params.dataType = val.id
      setStoreParams(this.params)

      setTimeout(()=> {
        this.toMainPage()
      }, 100)
    },

    fnSelectDataBase(val) {
      this.$router.push("/")
      this.params.dataBase = val.id
      this.params.model = val.modelname
      setStoreParams(this.params)
      setTimeout(()=> {
        this.toMainPage()
      }, 100)
    },

    mainApp() {
      open(urlMainApp, "_self");
    },

    toMainPage() {
      //console.info("toMainPage", this.params, this.dataType)
      this.$router["push"]("/"+this.dataType.id)
    },

    loginOnOff() {
      //console.info("OnOff")
      if (getUserName.value === "") {
        const lang = localStorage.getItem("curLang");
        this.$q
          .dialog({
            component: LoginUser,
            componentProps: {
              lg: lang,
              // ...
            },
          })
          .onOk((r) => {
            api
              .post(baseURL, {
                method: "data/getCurUserInfo",
                params: [],
              })
              .then(
                (response) => {
                  setUserStore(response.data.result)
                  setStoreParams(this.params)
                  this.toMainPage()
                },
                (error) => {
                  console.log("error", error);
                  setUserStore({});
                  notifyError(error.message);
                }
              )
              .finally(() => {
                //this.$router.push("/");
                //location.reload()
              });
          });
      } else {
        api
          .post(authURL + "/logout", {
            params: {},
          })
          .then((response) => {
            setUserName("")
            setUserStore({})
            setStoreParams({})
            this.$router.push("/")
          })
          .finally(() => {
            //location.reload()
            setTimeout(()=> {
              this.$router.push("/")
            }, 30)

          });
      }
    },

    nameIcon() {
      if (getUserName.value === "") return "login";
      else return "logout";
    },

    notAccess() {
      return getTarget.value.length === 0 && !isSysAdmin.value;
    },

    hasTarget(tg) {
      if (isSysAdmin.value) return true;
      if (getTarget.value.length === 0) return false;
      return getTarget.value.includes(tg);
    },

  },

  created() {
    if (getUserName.value === "") {
      notifyError(this.$t("notLogined"))
      this.$router.push("/")
    }

      this.loading = ref(true)
      api.post(baseURL, {
        method: "data/loadDataBase",
        params: [],
      })
        .then((response) => {
          this.dataBaseOpt = response.data.result.store.records
          this.dataBase = this.dataBaseOpt[0]
          this.params.dataBase = this.dataBaseOpt[0].id
          this.params.model = this.dataBaseOpt[0].modelname
          this.params.metamodel = response.data.result.metamodel
          setStoreParams(this.params)

          //console.info("dataBaseOpt", this.dataBaseOpt)
          //console.info("params", this.params)
        })
        .catch(error => {
          notifyError(error.message)
        })
        .finally(() => {
          this.loading = false
        })

  },

})
</script>

<style></style>
