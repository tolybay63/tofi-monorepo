import { Notify } from "quasar";
import { ref } from "vue";
import {useUserStore} from "stores/user-store";
import {storeToRefs} from "pinia";
const store = useUserStore();
const { isSysAdmin, getTarget } =
  storeToRefs(store);


const ucFirst = (str) => {
  if (!str) return str;
  return str[0].toUpperCase() + str.slice(1);
};

//positive, negative, warning, info
const notifySuccess = (msg, timeout) => {
  timeout = timeout === undefined ? 1000 : timeout;
  Notify.create({
    type: "positive",
    position: "bottom-right",
    timeout: timeout,
    message: msg,
  });
};

const notifyError = (msg) => {
  Notify.create({
    type: "negative",
    position: "bottom-right",
    timeout: 5000,
    message: msg,
  });
};

const notifyInfo = (msg) => {
  Notify.create({
    type: "info",
    position: "bottom-right",
    timeout: 5000,
    message: msg,
  });
};

const pack = (arr, ord) => {
  const map = Object.assign(
    {},
    ...arr.map((v) => ({ [v.id]: Object.assign(v, { children: [] }) }))
  );

  const tree = Object.values(map).filter(
    (v) => !(v.parent && map[v.parent].children.push(v))
  );

  if (ord !== null) {
    let i = 0;
    while (i < tree.length) {
      tree[i].children.sort((a, b) => (a[ord] > b[ord] ? 1 : -1));
      i++;
    }
  }
  return tree;
};

const tt = (node, curNodeParent, parentNode) => {
  if (node.id === curNodeParent) {
    parentNode.push(node);
  }
  let children = node.children;
  if (children.length > 0) {
    children.forEach((ch) => tt(ch, curNodeParent, parentNode));
  }
};

const getParentNode = (data, curNodeParent, parentNode) => {
  for (let i = 0; i < data.length; i++) {
    tt(data[i], curNodeParent, parentNode);
    if (parentNode.length > 0) break;
  }
};

const expandAll = (data) => {
  let i = 0;
  while (i < data.length) {
    let nd = data[i];
    nd.expend = ref(true);
    const { children } = nd;
    if (children.length > 0) {
      nd.leaf = ref(false);
      expandAll(children);
    } else {
      nd.leaf = ref(true);
      nd.expend = undefined;
    }
    i = i + 1;
  }
};

const cnvData = (data) => {
  let i = 0;
  while (i < data.length) {
    let nd = data[i];
    let arr = nd.id.split("_");
    nd.id = arr[arr.length - 1];
    const { children } = nd;
    if (children.length > 0) {
      cnvData(children);
    }
    i = i + 1;
  }
};

const collapsAll = (data) => {
  let i = 0;
  while (i < data.length) {
    let nd = data[i];
    nd.expend = ref(false);
    const { children } = nd;
    if (children.length > 0) {
      nd.leaf = ref(false);
      collapsAll(children);
    } else {
      nd.leaf = ref(true);
      nd.expend = undefined;
    }
    i = i + 1;
  }
};

const hasTarget = (tg) => {
  if (isSysAdmin.value) return true;
  if (getTarget.value.length === 0) return false;
  return getTarget.value.includes(tg);
};


export {
  ucFirst,
  notifySuccess,
  notifyError,
  notifyInfo,
  getParentNode,
  pack,
  expandAll,
  collapsAll,
  cnvData,
  hasTarget
};
