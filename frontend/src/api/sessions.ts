import { apiRequest } from "./client";
import type { CancelResult, CreateSessionPayload, SessionView, SignupResult } from "../types";

export function listSessions(date: string): Promise<SessionView[]> {
  return apiRequest<SessionView[]>(`/sessions?date=${encodeURIComponent(date)}`);
}

export function createSession(payload: CreateSessionPayload): Promise<SessionView> {
  return apiRequest<SessionView>("/sessions", { method: "POST", body: JSON.stringify(payload) });
}

export function setSessionStatus(id: number, action: "publish" | "unpublish"): Promise<SessionView> {
  return apiRequest<SessionView>(`/sessions/${id}/${action}`, { method: "POST" });
}

export function updateSeatCount(id: number, seatCount: number): Promise<SessionView> {
  return apiRequest<SessionView>(`/sessions/${id}`, { method: "PATCH", body: JSON.stringify({ seatCount }) });
}

export function signupSession(id: number, playerName: string): Promise<SignupResult> {
  return apiRequest<SignupResult>(`/sessions/${id}/signups`, { method: "POST", body: JSON.stringify({ playerName }) });
}

export function cancelSignup(id: number, playerName: string): Promise<CancelResult> {
  return apiRequest<CancelResult>(`/sessions/${id}/cancel`, { method: "POST", body: JSON.stringify({ playerName }) });
}
