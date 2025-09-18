import Foundation
import Capacitor
import Network
import WiFiAware
import DeviceDiscoveryUI
import AccessorySetupKit

@objc(WifiAwarePlugin)
public class WifiAwarePlugin: CAPPlugin {
    private let impl = WifiAwareImpl()

    @objc func isSupported(_ call: CAPPluginCall) {
        let supported = WifiAware.isSupported // pseudocode: check Availability
        call.resolve(["supported": supported, "platform": "ios"])
    }

    @objc func init(_ call: CAPPluginCall) {
        guard let svc = call.getString("serviceName") else {
            call.reject("serviceName is required"); return
        }
        let instance = call.getString("instanceId") ?? UUID().uuidString
        let autoAdv = call.getBool("autoAdvertise") ?? false
        let autoDisc = call.getBool("autoDiscover") ?? false
        impl.configure(serviceName: svc, instanceId: instance, notify: emit)
        if autoAdv { impl.startAdvertising() }
        if autoDisc { impl.startDiscovery() }
        call.resolve(["ok": true])
    }

    @objc func advertise(_ call: CAPPluginCall) {
        impl.startAdvertising()
        call.resolve(["ok": true])
    }
    @objc func stopAdvertise(_ call: CAPPluginCall) {
        impl.stopAdvertising(); call.resolve(["ok": true])
    }
    @objc func discover(_ call: CAPPluginCall) {
        impl.startDiscovery(); call.resolve(["ok": true])
    }
    @objc func stopDiscover(_ call: CAPPluginCall) {
        impl.stopDiscovery(); call.resolve(["ok": true])
    }
    @objc func getKnownPeers(_ call: CAPPluginCall) {
        call.resolve(["peers": impl.knownPeersPayload()])
    }
    @objc func pair(_ call: CAPPluginCall) {
        guard let pid = call.getString("peerId") else { call.reject("peerId required"); return }
        impl.pair(peerId: pid) { ok, err in
            if ok { call.resolve(["ok": true]) } else { call.reject(err ?? "pair failed") }
        }
    }
    @objc func connect(_ call: CAPPluginCall) {
        guard let pid = call.getString("peerId") else { call.reject("peerId required"); return }
        impl.connect(peerId: pid) { ok, err in
            if ok { call.resolve(["ok": true]) } else { call.reject(err ?? "connect failed") }
        }
    }
    @objc func disconnect(_ call: CAPPluginCall) {
        guard let pid = call.getString("peerId") else { call.reject("peerId required"); return }
        impl.disconnect(peerId: pid); call.resolve(["ok": true])
    }
    @objc func sendMessage(_ call: CAPPluginCall) {
        guard let pid = call.getString("peerId"), let kind = call.getString("kind") else {
            call.reject("peerId and kind required"); return
        }
        let payloadId = call.getString("payloadId") ?? UUID().uuidString
        do {
            try impl.sendMessage(peerId: pid, payloadId: payloadId, kind: kind, text: call.getString("text"), base64: call.getString("dataBase64"))
            call.resolve(["ok": true, "payloadId": payloadId])
        } catch {
            call.reject("sendMessage error: \(error)")
        }
    }
    @objc func sendFile(_ call: CAPPluginCall) {
        guard let pid = call.getString("peerId"), let path = call.getString("filePath") else {
            call.reject("peerId and filePath required"); return
        }
        let payloadId = call.getString("payloadId") ?? UUID().uuidString
        impl.sendFile(peerId: pid, payloadId: payloadId, filePath: path) { ok, err in
            if ok { call.resolve(["ok": true, "payloadId": payloadId]) }
            else { call.reject(err ?? "sendFile failed") }
        }
    }
    @objc func acceptIncoming(_ call: CAPPluginCall) {
        guard let payloadId = call.getString("payloadId") else { call.reject("payloadId required"); return }
        let accept = call.getBool("accept") ?? false
        impl.acceptIncoming(payloadId: payloadId, accept: accept)
        call.resolve(["ok": true])
    }

    private func emit(_ event: String, _ data: [String: Any]) {
        self.notifyListeners(event, data: data)
    }
}

// MARK: - Core Impl
class WifiAwareImpl {
    private var serviceName = "com.asafmeizner.wifi-aware.demo"
    private var instanceId = UUID().uuidString

    private var listener: NWListener?
    private var connections: [String: NWConnection] = [:]
    private var browser: NWBrowser?

    private var peers: [String: DeviceInfo] = [:]
    private var notify: ((String,[String:Any])->Void)!

    struct DeviceInfo: Codable {
        var id: String; var os: String; var name: String?; var paired: Bool
    }

