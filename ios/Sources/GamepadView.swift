import SwiftUI
import UIKit

struct PadButtonView: View {
    let button: PadButton
    let diameter: CGFloat
    let onDown: () -> Void
    let onUp: () -> Void

    @State private var pressed = false

    private var fill: Color {
        switch button.tint {
        case "green": return Color(red: 59.0 / 255, green: 227.0 / 255, blue: 142.0 / 255)
        case "red": return Color(red: 255.0 / 255, green: 98.0 / 255, blue: 98.0 / 255)
        case "blue": return Color(red: 86.0 / 255, green: 156.0 / 255, blue: 255.0 / 255)
        case "yellow": return Color(red: 242.0 / 255, green: 201.0 / 255, blue: 76.0 / 255)
        default: return Color(white: 0.55)
        }
    }

    var body: some View {
        Text(button.label)
            .font(.system(size: max(10, diameter * 0.24), weight: .bold))
            .foregroundColor(.white)
            .lineLimit(2)
            .minimumScaleFactor(0.5)
            .frame(width: diameter, height: diameter)
            .background(
                Circle()
                    .fill(fill.opacity(pressed ? 0.92 : 0.45))
                    .overlay(Circle().stroke(Color.white.opacity(0.35), lineWidth: 1.5))
            )
            .scaleEffect(pressed ? 1.06 : 1.0)
            .contentShape(Circle())
            .gesture(
                DragGesture(minimumDistance: 0)
                    .onChanged { _ in
                        if !pressed {
                            pressed = true
                            onDown()
                        }
                    }
                    .onEnded { _ in
                        if pressed {
                            pressed = false
                            onUp()
                        }
                    }
            )
            .animation(.easeOut(duration: 0.08), value: pressed)
    }
}

struct StickView: View {
    let diameter: CGFloat
    let onVector: (CGVector) -> Void

    @State private var offset: CGSize = .zero

    var body: some View {
        ZStack {
            Circle()
                .fill(Color.white.opacity(0.08))
            Circle()
                .stroke(Color.white.opacity(0.22), lineWidth: 1.5)
            Circle()
                .fill(Palette.green.opacity(0.55))
                .frame(width: diameter * 0.40, height: diameter * 0.40)
                .overlay(Circle().stroke(Color.white.opacity(0.35), lineWidth: 1))
                .offset(offset)
        }
        .frame(width: diameter, height: diameter)
        .contentShape(Circle())
        .gesture(
            DragGesture(minimumDistance: 0)
                .onChanged { value in
                    let maxR = diameter / 2
                    var dx = value.translation.width
                    var dy = value.translation.height
                    let len = sqrt(dx * dx + dy * dy)
                    if len > maxR && len > 0 {
                        dx = dx / len * maxR
                        dy = dy / len * maxR
                    }
                    offset = CGSize(width: dx, height: dy)
                    onVector(CGVector(dx: dx / maxR, dy: dy / maxR))
                }
                .onEnded { _ in
                    offset = .zero
                    onVector(.zero)
                }
        )
    }
}

struct GamepadView: View {
    @ObservedObject var model: AppModel
    @State private var lastDrag = Date.distantPast

