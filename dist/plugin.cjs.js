'use strict';

var core = require('@capacitor/core');

const BLEServ = core.registerPlugin('BLEServ', {
    web: () => Promise.resolve().then(function () { return web; }).then((m) => new m.BLEServWeb()),
});

class BLEServWeb extends core.WebPlugin {
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

var web = /*#__PURE__*/Object.freeze({
    __proto__: null,
    BLEServWeb: BLEServWeb
});

exports.BLEServ = BLEServ;
//# sourceMappingURL=plugin.cjs.js.map
