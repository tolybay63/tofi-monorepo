import { defineStore } from "pinia";


export const useParamsStore = defineStore("params", {
  state: () => {
    let dt = sessionStorage.getItem("datatype") || 'std'
    let db = sessionStorage.getItem("database") || 0
    let mn = sessionStorage.getItem("model") || "userdata"
    let me = sessionStorage.getItem("metamodel") || "fish"

    return {
      params: {
        dataType: dt,
        dataBase: db,
        model: mn,
        metamodel: me
      },
    };
  },

  getters: {
    getStoreParams: (state) => state.params,
    getDataType: (state) => state.params.dataType,
    getDataBase: (state) => parseInt(state.params.dataBase, 10),
    getModel: (state) => state.params.model,
    getMetaModel: (state) => state.params.metamodel,
  },

  actions: {
    setStoreParams(data) {
      if (JSON.stringify(data) !== "{}") {
        this.params.dataType = data.dataType
        this.params.dataBase = data.dataBase
        this.params.model = data.model
        this.params.metamodel = data.metamodel

        sessionStorage.setItem("dataType", data.dataType)
        sessionStorage.setItem("dataBase", data.dataBase)
        sessionStorage.setItem("model", data.model)
        sessionStorage.setItem("metamodel", data.metamodel)
      } else {
        sessionStorage.removeItem("dataType")
        sessionStorage.removeItem("dataBase")
        sessionStorage.removeItem("model")
        sessionStorage.removeItem("metamodel")
      }
    },
  },
});
