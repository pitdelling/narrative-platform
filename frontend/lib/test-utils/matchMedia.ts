type Listener = (event: MediaQueryListEvent) => void;

export interface MatchMediaMock {
  setMatches: (matches: boolean) => void;
}

/**
 * Installs a controllable `window.matchMedia` mock for `(prefers-color-scheme: dark)`.
 * jsdom has no native implementation, and tests need to simulate both an OS-dark
 * and an OS-light system, plus fire synthetic `change` events for live-sync tests.
 */
export function mockMatchMedia(initialMatches: boolean): MatchMediaMock {
  let matches = initialMatches;
  const listeners = new Set<Listener>();

  const mediaQueryList = {
    get matches() {
      return matches;
    },
    media: "(prefers-color-scheme: dark)",
    onchange: null,
    addEventListener: (_event: string, listener: Listener) => {
      listeners.add(listener);
    },
    removeEventListener: (_event: string, listener: Listener) => {
      listeners.delete(listener);
    },
    addListener: (listener: Listener) => listeners.add(listener),
    removeListener: (listener: Listener) => listeners.delete(listener),
    dispatchEvent: () => true,
  };

  window.matchMedia = ((query: string) => {
    void query;
    return mediaQueryList as unknown as MediaQueryList;
  }) as typeof window.matchMedia;

  return {
    setMatches(next: boolean) {
      matches = next;
      listeners.forEach((listener) => listener({ matches: next } as MediaQueryListEvent));
    },
  };
}
