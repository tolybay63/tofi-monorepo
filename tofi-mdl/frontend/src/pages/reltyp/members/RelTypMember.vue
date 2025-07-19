<template>
  <div class="no-padding no-margin">

    <q-table
        style="height: calc(100vh - 250px); width: 100%"
        color="primary"
        card-class="bg-amber-1"
        row-key="id"
        :columns="cols"
        :rows="rows"
        :wrap-cells="true"
        table-header-class="text-bold text-white bg-blue-grey-13"
        separator="cell"
        :loading="loading"
        :dense="dense"
        selection="single"
        v-model:selected="selected"
        @update:selected="updSelected"
        :rows-per-page-options="[0]"
    >
      <template v-slot:body="props">
        <q-tr :props="props">
          <td style="width: 5px">
            <q-btn
                :dense="dense"
                flat
                color="blue"
                :icon="
                    selected.length === 1 && props.row.id === selected[0].id
                      ? 'check_box'
                      : 'check_box_outline_blank'
                  "
                @click="selectedRow(props.row)"
            >
            </q-btn>
          </td>
          <q-td key="name" :props="props">
            {{ props.row.name }}
          </q-td>

          <q-td key="fullName" :props="props">
            {{ props.row.fullName }}
          </q-td>

          <q-td key="memberType" :props="props">
            <q-icon
                size="24px"
                :name="getIcon(props.row)"
                :color="getIconColor()"
            ></q-icon>
            {{ getMemberType(props.row.memberType) }}
          </q-td>

          <q-td key="memberName" :props="props">
            {{ props.row.memberName }}
          </q-td>

          <q-td key="cmt" :props="props">
            {{ props.row.cmt }}
          </q-td>
        </q-tr>
      </template>

      <template #bottom-row>
        <q-td colspan="100%" v-if="selected.length > 0">
          <span class="text-blue"> {{ $t("selectedRow") }}: </span>
          <span class="text-bold"> {{ this.infoSelected(selected[0]) }} </span>
        </q-td>
        <q-td
            colspan="100%"
            v-else-if="this.rows.length > 0"
            class="text-bold"
        >
          {{ $t("infoRow") }}
        </q-td>
      </template>

      <template v-slot:top>
        <div style="font-size: 1.2em; font-weight: bold;">{{ $t("membersRel") }}</div>

        <q-space/>
        <q-btn
            v-if="hasTarget('mdl:mn_ds:reltyp:memb:ins')"
            :dense="dense"
            icon="post_add"
            color="secondary"
            :disable="loading"
            @click="editRow(null, 'ins')"
        >
          <q-tooltip transition-show="rotate" transition-hide="rotate">
            {{ $t("newRecord") }}
          </q-tooltip>
        </q-btn>
        <q-btn
            v-if="hasTarget('mdl:mn_ds:reltyp:memb:upd')"
            :dense="dense"
            icon="edit"
            color="secondary"
            class="q-ml-sm"
            :disable="loading || selected.length === 0"
            @click="editRow(selected[0], 'upd')"
        >
          <q-tooltip transition-show="rotate" transition-hide="rotate">
            {{ $t("editRecord") }}
          </q-tooltip>
        </q-btn>
        <q-btn
            v-if="hasTarget('mdl:mn_ds:reltyp:memb:del')"
            :dense="dense"
            icon="delete"
            color="secondary"
            class="q-ml-sm"
            :disable="loading || selected.length === 0"
            @click="removeRow(selected[0])"
        >
          <q-tooltip transition-show="rotate" transition-hide="rotate">
            {{ $t("deletingRecord") }}
          </q-tooltip>
        </q-btn>

        <q-btn
            v-if="hasTarget('mdl:mn_ds:reltyp:memb:up')"
            :dense="dense"
            icon="swipe_up_alt"
            color="secondary"
            class="q-ml-lg"
            @click="fnUp(true)"
            :disable="onoffUp()"
        >
          <q-tooltip transition-show="rotate" transition-hide="rotate">
            {{ $t("up") }}
          </q-tooltip>
        </q-btn>

        <q-btn
            v-if="hasTarget('mdl:mn_ds:reltyp:memb:down')"
            :dense="dense"
            icon="swipe_down_alt"
            color="secondary"
            class="q-ml-sm"
            @click="fnUp(false)"
            :disable="onoffDown()"
        >
          <q-tooltip transition-show="rotate" transition-hide="rotate">
            {{ $t("down") }}
          </q-tooltip>
        </q-btn>
      </template>

      <template #loading>
        <q-inner-loading showing color="secondary"></q-inner-loading>
      </template>
    </q-table>

  </div>
