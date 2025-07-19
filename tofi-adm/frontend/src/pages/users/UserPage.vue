<template>
  <div class="q-pa-md">
    <q-splitter
      v-model="splitterModel"
      :model-value="splitterModel"
      :limits="[0, 100]"
      before-class="overflow-hidden q-mr-sm"
      after-class="overflow-hidden q-ml-sm"
      separator-class="bg-red"
    >
      <template v-slot:before>
        <div
          class="q-pa-sm-sm"
          style="height: calc(100vh - 200px); width: 100%"
        >
          <q-banner dense inline-actions class="bg-orange-1 q-mb-sm">
            <div style="font-size: 1.2em; font-weight: bold">
                <q-avatar color="black" text-color="white" icon="folder"/>
                {{ $t("userGr") }}
            </div>

            <template v-slot:action>
              <q-btn
                v-if="hasTarget('adm:usr:gr:ins')"
                dense
                icon="post_add"
                color="secondary"
                class="q-ml-sm"
                @click="fnInsGr('ins', false)"
              >
                <q-tooltip transition-show="rotate" transition-hide="rotate">
                  {{ $t("create1level") }}
                </q-tooltip>
              </q-btn>

              <q-btn
                v-if="hasTarget('adm:usr:gr:ins')"
                dense
                icon="post_add"
                color="secondary"
                class="q-ml-sm img-vert"
                @click="fnInsGr('ins', true)"
                :disable="currentNode == null"
              >
                <q-tooltip transition-show="rotate" transition-hide="rotate">
                  {{ $t("createChild") }}
                </q-tooltip>
              </q-btn>

              <q-btn
                v-if="hasTarget('adm:usr:gr:upd')"
                dense
                icon="edit"
                color="secondary"
                class="q-ml-sm"
                @click="fnInsGr('upd', null)"
                :disable="currentNode == null"
              >
                <q-tooltip transition-show="rotate" transition-hide="rotate">
                  {{ $t("editRecord") }}
                </q-tooltip>
              </q-btn>

              <q-btn
                v-if="hasTarget('adm:usr:gr:del')"
                dense
                icon="delete"
                color="secondary"
                class="q-ml-sm"
                @click="fnDelGr(currentNode)"
                :disable="currentNode == null"
              >
                <q-tooltip transition-show="rotate" transition-hide="rotate">
                  {{ $t("deletingRecord") }}
                </q-tooltip>
              </q-btn>

              <q-inner-loading :showing="visible" color="secondary" />
            </template>
          </q-banner>

          <QTreeTable
            :cols="cols"
            :rows="rows"
            :icon_leaf="''"
            @updateSelect="onUpdateSelect"
            ref="childComp"
            checked_visible="true"
          />
        </div>
      </template>

      <template v-slot:after>
        <q-banner dense inline-actions class="bg-orange-1 q-mb-sm">
          <div style="font-size: 1.2em; font-weight: bold">
            <q-avatar color="black" text-color="white" icon="supervisor_account"/>
            {{ $t("users") }}
          </div>

          <template v-slot:action>
            <q-btn
              v-if="hasTarget('adm:usr:gr:usr:ins')"
              dense
              icon="post_add"
              color="secondary"
              class="q-ml-sm"
              @click="fnIns('ins')"
              :disable="currentNode == null"
            >
              <q-tooltip transition-show="rotate" transition-hide="rotate">
                {{ $t("newRecord") }}
              </q-tooltip>
            </q-btn>

            <q-btn
              v-if="hasTarget('adm:usr:gr:usr:upd')"
              dense
              icon="edit"
              color="secondary"
              class="q-ml-sm"
              @click="fnIns('upd')"
              :disable="selected2.length === 0"
            >
              <q-tooltip transition-show="rotate" transition-hide="rotate">
                {{ $t("editRecord") }}
              </q-tooltip>
            </q-btn>

            <q-btn
              v-if="hasTarget('adm:usr:gr:usr:del')"
              dense
              icon="delete"
              color="secondary"
              class="q-ml-sm"
              @click="fnDel(selected2[0])"
              :disable="
                selected2.length === 0 || selected2[0].login === 'sysadmin'
              "
            >
              <q-tooltip transition-show="rotate" transition-hide="rotate">
                {{ $t("deletingRecord") }}
              </q-tooltip>
            </q-btn>
            <q-space></q-space>

            <q-btn
              :dense="dense"
              icon="pan_tool_alt"
              color="secondary"
              class="q-ml-lg"
              :disable="
                loading2 ||
                selected2.length === 0 ||
                selected2[0].login === 'sysadmin'
              "
              @click="authSelect()"
            >
              <q-tooltip transition-show="rotate" transition-hide="rotate">
                {{ $t("chooseRecord") }}
              </q-tooltip>
            </q-btn>
          </template>
        </q-banner>

        <div style="height: calc(100vh - 215px); width: 100%" class="scroll">
          <q-table
            style="height: 100%; width: 100%"
            color="primary"
            card-class="bg-amber-1"
            row-key="id"
            :columns="cols2"
            :rows="rows2"
            :wrap-cells="true"
            :table-colspan="7"
            table-header-class="text-bold text-white bg-blue-grey-13"
            separator="cell"
            :loading="loading2"
            :dense="dense"
            :rows-per-page-options="[20, 25, 0]"
            selection="single"
            v-model:selected="selected2"
          >
            <template #bottom-row>
              <q-td colspan="100%" v-if="selected2.length > 0">
                <span class="text-blue"> {{ $t("selectedRow") }}: </span>
                <span class="text-bold">
                  {{ this.infoSelected(selected2[0]) }}
                </span>
              </q-td>
              <q-td
                colspan="100%"
                v-else-if="this.rows.length > 0"
                class="text-bold"
              >
                {{ $t("infoRow") }}
              </q-td>
            </template>
          </q-table>
        </div>
      </template>
    </q-splitter>
  </div>
