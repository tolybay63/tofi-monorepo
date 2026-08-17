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
                color="red"
                class="q-ml-sm"
                @click="fnDelGr(currentNode)"
                :disable="currentNode == null"
              >
                <q-tooltip transition-show="rotate" transition-hide="rotate">
                  {{ $t("deletingRecord") }}
                </q-tooltip>
              </q-btn>

              <q-inner-loading :showing="visible" color="secondary"/>
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
              color="red"
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

        <div style="height: calc(100vh - 230px); width: 100%" class="scroll">
          <q-table
            class="sticky-header-table"
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
                  {{ infoSelected(selected2[0]) }}
                </span>
              </q-td>
              <q-td
                colspan="100%"
                v-else-if="rows.length > 0"
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

<script setup>
import { ref, getCurrentInstance } from "vue";
import { useRoute, useRouter } from "vue-router";
import { useQuasar } from "quasar";
import { api } from "@/boot/axios";
import { expandAll, getParentNode, hasTarget, notifyError, notifyInfo, pack } from "../../utils/jsutils";
import QTreeTable from "@/components/QTreeTable.vue";
import UpdateGroup from "@/pages/users/UpdateGroup.vue";
import UpdateUser from "@/pages/users/UpdateUser.vue";

const { proxy } = getCurrentInstance();
const $q = useQuasar();
const route = useRoute();
const router = useRouter();

const splitterModel = ref(30);
const cols = ref([]);
const rows = ref([]);
const currentNode = ref(null);
const visible = ref(false);
const dense = ref(true);

const cols2 = ref([]);
const rows2 = ref([]);
const FD_AccessLevel = ref(new Map());
const loading2 = ref(false);

const selected2 = ref([]);
const user_id = ref(0);
const userGr_id = ref(0);
const childComp = ref(null);

const findNode = (nodes, key, value, res) => {
  const walk = (node) => {
    if (node[key] === value) res.push(node);
    const children = node.children || [];
    children.forEach(walk);
  };
  for (let i = 0; i < nodes.length; i++) {
    walk(nodes[i]);
  }
};

const authSelect = () => {
  router.push({
    name: "UserSelected",
    params: {
      userGr: currentNode.value.id,
      user: selected2.value[0].id,
    },
  });
};

const onUpdateSelect = (item) => {
  user_id.value = 0;
  currentNode.value = item.selected !== undefined ? item.selected : null;
  if (currentNode.value) {
    selected2.value = [];
    fetchData(currentNode.value.id);
  } else {
    selected2.value = [];
    fetchData(0);
  }
};

const fetchDataGr = () => {
  visible.value = true;
  currentNode.value = null;
  selected2.value = [];

  api
    .post("", {
      method: "usr/loadGroup",
      params: [{}],
    })
    .then(
      (response) => {
        rows.value = pack(response.data.result.records, "id");
        fnExpand();
      },
      (error) => {
        router.push("/");
        let msg = error.message;
        if (error.response)
          msg = proxy?.$t(error.response.data.error.message);
        notifyError(msg);
      }
    )
    .then(() => {
      if (userGr_id.value > 0) {
        let res = [];
        findNode(rows.value, "id", userGr_id.value, res);
        if (res.length > 0) {
          currentNode.value = res[0];
          childComp.value?.restoreSelect(currentNode.value);
          fetchData(currentNode.value.id);
        }
      }
    })
    .finally(() => {
      visible.value = false;
    });
};

const fnInsGr = (mode, isChild) => {
  let data = {
    id: 0,
    name: "",
    fullName: "",
    cmt: null,
  };
  let parent = null;
  let parentName = null;
  if (isChild) {
    parent = currentNode.value.id;
    parentName = currentNode.value.name;
  }
  if (mode === "ins") {
    data.parent = parent;
  } else if (mode === "upd") {
    data = {
      id: currentNode.value.id,
      name: currentNode.value.name,
      fullName: currentNode.value.fullName,
      parent: currentNode.value.parent,
      cmt: currentNode.value.cmt,
    };
    if (currentNode.value.parent > 0) {
      let parentNode = [];
      getParentNode(rows.value, currentNode.value.parent, parentNode);
      parentName = parentNode[0].fullName;
      isChild = true;
    }
  }

  $q.dialog({
    component: UpdateGroup,
    componentProps: {
      data: data,
      mode: mode,
      isChild: isChild,
      tableName: "PropGr",
      parentName: parentName,
      dense: true,
    },
  })
    .onOk((r) => {
      fetchDataGr();
      currentNode.value = r;
      childComp.value?.restoreSelect(currentNode.value);
      onUpdateSelect({ selected: r });
    });
};

const fnDelGr = (rec) => {
  $q.dialog({
    title: proxy?.$t("confirmation"),
    message:
      proxy?.$t("deleteRecord") +
      '<div style="color: plum">(' +
      rec.name +
      ")</div>",
    html: true,
    cancel: true,
    persistent: true,
    focus: "cancel",
  })
    .onOk(() => {
      api
        .post("", {
          method: "usr/deleteGr",
          params: [rec.id],
        })
        .then(() => {
          fetchDataGr();
          if (childComp.value) {
            childComp.value.currentNode = null;
          }
        });
    })
    .onCancel(() => {
      notifyInfo(proxy?.$t("canceled"));
    });
};

