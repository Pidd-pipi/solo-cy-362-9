<script setup lang="ts">
import { computed, ref } from "vue";
import type { SessionView, SignupView } from "../types";

const props = defineProps<{
  session: SessionView;
  playerName: string;
  mutating: boolean;
}>();

const emit = defineEmits<{
  signup: [session: SessionView];
  cancel: [session: SessionView];
  toggle: [session: SessionView];
  saveSeats: [session: SessionView, seatCount: number];
}>();

const showRoster = ref(false);
const seatDraft = ref(props.session.seatCount);
const seatPopover = ref();

const myConfirmed = computed<SignupView | undefined>(() =>
  props.session.confirmed.find((item) => item.playerName === props.playerName),
);
const myWaiting = computed<SignupView | undefined>(() =>
  props.session.waitlist.find((item) => item.playerName === props.playerName),
);
const alreadySigned = computed(() => Boolean(myConfirmed.value || myWaiting.value));

const statusTag = computed(() => {
  if (props.session.status === "OFF") {
    return { type: "info" as const, text: "已下架" };
  }
  if (props.session.full) {
    return { type: "danger" as const, text: "已满员" };
  }
  return { type: "success" as const, text: "拼局中" };
});

const seatPercent = computed(() =>
  Math.min(100, Math.round((props.session.confirmedCount / props.session.seatCount) * 100)),
);

const signupButtonText = computed(() => {
  if (props.session.status === "OFF") return "已下架";
  if (myConfirmed.value) return "已上车";
  if (myWaiting.value) return `候补中 · 第 ${myWaiting.value.position} 位`;
  return props.session.full ? "加入候补" : "报名拼车";
});

function syncSeatDraft() {
  seatDraft.value = props.session.seatCount;
}

function confirmSeats() {
  seatPopover.value?.hide();
  if (seatDraft.value !== props.session.seatCount) {
    emit("saveSeats", props.session, seatDraft.value);
  }
}
</script>

<template>
  <article class="session-card">
    <div class="session-card__head">
      <div>
        <h3 class="session-title">{{ session.title }}</h3>
        <p class="session-meta">{{ session.sessionDate }} · {{ session.timeSlot }} 场</p>
      </div>
      <el-tag :type="statusTag.type" effect="dark">{{ statusTag.text }}</el-tag>
    </div>

    <el-progress
      :percentage="seatPercent"
      :status="session.full ? 'exception' : 'success'"
      :stroke-width="10"
    />
    <div class="session-stats">
      <span>已报 {{ session.confirmedCount }}/{{ session.seatCount }}</span>
      <span>余位 {{ session.remainingSeats }}</span>
      <span>候补 {{ session.waitlistCount }} 人</span>
      <el-tag v-if="myConfirmed" size="small" type="success" effect="plain">
        我已上车 · 第 {{ myConfirmed.position }} 位
      </el-tag>
      <el-tag v-else-if="myWaiting" size="small" type="warning" effect="plain">
        我候补第 {{ myWaiting.position }} 位
      </el-tag>
    </div>

    <div class="session-actions">
      <el-button
        type="primary"
        size="small"
        :disabled="mutating || alreadySigned || session.status === 'OFF'"
        @click="emit('signup', session)"
      >
        {{ signupButtonText }}
      </el-button>
      <el-button
        v-if="alreadySigned"
        type="danger"
        size="small"
        plain
        :disabled="mutating"
        @click="emit('cancel', session)"
      >
        取消报名
      </el-button>
      <el-button size="small" plain :disabled="mutating" @click="emit('toggle', session)">
        {{ session.status === "OPEN" ? "下架" : "上架" }}
      </el-button>
      <el-popover ref="seatPopover" trigger="click" placement="top" width="220">
        <template #reference>
          <el-button size="small" plain :disabled="mutating" @click="syncSeatDraft">座位数</el-button>
        </template>
        <div class="seat-editor">
          <el-input-number v-model="seatDraft" :min="1" :max="100" size="small" />
          <el-button type="primary" size="small" @click="confirmSeats">确定</el-button>
        </div>
      </el-popover>
      <el-button size="small" text @click="showRoster = !showRoster">
        {{ showRoster ? "收起名单" : "查看名单" }}
      </el-button>
    </div>

    <div v-show="showRoster" class="roster">
      <h4>已确认（按报名先后）</h4>
      <ol v-if="session.confirmed.length">
        <li v-for="item in session.confirmed" :key="item.id" :class="{ me: item.playerName === playerName }">
          {{ item.position }}. {{ item.playerName }}
        </li>
      </ol>
      <p v-else class="roster-empty">暂无已确认玩家</p>
      <h4>候补队列（按报名先后递补）</h4>
      <ol v-if="session.waitlist.length">
        <li v-for="item in session.waitlist" :key="item.id" :class="{ me: item.playerName === playerName }">
          {{ item.position }}. {{ item.playerName }}
        </li>
      </ol>
      <p v-else class="roster-empty">暂无候补玩家</p>
    </div>
  </article>
</template>
