<template>
  <div class="q-pa-md">
    <q-splitter
      v-model="splitterModel"
      :model-value="splitterModel"
      after-class="overflow-hidden q-ml-sm"
      before-class="overflow-hidden q-mr-sm"
      class="bg-amber-1"
      separator-class="bg-red"
    >

      <template v-slot:before>

        <div class="q-mb-lg" style="font-size: 1.2em; font-weight: bold">
          <q-avatar color="black" icon="apartment" text-color="white">
          </q-avatar>
          {{ $t("struct_enterprise") }}
        </div>

        <div class="scroll" style="height: calc(100vh - 250px); width: 100%">
          <QTreeTable
            ref="childComp"
            :cols="cols"
            :icon_leaf="''"
            :rows="rows"
            checked_visible="true"
            @updateSelect="onUpdateSelect"
          />
        </div>
      </template>

      <template v-slot:after>

        <q-table
          v-model:selected="selected2" :columns="cols2"
          :loading="loading2"
          :rows="rows2"
          :rows-per-page-options="[0]"
          :table-colspan="4"
          :wrap-cells="true"
          card-class="bg-amber-1 text-brown"
          color="primary"
          dense
          row-key="id"
          selection="single"
          separator="cell"
          table-header-class="text-bold text-white bg-blue-grey-13"
        >

          <template v-slot:top>
            <div v-if="currentNode" style="font-size: 1.2em; font-weight: bold">
              <q-avatar :icon="currentNode['ent']===1024 ? 'apartment'
                          : currentNode['ent']===1025 ? 'store'
                          : currentNode['ent']===1026 ? 'business'
                          : 'biotech'" color="black"
                        text-color="white"/>
              {{ currentNode.name }}
            </div>

            <q-space/>
            <q-btn
              v-if="hasTarget(tagIns)"
              :disable="loading || !currentNode ||
              (currentNode['ent'] !== clsEnterPrise && parentId === 0) ||
              (currentNode['ent'] === clsEnterPrise && rows2.length===1)" color="secondary"
              dense
              icon="post_add"
              @click="editRow(null, 'ins')"
            >
              <q-tooltip transition-hide="rotate" transition-show="rotate">
                {{ $t("newRecord") }}
              </q-tooltip>
            </q-btn>
            <q-btn
              v-if="hasTarget(tagUpd)"
              :disable="loading2 || selected2.length === 0" class="q-ml-sm"
              color="secondary"
              dense
              icon="edit"
              @click="editRow(selected2[0], 'upd')"
            >
              <q-tooltip transition-hide="rotate" transition-show="rotate">
                {{ $t("editRecord") }}
              </q-tooltip>
            </q-btn>
            <q-btn
              v-if="hasTarget(tagDel)"
              :disable="loading2 || selected2.length === 0" class="q-ml-lg"
              color="red"
              dense
              icon="delete"
              @click="removeRow(selected2[0])"
            >
              <q-tooltip transition-hide="rotate" transition-show="rotate">
                {{ $t("deletingRecord") }}
              </q-tooltip>
            </q-btn>
          </template>

          <template #bottom-row>
            <q-td v-if="selected2.length > 0" colspan="100%">
              <span class="text-blue"> {{ $t("selectedRow") }}: </span>
              <span class="text-bold"> {{ this.infoSelected(selected2[0]) }} </span>
            </q-td>
            <q-td v-else-if="this.rows2.length > 0" class="text-bold" colspan="100%">
              {{ $t("infoRow") }}
            </q-td>
          </template>


        </q-table>

      </template>

    </q-splitter>

  </div>

</template>

<script>

import {defineComponent} from "vue";
import QTreeTable from "components/QTreeTable.vue";
import {api} from "boot/axios";
import {expandAll, hasTarget, notifyError, notifyInfo, pack} from "src/utils/jsutils";
import UpdaterStructEnterprise from "pages/struct_enterprise/UpdaterStructEnterprise.vue";

