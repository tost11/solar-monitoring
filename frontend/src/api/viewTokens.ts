import {getCookie} from "./cookie";

const COOKIE = "viewTokens";
const COOKIE_DAYS = 365;

export interface ViewTokenEntry {
  password: string;
  expiresAt?: string;
  sourceCode?: string;
  name?: string;
  valid?: boolean;
}

type ViewTokenMap = Record<string, ViewTokenEntry>;

function read(): ViewTokenMap {
  const raw = getCookie(COOKIE);
  if (!raw) return {};
  try {
    return JSON.parse(decodeURIComponent(raw)) as ViewTokenMap;
  } catch {
    return {};
  }
}

function write(map: ViewTokenMap) {
  const value = encodeURIComponent(JSON.stringify(map));
  const date = new Date();
  date.setTime(date.getTime() + COOKIE_DAYS * 24 * 60 * 60 * 1000);
  document.cookie = `${COOKIE}=${value}; expires=${date.toUTCString()}; path=/`;
}

export function getViewTokens(): ViewTokenMap {
  return read();
}

export function setViewToken(systemId: string, password: string, expiresAt?: string, sourceCode?: string) {
  const map = read();
  map[systemId] = {password, expiresAt, sourceCode};
  write(map);
}

export function findViewTokenBySecret(systemId: string, secret: string): ViewTokenEntry | null {
  const entry = read()[systemId];
  return entry && entry.sourceCode === secret ? entry : null;
}

export function patchViewToken(systemId: string, patch: Partial<ViewTokenEntry>) {
  const map = read();
  if (!map[systemId]) return;
  map[systemId] = {...map[systemId], ...patch};
  write(map);
}

export function removeViewToken(systemId: string) {
  const map = read();
  delete map[systemId];
  write(map);
}

export function clearViewTokens() {
  write({});
}

export function getActivePassword(systemId: string): string | null {
  const entry = read()[systemId];
  return entry ? entry.password : null;
}

const RESERVED_SEGMENTS = new Set([
  "public", "edit", "tokens", "search", "status", "statistics", "share-links",
  "allManager", "addManageBy", "deleteManager", "delete", "redeem", "validate"
]);

// Returns the stored password for the system targeted by a request path, or null.
export function viewTokenForPath(path: string): string | null {
  const query = path.match(/[?&]systemId=([^&]+)/);
  if (query) {
    return getActivePassword(decodeURIComponent(query[1]));
  }
  const seg = path.match(/\/api\/system\/(?:public\/)?([^/?]+)/);
  if (seg) {
    const id = decodeURIComponent(seg[1]);
    if (RESERVED_SEGMENTS.has(id)) return null;
    return getActivePassword(id);
  }
  return null;
}
