<template>
  <q-dialog
    ref="dialogRef"
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

      <q-inner-loading :showing="visible" color="secondary" />

      <q-card-section>
        <div class="row">
          {{ $t("parent") }}: <b> {{ parentName }} </b>
        </div>
        <hr />

        <!-- linkType -->
        <q-select
          v-if="form.parent != null"
          dense
          options-dense
          :disable="mode === 'upd'"
          v-model="lt"
          :options="optLT"
          :label="$t('linkType')"
          option-value="id"
          option-label="text"
          map-options
          @update:model-value="fnSelectLT"
          class="q-mb-md"
        />

        <!--  dimObjItemType -->
        <q-select
          :class="form.parent == null ? 'q-mt-md' : 'q-mt-none'"
          dense
          options-dense
          v-model="doit"
          :disable="mode === 'upd'"
          :options="optDOIT"
          :label="$t('fldCmpType')"
          option-value="id"
          option-label="text"
          map-options
          @update:model-value="fnSelectDOIT"
          class="q-mb-md"
        />

        <div v-if="form.linkType === 1">
          <!--  typ -->
          <q-select
            v-if="form.dimObjItemType === cTyp || form.dimObjItemType === cCls"
            dense
            options-dense
            v-model="typ"
            :options="optTyp"
            :label="$t('typ')"
            option-value="id"
            option-label="name"
            map-options
            @update:model-value="fnSelectTyp"
            class="q-mb-md"
            :disable="mode === 'upd' && hasChild"
          />

          <!--  cls -->
          <q-select
            v-if="form.dimObjItemType === cCls"
            dense
            options-dense
            v-model="cls"
            :options="optCls"
            :label="$t('cls')"
            option-value="id"
            option-label="name"
            map-options
            @update:model-value="fnSelectCls"
            class="q-mb-md"
            :disable="mode === 'upd' && hasChild"
          />

          <!--  relTyp -->
          <q-select
            v-if="form.dimObjItemType === cRelTyp || form.dimObjItemType === cRelCls"
            dense
            options-dense
            v-model="relTyp"
            :options="optRelTyp"
            :label="$t('reltyp')"
            option-value="id"
            option-label="name"
            map-options
            @update:model-value="fnSelectRelTyp"
            class="q-mb-md"
            :disable="mode === 'upd' && hasChild"
          />

          <!--  relCls -->
          <q-select
            v-if="form.dimObjItemType === cRelCls"
            dense
            options-dense
            v-model="relCls"
            :options="optRelCls"
            :label="$t('relcls')"
            option-value="id"
            option-label="name"
            map-options
            @update:model-value="fnSelectRelCls"
            class="q-mb-md"
            :disable="mode === 'upd' && hasChild"
          />

          <!--  relTypMember -->
          <div v-if="form.dimObjItemType === cMemberTyp || form.dimObjItemType === cMemberRelTyp">
            <q-select
              dense
              options-dense
              v-model="relTyp"
              :options="optRelTyp"
              :label="$t('reltyp')"
              option-value="id"
              option-label="name"
              map-options
              @update:model-value="fnSelectRelTyp"
              class="q-mb-md"
              :disable="mode === 'upd' && hasChild"
            />

            <q-select
              dense
              options-dense
              v-model="relTypMember"
              :options="optRelTypMember"
              :label="$t('memberRel')"
              option-value="id"
              option-label="name"
              map-options
              @update:model-value="fnSelectRelTypMember"
              class="q-mb-md"
              :disable="mode === 'upd' && hasChild"
            />
          </div>

          <!--  cMemberCls or relClsMember  -->
          <div v-if="form.dimObjItemType === cMemberCls || form.dimObjItemType === cMemberRelCls">
            <q-select
              dense
              options-dense
              v-model="relTyp"
              :options="optRelTyp"
              :label="$t('reltyp')"
              option-value="id"
              option-label="name"
              map-options
              @update:model-value="fnSelectRelTyp"
              class="q-mb-md"
              :disable="mode === 'upd' && hasChild"
            />

            <q-select
              dense
              options-dense
              v-model="relCls"
              :options="optRelCls"
              :label="$t('relcls')"
              option-value="id"
              option-label="name"
              map-options
              @update:model-value="fnSelectRelCls"
              class="q-mb-md"
              :disable="mode === 'upd' && hasChild"
            />

            <q-select
              dense
              options-dense
              v-model="relClsMember"
              :options="optRelClsMember"
              :label="$t('memberRelCls')"
              option-value="id"
              option-label="name"
              map-options
              @update:model-value="fnSelectRelClsMember"
              class="q-mb-md"
              :disable="mode === 'upd' && hasChild"
            />
          </div>
        </div>

        <div v-if="form.linkType === 2">
          <div v-if="form.dimObjItemType === cMemberTyp || form.dimObjItemType === cMemberRelTyp">
            <q-select
              dense
              options-dense
              v-model="relTyp"
              :options="optRelTyp"
              :label="$t('reltyp')"
              option-value="id"
              option-label="name"
              map-options
              @update:model-value="fnSelectRelTyp"
              :disable="mode === 'upd' && hasChild"
            />

            <q-select
              class="q-mt-md"
              dense
              options-dense
              v-model="relTypMember"
              :options="optRelTypMember"
              :label="$t('memberRel')"
              :disable="mode === 'upd' && hasChild"
              option-value="id"
              option-label="name"
              map-options
              @update:model-value="fnSelectRelTypMember"
            />
          </div>

          <div v-if="form.dimObjItemType === cMemberCls || form.dimObjItemType === cMemberRelCls">
            <q-select
              dense
              options-dense
              v-model="relCls"
              :options="optRelCls"
              :label="$t('relcls')"
              option-value="id"
              option-label="name"
              map-options
              @update:model-value="fnSelectRelCls"
              :disable="mode === 'upd' && hasChild"
            />

            <q-select
              dense
              options-dense
              v-model="relClsMember"
              :options="optRelClsMember"
              :label="$t('memberRelCls')"
              option-value="id"
              option-label="name"
              map-options
              @update:model-value="fnSelectRelClsMember"
              class="q-mt-md"
              :disable="mode === 'upd' && hasChild"
            />
          </div>
        </div>

        <div v-if="form.linkType === 3">
          <div v-if="parentDimObjItemType === cRelTyp || parentDimObjItemType === cMemberRelTyp">
            <q-select
              class="q-mt-md"
              dense
              options-dense
              v-model="relTypMember"
              :options="optRelTypMember"
              :label="$t('memberRel')"
              :disable="mode === 'upd' && hasChild"
              option-value="id"
              option-label="name"
              map-options
              @update:model-value="fnSelectRelTypMember"
            />
          </div>

          <div v-if="parentDimObjItemType === cRelCls || parentDimObjItemType === cMemberRelCls">
            <q-select
              class="q-mt-md"
              dense
              options-dense
              v-model="relClsMember"
              :options="optRelClsMember"
              :label="$t('memberRelCls')"
              :disable="mode === 'upd' && hasChild"
              option-value="id"
              option-label="name"
              map-options
              @update:model-value="fnSelectRelClsMember"
            />
          </div>
        </div>

        <div v-if="form.linkType === 4">
          <div
            v-if="
              parentDimObjItemType === cTyp ||
              parentDimObjItemType === cRelTyp ||
              parentDimObjItemType === cMemberTyp ||
              parentDimObjItemType === cMemberRelTyp
            "
          >
            <q-select
              class="q-mt-md"
              :disable="mode === 'upd' && hasChild"
              dense
              options-dense
              v-model="relTyp"
              :options="optRelTyp"
              :label="$t('reltyp')"
              option-value="id"
              option-label="name"
              map-options
              @update:model-value="fnSelectRelTyp"
            />
          </div>

          <div
            v-if="
              parentDimObjItemType === cCls ||
              parentDimObjItemType === cRelCls ||
              parentDimObjItemType === cMemberCls ||
              parentDimObjItemType === cMemberRelCls
            "
          >
            <q-select
              class="q-mt-md"
              :disable="mode === 'upd' && hasChild"
              dense
              options-dense
              v-model="relCls"
              :options="optRelCls"
              :label="$t('relcls')"
              option-value="id"
              option-label="name"
              map-options
              @update:model-value="fnSelectRelCls"
            />
          </div>
        </div>

        <div v-if="form.linkType === 5">
          <!--  prop -->
          <q-select
            dense
            options-dense
            map-options
            class="q-mb-md"
            v-model="prop"
            :options="optProp"
            :label="$t('prop')"
            option-value="id"
            option-label="name"
            @update:model-value="fnSelectProp"
            :disable="mode === 'upd' && hasChild"
          />
          <div v-if="visCompTyp">
            <q-select
              dense
              options-dense
              v-model="typ"
              :options="optTyp"
              :label="$t('typ')"
              option-value="id"
              option-label="name"
              map-options
              @update:model-value="fnSelectTyp"
              class="q-mb-md"
              :disable="mode === 'upd' && hasChild"
            />
          </div>

          <div v-if="visCompCls">
            <q-select
              dense
              options-dense
              v-model="cls"
              :options="optCls"
              :label="$t('cls')"
              option-value="id"
              option-label="name"
              map-options
              @update:model-value="fnSelectCls"
              class="q-mb-md"
              :disable="mode === 'upd' && hasChild"
            />
          </div>

          <div v-if="visCompRelTyp">
            <q-select
              dense
              options-dense
              v-model="relTyp"
              :options="optRelTyp"
              :label="$t('reltyp')"
              option-value="id"
              option-label="name"
              map-options
              @update:model-value="fnSelectRelTyp"
              class="q-mb-md"
              :disable="mode === 'upd' && hasChild"
            />
          </div>

          <div v-if="visCompRelCls">
            <q-select
              dense
              options-dense
              v-model="relCls"
              :options="optRelCls"
              :label="$t('relcls')"
              option-value="id"
              option-label="name"
              map-options
              @update:model-value="fnSelectRelCls"
              class="q-mb-md"
              :disable="mode === 'upd' && hasChild"
            />
          </div>
        </div>

        <div v-if="form.linkType === 6">
          <div v-if="parentDimObjItemType === cRelTyp || parentDimObjItemType === cMemberRelTyp">
            <!-- Промежуточный компонент-->
            <q-select
              class="q-mt-md"
              :disable="mode === 'upd' && hasChild"
              dense
              options-dense
              v-model="relTypMember"
              :options="optRelTypMember"
              :label="$t('memberRel')"
              option-value="id"
              option-label="name"
              map-options
              @update:model-value="fnSelectRelTypMember"
            />
            <!--  prop -->
            <q-select
              dense
              options-dense
              map-options
              class="q-mt-md q-mb-md"
              v-model="prop"
              :options="optProp"
              :label="$t('prop')"
              option-value="id"
              option-label="name"
              @update:model-value="fnSelectProp"
              :disable="mode === 'upd' && hasChild"
            />

            <div v-if="visCompTyp">
              <q-select
                dense
                options-dense
                v-model="typ"
                :options="optTyp"
                :label="$t('typ')"
                option-value="id"
                option-label="name"
                map-options
                @update:model-value="fnSelectTyp"
                class="q-mb-md"
                :disable="mode === 'upd' && hasChild"
              />
            </div>

            <div v-if="visCompCls">
              <q-select
                dense
                options-dense
                v-model="cls"
                :options="optCls"
                :label="$t('cls')"
                option-value="id"
                option-label="name"
                map-options
                @update:model-value="fnSelectCls"
                class="q-mb-md"
                :disable="mode === 'upd' && hasChild"
              />
            </div>

            <div v-if="visCompRelTyp">
              <q-select
                dense
                options-dense
                v-model="relTyp"
                :options="optRelTyp"
                :label="$t('reltyp')"
                option-value="id"
                option-label="name"
                map-options
                @update:model-value="fnSelectRelTyp"
                class="q-mb-md"
                :disable="mode === 'upd' && hasChild"
              />
            </div>

            <div v-if="visCompRelCls">
              <q-select
                dense
                options-dense
                v-model="relCls"
                :options="optRelCls"
                :label="$t('relcls')"
                option-value="id"
                option-label="name"
                map-options
                @update:model-value="fnSelectRelCls"
                class="q-mb-md"
                :disable="mode === 'upd' && hasChild"
              />
            </div>
          </div>

          <div v-if="parentDimObjItemType === cRelCls || parentDimObjItemType === cMemberRelCls">
            <!-- Промежуточный компонент-->
            <q-select
              class="q-mt-md"
              :disable="mode === 'upd' && hasChild"
              dense
              options-dense
              v-model="relClsMember"
              :options="optRelClsMember"
              :label="$t('memberRelCls')"
              option-value="id"
              option-label="name"
              map-options
              @update:model-value="fnSelectRelClsMember"
            />
            <!--  prop -->
            <q-select
              dense
              options-dense
              map-options
              class="q-mt-md q-mb-md"
              v-model="prop"
              :options="optProp"
              :label="$t('prop')"
              option-value="id"
              option-label="name"
              @update:model-value="fnSelectProp"
              :disable="mode === 'upd' && hasChild"
            />

            <div v-if="visCompTyp">
              <q-select
                dense
                options-dense
                v-model="typ"
                :options="optTyp"
                :label="$t('typ')"
                option-value="id"
                option-label="name"
                map-options
                @update:model-value="fnSelectTyp"
                class="q-mb-md"
                :disable="mode === 'upd' && hasChild"
              />
            </div>

            <div v-if="visCompCls">
              <q-select
                dense
                options-dense
                v-model="cls"
                :options="optCls"
                :label="$t('cls')"
                option-value="id"
                option-label="name"
                map-options
                @update:model-value="fnSelectCls"
                class="q-mb-md"
                :disable="mode === 'upd' && hasChild"
              />
            </div>

            <div v-if="visCompRelTyp">
              <q-select
                dense
                options-dense
                v-model="relTyp"
                :options="optRelTyp"
                :label="$t('reltyp')"
                option-value="id"
                option-label="name"
                map-options
                @update:model-value="fnSelectRelTyp"
                class="q-mb-md"
                :disable="mode === 'upd' && hasChild"
              />
            </div>

            <div v-if="visCompRelCls">
              <q-select
                dense
                options-dense
                v-model="relCls"
                :options="optRelCls"
                :label="$t('relcls')"
                option-value="id"
                option-label="name"
                map-options
                @update:model-value="fnSelectRelCls"
                class="q-mb-md"
                :disable="mode === 'upd' && hasChild"
              />
            </div>
          </div>
        </div>

        <div v-if="form.linkType === 7">
          <!--  prop -->
          <q-select
            dense
            options-dense
            map-options
            class="q-mb-md"
            v-model="prop"
            :options="optProp"
            :label="$t('prop')"
            option-value="id"
            option-label="name"
            @update:model-value="fnSelectProp"
            :disable="mode === 'upd' && hasChild"
          />

          <div v-if="visCompRelTyp">
            <q-select
              dense
              options-dense
              v-model="relTyp"
              :options="optRelTyp"
              :label="$t('reltyp')"
              option-value="id"
              option-label="name"
              map-options
              @update:model-value="fnSelectRelTyp"
              class="q-mb-md"
              :disable="mode === 'upd' && hasChild"
            />
          </div>

          <div v-if="visCompRelCls">
            <q-select
              dense
              options-dense
              v-model="relCls"
              :options="optRelCls"
              :label="$t('relcls')"
              option-value="id"
              option-label="name"
              map-options
              @update:model-value="fnSelectRelCls"
              class="q-mb-md"
              :disable="mode === 'upd' && hasChild"
            />
          </div>
        </div>

        <div v-if="form.linkType === 9">
          <!--  prop -->
          <q-select
            dense
            options-dense
            map-options
            class="q-mb-md"
            v-model="prop"
            :options="optProp"
            :label="$t('prop')"
            option-value="id"
            option-label="name"
            @update:model-value="fnSelectProp"
            :disable="mode === 'upd' && hasChild"
          />
          <div v-if="visCompTyp">
            <q-select
              dense
              options-dense
              v-model="typ"
              :options="optTyp"
              :label="$t('typ')"
              option-value="id"
              option-label="name"
              map-options
              @update:model-value="fnSelectTyp"
              class="q-mb-md"
              :disable="mode === 'upd' && hasChild"
            />
          </div>

          <div v-if="visCompCls">
            <q-select
              dense
              options-dense
              v-model="cls"
              :options="optCls"
              :label="$t('cls')"
              option-value="id"
              option-label="name"
              map-options
              @update:model-value="fnSelectCls"
              class="q-mb-md"
              :disable="mode === 'upd' && hasChild"
            />
          </div>

          <div v-if="visCompRelTyp">
            <q-select
              dense
              options-dense
              v-model="relTyp"
              :options="optRelTyp"
              :label="$t('reltyp')"
              option-value="id"
              option-label="name"
              map-options
              @update:model-value="fnSelectRelTyp"
              class="q-mb-md"
              :disable="mode === 'upd' && hasChild"
            />
          </div>

          <div v-if="visCompRelCls">
            <q-select
              dense
              options-dense
              v-model="relCls"
              :options="optRelCls"
              :label="$t('relcls')"
              option-value="id"
              option-label="name"
              map-options
              @update:model-value="fnSelectRelCls"
              class="q-mb-md"
              :disable="mode === 'upd' && hasChild"
            />
          </div>
        </div>

        <div v-if="form.linkType === 10">
          <q-select
            dense
            options-dense
            v-model="relTyp"
            :options="optRelTyp"
            :label="$t('reltyp')"
            option-value="id"
            option-label="name"
            map-options
            @update:model-value="fnSelectRelTyp"
            class="q-mb-md"
            :disable="mode === 'upd' && hasChild"
          />

          <div v-if="visCompRelTypMember">
            <q-select
              dense
              options-dense
              v-model="relTypMember"
              :options="optRelTypMember"
              :label="$t('memberRel')"
              option-value="id"
              option-label="name"
              map-options
              @update:model-value="fnSelectRelTypMember"
              class="q-mb-md"
              :disable="mode === 'upd' && hasChild"
            />
          </div>

          <div v-if="visCompRelCls">
            <q-select
              dense
              options-dense
              v-model="relCls"
              :options="optRelCls"
              :label="$t('relcls')"
              option-value="id"
              option-label="name"
              map-options
              @update:model-value="fnSelectRelCls"
              class="q-mb-md"
              :disable="mode === 'upd' && hasChild"
            />
          </div>

          <div v-if="visCompRelClsMember">
            <q-select
              dense
              options-dense
              v-model="relClsMember"
              :options="optRelClsMember"
              :label="$t('memberRelCls')"
              option-value="id"
              option-label="name"
              map-options
              @update:model-value="fnSelectRelClsMember"
              class="q-mb-md"
              :disable="mode === 'upd' && hasChild"
            />
          </div>

          <!--  prop -->
          <q-select
            dense
            options-dense
            map-options
            class="q-mb-md"
            v-model="prop"
            :options="optProp"
            :label="$t('prop')"
            option-value="id"
            option-label="name"
            @update:model-value="fnSelectProp"
          />
        </div>

        <div v-if="form.linkType === 11">
          <div v-if="visCompRelTypMember">
            <q-select
              dense
              options-dense
              v-model="relTypMember"
              :options="optRelTypMember"
              :label="$t('memberRel')"
              option-value="id"
              option-label="name"
              map-options
              @update:model-value="fnSelectRelTypMember"
              class="q-mb-md"
              :disable="mode === 'upd' && hasChild"
            />
          </div>

          <div v-if="visCompRelClsMember">
            <q-select
              dense
              options-dense
              v-model="relClsMember"
              :options="optRelClsMember"
              :label="$t('memberRelCls')"
              option-value="id"
              option-label="name"
              map-options
              @update:model-value="fnSelectRelClsMember"
              class="q-mb-md"
              :disable="mode === 'upd' && hasChild"
            />
          </div>
          <!--  prop -->
          <q-select
            dense
            options-dense
            map-options
            class="q-mb-md"
            v-model="prop"
            :options="optProp"
            :label="$t('prop')"
            option-value="id"
            option-label="name"
            @update:model-value="fnSelectProp"
          />
          <div v-if="visCompTyp">
            <q-select
              dense
              options-dense
              v-model="typ"
              :options="optTyp"
              :label="$t('typ')"
              option-value="id"
              option-label="name"
              map-options
              @update:model-value="fnSelectTyp"
              class="q-mb-md"
              :disable="mode === 'upd' && hasChild"
            />
          </div>

          <div v-if="visCompCls">
            <q-select
              dense
              options-dense
              v-model="cls"
              :options="optCls"
              :label="$t('cls')"
              option-value="id"
              option-label="name"
              map-options
              @update:model-value="fnSelectCls"
              class="q-mb-md"
              :disable="mode === 'upd' && hasChild"
            />
          </div>

          <div v-if="visCompRelTyp">
            <q-select
              dense
              options-dense
              v-model="relTyp"
              :options="optRelTyp"
              :label="$t('reltyp')"
              option-value="id"
              option-label="name"
              map-options
              @update:model-value="fnSelectRelTyp"
              class="q-mb-md"
              :disable="mode === 'upd' && hasChild"
            />
          </div>

          <div v-if="visCompRelCls">
            <q-select
              dense
              options-dense
              v-model="relCls"
              :options="optRelCls"
              :label="$t('relcls')"
              option-value="id"
              option-label="name"
              map-options
              @update:model-value="fnSelectRelCls"
              class="q-mb-md"
              :disable="mode === 'upd' && hasChild"
            />
          </div>
        </div>

        <!--  lev  -->
        <q-input
          class="q-mt-md"
          :disable="disableLev"
          dense
          type="number"
          stack-label
          clearable
          v-model="form.lev"
          :rules="[
            (val) =>
              val == null || false || (val && val > 0) || 'Уровень иерархии должна быть положительной',
          ]"
          :label="$t('level')"
          @update:model-value="fnUpdLev"
        />
      </q-card-section>

      <q-card-actions align="right">
        <q-btn
          dense
          color="primary"
          icon="save"
          :label="$t('save')"
          @click="onOKClick"
          :disable="validSave()"
        />
        <q-btn
          dense
          color="primary"
          icon="cancel"
          :label="$t('cancel')"
          @click="onDialogCancel"
        />
      </q-card-actions>
    </q-card>
  </q-dialog>
