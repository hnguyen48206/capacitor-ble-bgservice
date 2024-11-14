import { registerPlugin } from '@capacitor/core';

import type { BLEServPlugin } from './definitions';

const BLEServ = registerPlugin<BLEServPlugin>('BLEServ', {
  web: () => import('./web').then((m) => new m.BLEServWeb()),
});

export * from './definitions';
export { BLEServ };
