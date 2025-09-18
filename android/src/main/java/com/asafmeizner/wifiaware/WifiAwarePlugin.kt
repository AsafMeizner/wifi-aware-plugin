package com.asafmeizner.wifiaware

import android.net.*
import android.net.wifi.aware.*
import com.getcapacitor.*
import com.getcapacitor.annotation.CapacitorPlugin
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.FileInputStream
import java.net.InetSocketAddress
import java.net.ServerSocket
import java.net.Socket
import java.nio.ByteBuffer
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import kotlin.concurrent.thread

@CapacitorPlugin(name = "WifiAware")
class WifiAwarePlugin : Plugin() {
  private lateinit var impl: WifiAwareImpl

  override fun load() {
    impl = WifiAwareImpl(context, this::emit)
  }

  @PluginMethod
  fun isSupported(call: PluginCall) {
    val mgr = context.getSystemService(android.content.Context.WIFI_AWARE_SERVICE) as WifiAwareManager?
    call.resolve(JSObject().put("supported", mgr?.isAvailable == true).put("platform","android"))
  }

  @PluginMethod
  fun `init`(call: PluginCall) {
    val svc = call.getString("serviceName") ?: return call.reject("serviceName required")
    val inst = call.getString("instanceId") ?: UUID.randomUUID().toString()
    val autoAdv = call.getBoolean("autoAdvertise", false) ?: false   // Boolean? → Boolean
    val autoDisc = call.getBoolean("autoDiscover", false) ?: false   // Boolean? → Boolean
    impl.configure(svc, inst)
    if (autoAdv) impl.startPublish()
    if (autoDisc) impl.startSubscribe()
    call.resolve(JSObject().put("ok", true))
  }

  @PluginMethod fun advertise(call: PluginCall){ impl.startPublish(); call.resolve(JSObject().put("ok", true)) }
  @PluginMethod fun stopAdvertise(call: PluginCall){ impl.stopPublish(); call.resolve(JSObject().put("ok", true)) }
  @PluginMethod fun discover(call: PluginCall){ impl.startSubscribe(); call.resolve(JSObject().put("ok", true)) }
  @PluginMethod fun stopDiscover(call: PluginCall){ impl.stopSubscribe(); call.resolve(JSObject().put("ok", true)) }

  @PluginMethod fun getKnownPeers(call: PluginCall){ call.resolve(JSObject().put("peers", impl.knownPeers())) }
  @PluginMethod fun pair(call: PluginCall){ call.resolve(JSObject().put("ok", true)) }

  @PluginMethod
  fun connect(call: PluginCall){
    val pid = call.getString("peerId") ?: return call.reject("peerId required")
    impl.connect(pid) { ok, err -> if(ok) call.resolve(JSObject().put("ok", true)) else call.reject(err ?: "connect failed") }
  }

  @PluginMethod fun disconnect(call: PluginCall){ val pid = call.getString("peerId") ?: return call.reject("peerId required"); impl.disconnect(pid); call.resolve(JSObject().put("ok", true)) }

  @PluginMethod
  fun sendMessage(call: PluginCall){
    val pid = call.getString("peerId") ?: return call.reject("peerId required")
    val kind = call.getString("kind") ?: return call.reject("kind required")
    val payloadId = call.getString("payloadId") ?: UUID.randomUUID().toString()
    val text = call.getString("text")
    val b64 = call.getString("dataBase64")
    val ok = impl.sendMessage(pid, payloadId, kind, text, b64)
    if(ok) call.resolve(JSObject().put("ok", true).put("payloadId", payloadId)) else call.reject("send failed")
  }

  @PluginMethod
  fun sendFile(call: PluginCall){
    val pid = call.getString("peerId") ?: return call.reject("peerId required")
    val path = call.getString("filePath") ?: return call.reject("filePath required")
    val payloadId = call.getString("payloadId") ?: UUID.randomUUID().toString()
    impl.sendFile(pid, payloadId, path) { ok, err ->
      if(ok) call.resolve(JSObject().put("ok", true).put("payloadId", payloadId)) else call.reject(err ?: "sendFile failed")
    }
  }