</template>

<script setup>
import { ref, reactive, onMounted, getCurrentInstance } from "vue";
import { useDialogPluginComponent } from "quasar";
import { api } from "@/boot/axios";
import { notifyError, notifySuccess } from "@/utils/jsutils";
import allConsts from "@/pages/all-consts";

const props = defineProps({
  data: {
    type: Object,
    default: () => ({}),
  },
  parentName: String,
  parent: [Number, String],
  parentDimObjItemType: Number,
  mode: String,
  hasChild: Boolean,
  lg: String,
});

defineEmits([...useDialogPluginComponent.emits]);

const { proxy } = getCurrentInstance();
const { dialogRef, onDialogHide, onDialogOK, onDialogCancel } = useDialogPluginComponent();

const form = reactive({ ...props.data });
const lang = ref(props.lg);
const lt = ref(props.data.linkType);
const optLT = ref([]);
const doit = ref(props.data.dimObjItemType);
const optDOIT = ref([]);
const optDOITorg = ref([]);

const typ = ref(props.data.typ);
const optTyp = ref([]);

const cls = ref(props.data.cls);
const optCls = ref([]);

const relTyp = ref(props.data.relTyp);
const optRelTyp = ref([]);

const relCls = ref(props.data.relCls);
const optRelCls = ref([]);

