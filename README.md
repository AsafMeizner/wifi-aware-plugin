# @asafmeizner/wifi-aware

Cross-platform Wi-Fi Aware (iOS/Android) for Capacitor

## Install

```bash
npm install @asafmeizner/wifi-aware
npx cap sync
```

## API

<docgen-index>

* [`init(...)`](#init)
* [`isSupported()`](#issupported)
* [`advertise(...)`](#advertise)
* [`stopAdvertise()`](#stopadvertise)
* [`discover(...)`](#discover)
* [`stopDiscover()`](#stopdiscover)
* [`getKnownPeers()`](#getknownpeers)
* [`pair(...)`](#pair)
* [`connect(...)`](#connect)
* [`disconnect(...)`](#disconnect)
* [`sendMessage(...)`](#sendmessage)
* [`sendFile(...)`](#sendfile)
* [`acceptIncoming(...)`](#acceptincoming)
* [`addListener('peerFound', ...)`](#addlistenerpeerfound-)
* [`addListener('peerLost', ...)`](#addlistenerpeerlost-)
* [`addListener('pairingNeeded', ...)`](#addlistenerpairingneeded-)
* [`addListener('paired', ...)`](#addlistenerpaired-)
* [`addListener('connected', ...)`](#addlistenerconnected-)
* [`addListener('disconnected', ...)`](#addlistenerdisconnected-)
* [`addListener('message', ...)`](#addlistenermessage-)
* [`addListener('transferRequest', ...)`](#addlistenertransferrequest-)
* [`addListener('transferProgress', ...)`](#addlistenertransferprogress-)
* [`addListener('transferCompleted', ...)`](#addlistenertransfercompleted-)
* [`addListener('error', ...)`](#addlistenererror-)
* [Interfaces](#interfaces)
* [Type Aliases](#type-aliases)

</docgen-index>

<docgen-api>
<!--Update the source file JSDoc comments and rerun docgen to update the docs below-->

### init(...)

```typescript
init(options: InitOptions) => Promise<{ ok: boolean; }>
```

| Param         | Type                                                |
| ------------- | --------------------------------------------------- |
| **`options`** | <code><a href="#initoptions">InitOptions</a></code> |

**Returns:** <code>Promise&lt;{ ok: boolean; }&gt;</code>

--------------------


### isSupported()

```typescript
isSupported() => Promise<{ supported: boolean; platform: Platform; }>
```

**Returns:** <code>Promise&lt;{ supported: boolean; platform: <a href="#platform">Platform</a>; }&gt;</code>

--------------------


### advertise(...)

```typescript
advertise(options?: AdvertiseOptions | undefined) => Promise<{ ok: boolean; }>
```

| Param         | Type                                                          |
| ------------- | ------------------------------------------------------------- |
| **`options`** | <code><a href="#advertiseoptions">AdvertiseOptions</a></code> |

**Returns:** <code>Promise&lt;{ ok: boolean; }&gt;</code>

--------------------


### stopAdvertise()

```typescript
stopAdvertise() => Promise<{ ok: boolean; }>
```

**Returns:** <code>Promise&lt;{ ok: boolean; }&gt;</code>

--------------------


### discover(...)

```typescript
discover(options?: DiscoverOptions | undefined) => Promise<{ ok: boolean; }>
```

| Param         | Type                                                        |
| ------------- | ----------------------------------------------------------- |
| **`options`** | <code><a href="#discoveroptions">DiscoverOptions</a></code> |

**Returns:** <code>Promise&lt;{ ok: boolean; }&gt;</code>

--------------------


### stopDiscover()

```typescript
stopDiscover() => Promise<{ ok: boolean; }>
```

**Returns:** <code>Promise&lt;{ ok: boolean; }&gt;</code>

--------------------


### getKnownPeers()

```typescript
getKnownPeers() => Promise<{ peers: DeviceInfo[]; }>
```

**Returns:** <code>Promise&lt;{ peers: DeviceInfo[]; }&gt;</code>

--------------------


### pair(...)

```typescript
pair(options: PairOptions) => Promise<{ ok: boolean; }>
```

| Param         | Type                                                |
| ------------- | --------------------------------------------------- |
| **`options`** | <code><a href="#pairoptions">PairOptions</a></code> |

**Returns:** <code>Promise&lt;{ ok: boolean; }&gt;</code>

--------------------


### connect(...)

```typescript
connect(options: ConnectOptions) => Promise<{ ok: boolean; }>
```

| Param         | Type                                                      |
| ------------- | --------------------------------------------------------- |
| **`options`** | <code><a href="#connectoptions">ConnectOptions</a></code> |

**Returns:** <code>Promise&lt;{ ok: boolean; }&gt;</code>

--------------------


### disconnect(...)

```typescript
disconnect(args: { peerId: string; }) => Promise<{ ok: boolean; }>
```

| Param      | Type                             |
| ---------- | -------------------------------- |
| **`args`** | <code>{ peerId: string; }</code> |

**Returns:** <code>Promise&lt;{ ok: boolean; }&gt;</code>

--------------------


### sendMessage(...)

```typescript
sendMessage(options: SendMessageOptions) => Promise<{ ok: boolean; payloadId: string; }>
```

| Param         | Type                                                              |
| ------------- | ----------------------------------------------------------------- |
| **`options`** | <code><a href="#sendmessageoptions">SendMessageOptions</a></code> |

**Returns:** <code>Promise&lt;{ ok: boolean; payloadId: string; }&gt;</code>

--------------------


### sendFile(...)

```typescript
sendFile(options: SendFileOptions) => Promise<{ ok: boolean; payloadId: string; }>
```

| Param         | Type                                                        |
| ------------- | ----------------------------------------------------------- |
| **`options`** | <code><a href="#sendfileoptions">SendFileOptions</a></code> |

**Returns:** <code>Promise&lt;{ ok: boolean; payloadId: string; }&gt;</code>

--------------------


### acceptIncoming(...)

```typescript
acceptIncoming(args: { payloadId: string; accept: boolean; }) => Promise<{ ok: boolean; }>
```

| Param      | Type                                                 |
| ---------- | ---------------------------------------------------- |
| **`args`** | <code>{ payloadId: string; accept: boolean; }</code> |

**Returns:** <code>Promise&lt;{ ok: boolean; }&gt;</code>

--------------------


### addListener('peerFound', ...)

```typescript
addListener(eventName: 'peerFound', cb: (peer: DeviceInfo) => void) => Promise<any>
```

| Param           | Type                                                                 |
| --------------- | -------------------------------------------------------------------- |
| **`eventName`** | <code>'peerFound'</code>                                             |
| **`cb`**        | <code>(peer: <a href="#deviceinfo">DeviceInfo</a>) =&gt; void</code> |

**Returns:** <code>Promise&lt;any&gt;</code>

--------------------


### addListener('peerLost', ...)

```typescript
addListener(eventName: 'peerLost', cb: (peerId: string) => void) => Promise<any>
```

| Param           | Type                                     |
| --------------- | ---------------------------------------- |
| **`eventName`** | <code>'peerLost'</code>                  |
| **`cb`**        | <code>(peerId: string) =&gt; void</code> |

**Returns:** <code>Promise&lt;any&gt;</code>

--------------------


### addListener('pairingNeeded', ...)

```typescript
addListener(eventName: 'pairingNeeded', cb: (ev: { peer: DeviceInfo; pin?: string; }) => void) => Promise<any>
```

| Param           | Type                                                                                        |
| --------------- | ------------------------------------------------------------------------------------------- |
| **`eventName`** | <code>'pairingNeeded'</code>                                                                |
| **`cb`**        | <code>(ev: { peer: <a href="#deviceinfo">DeviceInfo</a>; pin?: string; }) =&gt; void</code> |

**Returns:** <code>Promise&lt;any&gt;</code>

--------------------


### addListener('paired', ...)

```typescript
addListener(eventName: 'paired', cb: (ev: { peer: DeviceInfo; }) => void) => Promise<any>
```

| Param           | Type                                                                          |
| --------------- | ----------------------------------------------------------------------------- |
| **`eventName`** | <code>'paired'</code>                                                         |
| **`cb`**        | <code>(ev: { peer: <a href="#deviceinfo">DeviceInfo</a>; }) =&gt; void</code> |

**Returns:** <code>Promise&lt;any&gt;</code>

--------------------


### addListener('connected', ...)

```typescript
addListener(eventName: 'connected', cb: (ev: { peer: DeviceInfo; }) => void) => Promise<any>
```

| Param           | Type                                                                          |
| --------------- | ----------------------------------------------------------------------------- |
| **`eventName`** | <code>'connected'</code>                                                      |
| **`cb`**        | <code>(ev: { peer: <a href="#deviceinfo">DeviceInfo</a>; }) =&gt; void</code> |

**Returns:** <code>Promise&lt;any&gt;</code>

--------------------


### addListener('disconnected', ...)

```typescript
addListener(eventName: 'disconnected', cb: (ev: { peerId: string; }) => void) => Promise<any>
```

| Param           | Type                                              |
| --------------- | ------------------------------------------------- |
| **`eventName`** | <code>'disconnected'</code>                       |
| **`cb`**        | <code>(ev: { peerId: string; }) =&gt; void</code> |

**Returns:** <code>Promise&lt;any&gt;</code>

--------------------


### addListener('message', ...)

```typescript
addListener(eventName: 'message', cb: (ev: { peerId: string; payloadId: string; kind: 'text' | 'binary'; text?: string; dataBase64?: string; }) => void) => Promise<any>
```

| Param           | Type                                                                                                                               |
| --------------- | ---------------------------------------------------------------------------------------------------------------------------------- |
| **`eventName`** | <code>'message'</code>                                                                                                             |
| **`cb`**        | <code>(ev: { peerId: string; payloadId: string; kind: 'text' \| 'binary'; text?: string; dataBase64?: string; }) =&gt; void</code> |

**Returns:** <code>Promise&lt;any&gt;</code>

--------------------


### addListener('transferRequest', ...)

```typescript
addListener(eventName: 'transferRequest', cb: (ev: { payloadId: string; peer: DeviceInfo; kind: PayloadKind; bytesTotal: number; displayName?: string; }) => void) => Promise<any>
```

| Param           | Type                                                                                                                                                                                     |
| --------------- | ---------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| **`eventName`** | <code>'transferRequest'</code>                                                                                                                                                           |
| **`cb`**        | <code>(ev: { payloadId: string; peer: <a href="#deviceinfo">DeviceInfo</a>; kind: <a href="#payloadkind">PayloadKind</a>; bytesTotal: number; displayName?: string; }) =&gt; void</code> |

**Returns:** <code>Promise&lt;any&gt;</code>

--------------------


### addListener('transferProgress', ...)

```typescript
addListener(eventName: 'transferProgress', cb: (state: TransferState) => void) => Promise<any>
```

| Param           | Type                                                                        |
| --------------- | --------------------------------------------------------------------------- |
| **`eventName`** | <code>'transferProgress'</code>                                             |
| **`cb`**        | <code>(state: <a href="#transferstate">TransferState</a>) =&gt; void</code> |

**Returns:** <code>Promise&lt;any&gt;</code>

--------------------


### addListener('transferCompleted', ...)

```typescript
addListener(eventName: 'transferCompleted', cb: (state: TransferState & { savedPath?: string; }) => void) => Promise<any>
```

| Param           | Type                                                                                                  |
| --------------- | ----------------------------------------------------------------------------------------------------- |
| **`eventName`** | <code>'transferCompleted'</code>                                                                      |
| **`cb`**        | <code>(state: <a href="#transferstate">TransferState</a> & { savedPath?: string; }) =&gt; void</code> |

**Returns:** <code>Promise&lt;any&gt;</code>

--------------------


### addListener('error', ...)

```typescript
addListener(eventName: 'error', cb: (ev: { code: string; message: string; }) => void) => Promise<any>
```

| Param           | Type                                                             |
| --------------- | ---------------------------------------------------------------- |
| **`eventName`** | <code>'error'</code>                                             |
| **`cb`**        | <code>(ev: { code: string; message: string; }) =&gt; void</code> |

**Returns:** <code>Promise&lt;any&gt;</code>

--------------------


### Interfaces


#### InitOptions

| Prop                | Type                 |
| ------------------- | -------------------- |
| **`serviceName`**   | <code>string</code>  |
| **`instanceId`**    | <code>string</code>  |
| **`autoAdvertise`** | <code>boolean</code> |
| **`autoDiscover`**  | <code>boolean</code> |


#### AdvertiseOptions

| Prop              | Type                                                            |
| ----------------- | --------------------------------------------------------------- |
| **`serviceData`** | <code><a href="#record">Record</a>&lt;string, string&gt;</code> |


#### DiscoverOptions

| Prop             | Type                |
| ---------------- | ------------------- |
| **`filterText`** | <code>string</code> |


#### DeviceInfo

| Prop                 | Type                                                         |
| -------------------- | ------------------------------------------------------------ |
| **`id`**             | <code>string</code>                                          |
| **`os`**             | <code><a href="#platform">Platform</a> \| 'unknown'</code>   |
| **`name`**           | <code>string</code>                                          |
| **`rssi`**           | <code>number</code>                                          |
| **`vendorSpecific`** | <code><a href="#record">Record</a>&lt;string, any&gt;</code> |
| **`paired`**         | <code>boolean</code>                                         |


#### PairOptions

| Prop         | Type                |
| ------------ | ------------------- |
| **`peerId`** | <code>string</code> |


#### ConnectOptions

| Prop         | Type                |
| ------------ | ------------------- |
| **`peerId`** | <code>string</code> |


#### SendMessageOptions

| Prop             | Type                                                |
| ---------------- | --------------------------------------------------- |
| **`peerId`**     | <code>string</code>                                 |
| **`payloadId`**  | <code>string</code>                                 |
| **`kind`**       | <code><a href="#payloadkind">PayloadKind</a></code> |
| **`text`**       | <code>string</code>                                 |
| **`dataBase64`** | <code>string</code>                                 |


#### SendFileOptions

| Prop              | Type                |
| ----------------- | ------------------- |
| **`peerId`**      | <code>string</code> |
| **`filePath`**    | <code>string</code> |
| **`displayName`** | <code>string</code> |
| **`payloadId`**   | <code>string</code> |


#### TransferState

| Prop                   | Type                                                |
| ---------------------- | --------------------------------------------------- |
| **`payloadId`**        | <code>string</code>                                 |
| **`peerId`**           | <code>string</code>                                 |
| **`bytesTotal`**       | <code>number</code>                                 |
| **`bytesTransferred`** | <code>number</code>                                 |
| **`direction`**        | <code>'send' \| 'receive'</code>                    |
| **`kind`**             | <code><a href="#payloadkind">PayloadKind</a></code> |
| **`accepted`**         | <code>boolean</code>                                |


### Type Aliases


#### Platform

<code>'ios' | 'android' | 'web'</code>


#### Record

Construct a type with a set of properties K of type T

<code>{ [P in K]: T; }</code>


#### PayloadKind

<code>'text' | 'binary' | 'file'</code>

</docgen-api>
