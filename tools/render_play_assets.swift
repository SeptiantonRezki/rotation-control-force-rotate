import AppKit
import CoreGraphics

func color(_ hex: UInt32, alpha: CGFloat = 1) -> NSColor {
    NSColor(calibratedRed: CGFloat((hex >> 16) & 0xff) / 255,
            green: CGFloat((hex >> 8) & 0xff) / 255,
            blue: CGFloat(hex & 0xff) / 255,
            alpha: alpha)
}

func gradient(_ colors: [NSColor], rect: NSRect, angle: CGFloat = 90) {
    NSGradient(colors: colors)!.draw(in: rect, angle: angle)
}

func drawText(_ text: String, at point: NSPoint, size: CGFloat,
              color textColor: NSColor, weight: NSFont.Weight = .regular) {
    let font = NSFont.systemFont(ofSize: size, weight: weight)
    (text as NSString).draw(at: point, withAttributes: [
        .font: font,
        .foregroundColor: textColor,
        .kern: size > 30 ? 0.2 : 0.4
    ])
}

func drawGlow(in ctx: CGContext, center: CGPoint, radius: CGFloat,
              color glowColor: NSColor, alpha: CGFloat) {
    let components = glowColor.usingColorSpace(.deviceRGB)!.cgColor.components!
    let start = CGColor(colorSpace: CGColorSpaceCreateDeviceRGB(),
                        components: [components[0], components[1], components[2], alpha])!
    let end = CGColor(colorSpace: CGColorSpaceCreateDeviceRGB(),
                      components: [components[0], components[1], components[2], 0])!
    let g = CGGradient(colorsSpace: CGColorSpaceCreateDeviceRGB(), colors: [start, end] as CFArray, locations: [0, 1])!
    ctx.drawRadialGradient(g, startCenter: center, startRadius: 0,
                           endCenter: center, endRadius: radius,
                           options: [.drawsAfterEndLocation])
}

func savePNG(size: NSSize, output: String, drawing: () -> Void) throws {
    let rep = NSBitmapImageRep(bitmapDataPlanes: nil, pixelsWide: Int(size.width), pixelsHigh: Int(size.height),
                               bitsPerSample: 8, samplesPerPixel: 4, hasAlpha: true,
                               isPlanar: false, colorSpaceName: .deviceRGB,
                               bytesPerRow: 0, bitsPerPixel: 0)!
    rep.size = size
    let graphics = NSGraphicsContext(bitmapImageRep: rep)!
    NSGraphicsContext.saveGraphicsState()
    NSGraphicsContext.current = graphics
    graphics.imageInterpolation = .high
    graphics.shouldAntialias = true
    drawing()
    graphics.flushGraphics()
    NSGraphicsContext.restoreGraphicsState()
    try rep.representation(using: .png, properties: [:])!.write(to: URL(fileURLWithPath: output), options: .atomic)
}

func drawLogo(_ size: CGFloat) {
    let bounds = NSRect(x: 0, y: 0, width: size, height: size)
    let scale = size / 512
    NSGraphicsContext.current!.cgContext.scaleBy(x: scale, y: scale)
    gradient([color(0x0D0B21), color(0x17102F), color(0x3B1E68)], rect: NSRect(x: 0, y: 0, width: 512, height: 512), angle: 38)
    let ctx = NSGraphicsContext.current!.cgContext
    drawGlow(in: ctx, center: CGPoint(x: 370, y: 350), radius: 200, color: color(0x5EEAD4), alpha: 0.11)
    drawGlow(in: ctx, center: CGPoint(x: 118, y: 112), radius: 190, color: color(0xD8B4FE), alpha: 0.13)

    let upper = NSBezierPath()
    upper.move(to: NSPoint(x: 130, y: 298))
    upper.curve(to: NSPoint(x: 380, y: 356), controlPoint1: NSPoint(x: 168, y: 430), controlPoint2: NSPoint(x: 303, y: 433))
    upper.lineWidth = 22; upper.lineCapStyle = .round
    color(0xD8B4FE).setStroke(); upper.stroke()
    let upArrow = NSBezierPath(); upArrow.move(to: NSPoint(x: 367, y: 398)); upArrow.line(to: NSPoint(x: 422, y: 363)); upArrow.line(to: NSPoint(x: 360, y: 340)); upArrow.close()
    color(0x5EEAD4).setFill(); upArrow.fill()

    let lower = NSBezierPath()
    lower.move(to: NSPoint(x: 382, y: 212))
    lower.curve(to: NSPoint(x: 132, y: 156), controlPoint1: NSPoint(x: 343, y: 80), controlPoint2: NSPoint(x: 197, y: 75))
    lower.lineWidth = 22; lower.lineCapStyle = .round
    color(0x5EEAD4).setStroke(); lower.stroke()
    let downArrow = NSBezierPath(); downArrow.move(to: NSPoint(x: 147, y: 114)); downArrow.line(to: NSPoint(x: 90, y: 149)); downArrow.line(to: NSPoint(x: 153, y: 172)); downArrow.close()
    color(0xD8B4FE).setFill(); downArrow.fill()

    let phone = NSBezierPath(roundedRect: NSRect(x: 190, y: 146, width: 132, height: 220), xRadius: 27, yRadius: 27)
    color(0x241D3D, alpha: 0.96).setFill(); phone.fill()
    color(0xE6FFFFFF).setStroke(); phone.lineWidth = 7; phone.stroke()
    let ear = NSBezierPath(roundedRect: NSRect(x: 230, y: 333, width: 52, height: 7), xRadius: 4, yRadius: 4)
    color(0xD8B4FE).setFill(); ear.fill()
    let home = NSBezierPath(ovalIn: NSRect(x: 248, y: 165, width: 16, height: 16))
    color(0x5EEAD4).setFill(); home.fill()
    _ = bounds
}