const relTypMember = ref(props.data.relTypMember);
const optRelTypMember = ref([]);

const relClsMember = ref(props.data.relClsMember);
const optRelClsMember = ref([]);

const prop = ref(props.data.prop);
const optProp = ref([]);

const visible = ref(false);

const cTyp = allConsts.FD_DimObjItemType.typ;
const cCls = allConsts.FD_DimObjItemType.cls;
const cRelTyp = allConsts.FD_DimObjItemType.reltyp;
const cRelCls = allConsts.FD_DimObjItemType.relcls;
const cMemberTyp = allConsts.FD_DimObjItemType.utyp;
const cMemberCls = allConsts.FD_DimObjItemType.ucls;
const cMemberRelTyp = allConsts.FD_DimObjItemType.ureltyp;
const cMemberRelCls = allConsts.FD_DimObjItemType.urelcls;

const visCompTyp = ref(false);
const visCompCls = ref(false);
const visCompRelTyp = ref(false);
const visCompRelCls = ref(false);

const visCompRelTypMember = ref(false);
const visCompRelClsMember = ref(false);

const disableLev = ref(false);

const clearComps = () => {
  optTyp.value = [];
  form.typ = null;
  typ.value = null;

  optCls.value = [];
  form.cls = null;
  cls.value = null;

  optRelTyp.value = [];
  form.relTyp = null;
  relTyp.value = null;

  optRelCls.value = [];
  form.relCls = null;
  relCls.value = null;
};

const fnSelectLT = () => {
  form.linkType = lt.value?.id || lt.value;
  optDOIT.value = [];
  form.dimObjItemType = null;
  doit.value = null;
  clearComps();
  dimObjItemTypeFilter(props.parentDimObjItemType, form.linkType);
};

