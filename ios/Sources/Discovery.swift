import Foundation

/// 局域网识别：UDP 广播发现 + 监听电视每 2 秒的主动广播
final class Discovery: ObservableObject {

    static let listenPort: UInt16 = 47812
    static let discoverPort: UInt16 = 47810

    @Published var devices: [TVDevice] = []
    @Published var remaining: Int = 0
    @Published var active: Bool = false

    private var fd: Int32 = -1
    private var source: DispatchSourceRead?
    private var countdown: Timer?
    private var resend: Timer?

    deinit {
        closeSocket()
    }

    func start(seconds: Int = 15) {
        stopTimers()
        devices = []
        remaining = seconds
        active = true
        openSocket()
        sendDiscover()
        countdown = Timer.scheduledTimer(withTimeInterval: 1, repeats: true) { [weak self] _ in
            guard let self = self else { return }
            if self.remaining > 0 { self.remaining -= 1 }
            if self.remaining <= 0 { self.stop() }
        }
        resend = Timer.scheduledTimer(withTimeInterval: 1.5, repeats: true) { [weak self] _ in
            self?.sendDiscover()
        }
    }

    /// 重新识别：不清空已识别列表，直接再扫 15 秒
    func rescan(seconds: Int = 15) {
        stopTimers()
        remaining = seconds
        active = true
        if fd < 0 { openSocket() }
        sendDiscover()
        countdown = Timer.scheduledTimer(withTimeInterval: 1, repeats: true) { [weak self] _ in
            guard let self = self else { return }
            if self.remaining > 0 { self.remaining -= 1 }
            if self.remaining <= 0 { self.stop() }
        }
        resend = Timer.scheduledTimer(withTimeInterval: 1.5, repeats: true) { [weak self] _ in
            self?.sendDiscover()
        }
    }

    func stop() {
        active = false
        stopTimers()
    }

    private func stopTimers() {
        countdown?.invalidate()
        countdown = nil
        resend?.invalidate()
        resend = nil
    }

    // MARK: - socket

    private func openSocket() {
        if fd >= 0 { return }
        let s = socket(AF_INET, SOCK_DGRAM, 0)
        guard s >= 0 else { return }
        var yes: Int32 = 1
        setsockopt(s, SOL_SOCKET, SO_REUSEADDR, &yes, socklen_t(MemoryLayout<Int32>.size))
        setsockopt(s, SOL_SOCKET, SO_BROADCAST, &yes, socklen_t(MemoryLayout<Int32>.size))

        if !bindSocket(s, port: Discovery.listenPort) {
            _ = bindSocket(s, port: 0)
        }

        fd = s
        let src = DispatchSource.makeReadSource(fileDescriptor: s, queue: DispatchQueue.global(qos: .userInitiated))
        src.setEventHandler { [weak self] in
            self?.readPending()
        }
        src.setCancelHandler {
            close(s)
        }
        source = src
        src.resume()
    }

    private func closeSocket() {
        source?.cancel()
        source = nil
        fd = -1
    }

    private func bindSocket(_ s: Int32, port: UInt16) -> Bool {
        var addr = sockaddr_in()
        addr.sin_len = UInt8(MemoryLayout<sockaddr_in>.size)
        addr.sin_family = sa_family_t(AF_INET)
        addr.sin_port = in_port_t(port).bigEndian
        addr.sin_addr = in_addr(s_addr: 0)
        let r = withUnsafePointer(to: &addr) { ptr -> Int32 in
            ptr.withMemoryRebound(to: sockaddr.self, capacity: 1) { sa in
                bind(s, sa, socklen_t(MemoryLayout<sockaddr_in>.size))
            }
        }
        return r == 0
    }

    private func sendDiscover() {
        guard fd >= 0 else { return }
        guard let payload = "{\"t\":\"tvpad_discover\",\"ver\":1}".data(using: .utf8) else { return }
        var bc = sockaddr_in()
        bc.sin_len = UInt8(MemoryLayout<sockaddr_in>.size)
        bc.sin_family = sa_family_t(AF_INET)
        bc.sin_port = in_port_t(Discovery.discoverPort).bigEndian
        bc.sin_addr = in_addr(s_addr: 0xFFFF_FFFF)
        let sock = fd
        payload.withUnsafeBytes { raw in
            withUnsafePointer(to: &bc) { ptr in
                ptr.withMemoryRebound(to: sockaddr.self, capacity: 1) { sa in
                    _ = sendto(sock, raw.baseAddress, raw.count, 0, sa,
                               socklen_t(MemoryLayout<sockaddr_in>.size))
                }
            }
        }
    }

    private func readPending() {
        guard fd >= 0 else { return }
        var buf = [UInt8](repeating: 0, count: 2048)
        let n = buf.withUnsafeMutableBytes { recv(fd, $0.baseAddress, 2048, 0) }
        guard n > 0 else { return }
        let data = Data(buf[0..<n])
        guard let any = try? JSONSerialization.jsonObject(with: data),
              let obj = any as? [String: Any] else { return }
        guard (obj["t"] as? String) == "tvpad" else { return }
        let ip = (obj["ip"] as? String) ?? ""
        let port = (obj["port"] as? Int) ?? 47811
        guard !ip.isEmpty else { return }
        let device = TVDevice(
            name: (obj["name"] as? String) ?? "电视",
            model: (obj["model"] as? String) ?? "",
            ip: ip,
            port: port,
            screenW: (obj["sw"] as? Int) ?? 1920,
            screenH: (obj["sh"] as? Int) ?? 1080,
            a11y: (obj["a11y"] as? Bool) ?? false,
            root: (obj["root"] as? Bool) ?? false
        )
        DispatchQueue.main.async {
            if let idx = self.devices.firstIndex(where: { $0.ip == device.ip }) {
                self.devices[idx] = device
            } else {
                self.devices.append(device)
            }
        }
    }
}
