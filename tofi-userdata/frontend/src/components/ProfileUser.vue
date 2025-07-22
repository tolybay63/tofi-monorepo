<template>
  <q-dialog
    ref="dialog"
    @hide="onDialogHide"
    persistent
    autofocus
    transition-show="slide-down"
    transition-hide="slide-down"
  >
    <q-card class="q-dialog-plugin full-width">
      <q-bar class="text-white bg-primary">
        <div>{{ $t("createAccount") }}</div>
      </q-bar>

      <div class="row q-col-gutter-y-md">
        <div class="col">
          <q-card-section>

            <!-- user class -->
            <q-item-label class="q-mt-sm text-grey-7" style="font-size: 0.8em">{{
                $t("userCls")
              }}
            </q-item-label>

            <treeselect
              :options="optCls"
              maxHeight="800"
              v-model="cls"
              :normalizer="normalizer"
              :placeholder="$t('select')"
              :noChildrenText="$t('noChilds')"
              :noResultsText="$t('noResult')"
              :noOptionsText="$t('noResult')"
              @close="fnCloseCls" @select="fnSelectCls"
            />
            <!-- secondName -->
            <q-input
              v-model="form.UserSecondName"
              :model-value="form.UserSecondName"
              type="text"
              :label="$t('secondName')"
              :rules="[(val) => textTest(val) || $t('req')]"
              @change="onChangeSN"
            >
            </q-input>
            <!-- firstName -->
            <q-input
              v-model="form.UserFirstName"
              :model-value="form.UserFirstName"
              type="text"
              :label="$t('firstName')"
              :rules="[(val) => textTest(val) || $t('req')]"
              @change="onChangeFN"
            >
            </q-input>
            <!-- middleName -->
            <q-input
              v-model="form.UserMiddleName"
              :model-value="form.UserMiddleName"
              type="text"
              :label="$t('middleName')"
              @change="onChangeMN"
            >
            </q-input>
            <!-- sex -->
            <q-select
              v-model="sex"
              :model-value="sex"
              :options="optSex"
              :label="$t('sex')"
              option-value="id"
              option-label="name"
              @update:model-value="fnSelectSex()"
            />
            <!-- Birth Day-->
            <q-input
              type="date"
              stack-label class="q-mt-md"
              :model-value="form['UserDateBirth']"
              v-model="form['UserDateBirth']"
              :label="$t('birthDay')"
              clearable
            >
            </q-input>


          </q-card-section>
        </div>
        <!---->
        <div class="col">
          <q-card-section>
            <!-- userGroup -->
            <q-input
              disable
              v-model="form.userGroup"
              :model-value="form.userGroup"
              type="text"
              :label="$t('groupUser')"
            />

            <!-- name -->
            <q-input
              disable
              v-model="form.name"
              :model-value="form.name"
              type="text"
              :label="$t('usrName')"
              :rules="[(val) => textTest(val) || $t('req')]"
              @change="onChangeNm"
              @blur="onBlur"
            />

            <!-- fullName -->
            <q-input
              disable
              v-model="form.fullName"
              :model-value="form.fullName"
              type="text"
              :label="$t('usrFullName')"
              :rules="[(val) => textTest(val) || $t('req')]"
              @change="onChangeFnm"
            />

            <!-- login -->
            <q-input
              disable
              v-model="form.login"
              :model-value="form.login"
              type="text"
              :label="$t('login')"
            />

            <!-- email -->
            <q-input
              disable
              v-model="form.UserEmail"
              :model-value="form.UserEmail"
              type="email"
              :label="$t('email')"
              :rules="[(val) => emailTest(val) || $t('req')]"
              @change="onChangeEmil"
            >
            </q-input>

            <!-- phone -->
            <q-input
              disable clearable
              v-model="form.UserPhone"
              :model-value="form.UserPhone"
              unmasked-value
              :label="$t('phone')"
              prefix="+7"
              mask="### ### ####"
              fill-mask="_"
              :rules="[(val) => val.length > 9 || $t('req')]"
              @change="onChangePhone"
            />


          </q-card-section>
        </div>
      </div>

      <q-card-actions align="right">
        <q-btn
          :loading="loading"
          color="primary"
          icon="save"
          :label="$t('save')"
          @click="onOKClick"
          :disable="validSave()"
        >
          <template #loading>
            <q-spinner-hourglass color="white"/>
          </template>
        </q-btn>
        <q-btn
          color="primary"
          icon="cancel"
          :label="$t('cancel')"
          @click="onCancelClick"
        />
      </q-card-actions>
    </q-card>
  </q-dialog>
</template>
<script>


