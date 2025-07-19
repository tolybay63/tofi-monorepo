
<template>
  <div class="no-padding no-margin">

    <q-table
        style="height: 100%; width: 100%"
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
        <div style="font-size: 1.2em; font-weight: bold;">{{ $t("membersRelCls") }}</div>

        <q-space/>

        <q-btn
            v-if="hasTarget('mdl:mn_ds:reltyp:memb:upd')"
            :dense="dense"
            icon="edit"
            color="secondary"
            class="q-ml-sm"
            :disable="loading || selected.length === 0"
            @click="editRow(selected[0])"
        >
          <q-tooltip transition-show="rotate" transition-hide="rotate">
            {{ $t("editRecord") }}
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
import {hasTarget, notifyError} from "src/utils/jsutils";
import UpdateRelClsMember from "pages/reltyp/relcls/UpdateRelClsMember.vue";

export default {
  name: "RelClsMember",
  props: ["relcls"],

  data() {
    //console.log("data")
    return {
      cols: [],
      rows: [],
      loading: ref(false),
      selected: ref([]),
      FD_MemberType: null,
      dense: true,

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
      if (row.memberType === 3) return "square";
      else if (row.memberType === 4) return "group_work";
      //else return "hexagon";
    },

    getMemberType(val) {
      return this.FD_MemberType ? this.FD_MemberType.get(val) : null;
    },

    updSelected() {
    },

    selectedRow(item) {
      let vm = this;
      if (vm.selected.length > 0 && item.id === vm.selected[0].id) {
        vm.selected = [];
      } else {
        vm.selected = [];
        vm.selected.push(item);
      }
    },

    editRow(rec) {
      let data = {
        id: rec.id,
        relCls: rec.relCls,
        name: rec.name,
        fullName: rec.fullName,
        memberType: rec.memberType,
        card: rec.card,
        cls: rec.cls,
        relClsMemb: rec.relClsMemb,
        cmt: rec.cmt,
      }

      const lg = {name: this.lang};

      this.$q
          .dialog({
            component: UpdateRelClsMember,
            componentProps: {
              data: data,
              mode: "upd",
              lg: lg,
              // ...
            },
          })
          .onOk((r) => {
            //console.log("Ok! updated", r);
            for (let key in r) {
              if (r.hasOwnProperty(key)) {
                rec[key] = r[key];
              }
            }
          });
    },

    fetchData(relcls) {
      this.loading = ref(true);

      api
          .post(baseURL, {
            method: "relcls/loadRelClsMember",
            params: [relcls],
          })
          .then((response) => {
            this.rows = response.data.result.records;
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
          style: "width: 25%",
        },
        {
          name: "fullName",
          label: this.$t("fldFullName"),
          field: "fullName",
          align: "left",
          classes: "bg-blue-grey-1",
          headerStyle: "font-size: 1.2em",
          style: "width: 35%",
        },
        {
          name: "memberType",
          label: this.$t("memberType"),
          field: "memberType",
          align: "left",
          classes: "bg-blue-grey-1",
          headerStyle: "font-size: 1.2em",
          style: "width: 15%",
          format: (val) =>
              this.FD_MemberType ? this.FD_MemberType.get(val) : null,
        },
        {
          name: "cmt",
          label: this.$t("fldCmt"),
          field: "cmt",
          align: "left",
          classes: "bg-blue-grey-1",
          headerStyle: "font-size: 1.2em",
          style: "width: 25%",
        },
      ];
    },

    infoSelected(row) {
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
    //this.relclsId = this.$route.params.relcls;
    this.fetchData(this.relcls);
  },

  setup() {
  }

};
</script>

<style scoped></style>
