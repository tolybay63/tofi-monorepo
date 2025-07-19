<template>
  <div class="q-pa-md">
    <q-splitter
      v-model="splitterModel"
      :model-value="splitterModel"
      before-class="overflow-hidden q-mr-sm"
      after-class="overflow-hidden q-ml-sm"
      separator-class="bg-red"
      class="bg-amber-1"
    >

    <template v-slot:before>

      <div style="font-size: 1.2em; font-weight: bold" class="q-mb-lg">
        <q-avatar color="black" text-color="white" icon="apartment">
        </q-avatar>
        {{ $t("struct_enterprise") }}
      </div>

      <QTreeTable
        :cols="cols"
        :rows="rows"
        :icon_leaf="''"
        @updateSelect="onUpdateSelect"
        checked_visible="true"
        ref="childComp"
      />

    </template>

    <template v-slot:after>

      <q-table
        color="primary" dense
        card-class="bg-amber-1 text-brown"
        row-key="id"
        :columns="cols2"
        :rows="rows2"
        :wrap-cells="true"
        :table-colspan="4"
        table-header-class="text-bold text-white bg-blue-grey-13"
        separator="cell"
        :loading="loading2"
        selection="single"
        v-model:selected="selected2"
        :rows-per-page-options="[0]"
      >

        <template v-slot:top>
          <div style="font-size: 1.2em; font-weight: bold" v-if="currentNode">
            <q-avatar color="black" text-color="white"
                      :icon="currentNode['ent']===1024 ? 'apartment'
                          : currentNode['ent']===1025 ? 'store'
                          : currentNode['ent']===1026 ? 'business'
                          : 'biotech'"/>
            {{ currentNode.name }}
          </div>

          <q-space/>
          <q-btn
            v-if="hasTarget(tagIns)"
            icon="post_add" dense
            color="secondary"
            :disable="loading || !currentNode ||
              (currentNode['ent'] !== clsEnterPrise && parentId === 0) ||
              (currentNode['ent'] === clsEnterPrise && rows2.length===1)"
            @click="editRow(null, 'ins')"
          >
            <q-tooltip transition-show="rotate" transition-hide="rotate">
              {{ $t("newRecord") }}
            </q-tooltip>
          </q-btn>
          <q-btn
            v-if="hasTarget(tagUpd)"
            icon="edit" dense
            color="secondary"
            class="q-ml-sm"
            :disable="loading2 || selected2.length === 0"
            @click="editRow(selected2[0], 'upd')"
          >
            <q-tooltip transition-show="rotate" transition-hide="rotate">
              {{ $t("editRecord") }}
            </q-tooltip>
          </q-btn>
          <q-btn
            v-if="hasTarget(tagDel)"
            icon="delete" dense
            color="secondary"
            class="q-ml-lg"
            :disable="loading2 || selected2.length === 0"
            @click="removeRow(selected2[0])"
          >
            <q-tooltip transition-show="rotate" transition-hide="rotate">
              {{ $t("deletingRecord") }}
            </q-tooltip>
          </q-btn>
        </template>

        <template #bottom-row>
          <q-td colspan="100%" v-if="selected2.length > 0">
            <span class="text-blue"> {{ $t("selectedRow") }}: </span>
            <span class="text-bold"> {{ this.infoSelected(selected2[0]) }} </span>
          </q-td>
          <q-td colspan="100%" v-else-if="this.rows2.length > 0" class="text-bold">
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
import {api, baseURL} from "boot/axios";
import {expandAll, hasTarget, notifyError, notifyInfo, pack} from "src/utils/jsutils";
import UpdaterStructEnterprise from "pages/struct_enterprise/UpdaterStructEnterprise.vue";
import {extend} from "quasar";

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
      if (cls===1024) {
        this.tagIns= "nsi:ose:ent:ins"
        this.tagUpd= "nsi:ose:ent:upd"
        this.tagDel= "nsi:ose:ent:del"
      }
      if (cls===1025) {
        this.tagIns= "nsi:ose:otd:ins"
        this.tagUpd= "nsi:ose:otd:upd"
        this.tagDel= "nsi:ose:otd:del"
      }
      if (cls===1026) {
        this.tagIns= "nsi:ose:fil:ins"
        this.tagUpd= "nsi:ose:fil:upd"
        this.tagDel= "nsi:ose:fil:del"
      }
      if (cls===1026) {
        this.tagIns= "nsi:ose:lab:ins"
        this.tagUpd= "nsi:ose:lab:upd"
        this.tagDel= "nsi:ose:lab:del"
      }
      this.loadObj(cls)
    },

    editRow(row, mode) {
      let data = {
        accessLevel: 1,
        cls: this.currentNode["ent"],
      }
      if (mode === "ins") {
        if (this.currentNode["ent"]===this.clsEnterPrise)
          data.parent = null
        else
          data.parent = this.parentId
      } else if (mode === "upd") {
        extend(true, data, this.selected2[0])
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
            if (this.currentNode["ent"]===this.clsEnterPrise) {
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
          this.$axios
            .post(baseURL, {
              method: "data/deleteOwner",
              params: [ rec.id, 1 ],
            })
            .then(
              () => {
                this.loadObj(this.currentNode["ent"])
                this.selected2 = []
              })
            .then(()=> {
              if (this.currentNode["ent"]===this.clsEnterPrise)
                this.idNameParent()
            })
            .catch(error => {
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
        .post(baseURL, {
          method: "data/loadClsTree",
          params: [{typCod: 'Typ_Enterprise', typNodeVisible: false}],
        })
        .then(
          (response) => {
            this.rows = pack( response.data.result["records"], "ord" )
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
        .post(baseURL, {
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
        .post(baseURL, {
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
      .post(baseURL, {
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