const edit = (data, mode) => {
  $q.dialog({
    component: UpdateUser,
    componentProps: {
      rec: data,
      mode: mode,
    },
  })
    .onOk((updatedData) => {
      fetchData(currentNode.value.id);
      selected2.value.push(updatedData);
    });
};

const fnIns = (mode) => {
  if (mode === "ins") {
    api
      .post("", {
        method: "usr/newRec",
        params: [currentNode.value.id],
      })
      .then((response) => {
        edit(response.data.result.records[0], mode);
      });
  } else {
    edit(selected2.value[0], mode);
  }
};

const fnDel = (rec) => {
  $q.dialog({
    title: proxy?.$t("confirmation"),
    message:
      proxy?.$t("deleteRecord") +
      '<div style="color: plum">(' +
      rec.name +
      ")</div>",
    html: true,
    cancel: true,
    persistent: true,
    focus: "cancel",
  })
    .onOk(() => {
      api
        .post("", {
          method: "usr/delete",
          params: [rec.id],
        })
        .then(() => {
          fetchData(currentNode.value.id);
          selected2.value = [];
        });
    })
    .onCancel(() => {
      notifyInfo(proxy?.$t("canceled"));
    });
};

const fetchData = (gr) => {
  loading2.value = true;
  api
    .post("", {
      method: "usr/load",
      params: [gr],
    })
    .then(
      (response) => {
        rows2.value = response.data.result.records;
      },
      (error) => {
        let msg = error.message;
        if (error.response)
          msg = proxy?.$t(error.response.data.error.message);
        notifyError(msg);
      }
    )
    .then(() => {
      selected2.value = [];
      if (user_id.value > 0) {
        let index = rows2.value.findIndex((row) => row.id === user_id.value);
        if (index > 0) selected2.value.push(rows2.value[index]);
      }
    })
    .finally(() => {
      loading2.value = false;
    });
};

const fnPhone = (val) => {
  return (
    "+7 " +
    val.substring(0, 3) +
    " " +
    val.substring(3, 6) +
    " " +
    val.substring(6, 10)
  );
};

const getColumns = () => [
  {
    name: "name",
    label: proxy?.$t("fldName"),
    field: "name",
    align: "left",
    headerStyle: "font-size: 1.2em; width: 50%",
  },
  {
    name: "cmt",
    label: proxy?.$t("fldCmt"),
    field: "cmt",
    align: "left",
    headerStyle: "font-size: 1.2em; width: 50%",
  },
];

const getColumns2 = () => [
  {
    name: "login",
    label: proxy?.$t("login"),
    field: "login",
    align: "left",
    classes: "bg-blue-grey-1",
    headerStyle: "font-size: 1.2em; width: 5%",
  },
  {
    name: "name",
    label: proxy?.$t("fldName"),
    field: "name",
    align: "left",
    classes: "bg-blue-grey-1",
    headerStyle: "font-size: 1.2em; width: 15%",
  },
  {
    name: "fullName",
    label: proxy?.$t("fldFullName"),
    field: "fullName",
    align: "left",
    classes: "bg-blue-grey-1",
    headerStyle: "font-size: 1.2em; width: 30%",
  },
  {
    name: "email",
    label: proxy?.$t("email"),
    field: "email",
    align: "left",
    classes: "bg-blue-grey-1",
    headerStyle: "font-size: 1.2em; width: 15%",
  },
  {
    name: "phone",
    label: proxy?.$t("phone"),
    field: "phone",
    align: "left",
    classes: "bg-blue-grey-1",
    headerStyle: "font-size: 1.2em; width: 10%",
    format: (val) => (val ? fnPhone(val) : null),
  },
  {
    name: "accessLevel",
    label: proxy?.$t("accessLevel"),
    field: "accessLevel",
    align: "left",
    classes: "bg-blue-grey-1",
    headerStyle: "font-size: 1.2em; width: 25%",
    format: (val) => (val != null ? FD_AccessLevel.value[val] : null),
  },
];

const fnExpand = () => {
  expandAll(rows.value);
};

const infoSelected = (row) => "" + row.login + " (" + row.fullName + ")";

// Инициализация при загрузке компонента
user_id.value = parseInt(route.params.user, 10) || 0;
userGr_id.value = parseInt(route.params.userGr, 10) || 0;

visible.value = true;
api
  .post("", {
    method: "dict/loadDict",
    params: ["FD_AccessLevel"],
  })
  .then((response) => {
    FD_AccessLevel.value = response.data.result;
  })
  .finally(() => {
    visible.value = false;
  });

cols.value = getColumns();
cols2.value = getColumns2();
fetchDataGr();
</script>

<style lang="sass">

.img-vert
  transform: scaleY(-1)
  filter: "FlipV"
  -ms-filter: "FlipV"

.sticky-header-table
  height: calc(100vh - 130px)

  thead tr th
    position: sticky
    z-index: 1

  thead tr:first-child th
    top: 0

  &.q-table--loading thead tr:last-child th
    top: 48px

  tbody
    scroll-margin-top: 48px
</style>
