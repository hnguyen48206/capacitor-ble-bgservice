import { WebPlugin } from '@capacitor/core';

import type { BLEServPlugin } from './definitions';

export class BLEServWeb extends WebPlugin implements BLEServPlugin {

  async startService(): Promise<void> {
    console.log('Starting BLE checking service...');
    // Implement BLE scanning logic here
  }

  async stopService(): Promise<void> {
    console.log('Stopping BLE checking service...');
    // Implement logic to stop BLE scanning here
  }
}
