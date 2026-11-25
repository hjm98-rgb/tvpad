import SwiftUI

enum Palette {
    static let bg = Color(red: 11.0 / 255, green: 15.0 / 255, blue: 20.0 / 255)
    static let panel = Color(red: 20.0 / 255, green: 28.0 / 255, blue: 38.0 / 255)
    static let panel2 = Color(red: 28.0 / 255, green: 38.0 / 255, blue: 50.0 / 255)
    static let green = Color(red: 59.0 / 255, green: 227.0 / 255, blue: 142.0 / 255)
    static let dim = Color(red: 124.0 / 255, green: 138.0 / 255, blue: 153.0 / 255)
    static let yellow = Color(red: 242.0 / 255, green: 201.0 / 255, blue: 76.0 / 255)
    static let red = Color(red: 255.0 / 255, green: 107.0 / 255, blue: 107.0 / 255)
}

@main
struct TVPadApp: App {
    var body: some Scene {
        WindowGroup {
            RootView()
                .preferredColorScheme(.dark)
        }
    }
}

struct RootView: View {
    @StateObject private var model = AppModel()

    var body: some View {
        ZStack {
            Palette.bg.ignoresSafeArea()
            switch model.screen {
            case .discover:
                DiscoverView(model: model, discovery: model.discovery)
            case .home:
                HomeView(model: model, discovery: model.discovery)
            case .devices:
                DevicesView(model: model)
            case .pad:
                GamepadView(model: model)
            }
            if let toast = model.toast {
                VStack {
                    Spacer()
                    Text(toast)
                        .font(.system(size: 14, weight: .medium))
                        .padding(.horizontal, 18)
                        .padding(.vertical, 10)
                        .background(Capsule().fill(Palette.panel2))
                        .foregroundColor(.white)
                        .padding(.bottom, 22)
                        .transition(.opacity)
                }
                .animation(.easeInOut(duration: 0.25), value: model.toast)
            }
        }
        .background(Palette.bg)
    }
}

// MARK: - 通用小组件

struct TVBackButton: View {
    var title: String = "返回"
    var action: () -> Void

    var body: some View {
        Button(action: action) {
            HStack(spacing: 5) {
                Image(systemName: "chevron.left")
                    .font(.system(size: 13, weight: .bold))
                Text(title)
                    .font(.system(size: 14, weight: .medium))
            }
            .foregroundColor(Palette.green)
            .padding(.horizontal, 14)
            .padding(.vertical, 8)
            .background(RoundedRectangle(cornerRadius: 10).fill(Palette.panel))
        }
        .buttonStyle(.plain)
    }
}

struct TVCard<Content: View>: View {
    var content: () -> Content

    var body: some View {
        content()
            .background(RoundedRectangle(cornerRadius: 16).fill(Palette.panel))
            .overlay(RoundedRectangle(cornerRadius: 16).stroke(Color.white.opacity(0.08), lineWidth: 1))
    }
}

// MARK: - 识别页

struct DiscoverView: View {
    @ObservedObject var model: AppModel
    @ObservedObject var discovery: Discovery

