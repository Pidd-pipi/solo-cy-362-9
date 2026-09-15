<script setup lang="ts">
import { reactive, ref, watch } from "vue";
import type { FormInstance, FormRules } from "element-plus";
import { TIME_SLOTS } from "../constants/app";
import type { CreateSessionPayload } from "../types";

const props = defineProps<{
  modelValue: boolean;
  defaultDate: string;
  submitting: boolean;
}>();

const emit = defineEmits<{
  "update:modelValue": [value: boolean];
  submit: [payload: CreateSessionPayload];
}>();

const formRef = ref<FormInstance>();
const form = reactive<CreateSessionPayload>({
  title: "",
  sessionDate: props.defaultDate,
  timeSlot: "19:00",
  seatCount: 6,
});

const rules: FormRules = {
  title: [{ required: true, message: "请输入场次标题（剧本/主题）", trigger: "blur" }],
  sessionDate: [{ required: true, message: "请选择场次日期", trigger: "change" }],
  timeSlot: [
    { required: true, message: "请选择或输入时段", trigger: "change" },
    { pattern: /^([01]\d|2[0-3]):[0-5]\d$/, message: "时段格式应为 HH:mm", trigger: "change" },
  ],
  seatCount: [{ required: true, message: "请设置座位数", trigger: "change" }],
};

watch(
  () => props.modelValue,
  (open) => {
    if (open) {
      form.sessionDate = props.defaultDate;
    }
  },
);

function close() {
  emit("update:modelValue", false);
}

async function submit() {
  if (!formRef.value) return;
  try {
    await formRef.value.validate();
  } catch {
    return;
  }
  emit("submit", { ...form, title: form.title.trim() });
}
</script>

<template>
  <el-dialog
    :model-value="modelValue"
    title="开设场次"
    width="min(480px, 92vw)"
    @update:model-value="emit('update:modelValue', $event)"
  >
    <el-form ref="formRef" :model="form" :rules="rules" label-width="80px">
      <el-form-item label="场次标题" prop="title">
        <el-input v-model="form.title" maxlength="120" placeholder="如：《年轮》硬核推理局" show-word-limit />
      </el-form-item>
      <el-form-item label="日期" prop="sessionDate">
        <el-date-picker
          v-model="form.sessionDate"
          type="date"
          value-format="YYYY-MM-DD"
          placeholder="选择日期"
          style="width: 100%"
        />
      </el-form-item>
      <el-form-item label="时段" prop="timeSlot">
        <el-select v-model="form.timeSlot" filterable allow-create default-first-option placeholder="选择或输入时段" style="width: 100%">
          <el-option v-for="slot in TIME_SLOTS" :key="slot" :label="`${slot} 场`" :value="slot" />
        </el-select>
      </el-form-item>
      <el-form-item label="座位数" prop="seatCount">
        <el-input-number v-model="form.seatCount" :min="1" :max="100" />
      </el-form-item>
    </el-form>
    <template #footer>
      <el-button @click="close">取消</el-button>
      <el-button type="primary" :loading="submitting" @click="submit">开设并上架</el-button>
    </template>
  </el-dialog>
</template>
