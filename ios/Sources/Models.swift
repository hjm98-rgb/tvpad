import Foundation

struct TVDevice: Identifiable, Codable, Equatable {
    var id: String { "\(ip):\(port)" }
    var name: String
    var model: String
    var ip: String
    var port: Int
    var screenW: Int = 1920
    var screenH: Int = 1080
    var a11y: Bool = false
    var root: Bool = false
}

enum ButtonKind: String, Codable {
    case tap
    case hold
    case stick
}

struct PadButton: Identifiable {
    var id: String { label + "\(x)-\(y)" }
    var label: String
    var kind: ButtonKind
    /// 归一化屏幕坐标（0~1），手机把按键位置直接映射成电视上的触屏点
    var x: Double
    var y: Double
    /// 直径，相对 min(宽, 高)
    var size: Double
    var tint: String = "normal"
    /// 手机上画在哪儿（不填就跟电视坐标一致），-1 表示沿用 x/y
    var px: Double = -1
    var py: Double = -1
}

struct PadPreset: Identifiable {
    var id: String { name }
    var name: String
    var note: String
    var buttons: [PadButton]
}

enum Presets {

    static let all: [PadPreset] = [universal, minecraftJava, minecraftBedrock, undertale, emulator]

    static let universal = PadPreset(
        name: "通用手柄",
        note: "模拟器 / 通用触屏游戏，A·B·X·Y + 摇杆",
        buttons: [
            b("移动", .stick, 0.15, 0.62, 0.34),
            b("A", .tap, 0.88, 0.70, 0.15, "green"),
            b("B", .tap, 0.78, 0.85, 0.15, "red"),
            b("X", .tap, 0.78, 0.55, 0.15, "blue"),
            b("Y", .tap, 0.68, 0.70, 0.15, "yellow"),
            b("L1", .tap, 0.10, 0.12, 0.10),
            b("R1", .tap, 0.90, 0.12, 0.10),
            b("START", .tap, 0.50, 0.90, 0.10),
            b("SELECT", .tap, 0.40, 0.90, 0.09)
        ]
    )

    static let minecraftJava = PadPreset(
        name: "我的世界 Java 版",
        note: "PojavLauncher 默认控件：左下 W/A/S/D 十字键、右下跳跃(空格)、十字中心潜行(Shift)、点屏幕=攻击",
        buttons: [
            // 电视坐标全部按 Pojav 默认布局公式算出：控件 100px、边距 20px、屏幕 1920x1080
            PadButton(label: "上", kind: .hold, x: 0.099, y: 0.713, size: 0.14, tint: "normal", px: 0.12, py: 0.30),
            PadButton(label: "下", kind: .hold, x: 0.099, y: 0.935, size: 0.14, tint: "normal", px: 0.12, py: 0.52),
            PadButton(label: "左", kind: .hold, x: 0.037, y: 0.824, size: 0.14, tint: "normal", px: 0.045, py: 0.41),
            PadButton(label: "右", kind: .hold, x: 0.161, y: 0.824, size: 0.14, tint: "normal", px: 0.195, py: 0.41),
            PadButton(label: "潜行", kind: .hold, x: 0.099, y: 0.824, size: 0.13, tint: "blue", px: 0.30, py: 0.41),
            PadButton(label: "跳跃", kind: .hold, x: 0.901, y: 0.824, size: 0.15, tint: "green", px: 0.87, py: 0.66),
            PadButton(label: "背包", kind: .tap, x: 0.161, y: 0.935, size: 0.12, tint: "yellow", px: 0.90, py: 0.28),
            PadButton(label: "攻击", kind: .tap, x: 0.55, y: 0.45, size: 0.16, tint: "green", px: 0.62, py: 0.30),
            PadButton(label: "使用", kind: .hold, x: 0.70, y: 0.45, size: 0.14, tint: "blue", px: 0.80, py: 0.30),
            PadButton(label: "视角", kind: .stick, x: 0.55, y: 0.55, size: 0.26, px: 0.55, py: 0.62)
        ]
    )

    static let minecraftBedrock = PadPreset(
        name: "我的世界 基岩版",
        note: "按基岩版手机触屏默认控件实测位置：左摇杆 + 右侧竖排跳/潜/挖/放",
        buttons: [
            b("移动", .stick, 0.12, 0.60, 0.34),
            b("跳跃", .hold, 0.86, 0.31, 0.15, "green"),
            b("潜行", .hold, 0.86, 0.45, 0.15),
            b("挖掘", .hold, 0.86, 0.58, 0.15, "blue"),
            b("放置", .hold, 0.90, 0.73, 0.15, "yellow"),
            b("背包", .tap, 0.95, 0.12, 0.11, "red"),
            b("换栏", .tap, 0.50, 0.96, 0.11)
        ]
    )

    static let undertale = PadPreset(
        name: "传说之下 Undertale",
        note: "按安卓版默认布局实测：左下方向键 + 右侧 Z 确认 / X 取消 / C 菜单",
        buttons: [
            b("上", .tap, 0.13, 0.56, 0.14),
            b("下", .tap, 0.13, 0.82, 0.14),
            b("左", .tap, 0.06, 0.69, 0.14),
            b("右", .tap, 0.20, 0.69, 0.14),
            b("Z 确认", .tap, 0.82, 0.86, 0.16, "green"),
            b("X 取消", .tap, 0.85, 0.71, 0.15, "red"),
            b("C 菜单", .tap, 0.88, 0.56, 0.14, "blue"),
            b("返回", .tap, 0.05, 0.09, 0.10, "yellow")
        ]
    )

    static let emulator = PadPreset(
        name: "模拟器通用",
        note: "GBA / PS / FC 模拟器：十字键 + A·B + 连发 + L·R",
        buttons: [
            b("上", .tap, 0.22, 0.34, 0.12),
            b("下", .tap, 0.22, 0.74, 0.12),
            b("左", .tap, 0.10, 0.54, 0.12),
            b("右", .tap, 0.34, 0.54, 0.12),
            b("A", .tap, 0.78, 0.76, 0.15, "green"),
            b("B", .tap, 0.89, 0.58, 0.15, "red"),
            b("连发A", .hold, 0.66, 0.89, 0.12, "yellow"),
            b("L", .tap, 0.10, 0.12, 0.10),
            b("R", .tap, 0.90, 0.12, 0.10),
            b("SELECT", .tap, 0.44, 0.91, 0.09),
            b("START", .tap, 0.58, 0.10, 0.09)
        ]
    )

    private static func b(_ label: String, _ kind: ButtonKind,
                          _ x: Double, _ y: Double, _ size: Double,
                          _ tint: String = "normal") -> PadButton {
        PadButton(label: label, kind: kind, x: x, y: y, size: size, tint: tint)
    }
}
