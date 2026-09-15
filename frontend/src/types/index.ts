export interface FeatureItem {
  id: number;
  title: string;
  description: string;
  status: string;
  metric: string;
}

export interface KpiItem {
  label: string;
  value: string;
  trend: string;
  tone: string;
}

export interface OperationRecord {
  key: string;
  name: string;
  owner: string;
  status: string;
  metric: string;
  priority: string;
}

export interface OverviewResponse {
  appName: string;
  appCode: string;
  description: string;
  features: FeatureItem[];
  kpis: KpiItem[];
  records: OperationRecord[];
}

export type SessionStatus = "OPEN" | "OFF";
export type SignupStatus = "CONFIRMED" | "WAITLIST" | "CANCELLED";

export interface SignupView {
  id: number;
  playerName: string;
  status: SignupStatus;
  position: number;
  createdAt: string;
}

export interface SessionView {
  id: number;
  title: string;
  sessionDate: string;
  timeSlot: string;
  seatCount: number;
  confirmedCount: number;
  remainingSeats: number;
  waitlistCount: number;
  status: SessionStatus;
  full: boolean;
  confirmed: SignupView[];
  waitlist: SignupView[];
}

export interface CreateSessionPayload {
  title: string;
  sessionDate: string;
  timeSlot: string;
  seatCount: number;
}

export interface SignupResult {
  session: SessionView;
  status: SignupStatus;
  position: number;
}

export interface CancelResult {
  session: SessionView;
  promoted: string[];
}
