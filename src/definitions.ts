export interface BLEServPlugin {
  startService(): Promise<void>;
  stopService(): Promise<void>;
}
