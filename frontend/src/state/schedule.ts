import { reactive } from "vue";
import { ElMessage } from "element-plus";
import * as sessionApi from "../api/sessions";
import type { CreateSessionPayload, SessionView } from "../types";
import { SCHEDULE_MESSAGES } from "../constants/messages";

const PLAYER_NAME_KEY = "ldmurdergame.playerName";

function todayString(): string {
  const now = new Date();
  const pad = (value: number) => String(value).padStart(2, "0");
  return `${now.getFullYear()}-${pad(now.getMonth() + 1)}-${pad(now.getDate())}`;
}

function messageOf(error: unknown): string {
  return error instanceof Error ? error.message : SCHEDULE_MESSAGES.unknownError;
}

export const scheduleState = reactive({
  date: todayString(),
  playerName: localStorage.getItem(PLAYER_NAME_KEY) ?? "",
  sessions: [] as SessionView[],
  loading: false,
  mutating: false,
});

/** 用接口返回的最新场次视图替换本地数据，保证页面与服务端一致。 */
function upsertSession(session: SessionView) {
  const index = scheduleState.sessions.findIndex((item) => item.id === session.id);
  if (index >= 0) {
    scheduleState.sessions.splice(index, 1, session);
  }
}

export function setPlayerName(name: string) {
  scheduleState.playerName = name.trim();
  localStorage.setItem(PLAYER_NAME_KEY, scheduleState.playerName);
}

export async function loadSessions() {
  scheduleState.loading = true;
  try {
    scheduleState.sessions = await sessionApi.listSessions(scheduleState.date);
  } catch (error) {
    ElMessage.error(messageOf(error));
  } finally {
    scheduleState.loading = false;
  }
}

async function mutate(action: () => Promise<void>) {
  if (scheduleState.mutating) {
    return;
  }
  scheduleState.mutating = true;
  try {
    await action();
  } catch (error) {
    ElMessage.error(messageOf(error));
  } finally {
    scheduleState.mutating = false;
  }
}

export async function createSession(payload: CreateSessionPayload) {
  await mutate(async () => {
    const session = await sessionApi.createSession(payload);
    if (session.sessionDate !== scheduleState.date) {
      scheduleState.date = session.sessionDate; // 日期变化由 watcher 触发刷新
    } else {
      await loadSessions();
    }
    ElMessage.success(SCHEDULE_MESSAGES.sessionCreated(session.title));
  });
}

export async function togglePublish(session: SessionView) {
  await mutate(async () => {
    const action = session.status === "OPEN" ? "unpublish" : "publish";
    const updated = await sessionApi.setSessionStatus(session.id, action);
    upsertSession(updated);
    ElMessage.success(updated.status === "OPEN" ? SCHEDULE_MESSAGES.published : SCHEDULE_MESSAGES.unpublished);
  });
}

export async function changeSeatCount(session: SessionView, seatCount: number) {
  await mutate(async () => {
    const updated = await sessionApi.updateSeatCount(session.id, seatCount);
    upsertSession(updated);
    ElMessage.success(SCHEDULE_MESSAGES.seatsUpdated);
  });
}

export async function signup(session: SessionView) {
  if (!scheduleState.playerName) {
    ElMessage.warning(SCHEDULE_MESSAGES.needPlayerName);
    return;
  }
  await mutate(async () => {
    const result = await sessionApi.signupSession(session.id, scheduleState.playerName);
    upsertSession(result.session);
    if (result.status === "CONFIRMED") {
      ElMessage.success(SCHEDULE_MESSAGES.signupConfirmed);
    } else {
      ElMessage.warning(SCHEDULE_MESSAGES.signupWaitlisted(result.position));
    }
  });
}

export async function cancelSignup(session: SessionView) {
  if (!scheduleState.playerName) {
    ElMessage.warning(SCHEDULE_MESSAGES.needPlayerName);
    return;
  }
  await mutate(async () => {
    const result = await sessionApi.cancelSignup(session.id, scheduleState.playerName);
    upsertSession(result.session);
    if (result.promoted.length > 0) {
      ElMessage.success(SCHEDULE_MESSAGES.cancelledWithPromotion(result.promoted.join("、")));
    } else {
      ElMessage.success(SCHEDULE_MESSAGES.cancelled);
    }
  });
}
