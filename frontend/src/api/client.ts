import { API_BASE_URL } from "../constants/app";
import type { OverviewResponse } from "../types";

export async function apiRequest<T>(path: string, options: RequestInit = {}): Promise<T> {
  const response = await fetch(`${API_BASE_URL}${path}`, {
    headers: { Accept: "application/json", "Content-Type": "application/json" },
    ...options,
  });

  if (!response.ok) {
    let message = `请求失败（${response.status}）`;
    try {
      const body: unknown = await response.json();
      if (body && typeof body === "object" && "message" in body && typeof body.message === "string") {
        message = body.message;
      }
    } catch {
      // 保留默认错误信息
    }
    throw new Error(message);
  }

  return response.json() as Promise<T>;
}

export async function fetchOverview(): Promise<OverviewResponse> {
  const response = await fetch(`${API_BASE_URL}/overview`, {
    headers: { Accept: "application/json" },
  });

  if (!response.ok) {
    throw new Error(`Overview request failed: ${response.status}`);
  }

  return response.json() as Promise<OverviewResponse>;
}