import Treeselect from "vue3-treeselect"
import 'vue3-treeselect/dist/vue3-treeselect.css'

import {api, baseURL} from "boot/axios";
import {ref} from "vue";
import {notifyError, pack} from "src/utils/jsutils";
import {useUserStore} from "stores/user-store";

const store = useUserStore();
const {setUserName} = store;


export default {
  props: ["lg", "userId"],
  components: {Treeselect},

  data() {
    return {
      //
      form: {UserId: parseInt(this.userId, 10)},
      loading: ref(false),
      //
      cls: null,
      optCls: [],
      sex: null,
      optSex: [],
    };
  },

  emits: ["ok", "hide"],

  methods: {

    fnCloseCls(v) {
      //console.info("fnCloseCls", v)
      this.form.cls = v;
    },

    fnSelectCls(v) {
      //console.info("fnCloseCls", v)
      this.form.accessLevel = v.accessLevel;
    },

    normalizer(node) {
      return {
        id: node['ent'],
        label: node.name,
      };
    },

    fnSelectSex() {
      this.form.UserSex = this.sex.id
    },

    setUserData(data) {
      console.info("data", data)
      this.form.userGroup = data.userGroup
      this.form.name = data.name
      this.form.fullName = data.fullName
      this.form.login = data.login
      this.form.UserEmail = data.email
      this.form.UserPhone = data.phone

      this.form.UserSecondName = data.fullName
      this.form.UserFirstName = data.name
      let arr = data.fullName.split(" ")
      if (arr.length > 2)
        this.form.UserMiddleName = arr[2]
      else
        this.form.UserMiddleName = ""
    },

    onBlur() {
      this.form.fullName = this.form.name;
    },

    validSave() {
      return !(
        this.form.cls &&
        this.textTest(this.form.UserFirstName) &&
        this.textTest(this.form.UserSecondName) &&
        this.form.UserSex
      )

    },

    onChangeEmil() {

    },
    onChangePhone() {

    },
    onChangeSN() {

    },
    onChangeFN() {

    },
    onChangeMN() {

    },

    onChangeNm() {

    },

    onChangeFnm() {

    },

    emailTest: function (v) {
      return /^(?=[a-zA-Z0-9@._%+-]{6,254}$)[a-zA-Z0-9._%+-]{1,64}@(?:[a-zA-Z0-9-]{1,63}\.){1,8}[a-zA-Z]{2,63}$/.test(
        v
      );
    },

    textTest(val) {
      return !!val && !!val.trim();
    },

    show() {
      this.$refs.dialog.show();
    },
    hide() {
      this.$refs.dialog.hide();
    },

    onDialogHide() {
      this.$emit("hide");
    },

    onOKClick() {
      const store = useUserStore();
      const {setUserName} = store;

      //console.log("Form", this.form);

      this.loading = ref(true);
      api
        .post(baseURL, {
          method: "entity/saveProfile",
          params: [this.form],
        })
        .then(
          () => {
            setUserName(this.form.fullName)
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
    },

    onCancelClick() {
      this.hide();
    },
  },

  created() {
    this.loading = ref(true);
    api
      .post(baseURL, {
        method: "adm/loadAuthUser",
        params: [this.userId],
      })
      .then(
        (response) => {
          console.info("loadAuthUser", response.data.result.records)

          setUserName(response.data.result.records[0].fullName)
          this.setUserData(response.data.result.records[0]);
        },
        (error) => {
          console.info("error", error.response.data.error.message)
          let msgs = error.response.data.error.message.split("@")
          let m1 = this.$t(`${msgs[0]}`)
          let m2 = (msgs.length > 1) ? " [" + msgs[1] + "]" : ""
          let msg = m1 + m2
          notifyError(msg);
        }
      )
      .finally(() => {
        this.loading = ref(false);
      });

    //

    this.loading = ref(true);
    api
      .post(baseURL, {
        method: "meta/loadClsTree",
        params: [{typCod: "Typ_Users", typNodeVisible: false}],
      })
      .then(
        (response) => {
          //console.info("ClsTree", response.data.result.records)
          this.optCls = pack(response.data.result.records, "id");
        },
        (error) => {
          console.info("error 555", error.response.data.error.message)
          let msgs = error.response.data.error.message.split("@")
          console.info("msgs", msgs, msgs.length)
          let m1 = this.$t(`${msgs[0]}`)
          let m2 = (msgs.length > 1) ? " [" + msgs[1] + "]" : ""
          console.info("msg", m1, m2)

          let msg = m1 + m2
          notifyError(msg);
          //notifyError(error.response.data.error.message);
        }
      )
      .finally(() => {
        this.loading = ref(false);
      });

    //
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
      });


  },
};
</script>
