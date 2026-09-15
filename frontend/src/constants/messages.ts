export const REQUEST_MESSAGES = {
  overviewFallback: "已加载本地运营样例，后端联通后会自动展示实时数据。",
  healthPath: "/api/health",
};

export const SCHEDULE_MESSAGES = {
  needPlayerName: "请先填写玩家昵称，再报名拼车",
  unknownError: "操作失败，请稍后重试",
  sessionCreated: (title: string) => `场次「${title}」已开设并上架`,
  published: "场次已上架，开放报名",
  unpublished: "场次已下架，停止报名",
  seatsUpdated: "座位数已更新",
  signupConfirmed: "报名成功，已锁定座位",
  signupWaitlisted: (position: number) => `本场已满员，已加入候补队列（第 ${position} 位）`,
  cancelled: "已取消报名",
  cancelledWithPromotion: (names: string) => `已取消报名，${names} 按报名先后递补上车`,
};
