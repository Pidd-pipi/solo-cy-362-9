<script setup lang="ts">
import { onMounted, ref, watch } from "vue";
import {
  cancelSignup,
  changeSeatCount,
  createSession,
  loadSessions,
  scheduleState,
  setPlayerName,
  signup,
  togglePublish,
} from "../state/schedule";
import type { CreateSessionPayload, SessionView } from "../types";
import CreateSessionDialog from "./CreateSessionDialog.vue";
import SessionCard from "./SessionCard.vue";

const dialogVisible = ref(false);

onMounted(loadSessions);
watch(
  () => scheduleState.date,
  () => loadSessions(),
);

function onPlayerNameInput(value: string) {
  setPlayerName(value);
}

async function onCreate(payload: CreateSessionPayload) {
  await createSession(payload);
  dialogVisible.value = false;
}

function onSaveSeats(session: SessionView, seatCount: number) {
  void changeSeatCount(session, seatCount);
}
</script>

<template>
  <section class="work-panel schedule-board">
    <div class="board-head">
      <div>
        <h2>场次排期与拼车</h2>
        <p class="board-sub">开设场次 → 上架报名 → 满员候补 → 退位自动递补，余位与候补顺序实时同步。</p>
      </div>
      <div class="board-controls">
        <el-input
          :model-value="scheduleState.playerName"
          placeholder="玩家昵称（报名用）"
          clearable
          class="player-input"
          @update:model-value="onPlayerNameInput"
        />
        <el-date-picker
          v-model="scheduleState.date"
          type="date"
          value-format="YYYY-MM-DD"
          :clearable="false"
          placeholder="选择日期"
          class="date-picker"
        />
        <el-button :loading="scheduleState.loading" @click="loadSessions">刷新</el-button>
        <el-button type="primary" @click="dialogVisible = true">开设场次</el-button>
      </div>
    </div>

    <div v-loading="scheduleState.loading" class="board-body">
      <el-empty
        v-if="!scheduleState.sessions.length && !scheduleState.loading"
        description="当日暂无场次，点击「开设场次」创建"
      />
      <div v-else class="session-grid">
        <SessionCard
          v-for="session in scheduleState.sessions"
          :key="session.id"
          :session="session"
          :player-name="scheduleState.playerName"
          :mutating="scheduleState.mutating"
          @signup="signup"
          @cancel="cancelSignup"
          @toggle="togglePublish"
          @save-seats="onSaveSeats"
        />
      </div>
    </div>

    <CreateSessionDialog
      v-model="dialogVisible"
      :default-date="scheduleState.date"
      :submitting="scheduleState.mutating"
      @submit="onCreate"
    />
  </section>
</template>