  @PluginMethod
  fun acceptIncoming(call: PluginCall){
    val payloadId = call.getString("payloadId") ?: return call.reject("payloadId required")
    val accept = call.getBoolean("accept", false) ?: false
    impl.acceptIncoming(payloadId, accept)
    call.resolve(JSObject().put("ok", true))
  }

  private fun emit(ev: String, data: JSObject){ notifyListeners(ev, data) }
}

// ---------------- Implementation ----------------

private class WifiAwareImpl(
  private val ctx: android.content.Context,
  private val emit: (String, JSObject) -> Unit
) {
  private val aware by lazy { ctx.getSystemService(android.content.Context.WIFI_AWARE_SERVICE) as WifiAwareManager }
  private val conn by lazy { ctx.getSystemService(android.content.Context.CONNECTIVITY_SERVICE) as ConnectivityManager }

  private var session: WifiAwareSession? = null
  private var publishSession: PublishDiscoverySession? = null
  private var subscribeSession: SubscribeDiscoverySession? = null

  private var serviceName = "com.asafmeizner.wifi-aware.demo"
  private var instanceId = UUID.randomUUID().toString()

  // Map peerId -> PeerHandle (+ which session discovered it)
  private data class PeerRef(val handle: PeerHandle, val fromPublish: Boolean)
  private val peerMap = ConcurrentHashMap<String, PeerRef>()

  private val peers = ConcurrentHashMap<String, JSONObject>()
  private val sockets = ConcurrentHashMap<String, Socket>()
  private val serverSockets = ConcurrentHashMap<String, ServerSocket>()

  fun configure(svc: String, inst: String) { serviceName = svc; instanceId = inst; attachIfNeeded() }

  private fun attachIfNeeded() {
    if (session != null) return
    aware.attach(object: AttachCallback() {
      override fun onAttached(sess: WifiAwareSession) { session = sess }
      override fun onAttachFailed() { emit("error", JSObject().put("code","ATTACH_FAILED").put("message","Wi-Fi Aware attach failed")) }
    }, null)
  }

  fun startPublish() {
    val s = session ?: return
    val cfg = PublishConfig.Builder()
      .setServiceName(serviceName)
      .setPublishType(PublishConfig.PUBLISH_TYPE_UNSOLICITED)
      .build()
    s.publish(cfg, object: DiscoverySessionCallback() {
      override fun onPublishStarted(ps: PublishDiscoverySession) { publishSession = ps }
      override fun onMessageReceived(peerHandle: PeerHandle, message: ByteArray) {
        handleL2Message(fromPublisher = true, peer = peerHandle, message = message)
      }
      override fun onServiceDiscovered(peerHandle: PeerHandle, info: ByteArray?, mf: List<ByteArray>?) {
        val pid = peerHandle.hashCode().toString()
        peerMap[pid] = PeerRef(peerHandle, fromPublish = true)
        val obj = JSONObject().put("id", pid).put("os","android").put("paired", true)
        peers[pid] = obj
        emit("peerFound", JSObject.fromJSONObject(obj))
        // Reply hello from publisher as well
        publishSession?.sendMessage(peerHandle, "HELLO_ACK:$instanceId".toByteArray(), 0)
      }
    }, null)
  }
  fun stopPublish(){ publishSession?.close(); publishSession = null }

  fun startSubscribe() {
    val s = session ?: return
    val cfg = SubscribeConfig.Builder()
      .setServiceName(serviceName)
      .setSubscribeType(SubscribeConfig.SUBSCRIBE_TYPE_PASSIVE)
      .build()
    s.subscribe(cfg, object: DiscoverySessionCallback() {
      override fun onSubscribeStarted(ss: SubscribeDiscoverySession) { subscribeSession = ss }
      override fun onServiceDiscovered(peerHandle: PeerHandle, info: ByteArray?, mf: List<ByteArray>?) {
        val pid = peerHandle.hashCode().toString()
        peerMap[pid] = PeerRef(peerHandle, fromPublish = false)
        val obj = JSONObject().put("id", pid).put("os","android").put("paired", true)
        peers[pid] = obj
        emit("peerFound", JSObject.fromJSONObject(obj))
        // send hello
        subscribeSession?.sendMessage(peerHandle, "HELLO:$instanceId".toByteArray(), 0)
      }
      override fun onMessageReceived(peerHandle: PeerHandle, message: ByteArray) {
        handleL2Message(fromPublisher = false, peer = peerHandle, message = message)
      }
    }, null)
  }
  fun stopSubscribe(){ subscribeSession?.close(); subscribeSession = null }

  fun knownPeers(): JSArray { val arr = JSArray(); peers.values.forEach { arr.put(it) }; return arr }

  private fun handleL2Message(fromPublisher: Boolean, peer: PeerHandle, message: ByteArray) {
    val msg = String(message)
    val pid = peer.hashCode().toString()
    if (msg.startsWith("HELLO:")) {
      (publishSession ?: subscribeSession)?.sendMessage(peerHandle, "HELLO_ACK:$instanceId".toByteArray(), 0)
    } else if (msg.startsWith("PORT:")) {
      val parts = msg.split(":")
      val port = parts.getOrNull(1)?.toIntOrNull() ?: return
      // This side becomes CLIENT and connects to the announced port
      requestNetworkAndConnect(discoveryForPeer(pid) ?: return, peer, port, asServer = false)
    }
  }

  fun connect(peerId: String, cb: (Boolean,String?)->Unit) {
    val pref = peerMap[peerId] ?: return cb(false, "unknown peer")
    val ds = discoveryForPeer(peerId) ?: return cb(false, "no discovery session")
    // This side becomes SERVER: open server socket, announce PORT over L2, wait accept
    val port = (20000..40000).random()
    thread {
      try {
        val server = ServerSocket(port)
        serverSockets[peerId] = server
        // announce port over discovery L2
        ds.sendMessage(pref.handle, "PORT:$port".toByteArray(), 0)
        cb(true, null)
        val socket = server.accept()
        sockets[peerId] = socket
        emit("connected", JSObject().put("peer", JSObject().put("id", peerId)))
        readLoop(peerId, socket)
      } catch (t: Throwable) {
        cb(false, t.message)
      }
    }
  }

  fun disconnect(peerId: String) {
    sockets.remove(peerId)?.close()
    serverSockets.remove(peerId)?.close()
    emit("disconnected", JSObject().put("peerId", peerId))
  }

  private fun discoveryForPeer(peerId: String): DiscoverySession? {
    val pr = peerMap[peerId] ?: return null
    return if (pr.fromPublish) publishSession else subscribeSession
  }

  private fun requestNetworkAndConnect(ds: DiscoverySession, peer: PeerHandle, port: Int, asServer: Boolean) {
    // IMPORTANT: WifiAwareNetworkSpecifier.Builder requires the discovery session (publish/subscribe), not WifiAwareSession
    val builder = when(ds) {
      is PublishDiscoverySession -> WifiAwareNetworkSpecifier.Builder(ds, peer)
      is SubscribeDiscoverySession -> WifiAwareNetworkSpecifier.Builder(ds, peer)
      else -> return
    }
    val spec = builder
      .setPskPassphrase("cap-aware-psk") // optional extra app-level protection
      .build()

    val req = NetworkRequest.Builder()
      .addTransportType(NetworkCapabilities.TRANSPORT_WIFI_AWARE)
      .setNetworkSpecifier(spec)
      .build()

    conn.requestNetwork(req, object: ConnectivityManager.NetworkCallback(){
      override fun onAvailable(network: Network) {
        thread {
          try {
            val factory = network.socketFactory
            val sock = factory.createSocket() as Socket
            // The Aware IP is abstracted; 0.0.0.0 + port works within the data path
            sock.connect(InetSocketAddress("0.0.0.0", port))
            val pid = peer.hashCode().toString()
            sockets[pid] = sock
            emit("connected", JSObject().put("peer", JSObject().put("id", pid)))
            readLoop(pid, sock)
          } catch (t: Throwable) {
            emit("error", JSObject().put("code","CONNECT").put("message", t.message))
          }
        }
      }
    })
  }

  private fun readLoop(peerId: String, socket: Socket) {
    val input = socket.getInputStream()
    val hdr = ByteArray(9)
    while (!socket.isClosed) {
      val r = input.read(hdr)
      if (r < 9) break
      val kind: Byte = hdr[0]
      val len = ByteBuffer.wrap(hdr,1,8).long
      val body = input.readNBytes(len.toInt())
      val payloadId = UUID.randomUUID().toString()
      when {
        kind == 0.toByte() -> {
          emit("message", JSObject().put("peerId", peerId).put("payloadId", payloadId).put("kind","text").put("text", String(body)))
        }
        kind == 1.toByte() -> {
          val b64 = android.util.Base64.encodeToString(body, android.util.Base64.NO_WRAP)
          emit("message", JSObject().put("peerId", peerId).put("payloadId", payloadId).put("kind","binary").put("dataBase64", b64))
        }
        kind == 2.toByte() -> {
          val meta = JSONObject(String(body))
          val size = meta.optLong("size")
          val name = meta.optString("name","file.bin")
          emit("transferRequest", JSObject()
            .put("payloadId", payloadId)
            .put("peer", JSObject().put("id", peerId))
            .put("kind","file")
            .put("bytesTotal", size)
            .put("displayName", name))
          // store pending state if you want accept/decline gating here
        }
      }
    }
  }

  fun sendMessage(peerId: String, payloadId: String, kind: String, text: String?, b64: String?): Boolean {
    val sock = sockets[peerId] ?: return false
    val out = sock.getOutputStream()
    val (kindByte, body) = when(kind){
      "text" -> 0.toByte() to (text ?: "").toByteArray()
      "binary" -> 1.toByte() to (b64?.let { android.util.Base64.decode(it, android.util.Base64.DEFAULT) } ?: ByteArray(0))
      else -> return false
    }
    val hdr = ByteBuffer.allocate(9).put(kindByte).putLong(body.size.toLong()).array()
    out.write(hdr); out.write(body); out.flush()
    return true
  }

  fun sendFile(peerId: String, payloadId: String, path: String, cb:(Boolean,String?)->Unit) {
    val sock = sockets[peerId] ?: return cb(false,"no-conn")
    thread {
      try {
        val f = File(path)
        val meta = JSONObject().put("name", f.name).put("size", f.length())
        val metaBytes = meta.toString().toByteArray()
        val out = sock.getOutputStream()
        // meta frame
        val hdr = ByteBuffer.allocate(9).put(2.toByte()).putLong(metaBytes.size.toLong()).array()
        out.write(hdr); out.write(metaBytes); out.flush()
        // stream chunks
        var total = 0L
        val buf = ByteArray(64 * 1024)
        FileInputStream(f).use { fis ->
          while (true) {
            val r = fis.read(buf); if (r <= 0) break
            val bh = ByteBuffer.allocate(9).put(1.toByte()).putLong(r.toLong()).array()
            out.write(bh); out.write(buf,0,r); out.flush()
            total += r
            emit("transferProgress", JSObject()
              .put("payloadId", payloadId)
              .put("peerId", peerId)
              .put("bytesTotal", f.length())
              .put("bytesTransferred", total)
              .put("direction","send")
              .put("kind","file"))
          }
        }
        emit("transferCompleted", JSObject()
          .put("payloadId", payloadId)
          .put("peerId", peerId)
          .put("bytesTotal", f.length())
          .put("bytesTransferred", f.length())
          .put("direction","send")
          .put("kind","file"))
        cb(true,null)
      } catch (t: Throwable){ cb(false,t.message) }
    }
  }

  fun acceptIncoming(payloadId: String, accept: Boolean) {
    // implement your gating if you buffered pending transfers
  }
}
