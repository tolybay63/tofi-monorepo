  <template>
    <q-page class="q-pa-md" style="height: 100px">

      <q-table
        style="height: 100%; width: 100%"
        dense color="primary"
        card-class="bg-amber-1"
        row-key="id"
        :columns="cols"
        :rows="rows"
        :wrap-cells="true"
        :table-colspan="4"
        table-header-class="text-bold text-white bg-blue-grey-13"
        separator="cell"
        :filter="filter"
        :loading="loading"
        :rows-per-page-options="[20, 25, 0]"
        selection="single"
        v-model:selected="selected"
      >
        <template #bottom-row>
          <q-td colspan="100%" v-if="selected.length > 0">
            <span class="text-blue"> {{ $t("selectedRow") }}: </span>
            <span class="text-bold"> {{ this.infoSelected(selected[0]) }} </span>
          </q-td>
          <q-td colspan="100%" v-else-if="this.rows.length > 0" class="text-bold">
            {{ $t("infoRow") }}
          </q-td>
        </template>

        <template v-slot:top>
          <div style="font-size: 1.2em; font-weight: bold;">
            <q-avatar color="black" text-color="white" icon="pin"></q-avatar>
            {{ $t("syscoding") }}
          </div>

          <q-space/>
          <q-btn
            v-if="hasTarget('mdl:mn_dop:syscoding:ins')"
            dense icon="post_add" color="secondary"
            :disable="loading" @click="editRow(null, 'ins')"
          >
            <q-tooltip transition-show="rotate" transition-hide="rotate">
              {{ $t("newRecord") }}
            </q-tooltip>
          </q-btn>
          <q-btn
            v-if="hasTarget('mdl:mn_dop:syscoding:upd')"
            dense icon="edit" color="secondary" class="q-ml-sm"
            :disable="loading || selected.length === 0"
            @click="editRow(selected[0], 'upd')"
          >
            <q-tooltip transition-show="rotate" transition-hide="rotate">
              {{ $t("editRecord") }}
            </q-tooltip>
          </q-btn>
          <q-btn
            v-if="hasTarget('mdl:mn_dop:syscoding:del')"
            dense icon="delete" color="red" class="q-ml-sm"
            :disable="loading || selected.length === 0"
            @click="removeRow(selected[0])"
          >
            <q-tooltip transition-show="rotate" transition-hide="rotate">
              {{ $t("deletingRecord") }}
            </q-tooltip>
          </q-btn>
          <q-btn
            v-if="false"
            dense icon="pan_tool_alt" color="secondary" class="q-ml-lg"
            :disable="loading || selected.length === 0"
            @click="selectSysCoding()"
          >
            <q-tooltip transition-show="rotate" transition-hide="rotate">
              {{ $t("chooseRecord") }}
            </q-tooltip>
          </q-btn>

          <q-space/>
          <q-input
            dense debounce="300" color="primary" :model-value="filter" v-model="filter"
            :label="$t('txt_filter')"
          >
            <template v-slot:append>
              <q-icon name="search"/>
            </template>
          </q-input>
        </template>

        <template #loading>
          <q-inner-loading showing color="secondary"></q-inner-loading>
        </template>
      </q-table>


    </q-page>
  </template>

  <script>
  import {api} from "boot/axios";
  import {hasTarget, notifyError, notifySuccess} from "src/utils/jsutils";
  import UpdateSysCoding from "pages/syscoding/UpdateSysCoding.vue";


  export default {
    name: 'SysCodingPage',

    data: function () {
      return {
        cols: [],
        rows: [],
        FD_AccessLevel: new Map(),
        FD_SysCodingType: new Map(),
        filter: "",
        loading: false,
        selected: [],
      };
    },

    methods: {
      hasTarget,

      fetchData() {
        this.loading = true;
        //
        api
          .post('', {
            method: "syscoding/load",
            params: [],
          })
          .then(
            (response) => {
              this.rows = response.data.result.records;
              this.selected = [];
            },
            (error) => {

              let msg = error.message;
              if (error.response)
                msg = this.$t(error.response.data.error.message);

              notifyError(msg);
            }
          )
          .finally(() => {
            //setTimeout(() => {
            this.loading = false;
            //}, 500)
          });
      },

      selectSysCoding() {
        this.$router["push"]({
          name: "SysCodingSelectedPage",
          params: {
            syscoding: this.selected[0].id,
          },
        });
      },

      removeRow(rec) {
        this.$q
          .dialog({
            title: this.$t("confirmation"),
            message:
              this.$t("deleteRecord") +
              '<div style="color: plum">(' +
              rec.cod +
              ": " +
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
              .post('', {
                method: "syscoding/delete",
                params: [ rec ],
              })
              .then(
                () => {
                  //console.log("response=>>>", response.data)
                  this.rows.splice(index, 1);
                  this.selected = [];
                  notifySuccess(this.$t("success"));
                },
                (error) => {
                  let msg = error.message;
                  if (error.response)
                    msg = error.response.data.error.message;

                  notifyError(msg)
                }
              );
          });


      },

      editRow(rec, mode) {
        let data = {};
        if (mode === "ins") {
          this.loading = true;
          api
            .post('', {
              method: "syscoding/newRec",
              params: [],
            })
            .then(
              (response) => {
                data = response.data.result.records[0];
              },
              (error) => {
                let msg = error.message;
                if (error.response)
                  msg = this.$t(error.response.data.error.message);
                notifyError(msg);
              }
            )
            .finally(() => {
              this.loading = false;
            });
        } else {
          data = {
            id: rec.id,
            cod: rec.cod,
            accessLevel: rec.accessLevel,
            sysCodingType: rec.sysCodingType,
            sourceStock: rec.sourceStock,
            name: rec.name,
            fullName: rec.fullName,
            cmt: rec.cmt,
          };
        }

        this.$q
          .dialog({
            component: UpdateSysCoding,
            componentProps: {
              data: data,
              mode: mode,
              // ...
            },
          })
          .onOk((r) => {
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

      infoSelected(row) {
        return " " + row.cod + " - " + row.name;
      },

      getColumns() {
        return [
          {
            name: "cod",
            label: this.$t("code"),
            field: "cod",
            align: "left",
            classes: "bg-blue-grey-1",
            headerStyle: "font-size: 1.2em; width: 5%",
          },
          {
            name: "name",
            label: this.$t("fldName"),
            field: "name",
            align: "left",
            sortable: true,
            classes: "bg-blue-grey-1",
            headerStyle: "font-size: 1.2em; width: 15%",
          },
          {
            name: "fullName",
            label: this.$t("fldFullName"),
            field: "fullName",
            align: "left",
            classes: "bg-blue-grey-1",
            headerStyle: "font-size: 1.2em; width: 25%",
          },
          {
            name: "sysCodingType",
            label: this.$t("sysCodingType"),
            field: "sysCodingType",
            align: "left",
            classes: "bg-blue-grey-1",
            headerStyle: "font-size: 1.2em; width: 15%",
            format: (val) =>
              this.FD_SysCodingType ? this.FD_SysCodingType.get(val) : null,
          },
          {
            name: "accessLevel",
            label: this.$t("accessLevel"),
            field: "accessLevel",
            align: "left",
            classes: "bg-blue-grey-1",
            headerStyle: "font-size: 1.2em; width: 15%",
            format: (val) =>
              this.FD_AccessLevel ? this.FD_AccessLevel.get(val) : null,
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

    },

    created() {

      this.cols = this.getColumns()

      api
        .post('', {
          method: "dict/load",
          params: [{dict: "FD_AccessLevel"}],
        })
        .then((response) => {
          response.data.result.records.forEach((it) => {
            this.FD_AccessLevel.set(it["id"], it["text"]);
          });
        });

      api
        .post('', {
          method: "dict/load",
          params: [{dict: "FD_SysCodingType"}],
        })
        .then((response) => {
          response.data.result.records.forEach((it) => {
            this.FD_SysCodingType.set(it["id"], it["text"]);
          });
        });

      this.fetchData()
    },


    setup() {
    }



  }
  </script>