    var body: some View {
        GeometryReader { geo in
            let tall = geo.size.height
            VStack(alignment: .leading, spacing: 0) {
                HStack {
                    TVBackButton(title: "主界面") { model.screen = .home }
                    Spacer()
                    Text("局域网识别")
                        .font(.system(size: 12, weight: .medium))
                        .foregroundColor(Palette.dim)
                }
                .padding(.top, 10)

                HStack(alignment: .firstTextBaseline, spacing: 10) {
                    Text(discovery.active ? "正在识别设备" : "识别结束")
                        .font(.system(size: 26, weight: .bold))
                        .foregroundColor(.white)
                    Text(discovery.active ? "\(discovery.remaining) 秒" : "")
                        .font(.system(size: 18, weight: .semibold))
                        .foregroundColor(Palette.green)
                }
                .padding(.top, 14)

                Text("电视和手机必须连同一个 WiFi。电视上打开「TV 手柄」，等它显示正在等待识别。")
                    .font(.system(size: 13))
                    .foregroundColor(Palette.dim)
                    .padding(.top, 6)

                GeometryReader { barGeo in
                    ZStack(alignment: .leading) {
                        Capsule().fill(Color.white.opacity(0.08))
                        Capsule()
                            .fill(Palette.green)
                            .frame(width: barGeo.size.width * CGFloat(discovery.remaining) / 15.0)
                    }
                }
                .frame(height: 4)
                .padding(.top, 12)

                HStack(spacing: 12) {
                    Button {
                        discovery.rescan()
                    } label: {
                        HStack(spacing: 6) {
                            Image(systemName: "arrow.clockwise")
                            Text("重新识别")
                        }
                        .font(.system(size: 14, weight: .semibold))
                        .foregroundColor(.white)
                        .padding(.horizontal, 18)
                        .padding(.vertical, 9)
                        .background(Capsule().fill(Palette.green.opacity(0.22)))
                        .overlay(Capsule().stroke(Palette.green.opacity(0.6), lineWidth: 1))
                    }
                    .buttonStyle(.plain)
                    Text("识别到 \(discovery.devices.count) 台电视")
                        .font(.system(size: 13))
                        .foregroundColor(Palette.dim)
                }
                .padding(.top, 14)

                ScrollView {
                    LazyVStack(spacing: 10) {
                        if discovery.devices.isEmpty {
                            VStack(spacing: 10) {
                                Image(systemName: "tv")
                                    .font(.system(size: 34))
                                    .foregroundColor(Palette.dim.opacity(0.6))
                                Text(discovery.active ? "还没有发现电视，别急，让它再识别一会儿" : "没识别到。点「重新识别」，确认同一 WiFi")
                                    .font(.system(size: 13))
                                    .foregroundColor(Palette.dim)
                            }
                            .frame(maxWidth: .infinity, minHeight: tall * 0.35)
                        }
                        ForEach(discovery.devices) { device in
                            Button {
                                model.add(device)
                            } label: {
                                HStack(spacing: 14) {
                                    ZStack {
                                        Circle().fill(Palette.green.opacity(0.18)).frame(width: 40, height: 40)
                                        Image(systemName: "tv.fill")
                                            .foregroundColor(Palette.green)
                                    }
                                    VStack(alignment: .leading, spacing: 3) {
                                        Text(device.name)
                                            .font(.system(size: 16, weight: .semibold))
                                            .foregroundColor(.white)
                                        Text("\(device.ip):\(device.port) · \(device.screenW)×\(device.screenH)")
                                            .font(.system(size: 12))
                                            .foregroundColor(Palette.dim)
                                    }
                                    Spacer()
                                    Text(device.a11y ? "可操控" : "未开权限")
                                        .font(.system(size: 11, weight: .medium))
                                        .foregroundColor(device.a11y ? Palette.green : Palette.yellow)
                                    Image(systemName: "plus.circle.fill")
                                        .font(.system(size: 22))
                                        .foregroundColor(Palette.green)
                                }
                                .padding(.horizontal, 16)
                                .padding(.vertical, 12)
                            }
                            .buttonStyle(.plain)
                            .background(RoundedRectangle(cornerRadius: 14).fill(Palette.panel))
                            .overlay(RoundedRectangle(cornerRadius: 14).stroke(Color.white.opacity(0.07), lineWidth: 1))
                        }
                    }
                    .padding(.top, 14)
                }
                .padding(.bottom, 10)
            }
            .padding(.horizontal, 18)
        }
    }
}

// MARK: - 主界面

struct HomeView: View {
    @ObservedObject var model: AppModel
    @ObservedObject var discovery: Discovery

