<template>
  <q-dialog
    ref="dialog"
    @hide="onDialogHide"
    persistent
    autofocus
    transition-show="slide-up"
    transition-hide="slide-down"
  >
    <q-card class="q-dialog-plugin" style="width: 600px">
      <q-bar class="text-white bg-primary" v-if="form.id > 0">
        <div>{{ $t("editRecord") }}</div>
      </q-bar>
      <q-bar class="text-white bg-primary" v-else>
        <div>{{ $t("newRecord") }}</div>
      </q-bar>
      <q-inner-loading :showing="visible" color="secondary" />

      <q-card>
        <q-card-section>
          <q-bar class="bg-white no-padding no-margin">
            <q-input
              v-if="reqParams.dependPeriod"
              v-model="dt"
              stack-label
              :model-value="dt"
              type="date"
              dense
              style="width: 100px; margin-right: 20px"
              :disable="form.isComplex === true"
              :label="$t('date')"
              @update:model-value="fnDt"
            />

            <!-- todo :disable="mode==='upd'" -->
            <q-select
              v-if="reqParams.dependPeriod"
              v-model="form.periodType"
              :model-value="form.periodType"
              dense
              options-dense
              :options="optionsPeriod"
              :disable="mode === 'upd'"
              :label="$t('periodType')"
              option-value="id"
              option-label="text"
              map-options
              style="width: 200px"
              @update:model-value="fnPeriod"
            />
          </q-bar>

          <q-select
            v-if="requestParams.providerTyp > 0"
            class="q-pt-md"
            v-model="form.provider"
            :model-value="form.provider"
            use-input
            map-options
            input-debounce="0"
            :label="$t('providerTyp')"
            :options="optionsProvider"
            option-value="id"
            option-label="name"
            :dense="dense"
            :options-dense="dense"
            :disable="mode === 'upd'"
            @update:model-value="inputValueProvider"
            @filter="filterFnProvider"
          >
            <template v-slot:no-option>
              <q-item>
                <q-item-section class="text-grey">
                  {{ $t("noResults") }}
                </q-item-section>
              </q-item>
            </template>
          </q-select>

          <q-select
            v-if="requestParams.statusFactor > 0"
            class="q-pt-md"
            v-model="form.status"
            map-options
            :model-value="form.status"
            use-input
            input-debounce="0"
            :label="$t('statusFactor')"
            :options="optionsStatus"
            option-value="id"
            option-label="name"
            :dense="dense"
            :options-dense="dense"
            :disable="mode === 'upd'"
            @update:model-value="inputValueStatus"
            @filter="filterFnStatus"
          >
            <template v-slot:no-option>
              <q-item>
                <q-item-section class="text-grey">
                  {{ $t("noResults") }}
                </q-item-section>
              </q-item>
            </template>
          </q-select>

          <div v-if="mode === 'ins' && isFile()">
            <q-input
              autofocus
              :dense="dense"
              type="file"
              :model-value="file"
              :clearable="true"
              @update:model-value="updFile"
              @clear="clrFile"
              class="q-mt-md"
            />
          </div>

          <div v-if="mode === 'upd' && isFile()">
            <div class="q-mt-md text-blue-grey-5" style="font-size: smaller">{{ $t("fileName") }}</div>
            <div> {{form.fileName}} </div>
          </div>

          <div v-if="isStrVal()">
            <!-- str  -->
            <div v-if="reqParams.attribValType === 1">
              <q-input
                autofocus
                :dense="dense"
                :model-value="form.strVal"
                v-model="form.strVal"
                :label="$t('val')"
              />
            </div>
            <!-- mask  -->
            <div v-else-if="reqParams.attribValType === 2">
              <q-input
                autofocus
                :dense="dense"
                :model-value="form.strVal"
                v-model="form.strVal"
                :label="$t('val')"
                :mask="reqParams.maskReg"
                :hint="getMask()"
              />
            </div>

          </div>

          <div v-if="isDate()">
            <!-- d  -->
            <div v-if="reqParams.attribValType === 3">
              <q-input
                autofocus
                :dense="dense"
                :model-value="form.dateTimeVal"
                v-model="form.dateTimeVal"
                :label="$t('val')"
                type="date"
                stack-label
                class="q-mt-md"
              />
            </div>
            <!-- t  -->
            <div v-else-if="reqParams.attribValType === 4">
              <q-input
                autofocus
                :dense="dense"
                :model-value="form.dateTimeVal"
                v-model="form.dateTimeVal"
                :label="$t('val')"
                type="time"
                stack-label
              />
            </div>
            <!-- dt  -->
            <div v-else-if="reqParams.attribValType === 5" class="row">
              <q-input
                autofocus
                :dense="dense"
                v-model="form.dateTimeVal"
                :model-value="form.dateTimeVal"
                stack-label
                :label="$t('val')"
                type="date"
                style="width: 100px"
                class="q-mt-md"
              />

              <q-input
                autofocus
                :dense="dense"
                :model-value="t"
                v-model="t"
                type="time"
                style="width: 80px"
                class="q-ml-lg q-mt-lg"
                label=" "
              />
            </div>
          </div>

          <div v-if="isMultiStrVal()">
            <q-input
              autofocus
              :dense="dense"
              :model-value="form.multiStrVal"
              v-model="form.multiStrVal"
              :label="$t('val')"
              type="textarea"
              class="q-mt-lg"
            />
          </div>

          <!-- entity (propval) -->

          <div v-if="isEntity()">
            <q-select
              class="q-pt-md"
              v-model="form.propVal"
              :model-value="form.propVal"
              use-input
              map-options
              input-debounce="0"
              :label="$t('val')"
              :options="optionsPropVal"
              option-value="id"
              option-label="name"
              :dense="dense"
              :options-dense="dense"
              autofocus
              @update:model-value="inputValuePropVal"
              @filter="filterFnPropVal"
            >
              <template v-slot:no-option>
                <q-item>
                  <q-item-section class="text-grey">
                    {{ $t("noResults") }}
                  </q-item-section>
                </q-item>
              </template>
            </q-select>
          </div>

          <q-input
            :dense="dense"
            type="date"
            stack-label
            :disable="form.isComplex === true /*reqParams.periodType > 0*/"
            :model-value="form.dbeg"
            v-model="form.dbeg"
            class="q-mt-lg"
            :label="$t('fldDbeg')"
          >
          </q-input>

          <q-input
            :dense="dense"
            type="date"
            stack-label
            :disable="form.isComplex === true /*reqParams.periodType > 0*/"
            :model-value="form.dend"
            v-model="form.dend"
            class="q-mt-lg"
            :label="$t('fldDend')"
          >
          </q-input>

          <!-- cmt -->
          <q-input
            :dense="dense"
            :model-value="form.cmt"
            v-model="form.cmt"
            type="textarea"
            :label="$t('fldCmt')"
            class="q-mt-lg"
          >
          </q-input>
        </q-card-section>
      </q-card>

      <q-card-actions align="right">
        <q-btn
          :dense="dense"
          color="primary"
          icon="save"
          :label="$t('save')"
          @click="onOKClick"
          :disable="validSave()"
        />
        <q-btn
          :dense="dense"
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
import {api, baseURL} from "boot/axios";
import {notifyError, notifyInfo, notifySuccess} from "src/utils/jsutils";
import {ref} from "vue";
import allConsts from "pages/all-consts";
import {extend} from "quasar";
import {useParamsStore} from "stores/params-store";
import {storeToRefs} from "pinia";
const storeParams = useParamsStore();
const { getModel, getMetaModel } = storeToRefs(storeParams);