</template>

<script>
import {ref} from "vue";
import {api, baseURL} from "boot/axios";
import {hasTarget, notifyError, notifyInfo, notifySuccess} from "src/utils/jsutils";
import FD_Consts from "pages/all-consts.js";
import UpdateRelTypMember from "pages/reltyp/members/UpdateRelTypMember.vue";

export default {
  name: "RelTypMember",

  data() {
    //console.log("data")
    return {
      cols: [],
      rows: [],
      loading: ref(false),
      selected: ref([]),
      FD_MemberType: null,
      dense: true,
      maxLen: 0,
      reltypId: 0,
    };
  },

  methods: {
    hasTarget,
    getIconColor() {
      return "orange";
      /*
      if (row.isOwn === 1)
        return 'green'
      else
        return 'dark-gray'
*/
    },

    getIcon(row) {
      if (row.memberType === 1) return "square";
      else if (row.memberType === 2) return "group_work";
      //else return "hexagon";
    },

    getMemberType(val) {
      return this.FD_MemberType ? this.FD_MemberType.get(val) : null;
    },

    selectedRow(item) {
      let vm = this;
      if (vm.selected.length > 0 && item.id === vm.selected[0].id) {
        vm.selected = [];
      } else {
        vm.selected = [];
        vm.selected.push(item);
        this.updSelected(vm.selected);
      }
      //this.currentNode = vm.selected[0] !== undefined ? vm.selected[0] : null
      //temporary
      //console.log("selected", vm.selected[0])
      //console.log("currentNode", this.currentNode)
    },

    fnUp(up) {
      api
          .post(baseURL, {
            method: "reltyp/changeOrdMember",
            params: [{rec: this.selected[0], up: up}],
          })
          .then(
              () => {
                //reload...
                this.fetchData(this.reltypId);
              },
              (error) => {
                let msg = error.response.data.error.message
                    ? error.response.data.error.message
                    : error.message;
                notifyError(msg);
              }
          );
    },
    onoffUp() {
      //console.log("selected[0]", this.selected[0])
      if (this.selected[0] === undefined) return true;
      else {
        return this.indexOf(this.selected[0].id) <= 0;
      }
    },
    onoffDown() {
      if (this.selected[0] === undefined) return true;
      else {
        return this.indexOf(this.selected[0].id) >= this.maxLen - 1;
      }
    },
    indexOf: function (id) {
      let rez = -1;
      this.rows.forEach((row, index) => {
        if (row.id === id) {
          rez = index;
        }
      });
      return rez;
    },

    updSelected() {
      //console.log("cur", cur)
    },

    removeRow(rec) {
      //console.log("Delete Row:", JSON.stringify(rec))

      this.$q
          .dialog({
            title: this.$t("confirmation"),
            message:
                this.$t("deleteRecord") +
                '<div style="color: plum">(' +
                rec.name +
                ")</div>",
            html: true,
            cancel: true,
            persistent: true,
            focus: "cancel",
          })
          .onOk(() => {
            let index = this.rows.findIndex((row) => row.id === rec.id);
            api
                .post(baseURL, {
                  method: "reltyp/deleteRelTypMember",
                  params: [{rec: rec}],
                })
                .then(
                    () => {
                      //console.log("response=>>>", response.data)
                      this.rows.splice(index, 1);
                      this.selected = ref([]);
                      notifySuccess(this.$t("success"));
                    },
                    (error) => {
                      let msg = error.message;
                      if (error.response) msg = error.response.data.error.message;

                      notifyError(msg);
                    }
                );
          })
          .onCancel(() => {
            notifyInfo(this.$t("canceled"));
          });
    },

    editRow(rec, mode) {
      let data = {
        id: 0,
        name: "",
        fullName: "",
        relTyp: this.reltypId,
        memberType: FD_Consts.FD_MemberType.typ,
        card: 0,
        typ: 0,
        relTypMemb: 0,
        role: 0,
        cmt: null,
      };
      if (mode === "upd") {
        data = {
          id: rec.id,
          name: rec.name,
          fullName: rec.fullName,
          relTyp: rec.relTyp,
          memberType: rec.memberType,
          card: rec.card,
          typ: rec.typ,
          relTypMemb: rec.relTypMemb,
          role: rec.role,
          ord: rec.ord,
          cmt: rec.cmt,
        };
      }
      //const upd = {isIns: ins};
      const lg = {name: this.lang};

      //console.log("data",data)

      this.$q
          .dialog({
            component: UpdateRelTypMember,
            componentProps: {
              data: data,
              mode: mode,
              lg: lg,
              dense: true,
              // ...
            },
          })
          .onOk((r) => {
            //console.log("Ok! updated", r);
            if (mode === "ins") {
              this.rows.push(r);
              this.selected = [];
              this.selected.push(r);
            } else {
              for (let key in r) {
                if (r.hasOwnProperty(key)) {
                  rec[key] = r[key];
                }
              }
            }
          });
    },

    fetchData(reltyp) {
      this.loading = ref(true);

      api
          .post(baseURL, {
            method: "reltyp/loadRelTypMember",
            params: [reltyp],
          })
          .then((response) => {
            this.rows = response.data.result.records;
            this.maxLen = this.rows.length;
            //
            if (this.selected.length > 0) {
              let curId = this.selected[0].id;
              this.selected = ref([]);
              let index = this.rows.findIndex((row) => row.id === curId);
              this.selected[0] = this.rows[index];
            }
          })
          .catch((error) => {

            let msg = error.message;
            if (error.response) msg = this.$t(error.response.data.error.message);

            notifyError(msg);
          })
          .finally(() => {
            this.loading = ref(false);
          });
    },

    getColumns() {
      return [
        {
          name: "name",
          label: this.$t("fldName"),
          field: "name",
          align: "left",
          classes: "bg-blue-grey-1",
          headerStyle: "font-size: 1.2em",
          style: "width: 20%",
        },
        {
          name: "fullName",
          label: this.$t("fldFullName"),
          field: "fullName",
          align: "left",
          classes: "bg-blue-grey-1",
          headerStyle: "font-size: 1.2em",
          style: "width: 25%",
        },
        {
          name: "memberType",
          label: this.$t("memberType"),
          field: "memberType",
          align: "left",
          classes: "bg-blue-grey-1",
          headerStyle: "font-size: 1.2em",
          style: "width: 10%",
          format: (val) =>
              this.FD_MemberType ? this.FD_MemberType.get(val) : null,
        },
        {
          name: "memberName",
          label: this.$t("memberName"),
          field: "memberName",
          align: "left",
          classes: "bg-blue-grey-1",
          headerStyle: "font-size: 1.2em; width: 25%",
        },

        {
          name: "cmt",
          label: this.$t("fldCmt"),
          field: "cmt",
          align: "left",
          classes: "bg-blue-grey-1",
          headerStyle: "font-size: 1.2em",
          style: "width: 20%",
        },
      ];
    },

    infoSelected(row) {
      //this.fetchData2(row.id)
      return " " + row.name;
    },

  },

  created() {
    //console.log("create")
    this.lang = localStorage.getItem("curLang");
    this.lang = this.lang === "en-US" ? "en" : this.lang;

    api
        .post(baseURL, {
          method: "dict/load",
          params: [{dict: "FD_MemberType"}],
        })
        .then((response) => {
          this.FD_MemberType = new Map();
          response.data.result.records.forEach((it) => {
            this.FD_MemberType.set(it["id"], it["text"]);
          });
          //console.log("FD_MemberType", this.FD_MemberType)
        });

    this.cols = this.getColumns();
  },

  mounted() {
    //console.log("mounted")
    this.reltypId = parseInt(this.$route["params"].reltyp, 10);
    this.fetchData(this.reltypId);
  },

  setup() {
  }

};
</script>

<style scoped></style>