    func configure(serviceName: String, instanceId: String, notify: @escaping (String,[String:Any])->Void) {
        self.serviceName = serviceName
        self.instanceId = instanceId
        self.notify = notify
        setupListener()
    }

    private func setupListener() {
        let params = NWParameters.wifiAware
        let listener = try! NWListener(using: params)
        listener.newConnectionHandler = { [weak self] conn in
            self?.handleIncoming(conn)
        }
        listener.stateUpdateHandler = { state in
            // emit state if needed
        }
        listener.start(queue: .main)
        self.listener = listener
    }

    func startAdvertising() {
        // Wi-Fi Aware advertising is declared via Info.plist WiFiAwareServices; NWListener publishes service endpoints.
        // Nothing extra required here in iOS 26+ if plist is set. (System handles service publication.)
    }

    func stopAdvertising() {
        // No-op: stopping listener would stop accept but keeps service type in Info.plist
    }

    func startDiscovery() {
        let params = NWParameters.wifiAware
        let descriptor = NWBrowser.Descriptor.service(type: serviceName, domain: nil)
        browser = NWBrowser(for: descriptor, using: params)
        browser?.browseResultsChangedHandler = { [weak self] res, _ in
            for r in res {
                guard case let .service(name: n, type: _, domain: _, interface: _) = r.endpoint else { continue }
                let pid = n
                if self?.peers[pid] == nil {
                    let info = DeviceInfo(id: pid, os: "unknown", name: n, paired: false)
                    self?.peers[pid] = info
                    self?.notify("peerFound", ["id": pid, "os": "unknown", "name": n, "paired": false])
                }
            }
        }
        browser?.start(queue: .main)
    }

    func stopDiscovery() { browser?.cancel(); browser = nil }

    func knownPeersPayload() -> [[String:Any]] {
        return peers.values.map { ["id": $0.id, "os": $0.os, "name": $0.name ?? "", "paired": $0.paired] }
    }

    // Pairing via DeviceDiscoveryUI/AccessorySetupKit triggers system UI with PIN
    func pair(peerId: String, completion: @escaping (Bool,String?)->Void) {
        let cfg = DDDevicePickerConfiguration() // DeviceDiscoveryUI
        cfg.discoveryService = serviceName
        let picker = DDDevicePickerViewController(configuration: cfg)
        picker.completion = { device, error in
            if let error = error { completion(false, error.localizedDescription); return }
            // System handled PIN entry UI; if success, device is paired and available for connections.
            if let device = device {
                self.peers[peerId]?.paired = true
                self.notify("paired", ["peer": ["id": peerId]])
                completion(true, nil)
            } else {
                completion(false, "UserCancelled")
            }
        }
        // present UI
        DispatchQueue.main.async {
            if let root = UIApplication.shared.keyWindow?.rootViewController {
                root.present(picker, animated: true)
            } else {
                completion(false, "NoRootVC")
            }
        }
    }

    func connect(peerId: String, completion: @escaping (Bool,String?)->Void) {
        // Resolve endpoint from browser cache
        guard let _ = peers[peerId] else { completion(false, "Unknown peer"); return }
        let params = NWParameters.wifiAware
        // Here we demonstrate connecting by "service name" — in practice you may carry addressing metadata via TXT records.
        let endpoint = NWEndpoint.service(name: peerId, type: serviceName, domain: "", interface: nil)
        let conn = NWConnection(to: endpoint, using: params)
        conn.stateUpdateHandler = { [weak self] st in
            switch st {
            case .ready:
                self?.connections[peerId] = conn
                self?.notify("connected", ["peer": ["id": peerId]])
                completion(true, nil)
                self?.receiveLoop(peerId: peerId, conn: conn)
            case .failed(let err):
                completion(false, err.localizedDescription)
            default: break
            }
        }
        conn.start(queue: .main)
    }

    func disconnect(peerId: String) {
        if let c = connections[peerId] { c.cancel(); connections.removeValue(forKey: peerId) }
        notify("disconnected", ["peerId": peerId])
    }

    private func handleIncoming(_ conn: NWConnection) {
        // Identify peer id (could exchange hello header)
        let pid = UUID().uuidString
        connections[pid] = conn
        notify("connected", ["peer": ["id": pid]])
        conn.start(queue: .main)
        receiveLoop(peerId: pid, conn: conn)
    }