    var body: some View {
        GeometryReader { geo in
            let w = geo.size.width
            let h = geo.size.height
            let base = min(w, h)

            ZStack {
                Palette.bg.ignoresSafeArea()

                ForEach(Array(model.preset.buttons.enumerated()), id: \.offset) { index, btn in
                    let dia = CGFloat(btn.size) * base
                    if btn.kind == .stick {
                        StickView(diameter: dia) { vector in
                            sendDrag(btn, vector: vector)
                        }
                        .position(x: CGFloat(btn.x) * w, y: CGFloat(btn.y) * h)
                    } else {
                        PadButtonView(
                            button: btn,
                            diameter: dia,
                            onDown: {
                                model.client.send([
                                    "t": "press",
                                    "id": index,
                                    "x": btn.x,
                                    "y": btn.y,
                                    "hold": btn.kind == .hold
                                ])
                            },
                            onUp: {
                                model.client.send(["t": "release", "id": index])
                            }
                        )
                        .position(x: CGFloat(btn.x) * w, y: CGFloat(btn.y) * h)
                    }
                }

                VStack {
                    HStack(spacing: 10) {
                        Button {
                            model.client.disconnect()
                            model.screen = .home
                        } label: {
                            HStack(spacing: 4) {
                                Image(systemName: "chevron.left")
                                    .font(.system(size: 11, weight: .bold))
                                Text("断开")
                                    .font(.system(size: 12, weight: .semibold))
                            }
                            .foregroundColor(Palette.dim)
                            .padding(.horizontal, 10)
                            .padding(.vertical, 6)
                            .background(Capsule().fill(Palette.panel.opacity(0.85)))
                        }
                        .buttonStyle(.plain)

                        Spacer()

                        Menu {
                            ForEach(0..<Presets.all.count, id: \.self) { i in
                                Button(Presets.all[i].name) {
                                    model.presetIndex = i
                                }
                            }
                        } label: {
                            VStack(spacing: 1) {
                                Text("如果不适配，请选择")
                                    .font(.system(size: 10))
                                    .foregroundColor(Palette.dim)
                                HStack(spacing: 4) {
                                    Text(model.preset.name)
                                        .font(.system(size: 12, weight: .semibold))
                                        .foregroundColor(Palette.green)
                                    Image(systemName: "chevron.down")
                                        .font(.system(size: 9, weight: .bold))
                                        .foregroundColor(Palette.green)
                                }
                            }
                            .padding(.horizontal, 12)
                            .padding(.vertical, 5)
                            .background(Capsule().fill(Palette.panel.opacity(0.9)))
                            .overlay(Capsule().stroke(Palette.green.opacity(0.35), lineWidth: 1))
                        }

                        Spacer()

                        HStack(spacing: 5) {
                            Circle()
                                .fill(model.client.connected ? Palette.green : Palette.red)
                                .frame(width: 6, height: 6)
                            Text(model.client.device?.name ?? "未连接")
                                .font(.system(size: 11))
                                .foregroundColor(Palette.dim)
                                .lineLimit(1)
                        }
                        .padding(.horizontal, 10)
                        .padding(.vertical, 6)
                        .background(Capsule().fill(Palette.panel.opacity(0.85)))
                    }
                    .padding(.horizontal, 14)
                    .padding(.top, 8)

                    Spacer()
                }

                if !model.client.connected, let device = model.client.device {
                    VStack(spacing: 12) {
                        ProgressView()
                            .progressViewStyle(CircularProgressViewStyle(tint: Palette.green))
                        Text("正在连接 \(device.name)…")
                            .font(.system(size: 13))
                            .foregroundColor(.white)
                        Button("重新连接") {
                            model.open(device)
                        }
                        .font(.system(size: 12, weight: .semibold))
                        .foregroundColor(Palette.green)
                        .buttonStyle(.plain)
                    }
                    .padding(24)
                    .background(RoundedRectangle(cornerRadius: 16).fill(Palette.panel.opacity(0.92)))
                }
            }
        }
        .onAppear { UIApplication.shared.isIdleTimerDisabled = true }
        .onDisappear { UIApplication.shared.isIdleTimerDisabled = false }
    }

    private func sendDrag(_ btn: PadButton, vector: CGVector) {
        let now = Date()
        if now.timeIntervalSince(lastDrag) < 0.06 && vector != CGVector(dx: 0, dy: 0) { return }
        lastDrag = now
        let radius = 0.085
        let x2 = min(0.99, max(0.01, btn.x + Double(vector.dx) * radius))
        let y2 = min(0.99, max(0.01, btn.y + Double(vector.dy) * radius))
        model.client.send([
            "t": "drag",
            "x1": btn.x,
            "y1": btn.y,
            "x2": x2,
            "y2": y2,
            "ms": 140
        ])
    }
}