    var body: some View {
        GeometryReader { geo in
            ZStack(alignment: .topTrailing) {
                VStack(spacing: 0) {
                    Spacer(minLength: 8)

                    ZStack {
                        Circle()
                            .fill(Palette.green.opacity(0.14))
                            .frame(width: 86, height: 86)
                        Image(systemName: "tv.and.mediabox")
                            .font(.system(size: 38))
                            .foregroundColor(Palette.green)
                    }

                    Text("TV 手柄")
                        .font(.system(size: 32, weight: .bold))
                        .foregroundColor(.white)
                        .padding(.top, 14)

                    Text("手机变手柄 · 电视大屏玩")
                        .font(.system(size: 13))
                        .foregroundColor(Palette.dim)
                        .padding(.top, 4)

                    Button {
                        model.screen = .discover
                        discovery.rescan()
                    } label: {
                        Text("开始识别设备")
                            .font(.system(size: 16, weight: .semibold))
                            .foregroundColor(Palette.bg)
                            .frame(maxWidth: 260)
                            .padding(.vertical, 13)
                            .background(Capsule().fill(Palette.green))
                    }
                    .buttonStyle(.plain)
                    .padding(.top, 22)

                    Text("当前按键预设：\(model.preset.name)")
                        .font(.system(size: 12))
                        .foregroundColor(Palette.dim)

                    Spacer()

                    Text("电视端 App 保持打开，手机会自动找到它")
                        .font(.system(size: 11))
                        .foregroundColor(Palette.dim.opacity(0.8))
                        .padding(.bottom, 10)
                }
                .frame(maxWidth: .infinity)

                Button {
                    model.screen = .devices
                } label: {
                    HStack(spacing: 6) {
                        Image(systemName: "list.bullet.rectangle")
                            .font(.system(size: 13, weight: .semibold))
                        Text("已添加设备 \(model.savedDevices.count)")
                            .font(.system(size: 13, weight: .semibold))
                    }
                    .foregroundColor(Palette.green)
                    .padding(.horizontal, 14)
                    .padding(.vertical, 8)
                    .background(Capsule().fill(Palette.panel))
                    .overlay(Capsule().stroke(Color.white.opacity(0.08), lineWidth: 1))
                }
                .buttonStyle(.plain)
                .padding(.top, 10)
                .padding(.trailing, 16)
            }
        }
    }
}

// MARK: - 已添加设备

struct DevicesView: View {
    @ObservedObject var model: AppModel

    var body: some View {
        GeometryReader { geo in
            VStack(alignment: .leading, spacing: 0) {
                HStack {
                    TVBackButton { model.screen = .home }
                    Spacer()
                    Text("已添加的设备")
                        .font(.system(size: 15, weight: .semibold))
                        .foregroundColor(.white)
                    Spacer()
                    Color.clear.frame(width: 74, height: 1)
                }
                .padding(.top, 10)

                if model.savedDevices.isEmpty {
                    VStack(spacing: 10) {
                        Image(systemName: "tray")
                            .font(.system(size: 32))
                            .foregroundColor(Palette.dim.opacity(0.6))
                        Text("还没有添加设备，回主界面点「开始识别设备」")
                            .font(.system(size: 13))
                            .foregroundColor(Palette.dim)
                    }
                    .frame(maxWidth: .infinity, maxHeight: .infinity)
                } else {
                    ScrollView {
                        LazyVStack(spacing: 10) {
                            ForEach(model.savedDevices) { device in
                                HStack(spacing: 12) {
                                    Button {
                                        model.backToPad(device)
                                    } label: {
                                        HStack(spacing: 12) {
                                            ZStack {
                                                Circle().fill(Palette.green.opacity(0.16)).frame(width: 38, height: 38)
                                                Image(systemName: "tv.fill").foregroundColor(Palette.green)
                                            }
                                            VStack(alignment: .leading, spacing: 3) {
                                                Text(device.name)
                                                    .font(.system(size: 15, weight: .semibold))
                                                    .foregroundColor(.white)
                                                Text(device.ip)
                                                    .font(.system(size: 12))
                                                    .foregroundColor(Palette.dim)
                                            }
                                            Spacer()
                                            Text("连接")
                                                .font(.system(size: 12, weight: .semibold))
                                                .foregroundColor(Palette.bg)
                                                .padding(.horizontal, 12)
                                                .padding(.vertical, 6)
                                                .background(Capsule().fill(Palette.green))
                                        }
                                    }
                                    .buttonStyle(.plain)

                                    Button {
                                        model.remove(device)
                                    } label: {
                                        Image(systemName: "trash")
                                            .font(.system(size: 13))
                                            .foregroundColor(Palette.red.opacity(0.85))
                                            .padding(8)
                                    }
                                    .buttonStyle(.plain)
                                }
                                .padding(.horizontal, 14)
                                .padding(.vertical, 10)
                                .background(RoundedRectangle(cornerRadius: 14).fill(Palette.panel))
                            }
                        }
                        .padding(.top, 14)
                    }
                }
            }
            .padding(.horizontal, 18)
        }
    }
}
