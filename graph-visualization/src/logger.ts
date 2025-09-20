let DEBUG = false

export function setDebugMode(isEnabled: boolean): void {
  DEBUG = isEnabled
}

export function log(...args: unknown[]): void {
  if (DEBUG) console.log(...args)
}


