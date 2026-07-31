import { getToken } from "@/lib/auth";

const API_URL = process.env.NEXT_PUBLIC_API_URL ?? "http://localhost:8080/api";

const translatedProblemMessages: Record<string, string> = {
  ai_not_configured: "A geração por IA ainda não está configurada.",
};

export class ApiError extends Error {
  constructor(message: string, public readonly status: number, public readonly code?: string) {
    super(message);
  }
}

export class NetworkError extends Error {
  constructor() {
    super("Não foi possível falar com o servidor.");
  }
}

const BACKEND_DOWN_PATH = "/backend-down";

function redirectToBackendDown() {
  if (typeof window === "undefined") return;
  if (window.location.pathname === BACKEND_DOWN_PATH) return;
  window.location.assign(BACKEND_DOWN_PATH);
}

export async function api<T>(path: string, init: RequestInit = {}): Promise<T> {
  const headers = new Headers(init.headers);
  if (!headers.has("Content-Type") && init.body) headers.set("Content-Type", "application/json");
  const token = getToken();
  if (token) headers.set("Authorization", `Bearer ${token}`);
  let response: Response;
  try {
    response = await fetch(`${API_URL}${path}`, { ...init, headers, cache: "no-store" });
  } catch {
    redirectToBackendDown();
    throw new NetworkError();
  }
  if (!response.ok) {
    let message = `Request failed (${response.status}).`;
    let code: string | undefined;
    try {
      const problem = await response.json();
      code = problem.title;
      message = translatedProblemMessages[code ?? ""] ?? problem.detail ?? problem.title ?? message;
    } catch {
      // Keep the default message.
    }
    throw new ApiError(message, response.status, code);
  }
  if (response.status === 204) return undefined as T;
  const text = await response.text();
  if (!text) return undefined as T;
  return JSON.parse(text) as T;
}