func drawBanner() {
    let bounds = NSRect(x: 0, y: 0, width: 1024, height: 500)
    gradient([color(0x0D0B21), color(0x17102F), color(0x241445)], rect: bounds, angle: 90)
    let ctx = NSGraphicsContext.current!.cgContext
    drawGlow(in: ctx, center: CGPoint(x: 814, y: 257), radius: 300, color: color(0x5EEAD4), alpha: 0.15)
    drawGlow(in: ctx, center: CGPoint(x: 240, y: 490), radius: 250, color: color(0x8B5CF6), alpha: 0.12)

    // Fine, low-contrast orbital detail keeps the art calm while giving it depth.
    for (x, y, r) in [(78.0, 427.0, 3.0), (594.0, 420.0, 2.2), (925.0, 88.0, 2.5), (665.0, 164.0, 2.0)] {
        let dot = NSBezierPath(ovalIn: NSRect(x: x, y: y, width: r * 2, height: r * 2))
        color(0xE6FFFFFF, alpha: 0.48).setFill(); dot.fill()
    }

    drawText("ROTATION CONTROL", at: NSPoint(x: 76, y: 352), size: 47, color: color(0xF5F0FF), weight: .bold)
    drawText("Force Rotate", at: NSPoint(x: 79, y: 302), size: 29, color: color(0xD8B4FE), weight: .semibold)
    drawText("Choose the screen orientation that works for you.", at: NSPoint(x: 80, y: 251), size: 19, color: color(0xD2C9E0))

    let pills: [(String, CGFloat)] = [("9 ROTATION MODES", 193), ("PER-APP PROFILES", 191), ("RESTORE ON BOOT", 185)]
    var x: CGFloat = 80
    for (label, width) in pills {
        let pill = NSBezierPath(roundedRect: NSRect(x: x, y: 111, width: width, height: 42), xRadius: 21, yRadius: 21)
        color(0xFFFFFF, alpha: 0.075).setFill(); pill.fill()
        color(0xD8B4FE, alpha: 0.34).setStroke(); pill.lineWidth = 1; pill.stroke()
        let font = NSFont.systemFont(ofSize: 11, weight: .bold)
        let attrs: [NSAttributedString.Key: Any] = [.font: font, .foregroundColor: color(0xE6FFFFFF), .kern: 0.8]
        let textWidth = (label as NSString).size(withAttributes: attrs).width
        (label as NSString).draw(at: NSPoint(x: x + (width - textWidth) / 2, y: 126), withAttributes: attrs)
        x += width + 10
    }

    // Floating glass phone preview and orientation arcs.
    let card = NSBezierPath(roundedRect: NSRect(x: 762, y: 108, width: 178, height: 290), xRadius: 33, yRadius: 33)
    color(0xFFFFFF, alpha: 0.075).setFill(); card.fill()
    color(0xE6FFFFFF, alpha: 0.46).setStroke(); card.lineWidth = 2; card.stroke()
    let screen = NSBezierPath(roundedRect: NSRect(x: 776, y: 125, width: 150, height: 256), xRadius: 24, yRadius: 24)
    color(0x111027, alpha: 0.94).setFill(); screen.fill()
    let status = NSBezierPath(roundedRect: NSRect(x: 825, y: 361, width: 50, height: 6), xRadius: 3, yRadius: 3)
    color(0xD8B4FE).setFill(); status.fill()
    let halo = NSBezierPath(ovalIn: NSRect(x: 802, y: 190, width: 98, height: 98))
    color(0x5EEAD4, alpha: 0.08).setFill(); halo.fill()
    let centerPhone = NSBezierPath(roundedRect: NSRect(x: 832, y: 205, width: 39, height: 70), xRadius: 8, yRadius: 8)
    color(0xD8B4FE, alpha: 0.12).setFill(); centerPhone.fill()
    color(0xD8B4FE).setStroke(); centerPhone.lineWidth = 2; centerPhone.stroke()
    let centerDot = NSBezierPath(ovalIn: NSRect(x: 849, y: 211, width: 5, height: 5))
    color(0x5EEAD4).setFill(); centerDot.fill()
    let modeOne = NSBezierPath(roundedRect: NSRect(x: 796, y: 145, width: 63, height: 25), xRadius: 10, yRadius: 10)
    color(0xD8B4FE, alpha: 0.2).setFill(); modeOne.fill()
    let modeTwo = NSBezierPath(roundedRect: NSRect(x: 868, y: 145, width: 42, height: 25), xRadius: 10, yRadius: 10)
    color(0x5EEAD4, alpha: 0.18).setFill(); modeTwo.fill()
    drawText("PORTRAIT", at: NSPoint(x: 804, y: 152), size: 8, color: color(0xE6FFFFFF), weight: .bold)
    let bottomLine = NSBezierPath(roundedRect: NSRect(x: 798, y: 184, width: 126, height: 1), xRadius: 1, yRadius: 1)
    color(0xFFFFFF, alpha: 0.12).setFill(); bottomLine.fill()

    // A pair of restrained curved arrows frames the phone without crowding the copy.
    let arcTop = NSBezierPath(); arcTop.move(to: NSPoint(x: 732, y: 324)); arcTop.curve(to: NSPoint(x: 914, y: 359), controlPoint1: NSPoint(x: 753, y: 421), controlPoint2: NSPoint(x: 876, y: 427)); arcTop.lineWidth = 3; arcTop.lineCapStyle = .round; color(0xD8B4FE, alpha: 0.82).setStroke(); arcTop.stroke()
    let arrowTop = NSBezierPath(); arrowTop.move(to: NSPoint(x: 905, y: 382)); arrowTop.line(to: NSPoint(x: 940, y: 365)); arrowTop.line(to: NSPoint(x: 904, y: 348)); arrowTop.close(); color(0x5EEAD4).setFill(); arrowTop.fill()
    let arcBottom = NSBezierPath(); arcBottom.move(to: NSPoint(x: 949, y: 174)); arcBottom.curve(to: NSPoint(x: 754, y: 144), controlPoint1: NSPoint(x: 926, y: 72), controlPoint2: NSPoint(x: 795, y: 71)); arcBottom.lineWidth = 3; arcBottom.lineCapStyle = .round; color(0x5EEAD4, alpha: 0.84).setStroke(); arcBottom.stroke()
    let arrowBottom = NSBezierPath(); arrowBottom.move(to: NSPoint(x: 764, y: 122)); arrowBottom.line(to: NSPoint(x: 728, y: 140)); arrowBottom.line(to: NSPoint(x: 765, y: 159)); arrowBottom.close(); color(0xD8B4FE).setFill(); arrowBottom.fill()
}

let out = CommandLine.arguments.dropFirst().first ?? "play-store-assets"
try FileManager.default.createDirectory(atPath: "\(out)/app-icon", withIntermediateDirectories: true)
try FileManager.default.createDirectory(atPath: "\(out)/feature-graphic", withIntermediateDirectories: true)
try savePNG(size: NSSize(width: 512, height: 512), output: "\(out)/app-icon/rotation-control-icon-512.png") {
    drawLogo(512)
}
try savePNG(size: NSSize(width: 1024, height: 500), output: "\(out)/feature-graphic/rotation-control-feature-graphic.png") {
    drawBanner()
}
print("Rendered app icon 512×512 and Play feature graphic 1024×500")