</template>

<script>
import {ref} from "vue";
import {api, baseURL} from "boot/axios";
import { expandAll, getParentNode, hasTarget, notifyError, notifyInfo, pack,} from "src/utils/jsutils";
import QTreeTable from "components/QTreeTable.vue";
import UpdateGroup from "pages/users/UpdateGroup.vue";
import UpdateUser from "pages/users/UpdateUser.vue";

const findNode = (nodes, key, value, res) => {
  const walk = (node) => {
    if (node[key] === value) res.push(node);
    const children = node.children;
    children.forEach(walk);
  };
  for (let i = 0; i < nodes.length; i++) {
    walk(nodes[i]);
  }
};

export default {
  name: "UserPage",
  components: { QTreeTable },

  data() {
    return {
      splitterModel: 30,
      cols: [],
      rows: [],
      currentNode: null,
      visible: ref(false),

      dense: true,
      //
      cols2: [],
      rows2: [],
      FD_AccessLevel: null,
      loading2: ref(false),
      //
      selected2: [],
      user_id: 0,
      userGr_id: 0,
    };
  },

  methods: {
    hasTarget,
    authSelect() {
      this.$router.push({
        name: "UserSelected",
        params: {
          userGr: this.currentNode.id,
          user: this.selected2[0].id,
        },
      });
    },

    onUpdateSelect(item) {
      this.user_id = 0;
      this.currentNode = item.selected !== undefined ? item.selected : null;
      if (this.currentNode) {
        this.selected2 = [];
        this.fetchData(this.currentNode.id);
      } else {
        this.selected2 = [];
        this.fetchData(0);
      }
    },

    fetchDataGr() {
      this.visible = ref(true);

      this.currentNode = null;
      this.selected2 = [];

      api
        .post(baseURL, {
          method: "usr/loadGroup",
          params: [{}],
        })
        .then(
          (response) => {
            this.rows = pack(response.data.result.records, "id");
            this.fnExpand();
          },
          (error) => {

            this.$router.push("/");

            let msg = error.message;
            if (error.response)
              msg = this.$t(error.response.data.error.message);
            notifyError(msg);
          }
        )
        .then(() => {
          if (this.userGr_id > 0) {
            let res = [];
            findNode(this.rows, "id", this.userGr_id, res);
            if (res.length > 0) {
              this.currentNode = res[0];
              this.$refs.childComp.restoreSelect(this.currentNode);
              this.fetchData(this.currentNode.id);
            }
          }
        })
        .finally(() => {
          this.visible = ref(false);
        });
    },

    fnInsGr(mode, isChild) {
      let data = {
        id: 0,
        name: "",
        fullName: "",
        cmt: null,
      };
      const lg = this.lang;
      let parent = null;
      let parentName = null;
      if (isChild) {
        parent = this.currentNode.id;
        parentName = this.currentNode.name;
      }
      if (mode === "ins") {
        data.parent = parent;
      } else if (mode === "upd") {
        data = {
          id: this.currentNode.id,
          name: this.currentNode.name,
          fullName: this.currentNode.fullName,
          parent: this.currentNode.parent,
          cmt: this.currentNode.cmt,
        };
        if (this.currentNode.parent > 0) {
          let parentNode = [];
          getParentNode(this.rows, this.currentNode.parent, parentNode);
          parentName = parentNode[0].fullName;
          isChild = true;
        }
      }
      //
      this.$q
        .dialog({
          component: UpdateGroup,
          componentProps: {
            data: data,
            mode: mode,
            isChild: isChild,
            tableName: "PropGr",
            parentName: parentName,
            lg: lg,
            dense: true,
          },
        })
        .onOk((r) => {
          this.fetchDataGr();
          this.currentNode = r;
          this.$refs.childComp.restoreSelect(this.currentNode);
          this.onUpdateSelect({ selected: r });
        });
    },

    fnDelGr(rec) {
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
          //let index = this.rows.findIndex((row) => row.id === rec.id);
          api
            .post(baseURL, {
              method: "usr/deleteGr",
              params: [rec.id],
            })
            .then(
              () => {
                this.fetchDataGr();
                this.$refs.childComp.currentNode = null;
              },
              (error) => {
                let msg = error.message;
                if (error.response)
                  msg = error.response.data.error.message;
                notifyError(msg);
              }
            );
        })
        .onCancel(() => {
          notifyInfo(this.$t("canceled"));
        });
    },

    /*---------------------------------*/

    edit(data, mode) {
      const lg = this.lang;
      this.$q
        .dialog({
          component: UpdateUser,
          componentProps: {
            rec: data,
            mode: mode,
            lg: lg,
            dense: true,
          },
        })
        .onOk((data) => {
            this.fetchData(this.currentNode.id);
            this.selected2.push(data)

        });
    },

    fnIns(mode) {
      if (mode === "ins") {
        api
          .post(baseURL, {
            method: "usr/newRec",
            params: [this.currentNode.id],
          })
          .then((response) => {
            this.edit(response.data.result.records[0], mode);
          });
      } else {
        this.edit(this.selected2[0], mode);
      }
    },

    fnDel(rec) {
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
          //let index = this.rows.findIndex((row) => row.id === rec.id);
          api
            .post(baseURL, {
              method: "usr/delete",
              params: [rec.id],
            })
            .then(
              () => {
                this.fetchData(this.currentNode.id);
                this.selected2 = [];
              },
              (error) => {
                let msg = error.message;
                if (error.response)
                  msg = error.response.data.error.message;

                notifyError(msg);
              }
            );
        })
        .onCancel(() => {
          notifyInfo(this.$t("canceled"));
        });
    },

    fetchData(gr) {
      this.loading2 = ref(true);
      api
        .post(baseURL, {
          method: "usr/load",
          params: [gr],
        })
        .then(
          (response) => {
            this.rows2 = response.data.result.records;
          },
          (error) => {
            let msg = error.message;
            if (error.response)
              msg = this.$t(error.response.data.error.message);

            notifyError(msg);
          }
        )
        .then(() => {
          this.selected2 = [];
          if (this.user_id > 0) {
            let index = this.rows2.findIndex((row) => row.id === this.user_id);
            if (index > 0) this.selected2.push(this.rows2[index]);
          }
        })
        .finally(() => {
          this.loading2 = ref(false);
        });
    },

    fnPhone(val) {
      return (
        "+7 " +
        val.substring(0, 4) +
        " " +
        val.substring(3, 6) +
        " " +
        val.substring(6, 10)
      );
    },

    getColumns() {
      return [
        {
          name: "name",
          label: this.$t("fldName"),
          field: "name",
          align: "left",
          headerStyle: "font-size: 1.2em; width: 50%",
        },
        /*        {
          name: "fullName",
          label: this.$t("fldFullName"),
          field: "fullName",
          align: "left",
          headerStyle: "font-size: 1.2em; width: 60%",
        },*/
        {
          name: "cmt",
          label: this.$t("fldCmt"),
          field: "cmt",
          align: "left",
          headerStyle: "font-size: 1.2em; width: 50%",
        },
      ];
    },

    getColumns2() {
      return [
        {
          name: "login",
          label: this.$t("login"),
          field: "login",
          align: "left",
          classes: "bg-blue-grey-1",
          headerStyle: "font-size: 1.2em; width: 5%",
        },
        {
          name: "name",
          label: this.$t("fldName"),
          field: "name",
          align: "left",
          classes: "bg-blue-grey-1",
          headerStyle: "font-size: 1.2em; width: 15%",
        },
        {
          name: "fullName",
          label: this.$t("fldFullName"),
          field: "fullName",
          align: "left",
          classes: "bg-blue-grey-1",
          headerStyle: "font-size: 1.2em; width: 30%",
        },
        {
          name: "email",
          label: this.$t("email"),
          field: "email",
          align: "left",
          classes: "bg-blue-grey-1",
          headerStyle: "font-size: 1.2em; width: 15%",
        },
        {
          name: "phone",
          label: this.$t("phone"),
          field: "phone",
          align: "left",
          classes: "bg-blue-grey-1",
          headerStyle: "font-size: 1.2em; width: 10%",
          format: (val) => (val ? this.fnPhone(val) : null),
        },
        {
          name: "accessLevel",
          label: this.$t("accessLevel"),
          field: "accessLevel",
          align: "left",
          classes: "bg-blue-grey-1",
          headerStyle: "font-size: 1.2em; width: 25%",
          format: (val) => (val != null ? this.FD_AccessLevel[val] : null),
        },
        /*
        {
          name: "cmt",
          label: this.$t("fldCmt"),
          field: "cmt",
          align: "left",
          classes: "bg-blue-grey-1",
          headerStyle: "font-size: 1.2em; width: 20%",
        },
*/
      ];
    },

    fnExpand() {
      expandAll(this.rows);
    },

    infoSelected(row) {
      return "" + row.login + " (" + row.fullName + ")";
    },
  },

  created() {
    this.lang = localStorage.getItem("curLang");
    this.lang = this.lang === "en-US" ? "en" : this.lang;

    this.visible = ref(true)
    api
      .post(baseURL, {
        method: "dict/loadDict",
        params: ["FD_AccessLevel"],
      })
      .then((response) => {
        this.FD_AccessLevel = response.data.result
      })
      .finally(() => {
        this.visible = ref(false)
      })

    this.cols = this.getColumns();
    this.cols2 = this.getColumns2();

    this.fetchDataGr();
  },

  mounted() {
    this.user_id = parseInt(this.$route.params.user, 10);
    this.userGr_id = parseInt(this.$route.params.userGr, 10);
  },

  setup() {
    return {};
  },
};
</script>

<style scoped>
.img-vert {
  transform: scaleY(-1);
  -ms-filter: "FlipV";
}
</style>