const dimObjItemTypeFilter = (parentDOIT, linkType) => {
  if (linkType === allConsts.FD_LinkType.p_c$urt) {
    if (
      parentDOIT === allConsts.FD_DimObjItemType.typ ||
      parentDOIT === allConsts.FD_DimObjItemType.reltyp ||
      parentDOIT === allConsts.FD_DimObjItemType.utyp ||
      parentDOIT === allConsts.FD_DimObjItemType.ureltyp
    ) {
      optDOIT.value = optDOITorg.value.filter((it) => {
        return it.id === allConsts.FD_DimObjItemType.utyp || it.id === allConsts.FD_DimObjItemType.ureltyp;
      });
    }

    if (
      parentDOIT === allConsts.FD_DimObjItemType.cls ||
      parentDOIT === allConsts.FD_DimObjItemType.relcls ||
      parentDOIT === allConsts.FD_DimObjItemType.ucls ||
      parentDOIT === allConsts.FD_DimObjItemType.urelcls
    ) {
      optDOIT.value = optDOITorg.value.filter((it) => {
        return it.id === allConsts.FD_DimObjItemType.ucls || it.id === allConsts.FD_DimObjItemType.urelcls;
      });
    }
  }

  if (linkType === allConsts.FD_LinkType.c$up) {
    if (parentDOIT === allConsts.FD_DimObjItemType.reltyp || parentDOIT === allConsts.FD_DimObjItemType.ureltyp) {
      optDOIT.value = optDOITorg.value.filter((it) => {
        return it.id === allConsts.FD_DimObjItemType.utyp || it.id === allConsts.FD_DimObjItemType.ureltyp;
      });
    } else if (
      parentDOIT === allConsts.FD_DimObjItemType.relcls ||
      parentDOIT === allConsts.FD_DimObjItemType.urelcls
    ) {
      optDOIT.value = optDOITorg.value.filter((it) => {
        return it.id === allConsts.FD_DimObjItemType.ucls || it.id === allConsts.FD_DimObjItemType.urelcls;
      });
    }
  }

  if (linkType === allConsts.FD_LinkType.p$uc) {
    if (
      parentDOIT === allConsts.FD_DimObjItemType.typ ||
      parentDOIT === allConsts.FD_DimObjItemType.reltyp ||
      parentDOIT === allConsts.FD_DimObjItemType.ureltyp ||
      parentDOIT === allConsts.FD_DimObjItemType.utyp
    ) {
      optDOIT.value = optDOITorg.value.filter((it) => {
        return it.id === allConsts.FD_DimObjItemType.reltyp;
      });
    }

    if (
      parentDOIT === allConsts.FD_DimObjItemType.cls ||
      parentDOIT === allConsts.FD_DimObjItemType.relcls ||
      parentDOIT === allConsts.FD_DimObjItemType.urelcls ||
      parentDOIT === allConsts.FD_DimObjItemType.ucls
    ) {
      optDOIT.value = optDOITorg.value.filter((it) => {
        return it.id === allConsts.FD_DimObjItemType.relcls;
      });
    }
  }

  if (
    linkType === allConsts.FD_LinkType.c$val_p ||
    linkType === allConsts.FD_LinkType.c$val_up ||
    linkType === allConsts.FD_LinkType.r$val_c ||
    linkType === allConsts.FD_LinkType.up$val_c
  ) {
    optDOIT.value = optDOITorg.value.filter((it) => {
      return (
        it.id === allConsts.FD_DimObjItemType.typ ||
        it.id === allConsts.FD_DimObjItemType.cls ||
        it.id === allConsts.FD_DimObjItemType.reltyp ||
        it.id === allConsts.FD_DimObjItemType.relcls
      );
    });
  }

  if (
    linkType === allConsts.FD_LinkType.uc$val_p ||
    linkType === allConsts.FD_LinkType.uc$val_up ||
    linkType === allConsts.FD_LinkType.r$val_uc ||
    linkType === allConsts.FD_LinkType.up$val_uc
  ) {
    optDOIT.value = optDOITorg.value.filter((it) => {
      return it.id === allConsts.FD_DimObjItemType.reltyp || it.id === allConsts.FD_DimObjItemType.relcls;
    });
  }
};

const fnSelectDOIT = () => {
  form.dimObjItemType = doit.value?.id || doit.value;
  clearComps();

  const doitId = form.dimObjItemType;

  if (form.linkType === allConsts.FD_LinkType.none) {
    switch (doitId) {
      case allConsts.FD_DimObjItemType.typ:
      case allConsts.FD_DimObjItemType.cls:
        typ.value = null;
        loadTyp(0, props.parent, doitId, form.linkType);
        break;
      case allConsts.FD_DimObjItemType.reltyp:
      case allConsts.FD_DimObjItemType.relcls:
      case allConsts.FD_DimObjItemType.utyp:
      case allConsts.FD_DimObjItemType.ureltyp:
      case allConsts.FD_DimObjItemType.ucls:
      case allConsts.FD_DimObjItemType.urelcls:
        relTyp.value = null;
        loadRelTyp(doitId, props.parent, form.linkType);
        break;
    }
  }

  if (form.linkType === allConsts.FD_LinkType.p_c$urt) {
    switch (doitId) {
      case allConsts.FD_DimObjItemType.utyp:
      case allConsts.FD_DimObjItemType.ureltyp:
        relTyp.value = null;
        loadRelTyp(doitId, props.parent, form.linkType);
        break;
      case allConsts.FD_DimObjItemType.ucls:
      case allConsts.FD_DimObjItemType.urelcls:
        relCls.value = null;
        loadRelCls(0, doitId, props.parent, form.linkType);
        break;
    }
  }

  if (form.linkType === allConsts.FD_LinkType.c$up) {
    if (props.parentDimObjItemType === allConsts.FD_DimObjItemType.reltyp) {
      if (doitId === allConsts.FD_DimObjItemType.utyp) {
        loadRelTypMember(1, 0, props.parent, form.linkType);
      } else if (doitId === allConsts.FD_DimObjItemType.ureltyp) {
        loadRelTypMember(2, 0, props.parent, form.linkType);
      }
    }

    if (props.parentDimObjItemType === allConsts.FD_DimObjItemType.relcls) {
      if (doitId === allConsts.FD_DimObjItemType.ucls) {
        loadRelClsMember(allConsts.FD_MemberType.cls, 0, props.parent, form.linkType);
      } else {
        loadRelClsMember(allConsts.FD_MemberType.relcls, 0, props.parent, form.linkType);
      }
    }
  }

  if (form.linkType === allConsts.FD_LinkType.p$uc) {
    if (
      props.parentDimObjItemType === cTyp ||
      props.parentDimObjItemType === cRelTyp ||
      props.parentDimObjItemType === cMemberTyp ||
      props.parentDimObjItemType === cMemberRelTyp
    ) {
      loadRelTyp(doitId, props.parent, form.linkType);
    }

    if (
      props.parentDimObjItemType === cCls ||
      props.parentDimObjItemType === cRelCls ||
      props.parentDimObjItemType === cMemberCls ||
      props.parentDimObjItemType === cMemberRelCls
    ) {
      loadRelCls(0, doitId, props.parent, form.linkType);
    }
  }

  if (
    form.linkType === allConsts.FD_LinkType.c$val_p ||
    form.linkType === allConsts.FD_LinkType.r$val_c
  ) {
    prop.value = null;
    form.prop = null;

    visCompTyp.value = false;
    visCompCls.value = false;
    visCompRelTyp.value = false;
    visCompRelCls.value = false;

    if (doitId === cTyp || doitId === cMemberTyp) visCompTyp.value = true;
    else if (doitId === cCls || doitId === cMemberCls) visCompCls.value = true;
    else if (doitId === cRelTyp || doitId === cMemberRelTyp) visCompRelTyp.value = true;
    else if (doitId === cRelCls || doitId === cMemberRelCls) visCompRelCls.value = true;

    loadProp(doitId, props.parent, form.linkType);
  }

  if (form.linkType === allConsts.FD_LinkType.c$val_up) {
    prop.value = null;
    form.prop = null;

    visCompTyp.value = false;
    visCompCls.value = false;
    visCompRelTyp.value = false;
    visCompRelCls.value = false;

    if (doitId === cTyp || doitId === cMemberTyp) visCompTyp.value = true;
    else if (doitId === cCls || doitId === cMemberCls) visCompCls.value = true;
    else if (doitId === cRelTyp || doitId === cMemberRelTyp) visCompRelTyp.value = true;
    else if (doitId === cRelCls || doitId === cMemberRelCls) visCompRelCls.value = true;

    if (props.parentDimObjItemType === cRelTyp || props.parentDimObjItemType === cMemberRelTyp) {
      loadRelTypMember(0, 0, props.parent, form.linkType);
    }
    if (props.parentDimObjItemType === cRelCls || props.parentDimObjItemType === cMemberRelCls) {
      loadRelClsMember(0, 0, props.parent, form.linkType);
    }
  }

  if (form.linkType === allConsts.FD_LinkType.uc$val_p) {
    prop.value = null;
    form.prop = null;
    visCompRelTyp.value = false;
    visCompRelCls.value = false;

    if (doitId === cRelTyp || doitId === cMemberRelTyp) visCompRelTyp.value = true;
    else if (doitId === cRelCls || doitId === cMemberRelCls) visCompRelCls.value = true;

    loadProp(doitId, props.parent, form.linkType);
  }

  if (form.linkType === allConsts.FD_LinkType.r$val_uc) {
    prop.value = null;
    form.prop = null;

    visCompRelTypMember.value = false;
    visCompRelCls.value = false;
    visCompRelClsMember.value = false;

    if (doitId === cRelTyp) visCompRelTypMember.value = true;
    else if (doitId === cRelCls) {
      visCompRelCls.value = true;
      visCompRelClsMember.value = true;
    }

    loadRelTyp(doitId, props.parent, form.linkType);
  }

  if (form.linkType === allConsts.FD_LinkType.up$val_c) {
    prop.value = null;
    form.prop = null;

    relTypMember.value = null;
    form.relTypMember = null;
    relClsMember.value = null;
    form.relClsMember = null;
    clearComps();

    visCompTyp.value = false;
    visCompCls.value = false;
    visCompRelTyp.value = false;
    visCompRelCls.value = false;
    visCompRelTypMember.value = false;
    visCompRelClsMember.value = false;

    if (props.parentDimObjItemType === cRelTyp || props.parentDimObjItemType === cMemberRelTyp) {
      visCompRelTypMember.value = true;
      loadRelTypMember(0, 0, props.parent, form.linkType);
    }
    if (props.parentDimObjItemType === cRelCls || props.parentDimObjItemType === cMemberRelCls) {
      visCompRelClsMember.value = true;
      loadRelClsMember(0, 0, props.parent, form.linkType);
    }

    if (doitId === cTyp) visCompTyp.value = true;
    else if (doitId === cCls) visCompCls.value = true;
    else if (doitId === cRelTyp) visCompRelTyp.value = true;
    else if (doitId === cRelCls) visCompRelCls.value = true;
  }

  disableLev.value = !(
    doitId === cTyp ||
    doitId === cCls ||
    doitId === cMemberTyp ||
    doitId === cMemberCls
  );
};