    private func receiveLoop(peerId: String, conn: NWConnection) {
        // Our simple protocol: [1 byte kind][8 bytes length][N bytes body][optional headers JSON if kind==file header]
        func recvNextHeader() {
            conn.receive(minimumIncompleteLength: 9, maximumLength: 9) { data, _, _, err in
                if let err = err { self.notify("error", ["code":"RECV", "message":"\(err)"]); return }
                guard let data = data, data.count == 9 else { return }
                let kindByte = data[0]
                let len = data.subdata(in: 1..<9).withUnsafeBytes { $0.load(as: UInt64.self) }.bigEndian
                conn.receive(minimumIncompleteLength: Int(len), maximumLength: Int(len)) { body, _, _, err2 in
                    if let err2 = err2 { self.notify("error", ["code":"RECVB", "message":"\(err2)"]); return }
                    guard let body = body else { return }
                    let payloadId = UUID().uuidString
                    if kindByte == 0 {
                        if let text = String(data: body, encoding: .utf8) {
                            self.notify("message", ["peerId": peerId, "payloadId": payloadId, "kind": "text", "text": text])
                        }
                    } else if kindByte == 1 {
                        self.notify("message", ["peerId": peerId, "payloadId": payloadId, "kind": "binary", "dataBase64": body.base64EncodedString()])
                    } else if kindByte == 2 {
                        // file transfer header: JSON describing fileName & size; we follow with a second frame for file content
                        let meta = try? JSONSerialization.jsonObject(with: body) as? [String:Any]
                        let bytesTotal = meta?["size"] as? UInt64 ?? 0
                        let displayName = meta?["name"] as? String
                        self.notify("transferRequest", ["payloadId": payloadId, "peer": ["id": peerId], "kind":"file", "bytesTotal": bytesTotal, "displayName": displayName ?? "file"])
                        // wait until JS calls acceptIncoming(payloadId, accept) -> we’ll continue on a stored map (not shown for brevity)
                    }
                    recvNextHeader()
                }
            }
        }
        recvNextHeader()
    }

    func sendMessage(peerId: String, payloadId: String, kind: String, text: String?, base64: String?) throws {
        guard let conn = connections[peerId] else { throw NSError(domain:"no-conn", code:0) }
        var body = Data()
        var kindByte: UInt8 = 0
        if kind == "text", let t = text { kindByte = 0; body = t.data(using: .utf8)! }
        else if kind == "binary", let b64 = base64 { kindByte = 1; body = Data(base64Encoded: b64)! }
        else { throw NSError(domain:"bad-kind", code:0) }
        var header = Data([kindByte])
        var len = UInt64(body.count).bigEndian
        header.append(Data(bytes:&len, count:8))
        conn.send(content: header + body, completion: .contentProcessed({ _ in }))
    }

    func sendFile(peerId: String, payloadId: String, filePath: String, completion: @escaping (Bool,String?)->Void) {
        guard let conn = connections[peerId] else { completion(false,"no-conn"); return }
        let url = URL(fileURLWithPath: filePath)
        guard let attr = try? FileManager.default.attributesOfItem(atPath: url.path),
              let size = (attr[.size] as? NSNumber)?.uint64Value else { completion(false,"stat-failed"); return }
        let name = url.lastPathComponent
        // send meta frame
        let meta: [String:Any] = ["name": name, "size": size]
        let metaData = try! JSONSerialization.data(withJSONObject: meta)
        var header = Data([2])
        var len = UInt64(metaData.count).bigEndian
        header.append(Data(bytes:&len, count:8))
        conn.send(content: header + metaData, completion: .contentProcessed({_ in
            // stream file in chunks
            guard let stream = InputStream(url: url) else { completion(false,"open-failed"); return }
            stream.open()
            let bufSize = 64*1024
            var total: UInt64 = 0
            while stream.hasBytesAvailable {
                var buffer = [UInt8](repeating: 0, count: bufSize)
                let read = stream.read(&buffer, maxLength: bufSize)
                if read <= 0 { break }
                total += UInt64(read)
                // encode as binary frame (kind=1)
                var h = Data([1])
                var l = UInt64(read).bigEndian
                h.append(Data(bytes:&l, count:8))
                conn.send(content: h + Data(buffer.prefix(read)), completion: .contentProcessed({_ in }))
                self.notify("transferProgress", ["payloadId": payloadId, "peerId": peerId, "bytesTotal": size, "bytesTransferred": total, "direction":"send","kind":"file"])
            }
            stream.close()
            self.notify("transferCompleted", ["payloadId": payloadId, "peerId": peerId, "bytesTotal": size,"bytesTransferred": size,"direction":"send","kind":"file"])
            completion(true,nil)
        }))
    }

    func acceptIncoming(payloadId: String, accept: Bool) {
        // Implementation note: Map payloadId -> pending receiver state; if accepted, begin reading follow-up binary frames and persist to Documents.
        // For brevity, the persistence map code is omitted; the pattern mirrors sendFile’s chunk loop in reverse.
    }
}