export default {
  props: ["rec", "requestParams", "mode", "lg", "dense"],

  data() {
    let r = {}

    Object.assign("Params", this.requestParams)
    extend(true, r, this.rec)

    let tmTmp = "00:00"
    //let dtTmp = this.today()
    if (this.mode === "upd") {
      //console.info("!!!!!", r.dateTimeVal)
      if (
        this.requestParams.attribValType === 3 &&
        r.dateTimeVal !== undefined
      ) {
        r.dateTimeVal = r.dateTimeVal.substring(0, 10)
      } else if (
        this.requestParams.attribValType === 4 &&
        r.dateTimeVal !== undefined
      ) {
        r.dateTimeVal = r.dateTimeVal.substring(11, 16)
        tmTmp = r.dateTimeVal.substring(11, 16)
      } else if (
        this.requestParams.attribValType === 5 &&
        r.dateTimeVal !== undefined
      ) {
        //dtTmp = r.dateTimeVal
        tmTmp = r.dateTimeVal.substring(11, 16)
        r.dateTimeVal = r.dateTimeVal.substring(0, 10)
      }
      r.propVal = r.propVal===0 ? null : r.propVal
    } else {
      r.propVal = null
    }

    //console.log("upd data r:", r, this.requestParams)

    return {
      form: r,
      reqParams: this.requestParams,
      visible: ref(false),
      periodTypeName: this.$t("notDepend"),

      t: tmTmp,

      dt: r.dbeg,

      optionsProvider: [],
      optionsProviderOrg: [],

      optionsStatus: [],
      optionsStatusOrg: [],

      optionsPeriod: [],
      FD_PeriodType: null,

      optionsPropVal: [],
      optionsPropValOrg: [],
      file: ref(null),
    };
  },

  emits: [
    // REQUIRED
    "ok",
    "hide",
  ],

  methods: {

    clrFile() {
      this.file = ref(null)
      //console.log("FILE(clr)", this.file)
    },

    updFile(val) {
      if (val !== null) {
        this.file = val[0]
        //console.log("fn", this.file.name)
      }
    },

    getMask() {
      return this.$t("mask") + ": " + this.reqParams.maskReg
    },

    fnDt(val) {
      this.getPeriodInfo(val, this.form.periodType)
    },

    fnPeriod(val) {
      this.getPeriodInfo(this.dt, val.id)
    },

    inputValuePropVal(val) {
      this.form.propVal = val.id
    },

    filterFnPropVal(val, update) {
      if (val === null || val === "") {
        update(() => {
          this.optionsPropVal = this.optionsPropValOrg
        })
        return
      }
      update(() => {
        if (this.optionsPropValOrg.length < 2) return
        const needle = val.toLowerCase();
        let name = "name"
        this.optionsPropVal = this.optionsPropValOrg.filter((v) => {
          return v[name].toLowerCase().indexOf(needle) > -1
        })
      })
    },

    inputValueProvider(val) {
      this.form.provider = val.id
    },

    inputValueStatus(val) {
      this.form.status = val.id
    },

    filterFnStatus(val, update) {
      if (val === null || val === "") {
        update(() => {
          this.optionsStatus = this.optionsStatusOrg
        });
        return
      }
      update(() => {
        if (this.optionsStatusOrg.length < 2) return
        const needle = val.toLowerCase()
        let name = "name"
        this.optionsStatus = this.optionsStatusOrg.filter((v) => {
          return v[name].toLowerCase().indexOf(needle) > -1
        })
      })
    },

    filterFnProvider(val, update) {
      if (val === null || val === "") {
        update(() => {
          this.optionsProvider = this.optionsProviderOrg
        })
        return
      }
      update(() => {
        if (this.optionsProviderOrg.length < 2) return
        const needle = val.toLowerCase()
        let name = "name"
        this.optionsProvider = this.optionsProviderOrg.filter((v) => {
          return v[name].toLowerCase().indexOf(needle) > -1
        })
      })
    },

    today() {
      let d = new Date()
      let currDate = d.getDate()
      let currMonth = d.getMonth() + 1
      let currYear = d.getFullYear()
      return (
        currYear +
        "-" +
        (currMonth < 10 ? "0" + currMonth : currMonth) +
        "-" +
        (currDate < 10 ? "0" + currDate : currDate)
      )
    },

    validSave() {
      return (
        (this.mode === "ins" && this.isFile() && this.file === null) ||
        (this.isStrVal() && this.form.strVal === undefined) ||
        (this.isMultiStrVal() && this.form.multiStrVal === undefined) ||
        (this.isDate() && this.form.dateTimeVal === undefined) ||
        (this.isEntity() && this.form.propVal === undefined)
      )
    },

    isFile() {
      return (
        this.requestParams.attribValType === allConsts.FD_AttribValType.file
      )
    },

    isStrVal() {
      return (
        this.requestParams.attribValType === allConsts.FD_AttribValType.str ||
        this.requestParams.attribValType === allConsts.FD_AttribValType.mask
      )
    },

    isMultiStrVal() {
      return (
        this.requestParams.attribValType === allConsts.FD_AttribValType.multistr
      )
    },

    isDate() {
      return (
        this.requestParams.attribValType === allConsts.FD_AttribValType.dt ||
        this.requestParams.attribValType === allConsts.FD_AttribValType.dttm ||
        this.requestParams.attribValType === allConsts.FD_AttribValType.tm
      )
    },

    isEntity() {
      return (
        this.requestParams.attribValType === allConsts.FD_AttribValType.entity
      )
    },

    // following method is REQUIRED
    // (don't change its name --> "show")
    show() {
      this.$refs.dialog.show()
    },

    // following method is REQUIRED
    // (don't change its name --> "hide")
    hide() {
      this.$refs.dialog.hide()
    },

    setFields() {
      if (this.isStrVal()) {
        this.form.dateTimeVal = null
        this.form.multiStrVal = null
        this.form.propVal = null
        this.form.fileVal = null
      } else if (this.isMultiStrVal()) {
        this.form.dateTimeVal = null
        this.form.strVal = null
        this.form.propVal = null
        this.form.fileVal = null
      } else if (this.isDate()) {
        if (this.reqParams.attribValType === 4) {
          //console.info("dateTimeVal", this.form.dateTimeVal)
          this.form.dateTimeVal = "0000-01-01T" + this.t //this.form.dateTimeVal
        } else if (this.reqParams.attribValType === 5) {
          //console.info("dateTimeVal", this.form.dateTimeVal)
          //console.info("t", this.t)
          this.form.dateTimeVal =
            this.form.dateTimeVal.substring(0, 10) + "T" + this.t

          //console.info("dateTimeVal 2", this.form.dateTimeVal)
        }

        this.form.strVal = null
        this.form.multiStrVal = null
        this.form.propVal = null
        this.form.fileVal = null
      } else if (this.reqParams.attribValType === 10) {
        this.form.strVal = null
        this.form.multiStrVal = null
        this.form.dateTimeVal = null
        this.form.fileVal = null
      } else if (this.reqParams.attribValType === 11) {
        this.form.strVal = null
        this.form.multiStrVal = null
        this.form.dateTimeVal = null
        this.form.propVal = null
      }
    },

    onDialogHide() {
      // required to be emitted
      // when QDialog emits "hide" event
      this.$emit("hide")
    },

    onOKClick() {
      // on OK, it is REQUIRED to
      // emit "ok" event (with optional payload)
      // before hiding the QDialog
      this.visible = true
      this.form.model = getModel.value
      this.form.metamodel = getMetaModel.value
      this.form.objorrelobj = this.reqParams.owner
      this.form.isObj = this.reqParams.isObj ? 1 : 0
      this.form.isUniq = this.reqParams.isUniq
      this.form.prop = this.reqParams.prop
      this.form.attribValType = this.reqParams.attribValType
      this.form.mode = this.mode
      if (this.reqParams.parent > 0) this.form.parent = this.reqParams.parent
      let err = false

      if (this.mode === "ins" && this.isFile()) {
        //console.info("this.reqParams", this.reqParams)
        //console.info("this.form", this.form)
        extend(this.form, { filename: this.file.name })
        let fd = new FormData()
        fd.append("file", this.file)
        fd.append("params", JSON.stringify(this.form))

        api
          .post("/upload", fd, {
            headers: {
              "Content-Type": "multipart/form-data",
            },
          })
          .then(() => {
            err = false;
            this.$emit("ok", { res: true })
            notifySuccess(this.$t("success"))
            //console.info("file", resp.data)
            //console.log('SUCCESS!!');
          })
          .catch(error => {
            err = true
            //console.log("error.response.data=>>>", error.response.data.error.message)
            notifyError(error.response.data.error.message)
          })
          .finally(() => {
            this.visible = ref(false)
            if (!err) this.hide()
          });
      } else {
        this.setFields()
        //console.info("this.reqParams", this.reqParams)

        const method =
          this.mode === "ins" ? "insertAttribValue" : "updateAttribValue"

        api
          .post(baseURL, {
            method: "data/" + method,
            params: [this.form],
          })
          .then(
            () => {
              err = false;
              //console.info("response FlatTable Ins", response.data.result.records[0])
              this.$emit("ok", { res: true })
              notifySuccess(this.$t("success"))
            })
          .catch(error => {
            err = true
            notifyError(error.response.data.error.message)
          })
          .finally(() => {
            this.visible = ref(false)
            if (!err) this.hide()
          })
      }
    },

    onCancelClick() {
      // we just need to hide the dialog
      this.hide()
    },

    getPeriodInfo(dt, periodType) {
      api
        .post(baseURL, {
          method: "data/getPeriodInfo",
          params: [dt, periodType],
        })
        .then(
          (response) => {
            this.form.dbeg = response.data.result.dbeg
            this.form.dend = response.data.result.dend
            this.form.periodType = periodType
            this.periodTypeName = response.data.result.periodTypeName
          })
        .catch(error => {
          let msg = error.message
          //if (error.response.data.error.message)
          //msg = error.response.data.error.message
          notifyError(msg)
        })
        .finally(() => {
          this.loading = ref(false);
        })
    },
  },

  created() {
    if (this.form.periodType > 0)
      this.getPeriodInfo(this.dt, this.form.periodType)

    api
      .post(baseURL, {
        method: "data/loadDict",
        params: ["FD_PeriodType"],
      })
      .then((response) => {
        this.FD_PeriodType = response.data.result.records
      })

    if (this.reqParams.providerTyp > 0) {
      api
        .post(baseURL, {
          method: "data/loadProvider",
          params: [this.reqParams.prop, getModel.value, getMetaModel.value],
        })
        .then((response) => {
          this.optionsProviderOrg = response.data.result.records
          this.optionsProvider = response.data.result.records
          if (this.optionsProvider.length === 0) {
            notifyInfo(this.$t("notProvider"))
          } else {
            if (this.form.provider === undefined) {
              this.form.provider = this.optionsProvider[0].id
            }
          }
        })
    }
    //
    if (this.reqParams.statusFactor > 0) {
      api
        .post(baseURL, {
          method: "data/loadStatus",
          params: [this.reqParams.prop],
        })
        .then((response) => {
          this.optionsStatusOrg = response.data.result.records
          this.optionsStatus = response.data.result.records
          if (this.optionsStatus.length === 0) {
            notifyInfo(this.$t("notStatus"))
          } else if (this.form.status === undefined) {
            this.form.status = this.optionsStatus[0].id
          }
        })
    }
    //
    if (this.reqParams.periodType > 0) {
      api
        .post(baseURL, {
          method: "data/propPeriodType",
          params: [this.reqParams.prop],
        })
        .then((response) => {
          this.optionsPeriod = [];
          this.FD_PeriodType.forEach((r) => {
            if (response.data.result.includes(r.id)) {
              this.optionsPeriod.push(r)
            }
            if (
              this.form.periodType === undefined &&
              this.form.dt === undefined
            ) {
              this.dt = this.today()
              this.form.periodType = this.optionsPeriod[0].id
              this.getPeriodInfo(this.dt, this.form.periodType)
            }
          })
        })
        .finally(() => {})
    } else {
      if (this.form.dbeg === "1800-01-01") {
        this.form.dbeg = null
      }
      if (this.form.dend === "3333-12-31") {
        this.form.dend = null
      }
    }

    if (
      this.requestParams.attribValType === allConsts.FD_AttribValType.entity
    ) {
      api
        .post(baseURL, {
          method: "data/loadPropValEntityForSelect",
          params: [this.reqParams.prop, this.reqParams.entityType],
        })
        .then((response) => {
          this.optionsPropVal = response.data.result.records
          this.optionsPropValOrg = response.data.result.records
          if (this.optionsPropVal.length === 0) {
            this.form.propVal = null
            notifyInfo(this.$t("notPossibleValue"))
          }
        });
    }

    return {}
  },
}
</script>