const fnSelectProp = () => {
  form.prop = prop.value?.id || prop.value;

  if (form.linkType === allConsts.FD_LinkType.up$val_c) {
    loadCompFor_11(form.dimObjItemType, form.prop);
  } else {
    if (form.linkType !== allConsts.FD_LinkType.r$val_uc) {
      clearComps();
      if (
        form.linkType === allConsts.FD_LinkType.c$val_p ||
        form.linkType === allConsts.FD_LinkType.c$val_up ||
        form.linkType === allConsts.FD_LinkType.r$val_c
      ) {
        loadValueOfProp(form.dimObjItemType, form.prop, props.parent, form.linkType);
      }

      if (form.linkType === allConsts.FD_LinkType.uc$val_p) {
        loadValueOfProp_7(
          form.dimObjItemType,
          form.prop,
          0,
          prop.value?.typorrel,
          form.linkType
        );
      }
    }
  }
};

const fnUpdLev = () => {};

const fnSelectTyp = () => {
  if (form.linkType === allConsts.FD_LinkType.up$val_c) {
    form.typ = typ.value?.id || typ.value;
  } else {
    if (form.dimObjItemType === cTyp || form.dimObjItemType === cCls) {
      form.typ = typ.value?.id || typ.value;
      cls.value = null;

      if (form.dimObjItemType === cCls) {
        loadCls(form.typ);
      }
    }
  }
};

const fnSelectCls = () => {
  form.cls = cls.value?.id || cls.value;

  if (form.linkType < allConsts.FD_LinkType.c$val_p) {
    if (form.dimObjItemType === cMemberCls) {
      loadRelClsMember(allConsts.FD_MemberType.cls, form.relTyp, props.parent, form.linkType);
    }
    if (form.dimObjItemType === cMemberRelCls) {
      loadRelClsMember(allConsts.FD_MemberType.relcls, form.relTyp, props.parent, form.linkType);
    }
  }
};

const fnSelectRelTyp = () => {
  form.relTyp = relTyp.value?.id || relTyp.value;

  if (form.linkType !== allConsts.FD_LinkType.up$val_c) {
    relCls.value = null;
    form.relCls = null;
    relClsMember.value = null;
    form.relClsMember = null;
    relTypMember.value = null;
    form.relTypMember = null;

    if (form.linkType === allConsts.FD_LinkType.none) {
      if (form.dimObjItemType === cRelCls) {
        loadRelCls(form.relTyp, form.dimObjItemType, props.parent, form.linkType);
      }
      if (form.dimObjItemType === cMemberCls || form.dimObjItemType === cMemberRelCls) {
        loadRelCls(form.relTyp, form.dimObjItemType, props.parent, form.linkType);
      }
      if (form.dimObjItemType === cMemberTyp) {
        loadRelTypMember(allConsts.FD_MemberType.typ, form.relTyp, props.parent, form.linkType);
      }
      if (form.dimObjItemType === cMemberRelTyp) {
        loadRelTypMember(allConsts.FD_MemberType.reltyp, form.relTyp, props.parent, form.linkType);
      }
    }

    if (form.linkType === allConsts.FD_LinkType.p_c$urt) {
      if (form.dimObjItemType === cMemberTyp) {
        loadRelTypMember(allConsts.FD_MemberType.typ, form.relTyp, props.parent, form.linkType);
      }
      if (form.dimObjItemType === cMemberRelTyp) {
        loadRelTypMember(allConsts.FD_MemberType.reltyp, form.relTyp, props.parent, form.linkType);
      }
      if (form.dimObjItemType === cMemberCls) {
        loadRelCls(form.relTyp, form.dimObjItemType, props.parent, form.linkType);
      }
      if (form.dimObjItemType === cMemberRelCls) {
        loadRelCls(form.relTyp, form.dimObjItemType, props.parent, form.linkType);
      }
    }

    if (form.linkType === allConsts.FD_LinkType.uc$val_p) {
      form.relTypMember = relTyp.value?.rtm;
    }

    if (form.linkType === allConsts.FD_LinkType.r$val_uc) {
      prop.value = null;
      form.prop = null;
      if (form.dimObjItemType === allConsts.FD_DimObjItemType.reltyp) {
        loadRelTypMember(0, relTyp.value?.id, props.parent, form.linkType);
      } else if (form.dimObjItemType === allConsts.FD_DimObjItemType.relcls) {
        loadRelCls(form.relTyp, form.dimObjItemType, props.parent, form.linkType);
      }
    }
  }
};

const fnSelectRelCls = () => {
  form.relCls = relCls.value?.id || relCls.value;

  if (form.linkType !== allConsts.FD_LinkType.up$val_c) {
    if (form.linkType === allConsts.FD_LinkType.none) {
      if (form.dimObjItemType === allConsts.FD_DimObjItemType.ucls) {
        loadRelClsMember(allConsts.FD_MemberType.cls, form.relCls, props.parent, form.linkType);
      }
      if (form.dimObjItemType === allConsts.FD_DimObjItemType.urelcls) {
        loadRelClsMember(allConsts.FD_MemberType.relcls, form.relCls, props.parent, form.linkType);
      }
    }

    if (form.linkType === allConsts.FD_LinkType.p_c$urt) {
      if (form.dimObjItemType === cMemberCls) {
        loadRelClsMember(allConsts.FD_MemberType.cls, form.relCls, props.parent, form.linkType);
      }
      if (form.dimObjItemType === cMemberRelCls) {
        loadRelClsMember(allConsts.FD_MemberType.relcls, form.relCls, props.parent, form.linkType);
      }
    }

    if (form.linkType === allConsts.FD_LinkType.uc$val_p) {
      form.relClsMember = relCls.value?.rcm;
    }

    if (form.linkType === allConsts.FD_LinkType.r$val_uc) {
      prop.value = null;
      form.prop = null;
      relClsMember.value = null;
      form.relClsMember = null;
      loadRelClsMember(allConsts.FD_MemberType.relcls, form.relCls, props.parent, form.linkType);
    }
  }
};

const fnSelectRelTypMember = () => {
  form.relTypMember = relTypMember.value?.id || relTypMember.value;

  if (form.linkType === allConsts.FD_LinkType.c$val_up) {
    loadProp_6(
      relTypMember.value?.membertype,
      relTypMember.value?.typorrel,
      form.dimObjItemType,
      props.parent
    );
  }

  if (form.linkType === allConsts.FD_LinkType.r$val_uc) {
    prop.value = null;
    form.prop = null;
    loadProp_10_11(form.dimObjItemType, relTypMember.value?.id, props.parent, form.linkType);
  }

  if (form.linkType === allConsts.FD_LinkType.up$val_c) {
    prop.value = null;
    form.prop = null;
    clearComps();
    loadProp_10_11(form.dimObjItemType, relTypMember.value?.id, props.parent, form.linkType);
  }
};

