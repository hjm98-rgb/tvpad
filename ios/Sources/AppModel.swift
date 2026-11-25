import SwiftUI
import Combine

final class AppModel: ObservableObject {

    enum Screen {
        case discover
        case home
        case devices
        case pad
    }

    @Published var screen: Screen = .discover
    @Published var savedDevices: [TVDevice] = []
    @Published var presetIndex: Int = 0
    @Published var toast: String?

    let discovery = Discovery()
    let client = TVClient()

    private var bag = Set<AnyCancellable>()
    private let storeKey = "tvpad.saved.devices"

    var preset: PadPreset { Presets.all[presetIndex] }

    init() {
        loadSaved()
        discovery.objectWillChange
            .sink { [weak self] _ in self?.objectWillChange.send() }
            .store(in: &bag)
        client.objectWillChange
            .sink { [weak self] _ in self?.objectWillChange.send() }
            .store(in: &bag)
        discovery.start()
    }

    func add(_ device: TVDevice) {
        if let idx = savedDevices.firstIndex(where: { $0.ip == device.ip }) {
            savedDevices[idx] = device
        } else {
            savedDevices.append(device)
        }
        save()
        toast = "已添加「\(device.name)」"
        screen = .home
        DispatchQueue.main.asyncAfter(deadline: .now() + 2) { [weak self] in
            if self?.toast != nil { self?.toast = nil }
        }
    }

    func remove(_ device: TVDevice) {
        savedDevices.removeAll { $0.ip == device.ip }
        save()
    }

    func open(_ device: TVDevice) {
        client.connect(device) { ok in
            if !ok {
                self.toast = "连接失败，检查电视 App 是否打开、是否同一 WiFi"
                DispatchQueue.main.asyncAfter(deadline: .now() + 2.5) { self.toast = nil }
                return
            }
            self.screen = .pad
        }
    }

    func backToPad(_ device: TVDevice) {
        if client.connected && client.device?.ip == device.ip {
            screen = .pad
        } else {
            open(device)
        }
    }

    private func save() {
        if let data = try? JSONEncoder().encode(savedDevices) {
            UserDefaults.standard.set(data, forKey: storeKey)
        }
    }

    private func loadSaved() {
        if let data = UserDefaults.standard.data(forKey: storeKey),
           let list = try? JSONDecoder().decode([TVDevice].self, from: data) {
            savedDevices = list
        }
    }
}
