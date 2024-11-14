import { WebPlugin } from '@capacitor/core';
import type { BLEServPlugin } from './definitions';
export declare class BLEServWeb extends WebPlugin implements BLEServPlugin {
    startService(): Promise<void>;
    stopService(): Promise<void>;
}