const fnSelectRelClsMember = () => {
  form.relClsMember = relClsMember.value?.id || relClsMember.value;

  if (form.linkType === allConsts.FD_LinkType.c$val_up) {
    loadProp_6(
      relClsMember.value?.membertype,
      relClsMember.value?.clsorrel,
      form.dimObjItemType,
      props.parent
    );
  }

  if (form.linkType === allConsts.FD_LinkType.r$val_uc) {
    prop.value = null;
    form.prop = null;
    loadProp_10_11(form.dimObjItemType, relClsMember.value?.id, props.parent, form.linkType);
  }

  if (form.linkType === allConsts.FD_LinkType.up$val_c) {
    prop.value = null;
    form.prop = null;
    clearComps();
    loadProp_10_11(form.dimObjItemType, relClsMember.value?.id, props.parent, form.linkType);
  }
};

const validSave = () => {
  if (!form.dimObjItemType) return true;

  if (form.linkType === allConsts.FD_LinkType.none) {
    if (form.dimObjItemType === cTyp && !form.typ) return true;
    if (form.dimObjItemType === cCls && !form.cls) return true;
    if (form.dimObjItemType === cRelTyp && !form.relTyp) return true;
    if (form.dimObjItemType === cRelCls && !form.relCls) return true;
    if (
      (form.dimObjItemType === cMemberTyp || form.dimObjItemType === cMemberRelTyp) &&
      (form.relTyp == null || form.relTypMember == null)
    )
      return true;
    if (
      (form.dimObjItemType === cMemberCls || form.dimObjItemType === cMemberRelCls) &&
      (form.relCls == null || form.relClsMember == null)
    )
      return true;
  } else if (form.linkType === allConsts.FD_LinkType.p_c$urt) {
    if (!((form.relTyp && form.relTypMember) || (form.relCls && form.relClsMember))) return true;
  } else if (form.linkType === allConsts.FD_LinkType.c$up) {
    if (
      (form.dimObjItemType === cMemberTyp || form.dimObjItemType === cMemberRelTyp) &&
      form.relTypMember == null
    )
      return true;

    if (
      (form.dimObjItemType === cMemberCls || form.dimObjItemType === cMemberRelCls) &&
      form.relClsMember == null
    )
      return true;
  } else if (form.linkType === allConsts.FD_LinkType.p$uc) {
    return !(
      (form.dimObjItemType === cRelTyp && form.relTyp) ||
      (form.dimObjItemType === cRelCls && form.relCls)
    );
  } else if (
    form.linkType === allConsts.FD_LinkType.c$val_p ||
    form.linkType === allConsts.FD_LinkType.r$val_c
  ) {
    if (!form.prop) return true;
    else {
      if (form.dimObjItemType === cTyp && !form.typ) return true;
      if (form.dimObjItemType === cCls && !form.cls) return true;
      if (form.dimObjItemType === cRelTyp && !form.relTyp) return true;
      if (form.dimObjItemType === cRelCls && !form.relCls) return true;
    }
  } else if (form.linkType === allConsts.FD_LinkType.c$val_up) {
    if (!form.prop) return true;
    else {
      if (props.parentDimObjItemType === cRelTyp) {
        if (form.dimObjItemType === cTyp) return !(form.typ && form.relTypMember);
        if (form.dimObjItemType === cCls) return !(form.cls && form.relTypMember);
        if (form.dimObjItemType === cRelTyp) return !(form.relTyp && form.relTypMember);
        if (form.dimObjItemType === cRelCls) return !(form.relCls && form.relTypMember);
      } else if (props.parentDimObjItemType === cRelCls) {
        if (form.dimObjItemType === cTyp) return !(form.typ && form.relClsMember);
        if (form.dimObjItemType === cCls) return !(form.cls && form.relClsMember);
        if (form.dimObjItemType === cRelTyp) return !(form.relTyp && form.relClsMember);
        if (form.dimObjItemType === cRelCls) return !(form.relCls && form.relClsMember);
      }
    }
  } else if (
    form.linkType === allConsts.FD_LinkType.uc$val_p ||
    form.linkType === allConsts.FD_LinkType.r$val_uc
  ) {
    if (!form.prop) return true;
    else {
      if (form.dimObjItemType === cRelTyp) return !(form.relTyp && form.relTypMember);
      if (form.dimObjItemType === cRelCls) return !(form.relCls && form.relClsMember);
    }
  } else if (form.linkType === allConsts.FD_LinkType.up$val_c) {
    if (!form.prop) return true;
    else {
      if (props.parentDimObjItemType === cRelTyp) {
        if (form.dimObjItemType === cTyp) return !(form.typ && form.relTypMember);
        if (form.dimObjItemType === cCls) return !(form.cls && form.relTypMember);
        if (form.dimObjItemType === cRelCls) return !(form.relCls && form.relTypMember);
        if (form.dimObjItemType === cRelTyp) return !(form.relTyp && form.relTypMember);
      }
      if (props.parentDimObjItemType === cRelCls) {
        if (form.dimObjItemType === cTyp) return !(form.typ && form.relClsMember);
        if (form.dimObjItemType === cCls) return !(form.cls && form.relClsMember);
        if (form.dimObjItemType === cRelCls) return !(form.relCls && form.relClsMember);
        if (form.dimObjItemType === cRelTyp) return !(form.relTyp && form.relClsMember);
      }
    }
  }
};

const loadCompFor_11 = (doitVal, propVal) => {
  visible.value = true;
  api
    .post("", {
      method: "dimobjitem/loadCompFor_11",
      params: [doitVal, propVal],
    })
    .then((response) => {
      if (doitVal === cTyp) optTyp.value = response.data.result.records;
      if (doitVal === cCls) optCls.value = response.data.result.records;
      if (doitVal === cRelTyp) optRelTyp.value = response.data.result.records;
      if (doitVal === cRelCls) optRelCls.value = response.data.result.records;
    })
    .catch((error) => {
      let msg = error.message;
      if (error.response) msg = error.response.data.error.message;
      notifyError(msg);
    })
    .finally(() => {
      visible.value = false;
    });
};

const loadValueOfProp = (doitVal, propVal, parentVal, linkTypeVal) => {
  visible.value = true;
  parentVal = parentVal ? parseInt(parentVal, 10) : 0;

  api
    .post("", {
      method: "dimobjitem/loadValueOfProp",
      params: [doitVal, propVal, parentVal, linkTypeVal],
    })
    .then((response) => {
      if (
        form.linkType === allConsts.FD_LinkType.c$val_p ||
        form.linkType === allConsts.FD_LinkType.r$val_c
      ) {
        if (doitVal === cTyp || doitVal === cMemberTyp) {
          visCompTyp.value = true;
          optTyp.value = response.data.result.records;
        } else if (doitVal === cCls || doitVal === cMemberCls) {
          visCompCls.value = true;
          optCls.value = response.data.result.records;
        } else if (doitVal === cRelTyp || doitVal === cMemberRelTyp) {
          visCompRelTyp.value = true;
          optRelTyp.value = response.data.result.records;
        } else if (doitVal === cRelCls || doitVal === cMemberRelCls) {
          visCompRelCls.value = true;
          optRelCls.value = response.data.result.records;
        }
      }
      if (form.linkType === allConsts.FD_LinkType.uc$val_p) {
        if (doitVal === cRelTyp) {
          visCompRelTyp.value = true;
          optRelTyp.value = response.data.result.records;
        } else if (doitVal === cRelCls) {
          visCompRelCls.value = true;
          optRelCls.value = response.data.result.records;
        }
      }
    })
    .catch((error) => {
      let msg = error.message;
      if (error.response) msg = error.response.data.error.message;
      notifyError(msg);
    })
    .finally(() => {
      visible.value = false;
    });
};

const loadValueOfProp_7 = (doitVal, propVal, rtmORrcm, typORrel, linkTypeVal) => {
  visible.value = true;
  api
    .post("", {
      method: "dimobjitem/loadValueOfProp_7",
      params: [doitVal, propVal, rtmORrcm, typORrel, linkTypeVal],
    })
    .then((response) => {
      if (form.linkType === allConsts.FD_LinkType.uc$val_p) {
        if (doitVal === cRelTyp) {
          visCompRelTyp.value = true;
          optRelTyp.value = response.data.result.records;
        } else if (doitVal === cRelCls) {
          visCompRelCls.value = true;
          optRelCls.value = response.data.result.records;
        }
      }
    })
    .catch((error) => {
      let msg = error.message;
      if (error.response) msg = error.response.data.error.message;
      notifyError(msg);
    })
    .finally(() => {
      visible.value = false;
    });
};

