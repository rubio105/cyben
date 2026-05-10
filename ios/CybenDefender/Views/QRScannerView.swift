import SwiftUI
import AVFoundation

// MARK: - QR Scanner Sheet
struct QRScannerView: View {
    @Environment(\.dismiss) var dismiss
    let onScan: (String) -> Void

    var body: some View {
        NavigationView {
            ZStack {
                Color.black.ignoresSafeArea()

                QRCameraPreview(onScan: { code in
                    dismiss()
                    onScan(code)
                })
                .ignoresSafeArea()

                // Dimmed overlay with transparent hole (visual only)
                ZStack {
                    Color.black.opacity(0.45).ignoresSafeArea()

                    RoundedRectangle(cornerRadius: 20)
                        .frame(width: 250, height: 250)
                        .blendMode(.destinationOut)
                }
                .compositingGroup()

                VStack(spacing: 0) {
                    Spacer()

                    ZStack {
                        RoundedRectangle(cornerRadius: 20)
                            .stroke(Color.cyan, lineWidth: 3)
                            .frame(width: 250, height: 250)

                        CornerBrackets(size: 250, radius: 20, length: 30)
                            .stroke(Color.cyan, style: StrokeStyle(lineWidth: 4, lineCap: .round))
                            .frame(width: 250, height: 250)
                    }

                    VStack(spacing: 6) {
                        Text("Inquadra il QR code")
                            .font(.subheadline.bold())
                            .foregroundColor(.white)
                        Text("Il link verrà analizzato automaticamente")
                            .font(.caption)
                            .foregroundColor(.white.opacity(0.7))
                    }
                    .padding(.top, 24)

                    Spacer()
                }
            }
            .navigationTitle("Scansiona QR")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .cancellationAction) {
                    Button("Annulla") { dismiss() }
                        .foregroundColor(.cyan)
                }
            }
        }
    }
}

// MARK: - Corner brackets shape
struct CornerBrackets: Shape {
    var size: CGFloat
    var radius: CGFloat
    var length: CGFloat

    func path(in rect: CGRect) -> Path {
        var p = Path()
        let w = rect.width, h = rect.height

        // Top-left
        p.move(to: CGPoint(x: 0, y: length))
        p.addLine(to: CGPoint(x: 0, y: radius))
        p.addQuadCurve(to: CGPoint(x: radius, y: 0), control: CGPoint(x: 0, y: 0))
        p.addLine(to: CGPoint(x: length, y: 0))

        // Top-right
        p.move(to: CGPoint(x: w - length, y: 0))
        p.addLine(to: CGPoint(x: w - radius, y: 0))
        p.addQuadCurve(to: CGPoint(x: w, y: radius), control: CGPoint(x: w, y: 0))
        p.addLine(to: CGPoint(x: w, y: length))

        // Bottom-right
        p.move(to: CGPoint(x: w, y: h - length))
        p.addLine(to: CGPoint(x: w, y: h - radius))
        p.addQuadCurve(to: CGPoint(x: w - radius, y: h), control: CGPoint(x: w, y: h))
        p.addLine(to: CGPoint(x: w - length, y: h))

        // Bottom-left
        p.move(to: CGPoint(x: length, y: h))
        p.addLine(to: CGPoint(x: radius, y: h))
        p.addQuadCurve(to: CGPoint(x: 0, y: h - radius), control: CGPoint(x: 0, y: h))
        p.addLine(to: CGPoint(x: 0, y: h - length))

        return p
    }
}

// MARK: - Camera Preview with QR Detection
struct QRCameraPreview: UIViewRepresentable {
    let onScan: (String) -> Void

    func makeUIView(context: Context) -> UIView {
        let view = UIView(frame: UIScreen.main.bounds)
        view.backgroundColor = .black

        let session = AVCaptureSession()
        guard let device = AVCaptureDevice.default(for: .video),
              let input = try? AVCaptureDeviceInput(device: device) else { return view }

        session.addInput(input)

        let output = AVCaptureMetadataOutput()
        session.addOutput(output)
        output.setMetadataObjectsDelegate(context.coordinator, queue: .main)
        output.metadataObjectTypes = [.qr, .ean13, .ean8, .code128, .dataMatrix, .aztec, .pdf417, .upce]

        let preview = AVCaptureVideoPreviewLayer(session: session)
        preview.videoGravity = .resizeAspectFill
        preview.frame = view.bounds
        view.layer.addSublayer(preview)
        context.coordinator.previewLayer = preview

        DispatchQueue.global(qos: .userInitiated).async {
            session.startRunning()
        }
        context.coordinator.session = session
        return view
    }

    func updateUIView(_ uiView: UIView, context: Context) {
        DispatchQueue.main.async {
            context.coordinator.previewLayer?.frame = uiView.bounds
        }
    }

    func makeCoordinator() -> Coordinator { Coordinator(onScan: onScan) }

    class Coordinator: NSObject, AVCaptureMetadataOutputObjectsDelegate {
        let onScan: (String) -> Void
        var session: AVCaptureSession?
        var previewLayer: AVCaptureVideoPreviewLayer?
        private var hasScanned = false

        init(onScan: @escaping (String) -> Void) { self.onScan = onScan }

        func metadataOutput(_ output: AVCaptureMetadataOutput,
                            didOutput metadataObjects: [AVMetadataObject],
                            from connection: AVCaptureConnection) {
            guard !hasScanned,
                  let obj = metadataObjects.first as? AVMetadataMachineReadableCodeObject,
                  let code = obj.stringValue else { return }
            hasScanned = true
            session?.stopRunning()
            onScan(code)
        }
    }
}
