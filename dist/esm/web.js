import { WebPlugin } from '@capacitor/core';
export class BLEServWeb extends WebPlugin {
    async echo(options) {
        console.log('ECHO', options);
        return options;
    }
    async startService() {
        console.log('Starting BLE checking service...');
        // Implement BLE scanning logic here
    }
    async stopService() {
        console.log('Stopping BLE checking service...');
        // Implement logic to stop BLE scanning here
    }
}
//# sourceMappingURL=web.js.map