const loadProp_10_11 = (doitVal, rtmORrcm, parentVal, linkTypeVal) => {
  visible.value = true;
  parentVal = parentVal ? parseInt(parentVal, 10) : 0;
  api
    .post("", {
      method: "dimobjitem/loadProp_10_11",
      params: [doitVal, rtmORrcm, parentVal, linkTypeVal],
    })
    .then((response) => {
      optProp.value = response.data.result.records;
    })
    .catch((error) => {
      let msg = error.message;
      if (error.response) msg = error.response.data.error.message;
      notifyError(msg);
    })
    .finally(() => {
      visible.value = false;
    });
};

const loadProp_6 = (memberType, typORrel, doitVal, parentVal) => {
  visible.value = true;
  parentVal = parentVal ? parseInt(parentVal, 10) : 0;
  api
    .post("", {
      method: "dimobjitem/loadProp_6",
      params: [memberType, typORrel, doitVal, parentVal],
    })
    .then((response) => {
      optProp.value = response.data.result.records;
    })
    .catch((error) => {
      let msg = error.message;
      if (error.response) msg = error.response.data.error.message;
      notifyError(msg);
    })
    .finally(() => {
      visible.value = false;
    });
};

const loadProp = (doitVal, parentVal, linkTypeVal) => {
  visible.value = true;
  parentVal = parentVal ? parseInt(parentVal, 10) : 0;
  api
    .post("", {
      method: "dimobjitem/loadProp",
      params: [doitVal, parentVal, linkTypeVal],
    })
    .then((response) => {
      optProp.value = response.data.result.records;
    })
    .catch((error) => {
      let msg = error.message;
      if (error.response) msg = error.response.data.error.message;
      notifyError(msg);
    })
    .finally(() => {
      visible.value = false;
    });
};

const loadTyp = (relTypVal, parentVal, doitVal, linkTypeVal) => {
  visible.value = true;
  parentVal = parentVal ? parseInt(parentVal, 10) : 0;
  api
    .post("", {
      method: "dimobjitem/loadTypForSelect",
      params: [relTypVal, parentVal, doitVal, linkTypeVal],
    })
    .then((response) => {
      optTyp.value = response.data.result.records;
    })
    .catch((error) => {
      let msg = error.message;
      if (error.response) msg = error.response.data.error.message;
      notifyError(msg);
    })
    .finally(() => {
      visible.value = false;
    });
};

const loadRelTypMember = (memberType, relTypVal, parentVal, linkTypeVal) => {
  visible.value = true;
  api
    .post("", {
      method: "dimobjitem/loadRelTypMember",
      params: [memberType, relTypVal, parentVal, linkTypeVal],
    })
    .then((response) => {
      optRelTypMember.value = response.data.result.records;
    })
    .catch((error) => {
      let msg = error.message;
      if (error.response) msg = error.response.data.error.message;
      notifyError(msg);
    })
    .finally(() => {
      visible.value = false;
    });
};

const loadRelClsMember = (memberType, relclsORrelTyp, parentVal, linkTypeVal) => {
  visible.value = true;
  api
    .post("", {
      method: "dimobjitem/loadRelClsMember",
      params: [memberType, relclsORrelTyp, parentVal, linkTypeVal],
    })
    .then((response) => {
      optRelClsMember.value = response.data.result.records;
    })
    .catch((error) => {
      let msg = error.message;
      if (error.response) msg = error.response.data.error.message;
      notifyError(msg);
    })
    .finally(() => {
      visible.value = false;
    });
};

const loadCls = (typVal) => {
  visible.value = true;
  api
    .post("", {
      method: "dimobjitem/loadClsForSelect",
      params: [typVal],
    })
    .then((response) => {
      optCls.value = response.data.result.records;
    })
    .catch((error) => {
      let msg = error.message;
      if (error.response) msg = error.response.data.error.message;
      notifyError(msg);
    })
    .finally(() => {
      visible.value = false;
    });
};

const loadRelTyp = (doitVal, parentVal, linkTypeVal) => {
  visible.value = true;
  api
    .post("", {
      method: "dimobjitem/loadRelTypForSelect",
      params: [doitVal, parentVal, linkTypeVal],
    })
    .then((response) => {
      optRelTyp.value = response.data.result.records;
    })
    .catch((error) => {
      let msg = error.message;
      if (error.response) msg = error.response.data.error.message;
      notifyError(msg);
    })
    .finally(() => {
      visible.value = false;
    });
};

const loadRelCls = (reltypVal, doitVal, parentVal, linkTypeVal) => {
  visible.value = true;
  api
    .post("", {
      method: "dimobjitem/loadRelClsForSelect",
      params: [reltypVal, doitVal, parentVal, linkTypeVal],
    })
    .then((response) => {
      optRelCls.value = response.data.result.records;
    })
    .catch((error) => {
      let msg = error.message;
      if (error.response) msg = error.response.data.error.message;
      notifyError(msg);
    })
    .finally(() => {
      visible.value = false;
    });
};

const onOKClick = () => {
  visible.value = true;
  const method = props.mode === "ins" ? "insertDOI" : "updateDOI";

  api
    .post("", {
      id: form.id,
      method: "dimobjitem/" + method,
      params: [form],
    })
    .then(
      (response) => {
        onDialogOK(response.data.result.records[0]);
        notifySuccess(proxy?.$t("success"));
      },
      (error) => {
        notifyError(error.response?.data?.error?.message || error.message);
      }
    )
    .finally(() => {
      visible.value = false;
    });
};

const linkTypeFilter = (parentDOIT) => {
  if (props.parent > 0) {
    if (parentDOIT === allConsts.FD_DimObjItemType.typ || parentDOIT === allConsts.FD_DimObjItemType.cls) {
      optLT.value = optLT.value.filter((it) => {
        return !(
          it.id === allConsts.FD_LinkType.c$up ||
          it.id === allConsts.FD_LinkType.c$val_up ||
          it.id === allConsts.FD_LinkType.uc$val_up ||
          it.id === allConsts.FD_LinkType.up$val_c ||
          it.id === allConsts.FD_LinkType.up$val_uc
        );
      });
    }
    if (
      parentDOIT === allConsts.FD_DimObjItemType.reltyp ||
      parentDOIT === allConsts.FD_DimObjItemType.relcls ||
      parentDOIT === allConsts.FD_DimObjItemType.ureltyp ||
      parentDOIT === allConsts.FD_DimObjItemType.urelcls
    ) {
      optLT.value = optLT.value.filter(() => true);
    }
    if (parentDOIT === allConsts.FD_DimObjItemType.utyp || parentDOIT === allConsts.FD_DimObjItemType.ucls) {
      optLT.value = optLT.value.filter((it) => {
        return !(
          it.id === allConsts.FD_LinkType.c$up ||
          it.id === allConsts.FD_LinkType.c$val_up ||
          it.id === allConsts.FD_LinkType.uc$val_up ||
          it.id === allConsts.FD_LinkType.up$val_c ||
          it.id === allConsts.FD_LinkType.up$val_uc
        );
      });
    }
  }
};

