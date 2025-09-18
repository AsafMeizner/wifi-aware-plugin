/* eslint-disable @typescript-eslint/explicit-module-boundary-types */
import { WebPlugin } from '@capacitor/core';

import type {
  WifiAwarePlugin, InitOptions, AdvertiseOptions, DiscoverOptions, PairOptions,
  ConnectOptions, SendMessageOptions, SendFileOptions
} from './definitions';

export class WifiAwareWeb extends WebPlugin implements WifiAwarePlugin {
  private unsupported() { this.unavailable('Wi-Fi Aware is not available on Web'); }

  async init(_: InitOptions) { this.unsupported(); return { ok: false }; }
  async isSupported(){ return { supported: false, platform: 'web' as const }; }
  async advertise(_: AdvertiseOptions){ this.unsupported(); return { ok: false }; }
  async stopAdvertise(){ this.unsupported(); return { ok: false }; }
  async discover(_: DiscoverOptions){ this.unsupported(); return { ok: false }; }
  async stopDiscover(){ this.unsupported(); return { ok: false }; }
  async getKnownPeers(){ return { peers: [] }; }
  async pair(_: PairOptions){ this.unsupported(); return { ok: false }; }
  async connect(_: ConnectOptions){ this.unsupported(); return { ok: false }; }
  async disconnect(_: { peerId: string }){ this.unsupported(); return { ok: false }; }
  async sendMessage(_: SendMessageOptions){ this.unsupported(); return { ok: false, payloadId: '' }; }
  async sendFile(_: SendFileOptions){ this.unsupported(); return { ok: false, payloadId: '' }; }
  async acceptIncoming(_: { payloadId: string; accept: boolean }){ this.unsupported(); return { ok: false }; }
}
