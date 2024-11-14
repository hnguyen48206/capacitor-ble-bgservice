import { registerPlugin } from '@capacitor/core';
const BLEServ = registerPlugin('BLEServ', {
    web: () => import('./web').then((m) => new m.BLEServWeb()),
});
export * from './definitions';
export { BLEServ };
//# sourceMappingURL=index.js.map