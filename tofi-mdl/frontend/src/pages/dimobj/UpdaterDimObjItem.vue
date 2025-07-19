<template>
  <q-dialog
      ref="dialog"
      @hide="onDialogHide"
      persistent
      autofocus
      transition-show="slide-up"
      transition-hide="slide-down"
  >
    <q-card class="q-dialog-plugin" style="width: 600px">
      <q-bar v-if="mode === 'ins'" class="text-white bg-primary">
        <div>{{ $t("newRecord") }}</div>
      </q-bar>
      <q-bar v-if="mode === 'upd'" class="text-white bg-primary">
        <div>{{ $t("editRecord") }}</div>
      </q-bar>

      <q-inner-loading :showing="visible" color="secondary"/>

      <q-card-section>
        <div class="row">
          {{ $t("parent") }}: <b> {{ parentName }} </b>
        </div>
        <hr/>

        <!-- linkType -->
        <q-select
            v-if="form.parent != null"
            dense options-dense :disable="mode === 'upd'"
            v-model="lt" :model-value="lt" :options="optLT" :label="$t('linkType')"
            option-value="id" option-label="text" map-options @update:model-value="fnSelectLT()"
            class="q-mb-md"
        />


        <!--  dimObjItemType -->
        <q-select
            :class="form.parent == null ? 'q-mt-md' : 'q-mt-none'"
            dense options-dense v-model="doit" :model-value="doit" :disable="mode === 'upd'"
            :options="optDOIT" :label="$t('fldCmpType')"
            option-value="id" option-label="text" map-options @update:model-value="fnSelectDOIT()"
            class="q-mb-md"
        />

        <div v-if="form.linkType===1">
          <!--  typ -->
          <q-select
              v-if="form.dimObjItemType === cTyp || form.dimObjItemType === cCls"
              dense options-dense v-model="typ" :model-value="typ" :options="optTyp" :label="$t('typ')"
              option-value="id" option-label="name" map-options @update:model-value="fnSelectTyp()"
              class="q-mb-md" :disable="mode === 'upd' && hasChild"
          />

          <!--  cls -->
          <q-select
              v-if="form.dimObjItemType === cCls"
              dense options-dense v-model="cls" :model-value="cls" :options="optCls" :label="$t('cls')"
              option-value="id" option-label="name" map-options @update:model-value="fnSelectCls()"
              class="q-mb-md" :disable="mode === 'upd' && hasChild"
          />

          <!--  relTyp -->
          <q-select
              v-if="form.dimObjItemType === cRelTyp || form.dimObjItemType === cRelCls"
              dense options-dense v-model="relTyp" :model-value="relTyp" :options="optRelTyp"
              :label="$t('reltyp')" option-value="id" option-label="name" map-options
              @update:model-value="fnSelectRelTyp()"
              class="q-mb-md" :disable="mode === 'upd' && hasChild"
          />

          <!--  relCls -->
          <q-select
              v-if="form.dimObjItemType === cRelCls"
              dense otions-dense v-model="relCls" :model-value="relCls" :options="optRelCls"
              :label="$t('relcls')" option-value="id" option-label="name" map-options
              @update:model-value="fnSelectRelCls()"
              class="q-mb-md" :disable="mode === 'upd' && hasChild"
          />

          <!--  relTypMember -->
          <div v-if="form.dimObjItemType === cMemberTyp || form.dimObjItemType === cMemberRelTyp">
            <q-select
                dense options-dense v-model="relTyp" :model-value="relTyp" :options="optRelTyp"
                :label="$t('reltyp')" option-value="id" option-label="name" map-options
                @update:model-value="fnSelectRelTyp()"
                class="q-mb-md" :disable="mode === 'upd' && hasChild"
            />

            <q-select
                dense options-dense v-model="relTypMember" :model-value="relTypMember"
                :options="optRelTypMember" :label="$t('memberRel')"
                option-value="id" option-label="name" map-options @update:model-value="fnSelectRelTypMember()"
                class="q-mb-md" :disable="mode === 'upd' && hasChild"
            />
          </div>

          <!--  cMemberCls or relClsMember  -->
          <div v-if="form.dimObjItemType === cMemberCls || form.dimObjItemType === cMemberRelCls">
            <q-select
                dense options-dense v-model="relTyp" :model-value="relTyp" :options="optRelTyp"
                :label="$t('reltyp')" option-value="id" option-label="name" map-options
                @update:model-value="fnSelectRelTyp()"
                class="q-mb-md" :disable="mode === 'upd' && hasChild"
            />

            <q-select
                dense options-dense v-model="relCls" :model-value="relCls" :options="optRelCls"
                :label="$t('relcls')" option-value="id" option-label="name" map-options
                @update:model-value="fnSelectRelCls()"
                class="q-mb-md" :disable="mode === 'upd' && hasChild"
            />

            <q-select
                dense options-dense v-model="relClsMember" :model-value="relClsMember"
                :options="optRelClsMember" :label="$t('memberRelCls')"
                option-value="id" option-label="name" map-options @update:model-value="fnSelectRelClsMember()"
                class="q-mb-md" :disable="mode === 'upd' && hasChild"
            />
          </div>
        </div>

        <div v-if="form.linkType===2">
          <div v-if="form.dimObjItemType === cMemberTyp || form.dimObjItemType === cMemberRelTyp">
            <q-select
                dense options-dense v-model="relTyp" :model-value="relTyp" :options="optRelTyp"
                :label="$t('reltyp')" option-value="id" option-label="name" map-options
                @update:model-value="fnSelectRelTyp()" :disable="mode === 'upd' && hasChild"
            />

            <q-select
                class="q-mt-md"
                dense options-dense v-model="relTypMember" :model-value="relTypMember"
                :options="optRelTypMember" :label="$t('memberRel')" :disable="mode === 'upd' && hasChild"
                option-value="id" option-label="name" map-options @update:model-value="fnSelectRelTypMember()"
            />
          </div>

          <div v-if="form.dimObjItemType === cMemberCls || form.dimObjItemType === cMemberRelCls">

            <q-select
                dense options-dense v-model="relCls" :model-value="relCls" :options="optRelCls"
                :label="$t('relcls')" option-value="id" option-label="name" map-options
                @update:model-value="fnSelectRelCls()" :disable="mode === 'upd' && hasChild"
            />

            <q-select
                dense options-dense v-model="relClsMember" :model-value="relClsMember"
                :options="optRelClsMember" :label="$t('memberRelCls')"
                option-value="id" option-label="name" map-options @update:model-value="fnSelectRelClsMember()"
                class="q-mt-md" :disable="mode === 'upd' && hasChild"
            />
          </div>
        </div>

        <div v-if="form.linkType===3">
          <div v-if="parentDimObjItemType===cRelTyp || parentDimObjItemType===cMemberRelTyp">
            <q-select
                class="q-mt-md"
                dense options-dense v-model="relTypMember" :model-value="relTypMember"
                :options="optRelTypMember" :label="$t('memberRel')" :disable="mode === 'upd' && hasChild"
                option-value="id" option-label="name" map-options @update:model-value="fnSelectRelTypMember()"
            />
          </div>

          <div v-if="parentDimObjItemType===cRelCls || parentDimObjItemType===cMemberRelCls">
            <q-select
                class="q-mt-md"
                dense options-dense v-model="relClsMember" :model-value="relClsMember"
                :options="optRelClsMember" :label="$t('memberRelCls')" :disable="mode === 'upd' && hasChild"
                option-value="id" option-label="name" map-options @update:model-value="fnSelectRelClsMember()"
            />
          </div>
        </div>

        <div v-if="form.linkType===4">

          <div v-if="parentDimObjItemType===cTyp || parentDimObjItemType===cRelTyp ||
                parentDimObjItemType=== cMemberTyp ||  parentDimObjItemType=== cMemberRelTyp">
            <q-select
                class="q-mt-md" :disable="mode === 'upd' && hasChild"
                dense options-dense v-model="relTyp" :model-value="relTyp"
                :options="optRelTyp" :label="$t('reltyp')"
                option-value="id" option-label="name" map-options @update:model-value="fnSelectRelTyp()"
            />
          </div>

          <div v-if="parentDimObjItemType===cCls || parentDimObjItemType===cRelCls ||
                parentDimObjItemType=== cMemberCls ||  parentDimObjItemType=== cMemberRelCls">
            <q-select
                class="q-mt-md" :disable="mode === 'upd' && hasChild"
                dense options-dense v-model="relCls" :model-value="relCls"
                :options="optRelCls" :label="$t('relcls')"
                option-value="id" option-label="name" map-options @update:model-value="fnSelectRelCls()"
            />
          </div>

        </div>

        <div v-if="form.linkType===5">
          <!--  prop -->
          <q-select
              dense options-dense map-options class="q-mb-md"
              v-model="prop" :model-value="prop" :options="optProp"
              :label="$t('prop')" option-value="id" option-label="name"
              @update:model-value="fnSelectProp()" :disable="mode === 'upd' && hasChild"
          />
          <div v-if="visCompTyp">
            <q-select
                dense options-dense v-model="typ" :model-value="typ" :options="optTyp" :label="$t('typ')"
                option-value="id" option-label="name" map-options @update:model-value="fnSelectTyp()"
                class="q-mb-md" :disable="mode === 'upd' && hasChild"
            />
          </div>

          <div v-if="visCompCls">
            <q-select
                dense options-dense v-model="cls" :model-value="cls" :options="optCls" :label="$t('cls')"
                option-value="id" option-label="name" map-options @update:model-value="fnSelectCls()"
                class="q-mb-md" :disable="mode === 'upd' && hasChild"
            />
          </div>

          <div v-if="visCompRelTyp">
            <q-select
                dense options-dense v-model="relTyp" :model-value="relTyp" :options="optRelTyp" :label="$t('reltyp')"
                option-value="id" option-label="name" map-options @update:model-value="fnSelectRelTyp()"
                class="q-mb-md" :disable="mode === 'upd' && hasChild"
            />
          </div>

          <div v-if="visCompRelCls">
            <q-select
                dense options-dense v-model="relCls" :model-value="relCls" :options="optRelCls" :label="$t('relcls')"
                option-value="id" option-label="name" map-options @update:model-value="fnSelectRelCls()"
                class="q-mb-md" :disable="mode === 'upd' && hasChild"
            />
          </div>
        </div>

        <div v-if="form.linkType===6">
          <div v-if="parentDimObjItemType===cRelTyp || parentDimObjItemType===cMemberRelTyp">
            <!-- Промежуточный компонент-->
            <q-select
                class="q-mt-md" :disable="mode === 'upd' && hasChild"
                dense options-dense v-model="relTypMember" :model-value="relTypMember"
                :options="optRelTypMember" :label="$t('memberRel')"
                option-value="id" option-label="name" map-options @update:model-value="fnSelectRelTypMember()"
            />
            <!--  prop -->
            <q-select
                dense options-dense map-options class="q-mt-md q-mb-md"
                v-model="prop" :model-value="prop" :options="optProp"
                :label="$t('prop')" option-value="id" option-label="name"
                @update:model-value="fnSelectProp()" :disable="mode === 'upd' && hasChild"
            />

            <div v-if="visCompTyp">
              <q-select
                  dense options-dense v-model="typ" :model-value="typ" :options="optTyp" :label="$t('typ')"
                  option-value="id" option-label="name" map-options @update:model-value="fnSelectTyp()"
                  class="q-mb-md" :disable="mode === 'upd' && hasChild"
              />
            </div>

            <div v-if="visCompCls">
              <q-select
                  dense options-dense v-model="cls" :model-value="cls" :options="optCls" :label="$t('cls')"
                  option-value="id" option-label="name" map-options @update:model-value="fnSelectCls()"
                  class="q-mb-md" :disable="mode === 'upd' && hasChild"
              />
            </div>

            <div v-if="visCompRelTyp">
              <q-select
                  dense options-dense v-model="relTyp" :model-value="relTyp" :options="optRelTyp" :label="$t('reltyp')"
                  option-value="id" option-label="name" map-options @update:model-value="fnSelectRelTyp()"
                  class="q-mb-md" :disable="mode === 'upd' && hasChild"
              />
            </div>

            <div v-if="visCompRelCls">
              <q-select
                  dense options-dense v-model="relCls" :model-value="relCls" :options="optRelCls" :label="$t('relcls')"
                  option-value="id" option-label="name" map-options @update:model-value="fnSelectRelCls()"
                  class="q-mb-md" :disable="mode === 'upd' && hasChild"
              />
            </div>

          </div>

          <div v-if="parentDimObjItemType===cRelCls || parentDimObjItemType===cMemberRelCls">
            <!-- Промежуточный компонент-->
            <q-select
                class="q-mt-md" :disable="mode === 'upd' && hasChild"
                dense options-dense v-model="relClsMember" :model-value="relClsMember"
                :options="optRelClsMember" :label="$t('memberRelCls')"
                option-value="id" option-label="name" map-options @update:model-value="fnSelectRelClsMember()"
            />
            <!--  prop -->
            <q-select
                dense options-dense map-options class="q-mt-md q-mb-md"
                v-model="prop" :model-value="prop" :options="optProp"
                :label="$t('prop')" option-value="id" option-label="name"
                @update:model-value="fnSelectProp()" :disable="mode === 'upd' && hasChild"
            />

            <div v-if="visCompTyp">
              <q-select
                  dense options-dense v-model="typ" :model-value="typ" :options="optTyp" :label="$t('typ')"
                  option-value="id" option-label="name" map-options @update:model-value="fnSelectTyp()"
                  class="q-mb-md" :disable="mode === 'upd' && hasChild"
              />
            </div>

            <div v-if="visCompCls">
              <q-select
                  dense options-dense v-model="cls" :model-value="cls" :options="optCls" :label="$t('cls')"
                  option-value="id" option-label="name" map-options @update:model-value="fnSelectCls()"
                  class="q-mb-md" :disable="mode === 'upd' && hasChild"
              />
            </div>

            <div v-if="visCompRelTyp">
              <q-select
                  dense options-dense v-model="relTyp" :model-value="relTyp" :options="optRelTyp" :label="$t('reltyp')"
                  option-value="id" option-label="name" map-options @update:model-value="fnSelectRelTyp()"
                  class="q-mb-md" :disable="mode === 'upd' && hasChild"
              />
            </div>

            <div v-if="visCompRelCls">
              <q-select
                  dense options-dense v-model="relCls" :model-value="relCls" :options="optRelCls" :label="$t('relcls')"
                  option-value="id" option-label="name" map-options @update:model-value="fnSelectRelCls()"
                  class="q-mb-md" :disable="mode === 'upd' && hasChild"
              />
            </div>

          </div>

        </div>

        <div v-if="form.linkType===7">

          <!--  prop -->
          <q-select
              dense options-dense map-options class="q-mb-md"
              v-model="prop" :model-value="prop" :options="optProp"
              :label="$t('prop')" option-value="id" option-label="name"
              @update:model-value="fnSelectProp()" :disable="mode === 'upd' && hasChild"
          />


          <div v-if="visCompRelTyp">
            <q-select
                dense options-dense v-model="relTyp" :model-value="relTyp" :options="optRelTyp" :label="$t('reltyp')"
                option-value="id" option-label="name" map-options @update:model-value="fnSelectRelTyp()"
                class="q-mb-md" :disable="mode === 'upd' && hasChild"
            />
          </div>

          <div v-if="visCompRelCls">
            <q-select
                dense options-dense v-model="relCls" :model-value="relCls" :options="optRelCls" :label="$t('relcls')"
                option-value="id" option-label="name" map-options @update:model-value="fnSelectRelCls()"
                class="q-mb-md" :disable="mode === 'upd' && hasChild"
            />
          </div>

        </div>

        <div v-if="form.linkType===9">
          <!--  prop -->
          <q-select
              dense options-dense map-options class="q-mb-md"
              v-model="prop" :model-value="prop" :options="optProp"
              :label="$t('prop')" option-value="id" option-label="name"
              @update:model-value="fnSelectProp()" :disable="mode === 'upd' && hasChild"
          />
          <div v-if="visCompTyp">
            <q-select
                dense options-dense v-model="typ" :model-value="typ" :options="optTyp" :label="$t('typ')"
                option-value="id" option-label="name" map-options @update:model-value="fnSelectTyp()"
                class="q-mb-md" :disable="mode === 'upd' && hasChild"
            />
          </div>

          <div v-if="visCompCls">
            <q-select
                dense options-dense v-model="cls" :model-value="cls" :options="optCls" :label="$t('cls')"
                option-value="id" option-label="name" map-options @update:model-value="fnSelectCls()"
                class="q-mb-md" :disable="mode === 'upd' && hasChild"
            />
          </div>

          <div v-if="visCompRelTyp">
            <q-select
                dense options-dense v-model="relTyp" :model-value="relTyp" :options="optRelTyp" :label="$t('reltyp')"
                option-value="id" option-label="name" map-options @update:model-value="fnSelectRelTyp()"
                class="q-mb-md" :disable="mode === 'upd' && hasChild"
            />
          </div>

          <div v-if="visCompRelCls">
            <q-select
                dense options-dense v-model="relCls" :model-value="relCls" :options="optRelCls" :label="$t('relcls')"
                option-value="id" option-label="name" map-options @update:model-value="fnSelectRelCls()"
                class="q-mb-md" :disable="mode === 'upd' && hasChild"
            />
          </div>
        </div>

        <div v-if="form.linkType===10">

          <q-select
              dense options-dense v-model="relTyp" :model-value="relTyp" :options="optRelTyp" :label="$t('reltyp')"
              option-value="id" option-label="name" map-options @update:model-value="fnSelectRelTyp()"
              class="q-mb-md" :disable="mode === 'upd' && hasChild"
          />

          <div v-if="visCompRelTypMember">
            <q-select
                dense options-dense v-model="relTypMember" :model-value="relTypMember"
                :options="optRelTypMember" :label="$t('memberRel')"
                option-value="id" option-label="name" map-options @update:model-value="fnSelectRelTypMember()"
                class="q-mb-md" :disable="mode === 'upd' && hasChild"
            />
          </div>

          <div v-if="visCompRelCls">
            <q-select
                dense options-dense v-model="relCls" :model-value="relCls" :options="optRelCls" :label="$t('relcls')"
                option-value="id" option-label="name" map-options @update:model-value="fnSelectRelCls()"
                class="q-mb-md" :disable="mode === 'upd' && hasChild"
            />
          </div>

          <div v-if="visCompRelClsMember">
            <q-select
                dense options-dense v-model="relClsMember" :model-value="relClsMember"
                :options="optRelClsMember" :label="$t('memberRelCls')"
                option-value="id" option-label="name" map-options @update:model-value="fnSelectRelClsMember()"
                class="q-mb-md" :disable="mode === 'upd' && hasChild"
            />
          </div>

          <!--  prop -->
          <q-select
              dense options-dense map-options class="q-mb-md"
              v-model="prop" :model-value="prop" :options="optProp"
              :label="$t('prop')" option-value="id" option-label="name"
              @update:model-value="fnSelectProp()"
          />

        </div>

        <div v-if="form.linkType===11">

          <div v-if="visCompRelTypMember">
            <q-select
                dense options-dense v-model="relTypMember" :model-value="relTypMember"
                :options="optRelTypMember" :label="$t('memberRel')"
                option-value="id" option-label="name" map-options @update:model-value="fnSelectRelTypMember()"
                class="q-mb-md" :disable="mode === 'upd' && hasChild"
            />
          </div>

          <div v-if="visCompRelClsMember">
            <q-select
                dense options-dense v-model="relClsMember" :model-value="relClsMember"
                :options="optRelClsMember" :label="$t('memberRelCls')"
                option-value="id" option-label="name" map-options @update:model-value="fnSelectRelClsMember()"
                class="q-mb-md" :disable="mode === 'upd' && hasChild"
            />
          </div>
          <!--  prop -->
          <q-select
              dense options-dense map-options class="q-mb-md"
              v-model="prop" :model-value="prop" :options="optProp"
              :label="$t('prop')" option-value="id" option-label="name"
              @update:model-value="fnSelectProp()"
          />
          <div v-if="visCompTyp">
            <q-select
                dense options-dense v-model="typ" :model-value="typ" :options="optTyp" :label="$t('typ')"
                option-value="id" option-label="name" map-options @update:model-value="fnSelectTyp()"
                class="q-mb-md" :disable="mode === 'upd' && hasChild"
            />
          </div>

          <div v-if="visCompCls">
            <q-select
                dense options-dense v-model="cls" :model-value="cls" :options="optCls" :label="$t('cls')"
                option-value="id" option-label="name" map-options @update:model-value="fnSelectCls()"
                class="q-mb-md" :disable="mode === 'upd' && hasChild"
            />
          </div>

          <div v-if="visCompRelTyp">
            <q-select
                dense options-dense v-model="relTyp" :model-value="relTyp" :options="optRelTyp" :label="$t('reltyp')"
                option-value="id" option-label="name" map-options @update:model-value="fnSelectRelTyp()"
                class="q-mb-md" :disable="mode === 'upd' && hasChild"
            />
          </div>

          <div v-if="visCompRelCls">
            <q-select
                dense options-dense v-model="relCls" :model-value="relCls" :options="optRelCls" :label="$t('relcls')"
                option-value="id" option-label="name" map-options @update:model-value="fnSelectRelCls()"
                class="q-mb-md" :disable="mode === 'upd' && hasChild"
            />
          </div>

        </div>

        <!--  lev  -->
        <q-input
            class="q-mt-md"
            :disable="disableLev"
            dense type="number" stack-label clearable
            v-model="form.lev" :model-value="form.lev"
            :rules="[ (val) => val == null || false || (val && val > 0) || 'Уровень иерархии должна быть положительной',]"
            :label="$t('level')" @update:model-value="fnUpdLev"
        />

        <!---->
      </q-card-section>

      <q-card-actions align="right">
        <q-btn
            dense color="primary" icon="save" :label="$t('save')" @click="onOKClick" :disable="validSave()"
        />
        <q-btn
            dense color="primary" icon="cancel" :label="$t('cancel')" @click="onCancelClick"
        />
      </q-card-actions>
    </q-card>
  </q-dialog>
