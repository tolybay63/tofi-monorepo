import { Notify } from "quasar";
import { ref } from "vue";
import { useUserStore } from "stores/user-store";
import { storeToRefs } from "pinia";

/**
 * @typedef {Object} NotifyOptions
 * @property {'positive' | 'negative' | 'warning' | 'info'} type
 * @property {string} position
 * @property {number} timeout
 * @property {string} message
 */

/**
 * @typedef {Object} TreeNode
 * @property {string|number} id
 * @property {string|number} [parent]
 * @property {Array<TreeNode>} children
 * @property {boolean} [checked]
 * @property {import('vue').Ref<boolean>} [expend]
 * @property {import('vue').Ref<boolean>} [leaf]
 */

// Constants
const NOTIFY_POSITIONS = {
  BOTTOM_RIGHT: "bottom-right",
};

const NOTIFY_TYPES = {
  POSITIVE: "positive",
  NEGATIVE: "negative",
  INFO: "info",
  WARNING: "warning",
};

const NOTIFY_TIMEOUTS = {
  SHORT: 1000,
  NORMAL: 3000,
  LONG: 5000,
};

const store = useUserStore();
const { isSysAdmin, getTarget } = storeToRefs(store);

/**
 * Checks if node has a child with given id
 * @param {string|number} id
 * @param {TreeNode} node
 * @returns {boolean}
 */
const hasChild = (id, node) => {
  return node.children.some((child) => child.parent === id);
};

/**
 * Finds nodes by key-value pair
 * @param {Array<TreeNode>} nodes
 * @param {string} key
 * @param {any} value
 * @param {Array<TreeNode>} res
 */
const findNode = (nodes, key, value, res) => {
  const walk = (node) => {
    if (node[key] === value) {
      res.push(node);
    }
    node.children.forEach(walk);
  };

  nodes.forEach(walk);
};

/**
 * Recursively checks all child nodes
 * @param {TreeNode} node
 */
const checkChilds = (node) => {
  node.checked = true;
  node.children.forEach(checkChilds);
};

/**
 * Recursively unchecks all child nodes
 * @param {TreeNode} node
 */
const uncheckChilds = (node) => {
  node.checked = false;
  node.children.forEach(uncheckChilds);
};

/**
 * Capitalizes first letter of string
 * @param {string} str
 * @returns {string}
 */
const ucFirst = (str) => {
  if (!str) return str;
  return str[0].toUpperCase() + str.slice(1);
};

/**
 * Shows success notification
 * @param {string} msg
 * @param {number} [timeout]
 */
const notifySuccess = (msg, timeout = NOTIFY_TIMEOUTS.SHORT) => {
  Notify.create({
    type: NOTIFY_TYPES.POSITIVE,
    position: NOTIFY_POSITIONS.BOTTOM_RIGHT,
    timeout,
    message: msg,
  });
};

/**
 * Shows error notification
 * @param {string} msg
 */
const notifyError = (msg) => {
  Notify.create({
    type: NOTIFY_TYPES.NEGATIVE,
    position: NOTIFY_POSITIONS.BOTTOM_RIGHT,
    timeout: NOTIFY_TIMEOUTS.LONG,
    message: msg,
  });
};

/**
 * Shows info notification
 * @param {string} msg
 */
const notifyInfo = (msg) => {
  Notify.create({
    type: NOTIFY_TYPES.INFO,
    position: NOTIFY_POSITIONS.BOTTOM_RIGHT,
    timeout: NOTIFY_TIMEOUTS.LONG,
    message: msg,
  });
};

/**
 * Converts flat array to tree structure
 * @param {Array<TreeNode>} arr
 * @param {string|null} ord
 * @returns {Array<TreeNode>}
 */
const pack = (arr, ord = null) => {
  const map = new Map(arr.map((v) => [v.id, { ...v, children: [] }]));

  const tree = [];

  for (const node of map.values()) {
    if (node.parent && map.has(node.parent)) {
      map.get(node.parent).children.push(node);
    } else {
      tree.push(node);
    }
  }

  if (ord !== null) {
    tree.forEach((node) => {
      node.children.sort((a, b) => (a[ord] > b[ord] ? 1 : -1));
    });
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

//result: parentNode[]
const getParentNode = (data, curNodeParent, parentNode) => {
  for (let i = 0; i < data.length; i++) {
    tt(data[i], curNodeParent, parentNode);
    //if (parentNode.length > 0) break;
  }
};

const getParentNodeLeaf = (data, curNodeParent, parentNode) => {
  do {
    getParentNode(data, curNodeParent, parentNode);
    curNodeParent = parentNode[parentNode.length - 1].parent;
  } while (curNodeParent != null);
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

const expand = (item) => {
  item.expend = ref(true);
  const { children } = item;
  if (children.length > 0) item.leaf = ref(false);
  else item.leaf = ref(true);
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

/**
 * Checks if user has target permission
 * @param {string} tg - Target to check
 * @returns {boolean} - Whether user has target permission
 */
const hasTarget = (tg) => {
  if (isSysAdmin.value) return true;
  if (!getTarget.value?.length) return false;
  return getTarget.value.includes(tg);
};

export {
  hasTarget,
  getParentNodeLeaf,
  findNode,
  checkChilds,
  uncheckChilds,
  ucFirst,
  notifySuccess,
  notifyError,
  notifyInfo,
  getParentNode,
  pack,
  expand,
  expandAll,
  collapsAll,
  cnvData,
  hasChild,
  // Constants
  NOTIFY_POSITIONS,
  NOTIFY_TYPES,
  NOTIFY_TIMEOUTS,
};
