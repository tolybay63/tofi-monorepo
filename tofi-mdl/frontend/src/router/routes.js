const routes = [
    {
        path: "/",
        component: () => import("layouts/MainLayout.vue"),
        children: [
            {path: "", component: () => import("pages/IndexPage.vue")},
            {path: "/dbSetting", component: () => import("pages/IndexPage.vue")},
            {path: "/database", component: () => import("pages/database/DataBasePage.vue")},

            {path: "/dataSetting", component: () => import("pages/IndexPage.vue")},
            {
                path: "/measure",
                component: () => import("pages/measure/MeasurePage.vue"),
            },
            {
                path: "/attrib",
                component: () => import("pages/attrib/AttribPage.vue"),
            },

            {
                path: "/factor/:factor",
                name: "FactorPage",
                component: () => import("pages/factor/FactorPage.vue"),
            },
            {
                path: "/factor/:factor",
                name: "factorvalmain",
                component: () => import("pages/factor/FactorValMain.vue"),
            },

            {
                path: "/meter/:meter",
                name: "meterPage",
                component: () => import("pages/meter/MeterPage.vue")
            },
            {
                path: "/meter/:meter/:meterStruct",
                name: "meterSelected",
                component: () => import("pages/meter/MeterSelected.vue"),
            },

            {path: "/role", component: () => import("pages/role/RolePage.vue")},
            {path: "/typ/:typ", name: "typPage", component: () => import("pages/typ/TypPage.vue")},

            {
                path: "/typ/:typ/:tab/:cls",
                name: "typSelected",
                component: () => import("pages/typ/TypSelected.vue"),
            },

            {
                path: "/cls/:cls",
                name: "clsVer",
                component: () => import("pages/typ/cls/ClsVer.vue"),
            },

            {
                path: "/reltyp/:reltyp",
                name: "reltypPage",
                component: () => import("pages/reltyp/RelTypPage.vue"),
            },
            {
                path: "/reltyp/:reltyp/:relcls/:tab/",
                name: "reltypSelected",
                component: () => import("pages/reltyp/RelTypSelected.vue"),
            },

            {
                path: "/relcls/:reltyp/:relcls/:tab",
                name: "relclsSelected",
                component: () => import("pages/reltyp/relcls/RelClsSelected.vue"),
            },

            {
                path: "/props/:propGr/:prop",
                name: "propPage",
                component: () => import("pages/prop/PropPage.vue")
            },

            {
                path: "/props/:propGr/:prop",
                name: "propSelected",
                component: () => import("pages/prop/PropSelected.vue"),
            },

            {
                path: "/dimMultiProp/:dmpGr/:dmp",
                name: "DimMultiProp",
                component: () => import("pages/multiprop/dim/DimMultiProp.vue"),
            },
            {
                path: "/dimMultiProp/:dimMultiProp/:dimMultiPropName/:dimMultiPropGr/:dimMultiPropType",
                name: "DimMultiPropItem",
                component: () => import("pages/multiprop/dim/DimMultiPropItem.vue"),
            },

            {
                path: "/multiProp/:mpGr/:mp",
                name: "MultiProp",
                component: () => import("pages/multiprop/MultiProp.vue"),
            },
            {
                path: "/multiProp/:mpGr/:mp",
                name: "MultiPropSelected",
                component: () => import("pages/multiprop/MultiPropSelected.vue"),
            },
            {
                path: "/chargr/:chargr/:tab",
                name: "chargr",
                component: () => import("pages/chargr/CharGrPage.vue"),
            },

            {
                path: "/typchargr/:typCharGr",
                name: "typCharGrProp",
                component: () => import("pages/chargr/typchargr/TypCharGrProps.vue"),
            },

            {
                path: "/reltypchargr/:relTypCharGr",
                name: "relTypCharGrProp",
                component: () => import("pages/chargr/relchargr/RelTypCharGrProps.vue"),
            },

            {
                path: "/flatTable",
                component: () => import("pages/flattable/FlatTable.vue"),
            },

            {
                path: "/dataProcessing",
                component: () => import("pages/IndexPage.vue"),
            },

            {
                path: "/scale/:scale",
                name: "ScalePage",
                component: () => import("pages/scale/ScalePage.vue"),
            },

            {
                path: "/scale/:scale/:info",
                name: "ScaleSelectedPage",
                component: () => import("pages/scale/ScaleSelectedPage.vue"),
            },

            {
                path: "/dimsperiod/:dimperiod",
                name: "DimPeriod",
                component: () => import("pages/dimperiod/DimPeriod.vue"),
            },
            {
                path: "/dimsperiod/:dimperiod/:name",
                name: "DimPeriodItem",
                component: () => import("pages/dimperiod/DimPeriodItem.vue"),
            },

            {
                path: "/dimsprop/:dimPropGr/:dimProp",
                name: "DimProp",
                component: () => import("pages/dimprop/DimProp.vue"),
            },

            {
                path: "/dimsprop/:dimPropGr/:dimProp/:dimPropType/:name",
                name: "DimPropItem",
                component: () => import("pages/dimprop/DimPropItem.vue"),
            },

            { path: "/dimsobj/:dimObjGr/:dimObj",
                name: "DimObj",
                component: () => import("pages/dimobj/DimObj.vue") },
            {
                path: "/dimsobj/:dimObjGr/:dimObj/:name",
                name: "DimObjItem",
                component: () => import("pages/dimobj/DimObjItem.vue"),
            },
          {
            path: "/cubes/:cubesGr/:cubes",
            name: "CubesPage",
            component: () => import("pages/cubes/CubesPage.vue")
          },

          {
            path: "/cubes/:cubesGr/:cubes",
            name: "CubeSelected",
            component: () => import("pages/cubes/CubeSelected.vue"),
          },

            {
                path: "/toolsAnalitic",
                component: () => import("pages/IndexPage.vue"),
            },

          {
            path: "/testLang",
            component: () => import("pages/test/TestLang.vue"),
          },


            /*


                  {
                    path: "/impObj",
                    component: () => import("pages/instruments/import/obj/ImportObj.vue"),
                  },
                  {
                    path: "/impFile",
                    component: () => import("pages/instruments/import/ImportFile.vue"),
                  },
            */

        ],
    },

    // Always leave this as last one,
    // but you can also remove it
    {
        path: "/:catchAll(.*)*",
        component: () => import("pages/ErrorNotFound.vue"),
    },
];

export default routes;