onMounted(() => {
  visible.value = true;
  api
    .post("", {
      method: "dict/load",
      params: [{ dict: "FD_LinkType" }],
    })
    .then((response) => {
      optLT.value = response.data.result.records;
      linkTypeFilter(props.parentDimObjItemType);
    })
    .finally(() => {
      visible.value = false;
    });

  visible.value = true;
  api
    .post("", {
      method: "dict/load",
      params: [{ dict: "FD_DimObjItemType" }],
    })
    .then((response) => {
      optDOIT.value = response.data.result.records;
      optDOITorg.value = response.data.result.records;
    })
    .finally(() => {
      visible.value = false;
    });

  if (props.mode === "ins") {
    loadTyp(0, props.parent, props.data.dimObjItemType, props.data.linkType);
  } else {
    disableLev.value = !(
      props.data.dimObjItemType === cTyp ||
      props.data.dimObjItemType === cCls ||
      props.data.dimObjItemType === cMemberTyp ||
      props.data.dimObjItemType === cMemberCls
    );

    if (props.data.linkType === allConsts.FD_LinkType.none) {
      if (props.data.dimObjItemType === cCls) {
        loadTyp(0, props.parent, props.data.dimObjItemType, props.data.linkType);
        loadCls(props.data.typ);
      } else if (props.data.dimObjItemType === cTyp) {
        loadTyp(0, props.parent, props.data.dimObjItemType, props.data.linkType);
      }
      if (props.data.dimObjItemType === cRelTyp) {
        loadRelTyp(props.data.dimObjItemType, props.parent, props.data.linkType);
        loadRelTypMember(allConsts.FD_MemberType.typ, props.data.relTyp, props.parent, props.data.linkType);
      }
      if (props.data.dimObjItemType === cRelCls) {
        loadRelTyp(props.data.dimObjItemType, props.parent, props.data.linkType);
        loadRelCls(props.data.relTyp, props.data.dimObjItemType, props.parent, props.data.linkType);
      } else if (props.data.dimObjItemType === cMemberTyp || props.data.dimObjItemType === cMemberRelTyp) {
        loadRelTyp(props.data.dimObjItemType, props.parent, props.data.linkType);
        if (props.data.dimObjItemType === cMemberTyp) {
          loadRelTypMember(allConsts.FD_MemberType.typ, props.data.relTyp, props.parent, props.data.linkType);
        } else {
          loadRelTypMember(allConsts.FD_MemberType.reltyp, props.data.relTyp, props.parent, props.data.linkType);
        }
      } else if (props.data.dimObjItemType === cMemberCls || props.data.dimObjItemType === cMemberRelCls) {
        loadRelTyp(props.data.dimObjItemType, props.parent, props.data.linkType);
        loadRelCls(form.relTyp, form.dimObjItemType, props.parent, form.linkType);
        if (props.data.dimObjItemType === cMemberCls) {
          loadRelClsMember(allConsts.FD_MemberType.cls, props.data.relCls, props.parent, props.data.linkType);
        } else {
          loadRelClsMember(allConsts.FD_MemberType.relcls, props.data.relCls, props.parent, props.data.linkType);
        }
      }
    } else if (props.data.linkType === allConsts.FD_LinkType.p_c$urt) {
      if (props.data.dimObjItemType === cMemberTyp || props.data.dimObjItemType === cMemberRelTyp) {
        loadRelTyp(props.data.dimObjItemType, props.parent, props.data.linkType);
        if (props.data.dimObjItemType === cMemberTyp) {
          loadRelTypMember(allConsts.FD_MemberType.typ, props.data.relTyp, props.parent, props.data.linkType);
        } else {
          loadRelTypMember(allConsts.FD_MemberType.reltyp, props.data.relTyp, props.parent, props.data.linkType);
        }
      }
      if (props.data.dimObjItemType === cMemberCls || props.data.dimObjItemType === cMemberRelCls) {
        loadRelCls(0, props.data.dimObjItemType, props.parent, props.data.linkType);
        if (props.data.dimObjItemType === cMemberCls) {
          loadRelClsMember(allConsts.FD_MemberType.cls, props.data.relCls, props.parent, props.data.linkType);
        } else {
          loadRelClsMember(allConsts.FD_MemberType.relcls, props.data.relCls, props.parent, props.data.linkType);
        }
      }
    } else if (props.data.linkType === allConsts.FD_LinkType.c$up) {
      if (props.parentDimObjItemType === allConsts.FD_DimObjItemType.reltyp) {
        if (props.data.dimObjItemType === allConsts.FD_DimObjItemType.utyp) {
          loadRelTypMember(1, 0, props.parent, props.data.linkType);
        }
        if (props.data.dimObjItemType === allConsts.FD_DimObjItemType.ureltyp) {
          loadRelTypMember(2, 0, props.parent, props.data.linkType);
        }
      }

      if (props.parentDimObjItemType === allConsts.FD_DimObjItemType.relcls) {
        if (props.data.dimObjItemType === allConsts.FD_DimObjItemType.ucls) {
          loadRelClsMember(allConsts.FD_MemberType.cls, 0, props.parent, props.data.linkType);
        }
        if (props.data.dimObjItemType === allConsts.FD_DimObjItemType.urelcls) {
          loadRelClsMember(allConsts.FD_MemberType.relcls, 0, props.parent, props.data.linkType);
        }
      }
    } else if (props.data.linkType === allConsts.FD_LinkType.p$uc) {
      if (
        props.parentDimObjItemType === cTyp ||
        props.parentDimObjItemType === cRelTyp ||
        props.parentDimObjItemType === cMemberTyp ||
        props.parentDimObjItemType === cMemberRelTyp
      ) {
        loadRelTyp(props.data.dimObjItemType, props.parent, props.data.linkType);
      }

      if (
        props.parentDimObjItemType === cCls ||
        props.parentDimObjItemType === cRelCls ||
        props.parentDimObjItemType === cMemberCls ||
        props.parentDimObjItemType === cMemberRelCls
      ) {
        loadRelCls(0, props.data.dimObjItemType, props.parent, props.data.linkType);
      }
    } else if (
      props.data.linkType === allConsts.FD_LinkType.c$val_p ||
      props.data.linkType === allConsts.FD_LinkType.r$val_c
    ) {
      loadProp(props.data.dimObjItemType, props.parent, props.data.linkType);
      loadValueOfProp(props.data.dimObjItemType, props.data.prop, props.parent, props.data.linkType);
    } else if (props.data.linkType === allConsts.FD_LinkType.c$val_up) {
      if (props.parentDimObjItemType === cRelTyp || props.parentDimObjItemType === cMemberRelTyp) {
        loadRelTypMember(0, 0, props.parent, props.data.linkType);
        loadValueOfProp(props.data.dimObjItemType, props.data.prop, props.parent, props.data.linkType);

        if (props.data.dimObjItemType === cTyp || props.data.dimObjItemType === cCls) {
          loadProp_6(allConsts.FD_MemberType.typ, 0, props.data.dimObjItemType, props.parent);
        }

        if (props.data.dimObjItemType === cRelTyp || props.data.dimObjItemType === cRelCls) {
          loadProp_6(allConsts.FD_MemberType.reltyp, 0, props.data.dimObjItemType, props.parent);
        }
      }

      if (props.parentDimObjItemType === cRelCls || props.parentDimObjItemType === cMemberRelCls) {
        loadRelClsMember(0, 0, props.parent, props.data.linkType);
        loadProp_6(allConsts.FD_MemberType.reltyp, 0, props.data.dimObjItemType, props.parent);
        loadValueOfProp(props.data.dimObjItemType, props.data.prop, props.parent, props.data.linkType);
      }
    } else if (props.data.linkType === allConsts.FD_LinkType.uc$val_p) {
      loadProp(props.data.dimObjItemType, props.parent, props.data.linkType);

      if (props.data.dimObjItemType === allConsts.FD_DimObjItemType.reltyp) {
        loadValueOfProp_7(props.data.dimObjItemType, props.data.prop, props.data.relTypMember, 0, props.data.linkType);
      } else if (props.data.dimObjItemType === allConsts.FD_DimObjItemType.relcls) {
        loadValueOfProp_7(props.data.dimObjItemType, props.data.prop, props.data.relClsMember, 0, props.data.linkType);
      }
    } else if (props.data.linkType === allConsts.FD_LinkType.r$val_uc) {
      loadRelTyp(props.data.dimObjItemType, props.parent, props.data.linkType);
      if (props.data.dimObjItemType === cRelTyp) {
        visCompRelTypMember.value = true;
        loadRelTypMember(0, props.data.relTyp, props.parent, props.data.linkType);
        loadProp_10_11(props.data.dimObjItemType, props.data.relTypMember, props.parent, props.data.linkType);
      } else if (props.data.dimObjItemType === cRelCls) {
        visCompRelCls.value = true;
        visCompRelClsMember.value = true;
        loadRelCls(props.data.relTyp, props.data.dimObjItemType, props.parent, props.data.linkType);
        loadRelClsMember(0, props.data.relCls, props.parent, props.data.linkType);
        loadProp_10_11(props.data.dimObjItemType, props.data.relClsMember, props.parent, props.data.linkType);
      }
    } else if (props.data.linkType === allConsts.FD_LinkType.up$val_c) {
      visCompRelTypMember.value = false;
      visCompRelClsMember.value = false;
      visCompTyp.value = false;
      visCompCls.value = false;
      visCompRelTyp.value = false;
      visCompRelCls.value = false;

      if (props.parentDimObjItemType === cRelTyp || props.parentDimObjItemType === cMemberRelTyp) {
        visCompRelTypMember.value = true;
        loadRelTypMember(0, 0, props.parent, props.data.linkType);
        loadProp_10_11(props.data.dimObjItemType, props.data.relTypMember, props.parent, props.data.linkType);
      }
      if (props.parentDimObjItemType === cRelCls || props.parentDimObjItemType === cMemberRelCls) {
        visCompRelClsMember.value = true;
        loadRelClsMember(0, 0, props.parent, props.data.linkType);
        loadProp_10_11(props.data.dimObjItemType, props.data.relClsMember, props.parent, props.data.linkType);
      }
      loadCompFor_11(props.data.dimObjItemType, props.data.prop);

      if (props.data.dimObjItemType === cTyp) visCompTyp.value = true;
      else if (props.data.dimObjItemType === cCls) visCompCls.value = true;
      else if (props.data.dimObjItemType === cRelTyp) visCompRelTyp.value = true;
      else if (props.data.dimObjItemType === cRelCls) visCompRelCls.value = true;
    }
  }
});
</script>

<style scoped></style>