export default defineComponent({
  name: "StructEnterprise",
  components: {QTreeTable},

  data() {
    return {
      splitterModel: 20,
      cols: [],
      rows: [],
      currentNode: null,
      clsEnterPrise: 0,
      loading: false,

      cols2: [],
      rows2: [],
      selected2: [],
      loading2: false,
      parentId: 0,
      parentName: "",

      tagIns: "",
      tagUpd: "",
      tagDel: "",

    }
  },

  methods: {
    hasTarget,
    onUpdateSelect(data) {
      this.currentNode = data.selected !== undefined ? data.selected : null;
      this.selected2 = []
      let cls = this.currentNode ? this.currentNode["ent"] : 0;
      if (cls === 1024) {
        this.tagIns = "nsi:ose:ent:ins"
        this.tagUpd = "nsi:ose:ent:upd"
        this.tagDel = "nsi:ose:ent:del"
      }
      if (cls === 1025) {
        this.tagIns = "nsi:ose:otd:ins"
        this.tagUpd = "nsi:ose:otd:upd"
        this.tagDel = "nsi:ose:otd:del"
      }
      if (cls === 1026) {
        this.tagIns = "nsi:ose:fil:ins"
        this.tagUpd = "nsi:ose:fil:upd"
        this.tagDel = "nsi:ose:fil:del"
      }
      if (cls === 1026) {
        this.tagIns = "nsi:ose:lab:ins"
        this.tagUpd = "nsi:ose:lab:upd"
        this.tagDel = "nsi:ose:lab:del"
      }
      this.loadObj(cls)
    },

    editRow(row, mode) {
      let data = {
        accessLevel: 1,
        cls: this.currentNode["ent"],
      }
      if (mode === "ins") {
        if (this.currentNode["ent"] === this.clsEnterPrise)
          data.parent = null
        else
          data.parent = this.parentId
      } else if (mode === "upd") {
        //extend(true, data, this.selected2[0])
        Object.assign(data, this.selected2[0])
      }

      this.$q
        .dialog({
          component: UpdaterStructEnterprise,
          componentProps: {
            mode: mode,
            parentId: this.parentId,
            parentName: this.parentName,
            data: data,
            // ...
          },
        })
        .onOk((r) => {
          if (mode === "ins") {
            if (this.currentNode["ent"] === this.clsEnterPrise) {
              this.idNameParent()
            }
            this.rows2.push(r);
            this.selected2 = [];
            this.selected2.push(r);
          } else {
            for (let key in r) {
              if (r.hasOwnProperty(key)) {
                row[key] = r[key];
              }
            }
          }
        })
    },

    removeRow(rec) {
      this.$q
        .dialog({
          title: this.$t("confirmation"),
          message:
            this.$t("deleteRecord") +
            '<div style="color: plum">(' + rec.name + ' - ' + rec.fullName + ")</div>",
          html: true,
          cancel: true,
          persistent: true,
          focus: "cancel",
        })
        .onOk(() => {
          api
            .post('', {
              method: "data/deleteBranch",
              params: [rec.id],
            })
            .then(
              () => {
                this.loadObj(this.currentNode["ent"])
                this.selected2 = []
              })
            .then(() => {
              if (this.currentNode["ent"] === this.clsEnterPrise)
                this.idNameParent()
            })
            .catch(error => {
              /*
                            let msg = error.message
                            if (error.response.data.error.message.includes("@")) {
                              let msgs = error.response.data.error.message.split("@")
                              let m1 = this.$t(`${msgs[0]}`)
                              let m2 = (msgs.length > 1) ? " [" + msgs[1] + "]" : ""
                              msg = m1 + m2
                              notifyError(msg)
                            } else {
                              notifyError(msg)
                            }
              */
            })
        })
        .onCancel(() => {
          notifyInfo(this.$t("canceled"));
        })
    },

    getColumns1() {
      return [
        {
          name: "name",
          label: this.$t("fldName"),
          field: "name",
          align: "left",
          classes: "bg-blue-grey-1",
          headerStyle: "font-size: 1.2em; width:100%",
        },
      ]
    },

    getColumns2() {
      return [
        {
          name: "name",
          label: this.$t("fldName"),
          field: "name",
          align: "left",
          classes: "bg-blue-grey-1",
          headerStyle: "font-size: 1.2em; width:20%",
        },
        {
          name: "fullName",
          label: this.$t("fldFullName"),
          field: "fullName",
          align: "left",
          classes: "bg-blue-grey-1",
          headerStyle: "font-size: 1.2em; width:30%",
        },
        {
          name: "cmt",
          label: this.$t("fldCmt"),
          field: "cmt",
          align: "left",
          classes: "bg-blue-grey-1",
          headerStyle: "font-size: 1.2em; width: 50%",
        },
      ]
    },

    loadClsTree() {
      this.loading = true;
      api
        .post('', {
          method: "data/loadClsTree",
          params: [{typCod: 'Typ_Enterprise', typNodeVisible: false}],
        })
        .then(
          (response) => {
            this.rows = pack(response.data.result["records"], "ord")
            expandAll(this.rows)
          })
        .catch(error => {
          notifyError(this.$t(error.response.data.error.message))
        })
        .finally(() => {
          this.loading = false
        })

    },

    loadObj(cls) {
      this.loading2 = true;
      api
        .post('', {
          method: "data/loadObj",
          params: [cls],
        })
        .then(
          (response) => {
            this.rows2 = response.data.result["records"]
          })
        .catch(error => {
          notifyError(this.$t(error.response.data.error.message))
        })
        .finally(() => {
          this.loading2 = false
        })
    },

    idNameParent() {
      api
        .post('', {
          method: "data/idNameParent",
          params: [this.clsEnterPrise],
        })
        .then(
          (response) => {
            this.parentId = response.data.result.id
            this.parentName = response.data.result.name
            //notifySuccess(this.$t("success"))
          })
    },

    infoSelected(row) {
      return " " + row.name + " (" + row.fullName + ")"
    },
  },

  created() {
    this.cols = this.getColumns1()
    this.cols2 = this.getColumns2()
    this.loading = true
    api
      .post('', {
        method: "data/getClsIds",
        params: ["Cls_Enterprise"],
      })
      .then(
        (response) => {
          this.clsEnterPrise = response.data.result["Cls_Enterprise"]
          //notifySuccess(this.$t("success"))
        })
      .then(() => {
        this.idNameParent()
      })
      .catch(error => {
        //console.log(error.message)
        if (error.response.data.error.message.includes("@")) {
          let msgs = error.response.data.error.message.split("@")
          let m1 = this.$t(`${msgs[0]}`)
          let m2 = (msgs.length > 1) ? " [" + msgs[1] + "]" : ""
          let msg = m1 + m2
          notifyError(msg)
        } else {
          notifyError(error.message)
        }
      })
      .finally(() => {
        this.loading = false
      })


    this.loadClsTree()

  }

})

</script>

<style>

</style>