</template>

<script>
import {api, baseURL} from "boot/axios";
import {ref} from "vue";
import {notifyError, notifySuccess} from "src/utils/jsutils";
import allConsts from "pages/all-consts";

export default {
  props: [
    "data",
    "parentName",
    "parent",
    "parentDimObjItemType",
    "mode",
    "hasChild",
    "lg",
  ],

  data() {
    return {
      form: this.data,
      lang: this.lg,
      lt: this.data.linkType,
      optLT: [],
      doit: this.data.dimObjItemType,
      optDOIT: [],
      optDOITorg: [],

      typ: this.data.typ,
      optTyp: [],

      cls: this.data.cls,
      optCls: [],

      relTyp: this.data.relTyp,
      optRelTyp: [],

      relCls: this.data.relCls,
      optRelCls: [],

      relTypMember: this.data.relTypMember,
      optRelTypMember: [],

      relClsMember: this.data.relClsMember,
      optRelClsMember: [],

      prop: this.data.prop,
      optProp: [],

      visible: ref(false),
      cTyp: allConsts.FD_DimObjItemType.typ,
      cCls: allConsts.FD_DimObjItemType.cls,
      cRelTyp: allConsts.FD_DimObjItemType.reltyp,
      cRelCls: allConsts.FD_DimObjItemType.relcls,
      cMemberTyp: allConsts.FD_DimObjItemType.utyp,
      cMemberCls: allConsts.FD_DimObjItemType.ucls,
      cMemberRelTyp: allConsts.FD_DimObjItemType.ureltyp,
      cMemberRelCls: allConsts.FD_DimObjItemType.urelcls,

      visCompTyp: false,
      visCompCls: false,
      visCompRelTyp: false,
      visCompRelCls: false,

      visCompRelTypMember: false,
      visCompRelClsMember: false,

      disableLev: false,

    };
  },

  emits: [
    // REQUIRED
    "ok",
    "hide",
  ],

  methods: {
    clearComps() {
      this.optTyp = []
      this.form.typ = null;
      this.typ = null;

      this.optCls = []
      this.form.cls = null;
      this.cls = null;

      this.optRelTyp = []
      this.form.relTyp = null;
      this.relTyp = null;

      this.optRelCls = []
      this.form.relCls = null;
      this.relCls = null;
    },

    fnSelectLT() {
      this.form.linkType = this.lt.id;
      this.optDOIT = [];
      this.form.dimObjItemType = null;
      this.doit = null;
      this.clearComps()
      //
      this.dimObjItemTypeFilter(this.parentDimObjItemType, this.lt.id);
    },

    dimObjItemTypeFilter(parentDOIT, linkType) {
      //2: родительский и дочерний компоненты - участники отношения между типами объектов
      if (linkType === allConsts.FD_LinkType.p_c$urt) {
        if (parentDOIT === allConsts.FD_DimObjItemType.typ || parentDOIT === allConsts.FD_DimObjItemType.reltyp ||
            parentDOIT === allConsts.FD_DimObjItemType.utyp || parentDOIT === allConsts.FD_DimObjItemType.ureltyp) {
          this.optDOIT = this.optDOITorg.filter((it) => {
            return it.id === allConsts.FD_DimObjItemType.utyp || it.id === allConsts.FD_DimObjItemType.ureltyp;
          });
        }

        if (parentDOIT === allConsts.FD_DimObjItemType.cls || parentDOIT === allConsts.FD_DimObjItemType.relcls ||
            parentDOIT === allConsts.FD_DimObjItemType.ucls || parentDOIT === allConsts.FD_DimObjItemType.urelcls) {
          this.optDOIT = this.optDOITorg.filter((it) => {
            return it.id === allConsts.FD_DimObjItemType.ucls || it.id === allConsts.FD_DimObjItemType.urelcls;
          });
        }
      }

      //3: дочерний компонент участник родительского компонента (родительский компонент - отношение между типами объектов, отношение между объектами)
      //reltyp, relcls, reltypmemb, relclmemb
      if (linkType === allConsts.FD_LinkType.c$up) {
        if (parentDOIT === allConsts.FD_DimObjItemType.reltyp || parentDOIT === allConsts.FD_DimObjItemType.ureltyp) {
          this.optDOIT = this.optDOITorg.filter((it) => {
            return it.id === allConsts.FD_DimObjItemType.utyp || it.id === allConsts.FD_DimObjItemType.ureltyp;
          });
        } else if (parentDOIT === allConsts.FD_DimObjItemType.relcls || parentDOIT === allConsts.FD_DimObjItemType.urelcls) {
          this.optDOIT = this.optDOITorg.filter((it) => {
            return it.id === allConsts.FD_DimObjItemType.ucls || it.id === allConsts.FD_DimObjItemType.urelcls;
          });
        }
      }

      //4: родительский компонент участник дочернего компонента (дочерний компонент - отношение между типами объектов, отношение между объектами)
      if (linkType === allConsts.FD_LinkType.p$uc) {
        if (parentDOIT === allConsts.FD_DimObjItemType.typ || parentDOIT === allConsts.FD_DimObjItemType.reltyp ||
            parentDOIT === allConsts.FD_DimObjItemType.ureltyp || parentDOIT === allConsts.FD_DimObjItemType.utyp) {
          this.optDOIT = this.optDOITorg.filter((it) => {
            return it.id === allConsts.FD_DimObjItemType.reltyp;
          });
        }

        if (parentDOIT === allConsts.FD_DimObjItemType.cls || parentDOIT === allConsts.FD_DimObjItemType.relcls ||
            parentDOIT === allConsts.FD_DimObjItemType.urelcls || parentDOIT === allConsts.FD_DimObjItemType.ucls) {
          this.optDOIT = this.optDOITorg.filter((it) => {
            return it.id === allConsts.FD_DimObjItemType.relcls;
          });
        }
      }

      //5: дочерний компонент - значения одного свойства родительского компонента
      //6: дочерний компонент - значения одного свойства участника родительского компонента
      //9: родительский компонент - значения одного свойства дочернего компонента
      //11: участник родительского компонента - значение одного свойства дочернего компонента
      // cTyp, cCls, cRelTyp, cRelCls
      if (linkType === allConsts.FD_LinkType.c$val_p || linkType === allConsts.FD_LinkType.c$val_up ||
          linkType === allConsts.FD_LinkType.r$val_c || linkType === allConsts.FD_LinkType.up$val_c) {
        this.optDOIT = this.optDOITorg.filter((it) => {
          return it.id === allConsts.FD_DimObjItemType.typ || it.id === allConsts.FD_DimObjItemType.cls ||
              it.id === allConsts.FD_DimObjItemType.reltyp || it.id === allConsts.FD_DimObjItemType.relcls;
        });
      }

      //7: участник дочернего компонента - значения одного свойства родительского компонента
      //8: участник дочернего компонента - значения одного свойства участника родительского компонента
      //10: родительский компонент - значения одного свойства участника дочернего компонента
      //12: участник родительского компонента - значения одного свойства участника дочернего компонента
      if (linkType === allConsts.FD_LinkType.uc$val_p || linkType === allConsts.FD_LinkType.uc$val_up ||
          linkType === allConsts.FD_LinkType.r$val_uc || linkType === allConsts.FD_LinkType.up$val_uc) {
        this.optDOIT = this.optDOITorg.filter((it) => {
          return it.id === allConsts.FD_DimObjItemType.reltyp || it.id === allConsts.FD_DimObjItemType.relcls
        });
      }
    },

    fnSelectDOIT() {
      this.form.dimObjItemType = this.doit.id;
      this.clearComps()
      //отсутствует (декарово произведение)
      if (this.form.linkType === allConsts.FD_LinkType.none) {
        switch (this.doit.id) {
          case allConsts.FD_DimObjItemType.typ:
          case allConsts.FD_DimObjItemType.cls:
            this.typ = null;
            this.loadTyp(0, this.parent, this.doit.id, this.form.linkType);
            break;
          case allConsts.FD_DimObjItemType.reltyp:
          case allConsts.FD_DimObjItemType.relcls:
          case allConsts.FD_DimObjItemType.utyp:
          case allConsts.FD_DimObjItemType.ureltyp:
          case allConsts.FD_DimObjItemType.ucls:
          case allConsts.FD_DimObjItemType.urelcls:
            this.relTyp = null;
            this.loadRelTyp(this.doit.id, this.parent, this.form.linkType);
            break;
        }
      }
      //родительский и дочерний компоненты - участники отношения между типами объектов
      if (this.form.linkType === allConsts.FD_LinkType.p_c$urt) { //2
        switch (this.doit.id) {
          case allConsts.FD_DimObjItemType.utyp:
          case allConsts.FD_DimObjItemType.ureltyp:
            this.relTyp = null;
            this.loadRelTyp(this.doit.id, this.parent, this.form.linkType);
            break;
          case allConsts.FD_DimObjItemType.ucls:
          case allConsts.FD_DimObjItemType.urelcls:
            this.relCls = null;
            this.loadRelCls(0, this.doit.id, this.parent, this.form.linkType);
            break;
        }
      }
      //дочерний компонент участник родительского компонента (родительский компонент - отношение между типами объектов, отношение между классами объектов)
      if (this.form.linkType === allConsts.FD_LinkType.c$up) { //3
        if (this.parentDimObjItemType === allConsts.FD_DimObjItemType.reltyp) {
          if (this.doit.id === allConsts.FD_DimObjItemType.utyp) {
            this.loadRelTypMember(1, 0, this.parent, this.form.linkType);
          } else if (this.doit.id === allConsts.FD_DimObjItemType.ureltyp) {
            this.loadRelTypMember(2, 0, this.parent, this.form.linkType);
          }
        }

        if (this.parentDimObjItemType === allConsts.FD_DimObjItemType.relcls) {
          if (this.doit.id === allConsts.FD_DimObjItemType.ucls)
            this.loadRelClsMember(allConsts.FD_MemberType.cls, 0, this.parent, this.form.linkType);
          else
            this.loadRelClsMember(allConsts.FD_MemberType.relcls, 0, this.parent, this.form.linkType);
        }
      }
      //родительский компонент участник дочернего компонента (дочерний компонент - отношение между типами объектов, отношение между классами объектов)
      if (this.form.linkType === allConsts.FD_LinkType.p$uc) { //4
        if (this.parentDimObjItemType === this.cTyp || this.parentDimObjItemType === this.cRelTyp ||
            this.parentDimObjItemType === this.cMemberTyp || this.parentDimObjItemType === this.cMemberRelTyp) {
          this.loadRelTyp(this.doit.id, this.parent, this.form.linkType)
        }

        if (this.parentDimObjItemType === this.cCls || this.parentDimObjItemType === this.cRelCls ||
            this.parentDimObjItemType === this.cMemberCls || this.parentDimObjItemType === this.cMemberRelCls) {
          this.loadRelCls(0, this.doit.id, this.parent, this.form.linkType)
        }
      }
      // дочерний компонент - значения одного свойства родительского компонента-5
      // Родительский компонент - значения одного свойства дочернего компонента-9
      if (this.form.linkType === allConsts.FD_LinkType.c$val_p ||
          this.form.linkType === allConsts.FD_LinkType.r$val_c) {
        this.prop = null
        this.form.prop = null

        this.visCompTyp = false
        this.visCompCls = false
        this.visCompRelTyp = false
        this.visCompRelCls = false
        if (this.doit.id === this.cTyp || this.doit.id === this.cMemberTyp)
          this.visCompTyp = true
        else if (this.doit.id === this.cCls || this.doit.id === this.cMemberCls)
          this.visCompCls = true
        else if (this.doit.id === this.cRelTyp || this.doit.id === this.cMemberRelTyp)
          this.visCompRelTyp = true
        else if (this.doit.id === this.cRelCls || this.doit.id === this.cMemberRelCls)
          this.visCompRelCls = true

        this.loadProp(this.doit.id, this.parent, this.form.linkType)
      }
      //дочерний компонент - значения одного свойства участника родительского компонента
      if (this.form.linkType === allConsts.FD_LinkType.c$val_up) { //6 val_UchPar(child)
        this.prop = null
        this.form.prop = null

        this.visCompTyp = false
        this.visCompCls = false
        this.visCompRelTyp = false
        this.visCompRelCls = false
        if (this.doit.id === this.cTyp || this.doit.id === this.cMemberTyp)
          this.visCompTyp = true
        else if (this.doit.id === this.cCls || this.doit.id === this.cMemberCls)
          this.visCompCls = true
        else if (this.doit.id === this.cRelTyp || this.doit.id === this.cMemberRelTyp)
          this.visCompRelTyp = true
        else if (this.doit.id === this.cRelCls || this.doit.id === this.cMemberRelCls)
          this.visCompRelCls = true

        if (this.parentDimObjItemType === this.cRelTyp || this.parentDimObjItemType === this.cMemberRelTyp) {
          this.loadRelTypMember(0, 0, this.parent, this.form.linkType)
        }
        if (this.parentDimObjItemType === this.cRelCls || this.parentDimObjItemType === this.cMemberRelCls) {
          this.loadRelClsMember(0, 0, this.parent, this.form.linkType)
        }
      }
      //участник дочернего компонента - значения одного свойства родительского компонента
      if (this.form.linkType === allConsts.FD_LinkType.uc$val_p) {    //7
        this.prop = null
        this.form.prop = null
        this.visCompRelTyp = false
        this.visCompRelCls = false

        if (this.doit.id === this.cRelTyp || this.doit.id === this.cMemberRelTyp)
          this.visCompRelTyp = true
        else if (this.doit.id === this.cRelCls || this.doit.id === this.cMemberRelCls)
          this.visCompRelCls = true

        this.loadProp(this.doit.id, this.parent, this.form.linkType)
      }
      // Родительский компонент - значения одного свойства участника дочернего компонента
      if (this.form.linkType === allConsts.FD_LinkType.r$val_uc) {    //10
        this.prop = null
        this.form.prop = null

        this.visCompRelTypMember = false
        this.visCompRelCls = false
        this.visCompRelClsMember = false

        if (this.doit.id === this.cRelTyp)
          this.visCompRelTypMember = true
        else if (this.doit.id === this.cRelCls) {
          this.visCompRelCls = true
          this.visCompRelClsMember = true
        }

        this.loadRelTyp(this.doit.id, this.parent, this.form.linkType)
      }
      //Участник родительского компонента - значение одного свойства дочернего компонента
      if (this.form.linkType === allConsts.FD_LinkType.up$val_c) {    //11
        this.prop = null
        this.form.prop = null

        this.relTypMember = null
        this.form.relTypMember = null
        this.relClsMember = null
        this.form.relClsMember = null
        this.clearComps()

        this.visCompTyp = false
        this.visCompCls = false
        this.visCompRelTyp = false
        this.visCompRelCls = false
        this.visCompRelTypMember = false
        this.visCompRelClsMember = false

        if (this.parentDimObjItemType===this.cRelTyp || this.parentDimObjItemType===this.cMemberRelTyp) {
          this.visCompRelTypMember = true
          this.loadRelTypMember(0, 0, this.parent, this.form.linkType)
        }
        if (this.parentDimObjItemType===this.cRelCls || this.parentDimObjItemType===this.cMemberRelCls) {
          this.visCompRelClsMember = true
          this.loadRelClsMember(0, 0, this.parent, this.form.linkType)
        }

        if (this.doit.id === this.cTyp)
          this.visCompTyp = true
        else if (this.doit.id === this.cCls)
          this.visCompCls = true
        else if (this.doit.id === this.cRelTyp)
          this.visCompRelTyp = true
        else if (this.doit.id === this.cRelCls)
          this.visCompRelCls = true

      }

      //
      this.disableLev = !(this.doit.id === this.cTyp || this.doit.id === this.cCls || this.doit.id === this.cMemberTyp || this.doit.id === this.cMemberCls);

    },

    fnSelectProp() {
      this.form.prop = this.prop.id

      if (this.form.linkType === allConsts.FD_LinkType.up$val_c) {  // 11
          this.loadCompFor_11(this.form.dimObjItemType, this.prop.id)
      } else {
        if (this.form.linkType !== allConsts.FD_LinkType.r$val_uc) {  // != 10
          this.clearComps()
          //for lt: 5,6, 9
          if (this.form.linkType === allConsts.FD_LinkType.c$val_p ||
              this.form.linkType === allConsts.FD_LinkType.c$val_up ||
              this.form.linkType === allConsts.FD_LinkType.r$val_c)
            this.loadValueOfProp(this.form.dimObjItemType, this.form.prop, this.parent, this.form.linkType)

          if (this.form.linkType === allConsts.FD_LinkType.uc$val_p)  //7
            this.loadValueOfProp_7(this.form.dimObjItemType, this.prop.id, 0, this.prop["typorrel"], this.form.linkType)
        }
      }

    },

    fnUpdLev() {
      //console.log("fnUpdLev(val)", val)
    },

    fnSelectTyp() {

      if (this.form.linkType===allConsts.FD_LinkType.up$val_c) {
        this.form.typ = this.typ.id;
      } else {
        if (this.form.dimObjItemType === this.cTyp || this.form.dimObjItemType === this.cCls) {
          this.form.typ = this.typ.id;
          this.cls = null;

          if (this.form.dimObjItemType === this.cCls)
            this.loadCls(this.form.typ);
        }
      }
    },

    fnSelectCls() {
      this.form.cls = this.cls.id;

      if (this.form.linkType < allConsts.FD_LinkType.c$val_p) { //lt=5
        if (this.form.dimObjItemType === this.cMemberCls) {
          this.loadRelClsMember(allConsts.FD_MemberType.cls, this.form.relTyp, this.parent, this.form.linkType);
        }
        if (this.form.dimObjItemType === this.cMemberRelCls) {
          this.loadRelClsMember(allConsts.FD_MemberType.relcls, this.form.relTyp, this.parent, this.form.linkType);
        }
      }

      //
    },

    fnSelectRelTyp() {
      this.form.relTyp = this.relTyp.id;

      if (this.form.linkType !== allConsts.FD_LinkType.up$val_c) {
        this.relCls = null;
        this.form.relCls = null;

        this.relClsMember = null;
        this.form.relClsMember = null;

        this.relTypMember = null;
        this.form.relTypMember = null;
        //
        if (this.form.linkType === allConsts.FD_LinkType.none) {  //1

          if (this.form.dimObjItemType === this.cRelCls)
            this.loadRelCls(this.form.relTyp, this.form.dimObjItemType, this.parent, this.form.linkType);
          //
          if (this.form.dimObjItemType === this.cMemberCls || this.form.dimObjItemType === this.cMemberRelCls) {
            this.loadRelCls(this.form.relTyp, this.form.dimObjItemType, this.parent, this.form.linkType)
          }

          //
          if (this.form.dimObjItemType === this.cMemberTyp)
            this.loadRelTypMember(allConsts.FD_MemberType.typ, this.form.relTyp, this.parent, this.form.linkType);

          if (this.form.dimObjItemType === this.cMemberRelTyp)
            this.loadRelTypMember(allConsts.FD_MemberType.reltyp, this.form.relTyp, this.parent, this.form.linkType);
        }

        if (this.form.linkType === allConsts.FD_LinkType.p_c$urt) { //2

          if (this.form.dimObjItemType === this.cMemberTyp) {
            this.loadRelTypMember(allConsts.FD_MemberType.typ, this.form.relTyp, this.parent, this.form.linkType);
          }
          if (this.form.dimObjItemType === this.cMemberRelTyp) {
            this.loadRelTypMember(allConsts.FD_MemberType.reltyp, this.form.relTyp, this.parent, this.form.linkType);
          }

          if (this.form.dimObjItemType === this.cMemberCls) {
            this.loadRelCls(this.form.relTyp, this.form.dimObjItemType, this.parent, this.form.linkType);
          }
          if (this.form.dimObjItemType === this.cMemberRelCls) {
            this.loadRelCls(this.form.relTyp, this.form.dimObjItemType, this.parent, this.form.linkType);
          }

        }

        if (this.form.linkType === allConsts.FD_LinkType.uc$val_p) {  //7
          this.form.relTypMember = this.relTyp["rtm"]
        }

        if (this.form.linkType === allConsts.FD_LinkType.r$val_uc) {  //10
          this.prop = null
          this.form.prop = null
          if (this.form.dimObjItemType === allConsts.FD_DimObjItemType.reltyp)
            this.loadRelTypMember(0, this.relTyp.id, this.parent, this.form.linkType)
          else if (this.form.dimObjItemType === allConsts.FD_DimObjItemType.relcls)
            this.loadRelCls(this.form.relTyp, this.form.dimObjItemType, this.parent, this.form.linkType)
        }
      }
    },

    fnSelectRelCls() {
      this.form.relCls = this.relCls.id;

      if (this.form.linkType !== allConsts.FD_LinkType.up$val_c) {  //11

        if (this.form.linkType === allConsts.FD_LinkType.none) {  //1

          if (this.form.dimObjItemType === allConsts.FD_DimObjItemType.ucls) {
            this.loadRelClsMember(allConsts.FD_MemberType.cls, this.form.relCls, this.parent, this.form.linkType)
          }

          if (this.form.dimObjItemType === allConsts.FD_DimObjItemType.urelcls) {
            this.loadRelClsMember(allConsts.FD_MemberType.relcls, this.form.relCls, this.parent, this.form.linkType)
          }
        }

        if (this.form.linkType === allConsts.FD_LinkType.p_c$urt) { //2
          if (this.form.dimObjItemType === this.cMemberCls) {
            this.loadRelClsMember(allConsts.FD_MemberType.cls, this.form.relCls, this.parent, this.form.linkType);
          }
          if (this.form.dimObjItemType === this.cMemberRelCls) {
            this.loadRelClsMember(allConsts.FD_MemberType.relcls, this.form.relCls, this.parent, this.form.linkType);
          }
        }

        if (this.form.linkType === allConsts.FD_LinkType.uc$val_p) {  //7
          this.form.relClsMember = this.relCls["rcm"]
        }

        if (this.form.linkType === allConsts.FD_LinkType.r$val_uc) {  //10
          this.prop = null;
          this.form.prop = null;
          this.relClsMember = null;
          this.form.relClsMember = null;

          this.loadRelClsMember(allConsts.FD_MemberType.relcls, this.form.relCls, this.parent, this.form.linkType);
        }
      }
    },

    fnSelectRelTypMember() {
      this.form.relTypMember = this.relTypMember.id;

      if (this.form.linkType === allConsts.FD_LinkType.c$val_up) {  //6
        this.loadProp_6(this.relTypMember["membertype"], this.relTypMember["typorrel"], this.form.dimObjItemType, this.parent)
      }

      if (this.form.linkType === allConsts.FD_LinkType.r$val_uc) {  //10
        this.prop = null
        this.form.prop = null
        this.loadProp_10_11(this.form.dimObjItemType, this.relTypMember.id, this.parent, this.form.linkType)
      }

      if (this.form.linkType === allConsts.FD_LinkType.up$val_c) {  //11
        this.prop = null
        this.form.prop = null
        this.clearComps()
        this.loadProp_10_11(this.form.dimObjItemType, this.relTypMember.id, this.parent, this.form.linkType)
      }

    },

    fnSelectRelClsMember() {
      this.form.relClsMember = this.relClsMember.id;

      if (this.form.linkType === allConsts.FD_LinkType.c$val_up) {    //6
        this.loadProp_6(this.relClsMember["membertype"], this.relClsMember["clsorrel"], this.form.dimObjItemType, this.parent)
      }

      if (this.form.linkType === allConsts.FD_LinkType.r$val_uc) {    //10
        this.prop = null
        this.form.prop = null
        this.loadProp_10_11(this.form.dimObjItemType, this.relClsMember.id, this.parent, this.form.linkType)
      }

      if (this.form.linkType === allConsts.FD_LinkType.up$val_c) {  //11
        this.prop = null
        this.form.prop = null
        this.clearComps()
        this.loadProp_10_11(this.form.dimObjItemType, this.relClsMember.id, this.parent, this.form.linkType)
      }


    },

    validSave() {
      if (!this.form.dimObjItemType)
        return true

      if (this.form.linkType === allConsts.FD_LinkType.none) {  //1

        if (this.form.dimObjItemType === this.cTyp && !this.form.typ)
          return true;
        if (this.form.dimObjItemType === this.cCls && !this.form.cls)
          return true;
        if (this.form.dimObjItemType === this.cRelTyp && !this.form.relTyp)
          return true;
        if (this.form.dimObjItemType === this.cRelCls && !this.form.relCls)
          return true;
        if ((this.form.dimObjItemType === this.cMemberTyp || this.form.dimObjItemType === this.cMemberRelTyp)
            && (this.form.relTyp == null || this.form.relTypMember == null))
          return true;
        if ((this.form.dimObjItemType === this.cMemberCls || this.form.dimObjItemType === this.cMemberRelCls)
            && (this.form.relCls == null || this.form.relClsMember == null))
          return true;

      } else if (this.form.linkType === allConsts.FD_LinkType.p_c$urt) {    //2
        if (!((this.form.relTyp && this.form.relTypMember) || (this.form.relCls && this.form.relClsMember)))
          return true
      } else if (this.form.linkType === allConsts.FD_LinkType.c$up) {    //3

        if ((this.form.dimObjItemType === this.cMemberTyp || this.form.dimObjItemType === this.cMemberRelTyp)
            && this.form.relTypMember == null)
          return true;

        if ((this.form.dimObjItemType === this.cMemberCls || this.form.dimObjItemType === this.cMemberRelCls)
            && this.form.relClsMember == null)
          return true;

      } else if (this.form.linkType === allConsts.FD_LinkType.p$uc) {    //4

        return !((this.form.dimObjItemType === this.cRelTyp && this.form.relTyp) || (this.form.dimObjItemType === this.cRelCls && this.form.relCls))

      } else if (this.form.linkType === allConsts.FD_LinkType.c$val_p || this.form.linkType === allConsts.FD_LinkType.r$val_c) {    //5, 9
        if (!this.form.prop)
          return true;
        else {
          if (this.form.dimObjItemType === this.cTyp)
            if (!this.form.typ)
              return true;
          if (this.form.dimObjItemType === this.cCls)
            if (!this.form.cls)
              return true;
          if (this.form.dimObjItemType === this.cRelTyp)
            if (!this.form.relTyp)
              return true;
          if (this.form.dimObjItemType === this.cRelCls)
            if (!this.form.relCls)
              return true;
        }

      } else if (this.form.linkType === allConsts.FD_LinkType.c$val_up) {    //6
        if (!this.form.prop)
          return true;
        else {
          if (this.parentDimObjItemType === this.cRelTyp) {
            if (this.form.dimObjItemType === this.cTyp)
              return !(this.form.typ && this.form.relTypMember)

            if (this.form.dimObjItemType === this.cCls)
              return !(this.form.cls && this.form.relTypMember)

            if (this.form.dimObjItemType === this.cRelTyp)
              return !(this.form.relTyp && this.form.relTypMember)

            if (this.form.dimObjItemType === this.cRelCls)
              return !(this.form.relCls && this.form.relTypMember)

          } else if (this.parentDimObjItemType === this.cRelCls) {
            if (this.form.dimObjItemType === this.cTyp)
              return !(this.form.typ && this.form.relClsMember)

            if (this.form.dimObjItemType === this.cCls)
              return !(this.form.cls && this.form.relClsMember)

            if (this.form.dimObjItemType === this.cRelTyp)
              return !(this.form.relTyp && this.form.relClsMember)

            if (this.form.dimObjItemType === this.cRelCls)
              return !(this.form.relCls && this.form.relClsMember)
          }
        }
      } else if (this.form.linkType === allConsts.FD_LinkType.uc$val_p || this.form.linkType === allConsts.FD_LinkType.r$val_uc) {    //7, 10
        if (!this.form.prop)
          return true;
        else {
          if (this.form.dimObjItemType === this.cRelTyp)
            return !(this.form.relTyp && this.form.relTypMember)

          if (this.form.dimObjItemType === this.cRelCls)
            return !(this.form.relCls && this.form.relClsMember)
        }
      } else if (this.form.linkType === allConsts.FD_LinkType.up$val_c) {    //11
        if (!this.form.prop)
          return true;
        else {
          if (this.parentDimObjItemType===this.cRelTyp) {
            if (this.form.dimObjItemType === this.cTyp)
              return !(this.form.typ && this.form.relTypMember)
            if (this.form.dimObjItemType === this.cCls)
              return !(this.form.cls && this.form.relTypMember)
            if (this.form.dimObjItemType === this.cRelCls)
              return !(this.form.relCls && this.form.relTypMember)
            if (this.form.dimObjItemType === this.cRelTyp)
              return !(this.form.relTyp && this.form.relTypMember)
          }
          if (this.parentDimObjItemType===this.cRelCls) {
            if (this.form.dimObjItemType === this.cTyp)
              return !(this.form.typ && this.form.relClsMember)
            if (this.form.dimObjItemType === this.cCls)
              return !(this.form.cls && this.form.relClsMember)
            if (this.form.dimObjItemType === this.cRelCls)
              return !(this.form.relCls && this.form.relClsMember)
            if (this.form.dimObjItemType === this.cRelTyp)
              return !(this.form.relTyp && this.form.relClsMember)
          }
        }
      }

    },

    loadCompFor_11(doit, prop) {
      this.visible = ref(true);
      //
      api
          .post(baseURL, {
            method: "dimobj/loadCompFor_11",
            params: [doit, prop],
          })
          .then((response) => {
            if (doit === this.cTyp)
              this.optTyp = response.data.result.records;
            if (doit === this.cCls)
              this.optCls = response.data.result.records;
            if (doit === this.cRelTyp)
              this.optRelTyp = response.data.result.records;
            if (doit === this.cRelCls)
              this.optRelCls = response.data.result.records;
          })
          .catch((error) => {
            let msg = error.message;
            if (error.response) msg = error.response.data.error.message;
            notifyError(msg);
          })
          .finally(() => {
            this.visible = ref(false);
          });


    },

    loadValueOfProp(doit, prop, parent, linkType) {
      this.visible = ref(true);
      //
      parent = parent ? parseInt(parent, 10) : 0;

      api
          .post(baseURL, {
            method: "dimobj/loadValueOfProp",
            params: [doit, prop, parent, linkType],
          })
          .then((response) => {
            if (this.form.linkType === allConsts.FD_LinkType.c$val_p ||
                this.form.linkType === allConsts.FD_LinkType.r$val_c) { //5, 9
              if (doit === this.cTyp || doit === this.cMemberTyp) {
                this.visCompTyp = true
                this.optTyp = response.data.result.records;
              } else if (doit === this.cCls || doit === this.cMemberCls) {
                this.visCompCls = true
                this.optCls = response.data.result.records;
              } else if (doit === this.cRelTyp || doit === this.cMemberRelTyp) {
                this.visCompRelTyp = true
                this.optRelTyp = response.data.result.records;
              } else if (doit === this.cRelCls || doit === this.cMemberRelCls) {
                this.visCompRelCls = true
                this.optRelCls = response.data.result.records;
              }
            }
            if (this.form.linkType === allConsts.FD_LinkType.uc$val_p) {  //7
              if (doit === this.cRelTyp) {
                this.visCompRelTyp = true
                this.optRelTyp = response.data.result.records;
              } else if (doit === this.cRelCls) {
                this.visCompRelCls = true
                this.optRelCls = response.data.result.records;
              }
            }
          })
          .catch((error) => {
            let msg = error.message;
            if (error.response) msg = error.response.data.error.message;
            notifyError(msg);
          })
          .finally(() => {
            this.visible = ref(false);
          });
    },

    loadValueOfProp_7(doit, prop, rtmORrcm, typORrel, linkType) {
      this.visible = ref(true);
      //
      api
          .post(baseURL, {
            method: "dimobj/loadValueOfProp_7",
            params: [doit, prop, rtmORrcm, typORrel, linkType],
          })
          .then((response) => {
            if (this.form.linkType === allConsts.FD_LinkType.uc$val_p) {
              if (doit === this.cRelTyp) {
                this.visCompRelTyp = true
                this.optRelTyp = response.data.result.records;
              } else if (doit === this.cRelCls) {
                this.visCompRelCls = true
                this.optRelCls = response.data.result.records;
              }
            }
          })
          .catch((error) => {
            let msg = error.message;
            if (error.response) msg = error.response.data.error.message;
            notifyError(msg);
          })
          .finally(() => {
            this.visible = ref(false);
          });
    },

    loadProp_10_11(doit, rtmORrcm, parent, linkType) {
      this.visible = ref(true);
      //
      parent = parent ? parseInt(parent, 10) : 0;

      api
          .post(baseURL, {
            method: "dimobj/loadProp_10_11",
            params: [doit, rtmORrcm, parent, linkType],
          })
          .then((response) => {
            this.optProp = response.data.result.records;
          })
          .catch((error) => {
            let msg = error.message;
            if (error.response) msg = error.response.data.error.message;
            notifyError(msg);
          })
          .finally(() => {
            this.visible = ref(false);
          });
    },

    loadProp_6(memberType, typORrel, doit, parent) {
      this.visible = ref(true);
      //
      parent = parent ? parseInt(parent, 10) : 0;

      api
          .post(baseURL, {
            method: "dimobj/loadProp_6",
            params: [memberType, typORrel, doit, parent],
          })
          .then((response) => {
            this.optProp = response.data.result.records;
          })
          .catch((error) => {
            let msg = error.message;
            if (error.response) msg = error.response.data.error.message;
            notifyError(msg);
          })
          .finally(() => {
            this.visible = ref(false);
          });
    },

    loadProp(doit, parent, linkType) {
      this.visible = ref(true);
      //
      parent = parent ? parseInt(parent, 10) : 0;

      api
          .post(baseURL, {
            method: "dimobj/loadProp",
            params: [doit, parent, linkType],
          })
          .then((response) => {
            this.optProp = response.data.result.records;
          })
          .catch((error) => {
            let msg = error.message;
            if (error.response) msg = error.response.data.error.message;
            notifyError(msg);
          })
          .finally(() => {
            this.visible = ref(false);
          });
    },

    loadTyp(relTyp, parent, doit, linkType) {
      this.visible = ref(true);
      //
      parent = parent ? parseInt(parent, 10) : 0;

      api
          .post(baseURL, {
            method: "dimobj/loadTypForSelect",
            params: [relTyp, parent, doit, linkType],
          })
          .then((response) => {
            this.optTyp = response.data.result.records;
          })
          .catch((error) => {
            let msg = error.message;
            if (error.response) msg = error.response.data.error.message;
            notifyError(msg);
          })
          .finally(() => {
            this.visible = ref(false);
          });
    },

    loadRelTypMember(memberType, relTyp, parent, linkType) {
      this.visible = ref(true);
      //
      api
          .post(baseURL, {
            method: "dimobj/loadRelTypMember",
            params: [memberType, relTyp, parent, linkType],
          })
          .then((response) => {
            this.optRelTypMember = response.data.result.records;
          })
          .catch((error) => {
            let msg = error.message;
            if (error.response) msg = error.response.data.error.message;
            notifyError(msg);
          })
          .finally(() => {
            this.visible = ref(false);
          });
    },

    loadRelClsMember(memberType, relclsORrelTyp, parent, linkType) {
      this.visible = ref(true);
      //
      api
          .post(baseURL, {
            method: "dimobj/loadRelClsMember",
            params: [memberType, relclsORrelTyp, parent, linkType],
          })
          .then((response) => {
            this.optRelClsMember = response.data.result.records;
          })
          .catch((error) => {
            let msg = error.message;
            if (error.response) msg = error.response.data.error.message;
            notifyError(msg);
          })
          .finally(() => {
            this.visible = ref(false);
          });
    },

    loadCls(typ) {
      this.visible = ref(true);
      //
      api
          .post(baseURL, {
            method: "dimobj/loadClsForSelect",
            params: [typ],
          })
          .then((response) => {
            this.optCls = response.data.result.records;
          })
          .catch((error) => {
            let msg = error.message;
            if (error.response) msg = error.response.data.error.message;
            notifyError(msg);
          })
          .finally(() => {
            this.visible = ref(false);
          });
    },

    loadRelTyp(doit, parent, linkType) {
      this.visible = ref(true);
      //
      api
          .post(baseURL, {
            method: "dimobj/loadRelTypForSelect",
            params: [doit, parent, linkType],
          })
          .then((response) => {
            this.optRelTyp = response.data.result.records;
          })
          .catch((error) => {
            let msg = error.message;
            if (error.response) msg = error.response.data.error.message;
            notifyError(msg);
          })
          .finally(() => {
            this.visible = ref(false);
          });
    },

    loadRelCls(reltyp, doit, parent, linkType) {
      this.visible = ref(true);
      //
      api
          .post(baseURL, {
            method: "dimobj/loadRelClsForSelect",
            params: [reltyp, doit, parent, linkType],
          })
          .then((response) => {
            this.optRelCls = response.data.result.records;
          })
          .catch((error) => {
            let msg = error.message;
            if (error.response) msg = error.response.data.error.message;
            notifyError(msg);
          })
          .finally(() => {
            this.visible = ref(false);
          });

    },

    // following method is REQUIRED
    // (don't change its name --> "show")
    show() {
      this.$refs.dialog.show();
    },

    // following method is REQUIRED
    // (don't change its name --> "hide")
    hide() {
      this.$refs.dialog.hide();
    },

    onDialogHide() {
      // required to be emitted
      // when QDialog emits "hide" event
      this.$emit("hide");
    },

    onOKClick() {
      // on OK, it is REQUIRED to
      // emit "ok" event (with optional payload)
      // before hiding the QDialog

      this.visible = ref(true);
      const method = this.mode === "ins" ? "insertDOI" : "updateDOI";

      api
          .post(baseURL, {
            id: this.form.id,
            method: "dimobj/" + method,
            params: [this.form],
          })
          .then(
              (response) => {
                this.$emit("ok", response.data.result.records[0]);
                notifySuccess(this.$t("success"));
              },
              (error) => {
                notifyError(error.response.data.error.message);
              }
          )
          .finally(() => {
            this.hide();
            this.visible = ref(false);
          });
    },

    onCancelClick() {
      // we just need to hide the dialog
      this.hide();
    },

    linkTypeFilter(parentDOIT) {
      if (this.parent > 0) {
        if (parentDOIT === allConsts.FD_DimObjItemType.typ || parentDOIT === allConsts.FD_DimObjItemType.cls) { //1,2,4,5,7,9,10
          this.optLT = this.optLT.filter((it) => {
            return !(it.id === allConsts.FD_LinkType.c$up || it.id === allConsts.FD_LinkType.c$val_up ||
                it.id === allConsts.FD_LinkType.uc$val_up || it.id === allConsts.FD_LinkType.up$val_c ||
                it.id === allConsts.FD_LinkType.up$val_uc)
          });
        }
        if (parentDOIT === allConsts.FD_DimObjItemType.reltyp || parentDOIT === allConsts.FD_DimObjItemType.relcls ||
            parentDOIT === allConsts.FD_DimObjItemType.ureltyp || parentDOIT === allConsts.FD_DimObjItemType.urelcls) { //All
          this.optLT = this.optLT.filter(() => {
            return true
          });
        }
        if (parentDOIT === allConsts.FD_DimObjItemType.utyp || parentDOIT === allConsts.FD_DimObjItemType.ucls) { // //1,2,4,5,7,9,10
          this.optLT = this.optLT.filter((it) => {
            return !(it.id === allConsts.FD_LinkType.c$up || it.id === allConsts.FD_LinkType.c$val_up ||
                it.id === allConsts.FD_LinkType.uc$val_up || it.id === allConsts.FD_LinkType.up$val_c ||
                it.id === allConsts.FD_LinkType.up$val_uc)
          });
        }
      }
    },
  },

  created() {

    this.visible = ref(true);
    api
        .post(baseURL, {
          method: "dict/load",
          params: [{dict: "FD_LinkType"}],
        })
        .then((response) => {
          this.optLT = response.data.result.records;
          this.linkTypeFilter(this.parentDimObjItemType)
        })
        .finally(() => {
          this.visible = ref(false);
        });
    //
    this.visible = ref(true);
    api
        .post(baseURL, {
          method: "dict/load",
          params: [{dict: "FD_DimObjItemType"}],
        })
        .then((response) => {
          this.optDOIT = response.data.result.records;
          this.optDOITorg = response.data.result.records;
        })
        .finally(() => {
          this.visible = ref(false);
        });

    if (this.mode === "ins") {
      this.loadTyp(0, this.parent, this.data.dimObjItemType, this.data.linkType);
    } else {
      this.disableLev = !(this.data.dimObjItemType === this.cTyp || this.data.dimObjItemType === this.cCls ||
          this.data.dimObjItemType === this.cMemberTyp || this.data.dimObjItemType === this.cMemberCls);

      if (this.data.linkType === allConsts.FD_LinkType.none) {
        if (this.data.dimObjItemType === this.cCls) {
          this.loadTyp(0, this.parent, this.data.dimObjItemType, this.data.linkType);
          this.loadCls(this.data.typ);
        } else if (this.data.dimObjItemType === this.cTyp) {
          this.loadTyp(0, this.parent, this.data.dimObjItemType, this.data.linkType);
        }
        if (this.data.dimObjItemType === this.cRelTyp) {
          this.loadRelTyp(this.data.dimObjItemType, this.parent, this.data.linkType);
          this.loadRelTypMember(allConsts.FD_MemberType.typ, this.data.relTyp, this.parent, this.data.linkType);
        }
        if (this.data.dimObjItemType === this.cRelCls) {
          this.loadRelTyp(this.data.dimObjItemType, this.parent, this.data.linkType);
          this.loadRelCls(this.data.relTyp, this.data.dimObjItemType, this.parent, this.data.linkType);
        } else if (this.data.dimObjItemType === this.cMemberTyp || this.data.dimObjItemType === this.cMemberRelTyp) {
          this.loadRelTyp(this.data.dimObjItemType, this.parent, this.data.linkType);
          if (this.data.dimObjItemType === this.cMemberTyp)
            this.loadRelTypMember(allConsts.FD_MemberType.typ, this.data.relTyp, this.parent, this.data.linkType);
          else
            this.loadRelTypMember(allConsts.FD_MemberType.reltyp, this.data.relTyp, this.parent, this.data.linkType);
        } else if (this.data.dimObjItemType === this.cMemberCls || this.data.dimObjItemType === this.cMemberRelCls) {
          this.loadRelTyp(this.data.dimObjItemType, this.parent, this.data.linkType);
          this.loadRelCls(this.form.relTyp, this.form.dimObjItemType, this.parent, this.form.linkType);
          if (this.data.dimObjItemType === this.cMemberCls)
            this.loadRelClsMember(allConsts.FD_MemberType.cls, this.data.relCls, this.parent, this.data.linkType);
          else
            this.loadRelClsMember(allConsts.FD_MemberType.relcls, this.data.relCls, this.parent, this.data.linkType);
        }
      } else if (this.data.linkType === allConsts.FD_LinkType.p_c$urt) {  //2
        if (this.data.dimObjItemType === this.cMemberTyp || this.data.dimObjItemType === this.cMemberRelTyp) {
          this.loadRelTyp(this.data.dimObjItemType, this.parent, this.data.linkType);
          if (this.data.dimObjItemType === this.cMemberTyp)
            this.loadRelTypMember(allConsts.FD_MemberType.typ, this.data.relTyp, this.parent, this.data.linkType);
          else
            this.loadRelTypMember(allConsts.FD_MemberType.reltyp, this.data.relTyp, this.parent, this.data.linkType);
        }
        if (this.data.dimObjItemType === this.cMemberCls || this.data.dimObjItemType === this.cMemberRelCls) {
          this.loadRelCls(0, this.data.dimObjItemType, this.parent, this.data.linkType);
          if (this.data.dimObjItemType === this.cMemberCls)
            this.loadRelClsMember(allConsts.FD_MemberType.cls, this.data.relCls, this.parent, this.data.linkType);
          else
            this.loadRelClsMember(allConsts.FD_MemberType.relcls, this.data.relCls, this.parent, this.data.linkType);
        }
      } else if (this.data.linkType === allConsts.FD_LinkType.c$up) {  //3

        if (this.parentDimObjItemType === allConsts.FD_DimObjItemType.reltyp) {
          if (this.data.dimObjItemType === allConsts.FD_DimObjItemType.utyp)
            this.loadRelTypMember(1, 0, this.parent, this.data.linkType);
          if (this.data.dimObjItemType === allConsts.FD_DimObjItemType.ureltyp)
            this.loadRelTypMember(2, 0, this.parent, this.data.linkType);
        }

        if (this.parentDimObjItemType === allConsts.FD_DimObjItemType.relcls) {
          if (this.data.dimObjItemType === allConsts.FD_DimObjItemType.ucls)
            this.loadRelClsMember(allConsts.FD_MemberType.cls, 0, this.parent, this.data.linkType);
          if (this.data.dimObjItemType === allConsts.FD_DimObjItemType.urelcls) {
            this.loadRelClsMember(allConsts.FD_MemberType.relcls, 0, this.parent, this.data.linkType);
          }
        }

      } else if (this.data.linkType === allConsts.FD_LinkType.p$uc) {  //4
        if (this.parentDimObjItemType === this.cTyp || this.parentDimObjItemType === this.cRelTyp ||
            this.parentDimObjItemType === this.cMemberTyp || this.parentDimObjItemType === this.cMemberRelTyp) {
          this.loadRelTyp(this.data.dimObjItemType, this.parent, this.data.linkType)
        }

        if (this.parentDimObjItemType === this.cCls || this.parentDimObjItemType === this.cRelCls ||
            this.parentDimObjItemType === this.cMemberCls || this.parentDimObjItemType === this.cMemberRelCls) {
          this.loadRelCls(0, this.data.dimObjItemType, this.parent, this.data.linkType)
        }
      } else if (this.data.linkType === allConsts.FD_LinkType.c$val_p || this.data.linkType === allConsts.FD_LinkType.r$val_c) {  //5, 9
        this.loadProp(this.data.dimObjItemType, this.parent, this.data.linkType)
        this.loadValueOfProp(this.data.dimObjItemType, this.data.prop, this.parent, this.data.linkType)
      } else if (this.data.linkType === allConsts.FD_LinkType.c$val_up) {  //6
        if (this.parentDimObjItemType === this.cRelTyp || this.parentDimObjItemType === this.cMemberRelTyp) {
          this.loadRelTypMember(0, 0, this.parent, this.data.linkType)

          this.loadValueOfProp(this.data.dimObjItemType, this.data.prop, this.parent, this.data.linkType)

          if (this.data.dimObjItemType === this.cTyp || this.data.dimObjItemType === this.cCls)
            this.loadProp_6(allConsts.FD_MemberType.typ, 0, this.data.dimObjItemType, this.parent)

          if (this.data.dimObjItemType === this.cRelTyp || this.data.dimObjItemType === this.cRelCls)
            this.loadProp_6(allConsts.FD_MemberType.reltyp, 0, this.data.dimObjItemType, this.parent)
        }

        if (this.parentDimObjItemType === this.cRelCls || this.parentDimObjItemType === this.cMemberRelCls) {
          this.loadRelClsMember(0, 0, this.parent, this.data.linkType)
          this.loadProp_6(allConsts.FD_MemberType.reltyp, 0, this.data.dimObjItemType, this.parent)
          this.loadValueOfProp(this.data.dimObjItemType, this.data.prop, this.parent, this.data.linkType)
        }
      } else if (this.data.linkType === allConsts.FD_LinkType.uc$val_p) {  //7
        this.loadProp(this.data.dimObjItemType, this.parent, this.data.linkType)

        if (this.data.dimObjItemType === allConsts.FD_DimObjItemType.reltyp)
          this.loadValueOfProp_7(this.data.dimObjItemType, this.data.prop, this.data.relTypMember, 0, this.data.linkType)
        else if (this.data.dimObjItemType === allConsts.FD_DimObjItemType.relcls)
          this.loadValueOfProp_7(this.data.dimObjItemType, this.data.prop, this.data.relClsMember, 0, this.data.linkType)
      } else if (this.data.linkType === allConsts.FD_LinkType.r$val_uc) {  //10
        this.loadRelTyp(this.data.dimObjItemType, this.parent, this.data.linkType)
        if (this.data.dimObjItemType===this.cRelTyp) {
          this.visCompRelTypMember = true
          this.loadRelTypMember(0, this.data.relTyp, this.parent, this.data.linkType)
          this.loadProp_10_11(this.data.dimObjItemType, this.data.relTypMember, this.parent, this.data.linkType)
        } else if (this.data.dimObjItemType===this.cRelCls) {
          this.visCompRelCls = true
          this.visCompRelClsMember = true
          this.loadRelCls(this.data.relTyp, this.data.dimObjItemType, this.parent, this.data.linkType)
          this.loadRelClsMember(0, this.data.relCls, this.parent, this.data.linkType)
          this.loadProp_10_11(this.data.dimObjItemType, this.data.relClsMember, this.parent, this.data.linkType)
        }

      } else if (this.data.linkType===allConsts.FD_LinkType.up$val_c) {   //11
        this.visCompRelTypMember = false
        this.visCompRelClsMember = false
        this.visCompTyp = false
        this.visCompCls = false
        this.visCompRelTyp = false
        this.visCompRelCls = false

        if (this.parentDimObjItemType===this.cRelTyp || this.parentDimObjItemType===this.cMemberRelTyp) {
          this.visCompRelTypMember = true
          this.loadRelTypMember(0, 0, this.parent, this.data.linkType)
          this.loadProp_10_11(this.data.dimObjItemType, this.data.relTypMember, this.parent, this.data.linkType)
        }
        if (this.parentDimObjItemType===this.cRelCls || this.parentDimObjItemType===this.cMemberRelCls) {
          this.visCompRelClsMember = true
          this.loadRelClsMember(0, 0, this.parent, this.data.linkType)
          this.loadProp_10_11(this.data.dimObjItemType, this.data.relClsMember, this.parent, this.data.linkType)
        }
        this.loadCompFor_11(this.data.dimObjItemType, this.data.prop)

        if (this.data.dimObjItemType === this.cTyp)
          this.visCompTyp = true
        else if (this.data.dimObjItemType === this.cCls)
          this.visCompCls = true
        else if (this.data.dimObjItemType === this.cRelTyp)
          this.visCompRelTyp = true
        else if (this.data.dimObjItemType === this.cRelCls)
          this.visCompRelCls = true

      }
    }
  },
};
</script>
