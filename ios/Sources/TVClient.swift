import Foundation
import Network

/// 和电视的 TCP 长连接，每行一条 JSON 指令
final class TVClient: ObservableObject {

    @Published var connected = false
    @Published var device: TVDevice?

    private var conn: NWConnection?
    private let queue = DispatchQueue(label: "com.moustache.tvpad.tcp")

    func connect(_ device: TVDevice, completion: @escaping (Bool) -> Void = { _ in }) {
        disconnect()
        guard let port = NWEndpoint.Port(rawValue: UInt16(device.port)) else {
            completion(false)
            return
        }
        let c = NWConnection(host: NWEndpoint.Host(device.ip), port: port, using: .tcp)
        conn = c
        self.device = device
        c.stateUpdateHandler = { [weak self] state in
            DispatchQueue.main.async {
                switch state {
                case .ready:
                    self?.connected = true
                    completion(true)
                case .failed, .cancelled, .waiting:
                    self?.connected = false
                default:
                    break
                }
            }
        }
        c.start(queue: queue)
    }

    func disconnect() {
        conn?.cancel()
        conn = nil
        connected = false
    }

    func send(_ obj: [String: Any]) {
        guard let c = conn else { return }
        guard let data = try? JSONSerialization.data(withJSONObject: obj) else { return }
        var payload = data
        payload.append(0x0A)
        c.send(content: payload, completion: .contentProcessed({ _ in }))
    }
}
