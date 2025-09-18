export type Platform = 'ios' | 'android' | 'web';

export interface InitOptions {
  serviceName: string;
  instanceId?: string;
  autoAdvertise?: boolean;
  autoDiscover?: boolean;
}

export interface DeviceInfo {
  id: string;
  os: Platform | 'unknown';
  name?: string;
  rssi?: number;
  vendorSpecific?: Record<string, any>;
  paired?: boolean;
}

export interface AdvertiseOptions { serviceData?: Record<string, string>; }
export interface DiscoverOptions { filterText?: string; }
export interface PairOptions { peerId: string; }
export interface ConnectOptions { peerId: string; }

export type PayloadKind = 'text' | 'binary' | 'file';

export interface SendMessageOptions {
  peerId: string;
  payloadId?: string;
  kind: PayloadKind;
  text?: string;
  dataBase64?: string;
}

export interface SendFileOptions {
  peerId: string;
  filePath: string;
  displayName?: string;
  payloadId?: string;
}

export interface TransferState {
  payloadId: string;
  peerId: string;
  bytesTotal: number;
  bytesTransferred: number;
  direction: 'send' | 'receive';
  kind: PayloadKind;
  accepted?: boolean;
}

export interface WifiAwarePlugin {
  init(options: InitOptions): Promise<{ ok: boolean }>;
  isSupported(): Promise<{ supported: boolean; platform: Platform }>;
  advertise(options?: AdvertiseOptions): Promise<{ ok: boolean }>;
  stopAdvertise(): Promise<{ ok: boolean }>;
  discover(options?: DiscoverOptions): Promise<{ ok: boolean }>;
  stopDiscover(): Promise<{ ok: boolean }>;

  getKnownPeers(): Promise<{ peers: DeviceInfo[] }>;
  pair(options: PairOptions): Promise<{ ok: boolean }>;
  connect(options: ConnectOptions): Promise<{ ok: boolean }>;
  disconnect(args: { peerId: string }): Promise<{ ok: boolean }>;

  sendMessage(options: SendMessageOptions): Promise<{ ok: boolean; payloadId: string }>;
  sendFile(options: SendFileOptions): Promise<{ ok: boolean; payloadId: string }>;
  acceptIncoming(args: { payloadId: string; accept: boolean }): Promise<{ ok: boolean }>;

  addListener(eventName: 'peerFound', cb: (peer: DeviceInfo) => void): Promise<any>;
  addListener(eventName: 'peerLost', cb: (peerId: string) => void): Promise<any>;
  addListener(eventName: 'pairingNeeded', cb: (ev: { peer: DeviceInfo; pin?: string }) => void): Promise<any>;
  addListener(eventName: 'paired', cb: (ev: { peer: DeviceInfo }) => void): Promise<any>;
  addListener(eventName: 'connected', cb: (ev: { peer: DeviceInfo }) => void): Promise<any>;
  addListener(eventName: 'disconnected', cb: (ev: { peerId: string }) => void): Promise<any>;
  addListener(
    eventName: 'message',
    cb: (ev: { peerId: string; payloadId: string; kind: 'text' | 'binary'; text?: string; dataBase64?: string }) => void
  ): Promise<any>;
  addListener(
    eventName: 'transferRequest',
    cb: (ev: { payloadId: string; peer: DeviceInfo; kind: PayloadKind; bytesTotal: number; displayName?: string }) => void
  ): Promise<any>;
  addListener(eventName: 'transferProgress', cb: (state: TransferState) => void): Promise<any>;
  addListener(eventName: 'transferCompleted', cb: (state: TransferState & { savedPath?: string }) => void): Promise<any>;
  addListener(eventName: 'error', cb: (ev: { code: string; message: string }) => void): Promise<any>;
}
