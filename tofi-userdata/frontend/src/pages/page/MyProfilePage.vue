<template>
  <q-page padding>

    <div class="absolute-center bg-amber-1 q-my-lg "
         style="height: 100%; width: 800px;">

      <div class="row q-ma-md" style="align-items: center">
        <q-img src="~assets/user.png" width="60px" height="60px"></q-img>
        <h5 class="q-ml-lg"> {{ username() }}</h5>
      </div>


      <div class="q-ma-lg">

        <div class="row">
          <q-btn icon="edit" :label="$t('update')"
                 color="blue" :disable="isUpd" @click="edit()"
          />
          <q-space></q-space>
          <q-btn icon="cancel" :label="$t('cancel')" color="blue"
                 :disable="!isUpd" @click="cancel()" class="q-mr-lg"
          />

          <q-btn icon="save" :label="$t('save')" color="blue"
                 :disable="!isUpd" @click="save()"
                 :loading="loading"
          >
            <template #loading>
              <q-spinner-hourglass color="white"/>
            </template>
          </q-btn>
        </div>
        <hr/>
        <div class="row">
          <!-- secondName -->
          <q-input
            class="q-mx-lg"
            style="width: 200px"
            v-model="form.UserSecondName"
            :model-value="form.UserSecondName"
            type="text" :label="$t('secondName')"
            :rules="[(val) => textTest(val) || $t('req')]"
            :disable="!isUpd"
          />
          <!-- firstName -->
          <q-input
            class="q-mx-lg"
            style="width: 200px"
            v-model="form.UserFirstName"
            :model-value="form.UserFirstName"
            type="text" :label="$t('firstName')"
            :rules="[(val) => textTest(val) || $t('req')]"
            :disable="!isUpd"
          />
          <!-- middleName -->
          <q-input
            class="q-mx-lg"
            style="width: 200px"
            v-model="form.UserMiddleName"
            :model-value="form.UserMiddleName"
            type="text" :label="$t('middleName')"
            :disable="!isUpd"
          />
        </div>

        <div class="row">

          <!-- Birth Day-->
          <q-input
            dense type="date"
            stack-label class="q-mt-md q-mr-lg"
            :model-value="form.UserDateBirth"
            v-model="form.UserDateBirth"
            :label="$t('birthDay')"
            clearable :disable="!isUpd"
          >
          </q-input>

          <!-- email -->
          <q-input
            class="q-mx-lg" :disable="!isUpd"
            v-model="form.UserEmail"
            :model-value="form.UserEmail"
            type="email" :label="$t('email')"
            :rules="[(val) => emailTest(val) || $t('req')]"
          >
          </q-input>

          <!-- phone -->
          <q-input
            dense clearable class="q-mt-md q-mx-lg"
            v-model="form.UserPhone" :disable="!isUpd"
            :model-value="form.UserPhone"
            unmasked-value :label="$t('phone')"
            prefix="+7" mask="### ### ####" fill-mask="_"
            :rules="[(val) => val.length > 9 || $t('req')]"
          />

          <!-- sex -->
          <q-select
            v-model="sex" :model-value="sex" class="q-mt-md q-mx-lg"
            :options="optSex" :label="$t('sex')"
            option-value="id" option-label="name"
            map-options dense options-dense :disable="!isUpd"
            @update:model-value="fnSelectSex()"
          />

        </div>

      </div>

    </div>


  </q-page>
</template>

<script>
import {ref} from "vue";
import {api, baseURL} from "boot/axios.js";
import {notifyError} from "src/utils/jsutils.js";
import {useUserStore} from "stores/user-store.js";
import {storeToRefs} from "pinia";
import {extend} from "quasar";

const store = useUserStore();
const {getUserId} = storeToRefs(store);


export default {
  name: 'MyProfilePage',


  data() {
    return {
      loading: ref(false),
      isUpd: false,
      form: [],
      formCpy: [],
      sex: null,
      optSex: [],

    }
  },

  methods: {
    emailTest: function (v) {
      return /^(?=[a-zA-Z0-9@._%+-]{6,254}$)[a-zA-Z0-9._%+-]{1,64}@(?:[a-zA-Z0-9-]{1,63}\.){1,8}[a-zA-Z]{2,63}$/.test(
        v
      );
    },

    textTest(val) {
      return !!val && !!val.trim();
    },

    fnSelectSex() {
      this.form.UserSex = this.sex.id
    },

    username() {
      let n = this.form.UserSecondName + " " + this.form.UserFirstName
      let n1 = this.form.UserMiddleName ? " " + this.form.UserMiddleName : ""
      return n + n1
    },

    edit() {
      this.isUpd = !this.isUpd
      extend(true, this.formCpy, this.form)
    },

    save() {
      const store = useUserStore();
      const {setUserName} = store;

      //console.log("Form", this.form);

      this.loading = ref(true);
      api
        .post(baseURL, {
          method: "entity/updateProfile",
          params: [this.form],
        })
        .then(
          () => {
            setUserName(this.username())
          },
          (error) => {
            let msgs = error.response.data.error.message.split("@")
            let m1 = this.$t(`${msgs[0]}`)
            let m2 = (msgs.length > 1) ? " [" + msgs[1] + "]" : ""
            let msg = m1 + m2
            notifyError(msg)
          }
        )
        .finally(() => {
          this.loading = ref(false)
          this.hide()
        })

      //....
      this.isUpd = !this.isUpd
    },

    cancel() {
      extend(true, this.form, this.formCpy)
      this.isUpd = !this.isUpd
    },

    loadData() {
      this.loading = ref(true);
      api
        .post(baseURL, {
          method: "data/loadProfile",
          params: [getUserId.value],
        })
        .then(
          (response) => {
            console.info("form", response.data.result.records[0])
            this.form = response.data.result.records[0]
            this.sex = this.form.UserSex
          },
          (error) => {
            let msgs = error.response.data.error.message.split("@")
            let m1 = this.$t(`${msgs[0]}`)
            let m2 = (msgs.length > 1) ? " [" + msgs[1] + "]" : ""
            let msg = m1 + m2
            notifyError(msg);
            //notifyError(error.message);
          }
        )
        .finally(() => {
          this.loading = ref(false);
        })
    }
  },

  created() {
    this.loading = ref(true);
    api
      .post(baseURL, {
        method: "meta/loadFactorSexValues",
        params: [],
      })
      .then(
        (response) => {
          this.optSex = response.data.result.records;
        },
        (error) => {
          let msgs = error.response.data.error.message.split("@")
          let m1 = this.$t(`${msgs[0]}`)
          let m2 = (msgs.length > 1) ? " [" + msgs[1] + "]" : ""
          let msg = m1 + m2
          notifyError(msg);
          //notifyError(error.response.data.error.message);
        }
      )
      .finally(() => {
        this.loading = ref(false);
      })
    //
    this.loadData()
  }

}
</script>
