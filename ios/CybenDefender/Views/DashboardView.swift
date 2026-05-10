import SwiftUI
import PhotosUI

struct DashboardView: View {
    @EnvironmentObject var authState: AuthState
    @State private var inputText = ""
    @State private var selectedType = "sms"
    @State private var chatHistory: [[String: String]] = []
    @State private var messages: [ChatMessage] = []
    @State private var isAnalyzing = false
    @State private var dailyUsed = 0
    @State private var dailyLimit = 10
    @State private var errorMessage: String?
    @State private var analyses: [GuardAnalysis] = []
    @State private var loadedScore = false
    @State private var agentPulse = false

    // Image / Camera / QR / SOS
    @State private var showImagePicker = false
    @State private var showCamera = false
    @State private var showQRScanner = false
    @State private var showSOSSheet = false
    @State private var showPhoneChecker = false
    @State private var pendingImage: UIImage? = nil
    @State private var welcomeInjected = false

    let analysisTypes = [("sms", "📱 SMS"), ("email", "📧 Email"), ("url", "🔗 URL"), ("text", "📄 Testo")]

    struct ChatMessage: Identifiable {
        let id = UUID()
        let role: String
        let text: String
        var analysis: GuardAnalysis?
        var imageData: Data?
    }

    var securityScore: Int {
        guard !analyses.isEmpty else { return -1 }
        let total = analyses.count
        let safe = analyses.filter { $0.riskLevel == "safe" }.count
        let suspicious = analyses.filter { $0.riskLevel == "suspicious" }.count
        return Int((Double(safe) * 100.0 + Double(suspicious) * 45.0) / Double(total))
    }

    var scoreColor: Color {
        let s = securityScore
        if s < 0 { return .secondary }
        if s >= 75 { return .green }
        if s >= 45 { return .orange }
        return .red
    }

    var scoreLabel: String {
        let s = securityScore
        if s < 0 { return "—" }
        if s >= 75 { return "Ottimo" }
        if s >= 45 { return "Attenzione" }
        return "A rischio"
    }

    var body: some View {
        NavigationView {
            VStack(spacing: 0) {
                agentHeader

                if authState.currentUser?.isPaid == false {
                    PremiumBanner()
                }

                ScrollViewReader { proxy in
                    ScrollView {
                        LazyVStack(spacing: 12) {
                            ForEach(messages) { msg in
                                MessageBubble(
                                    message: msg,
                                    onSendImage: { showImagePicker = true },
                                    onSOS: { showSOSSheet = true }
                                )
                                .id(msg.id)

                                // Quick suggestion chips after the welcome message
                                if msg.id == messages.first?.id && messages.count == 1 && !isAnalyzing {
                                    quickSuggestionsView
                                        .id("quickSuggestions")
                                }
                            }

                            if isAnalyzing {
                                ThinkingBubble().id("thinking")
                            }
                        }
                        .padding(.horizontal).padding(.top, 12).padding(.bottom, 16)
                    }
                    .onChange(of: messages.count) { _ in
                        withAnimation { proxy.scrollTo(messages.last?.id ?? "thinking", anchor: .bottom) }
                    }
                    .onChange(of: isAnalyzing) { _ in
                        if isAnalyzing { withAnimation { proxy.scrollTo("thinking", anchor: .bottom) } }
                    }
                }

                Divider()
                inputBar
            }
            .navigationBarHidden(true)
            .background(Color(.systemGroupedBackground))
            .task { await loadAnalyses() }
            .task {
                if let token = KeychainService.shared.load(for: "guard_token") {
                    await ProtectionFeedService.shared.refresh(token: token)
                }
            }
            .sheet(isPresented: $showSOSSheet) { SOSSheet() }
            .sheet(isPresented: $showPhoneChecker) { PhoneCheckerView() }
            // Image picker (gallery)
            .sheet(isPresented: $showImagePicker, onDismiss: {
                if let img = pendingImage {
                    pendingImage = nil
                    sendImage(img)
                }
            }) {
                PhotoPickerView(image: $pendingImage)
            }
            // Camera
            .fullScreenCover(isPresented: $showCamera, onDismiss: {
                if let img = pendingImage {
                    pendingImage = nil
                    sendImage(img)
                }
            }) {
                CameraView(image: $pendingImage)
                    .ignoresSafeArea()
            }
            // QR Scanner
            .sheet(isPresented: $showQRScanner) {
                QRScannerView { code in
                    selectedType = "url"
                    inputText = code
                }
            }
        }
    }

    // MARK: - Agent Header
    private var agentHeader: some View {
        HStack(spacing: 12) {
            ZStack {
                Circle()
                    .fill(Color.cyan.opacity(0.15))
                    .frame(width: 42, height: 42)
                    .scaleEffect(agentPulse ? 1.10 : 1.0)
                    .animation(.easeInOut(duration: 1.8).repeatForever(autoreverses: true), value: agentPulse)
                Image("CbyAvatar")
                    .resizable()
                    .aspectRatio(contentMode: .fit)
                    .frame(width: 38, height: 38)
                    .clipShape(Circle())
            }
            .onAppear { agentPulse = true }

            VStack(alignment: .leading, spacing: 1) {
                HStack(spacing: 5) {
                    Text("Cyben")
                        .font(.headline)
                    Circle().fill(.green).frame(width: 7, height: 7)
                    Text("online").font(.caption2).foregroundColor(.green)
                }
                Text("Agente AI anti-phishing & truffe")
                    .font(.caption).foregroundColor(.secondary)
            }

            Spacer()

            VStack(alignment: .trailing, spacing: 2) {
                if dailyUsed > 0 || dailyLimit > 0 {
                    Text("\(dailyUsed)/\(dailyLimit)")
                        .font(.caption2.bold())
                        .padding(.horizontal, 7).padding(.vertical, 3)
                        .background(dailyUsed >= dailyLimit ? Color.red.opacity(0.15) : Color.cyan.opacity(0.1))
                        .foregroundColor(dailyUsed >= dailyLimit ? .red : .cyan)
                        .cornerRadius(6)
                }
                if securityScore >= 0 {
                    Text("Score \(securityScore)")
                        .font(.caption2.bold())
                        .padding(.horizontal, 7).padding(.vertical, 3)
                        .background(scoreColor.opacity(0.12))
                        .foregroundColor(scoreColor)
                        .cornerRadius(6)
                }
            }
        }
        .padding(.horizontal, 16).padding(.vertical, 10)
        .background(Color(.systemBackground))
        .overlay(Rectangle().frame(height: 0.5).foregroundColor(Color(.separator)), alignment: .bottom)
    }

    // MARK: - Quick suggestions (shown after welcome message)
    private var quickSuggestionsView: some View {
        VStack(alignment: .leading, spacing: 8) {
            Text("Cosa vuoi verificare?")
                .font(.caption.bold())
                .foregroundColor(.secondary)
                .padding(.leading, 48)

            ScrollView(.horizontal, showsIndicators: false) {
                HStack(spacing: 8) {
                    quickChip(emoji: "📱", label: "SMS sospetto") {
                        selectedType = "sms"
                        inputText = ""
                    }
                    quickChip(emoji: "📧", label: "Email di phishing") {
                        selectedType = "email"
                        inputText = ""
                    }
                    quickChip(emoji: "🔗", label: "Verifica un link") {
                        selectedType = "url"
                        inputText = ""
                    }
                    quickChip(emoji: "📄", label: "Testo insolito") {
                        selectedType = "text"
                        inputText = ""
                    }
                    quickChip(emoji: "📷", label: "Analizza foto") {
                        showImagePicker = true
                    }
                    quickChip(emoji: "📱", label: "Scansiona QR") {
                        showQRScanner = true
                    }
                    quickChip(emoji: "📞", label: "Verifica numero") {
                        showPhoneChecker = true
                    }
                }
                .padding(.horizontal).padding(.leading, 40)
            }

            if securityScore >= 0 {
                SecurityScoreCard(score: securityScore, total: analyses.count, analyses: analyses)
                    .padding(.horizontal)
                    .padding(.leading, 8)
            }
        }
    }

    @ViewBuilder
    private func quickChip(emoji: String, label: String, action: @escaping () -> Void) -> some View {
        Button(action: action) {
            HStack(spacing: 5) {
                Text(emoji).font(.caption)
                Text(label).font(.caption.bold())
            }
            .padding(.horizontal, 12).padding(.vertical, 8)
            .background(Color(.secondarySystemGroupedBackground))
            .cornerRadius(20)
            .overlay(RoundedRectangle(cornerRadius: 20).stroke(Color.cyan.opacity(0.35), lineWidth: 1))
        }
        .foregroundColor(.primary)
        .disabled(isAnalyzing)
    }

    // MARK: - Input bar
    private var inputBar: some View {
        VStack(spacing: 8) {
            // Type chips + media action buttons
            ScrollView(.horizontal, showsIndicators: false) {
                HStack(spacing: 8) {
                    // Text analysis type chips
                    ForEach(analysisTypes, id: \.0) { type in
                        Button { selectedType = type.0 } label: {
                            Text(type.1)
                                .font(.caption.bold())
                                .padding(.horizontal, 12).padding(.vertical, 6)
                                .background(selectedType == type.0
                                            ? AnyShapeStyle(LinearGradient(colors: [.cyan, Color(red:0.4,green:0,blue:0.9)],
                                                                           startPoint: .leading, endPoint: .trailing))
                                            : AnyShapeStyle(Color(.tertiarySystemGroupedBackground)))
                                .foregroundColor(selectedType == type.0 ? .white : .primary)
                                .cornerRadius(20)
                        }
                    }

                    Divider().frame(height: 22).padding(.horizontal, 2)

                    // Camera button
                    mediaButton(icon: "camera.fill", label: "Foto", color: .indigo) {
                        if UIImagePickerController.isSourceTypeAvailable(.camera) {
                            showCamera = true
                        } else {
                            showImagePicker = true
                        }
                    }

                    // Gallery button
                    mediaButton(icon: "photo.on.rectangle", label: "Galleria", color: .purple) {
                        showImagePicker = true
                    }

                    // QR Scanner button
                    mediaButton(icon: "qrcode.viewfinder", label: "QR", color: .cyan) {
                        showQRScanner = true
                    }
                }
                .padding(.horizontal)
            }

            // Text field row
            HStack(spacing: 10) {
                TextField("Incolla qui il contenuto da analizzare...", text: $inputText, axis: .vertical)
                    .lineLimit(1...5)
                    .padding(10)
                    .background(Color(.secondarySystemGroupedBackground))
                    .cornerRadius(12)

                Button(action: sendMessage) {
                    Image(systemName: isAnalyzing ? "ellipsis.circle.fill" : "arrow.up.circle.fill")
                        .font(.system(size: 34))
                        .foregroundStyle(
                            inputText.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty || isAnalyzing
                            ? AnyShapeStyle(Color.secondary.opacity(0.3))
                            : AnyShapeStyle(LinearGradient(colors: [.cyan, Color(red:0.4,green:0,blue:0.9)],
                                                           startPoint: .top, endPoint: .bottom))
                        )
                }
                .disabled(inputText.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty || isAnalyzing)
            }
            .padding(.horizontal).padding(.bottom, 8)
        }
        .background(Color(.systemBackground))
    }

    @ViewBuilder
    private func mediaButton(icon: String, label: String, color: Color, action: @escaping () -> Void) -> some View {
        Button(action: action) {
            VStack(spacing: 3) {
                ZStack {
                    RoundedRectangle(cornerRadius: 8)
                        .fill(color.opacity(0.12))
                        .frame(width: 34, height: 28)
                    Image(systemName: icon)
                        .font(.system(size: 14, weight: .medium))
                        .foregroundColor(color)
                }
                Text(label)
                    .font(.system(size: 9, weight: .semibold))
                    .foregroundColor(color.opacity(0.8))
            }
        }
        .disabled(isAnalyzing)
    }

    // MARK: - Send
    private func sendMessage() {
        let text = inputText.trimmingCharacters(in: .whitespacesAndNewlines)
        guard !text.isEmpty, !isAnalyzing else { return }

        messages.append(ChatMessage(role: "user", text: text))
        chatHistory.append(["role": "user", "content": text])
        inputText = ""
        isAnalyzing = true
        errorMessage = nil

        Task {
            do {
                let resp = try await APIService.shared.analyze(text: text, type: selectedType,
                                                               chatHistory: chatHistory.suffix(6).map { $0 })
                await MainActor.run {
                    let reply = resp.conversationalMessage ?? resp.analysis?.explanation ?? "Analisi completata."
                    let assistantMsg = ChatMessage(role: "assistant", text: reply, analysis: resp.analysis)
                    messages.append(assistantMsg)
                    chatHistory.append(["role": "assistant", "content": reply])
                    dailyUsed = resp.dailyUsed ?? dailyUsed
                    dailyLimit = resp.dailyLimit ?? dailyLimit
                    isAnalyzing = false
                    if let a = resp.analysis { analyses.insert(a, at: 0) }
                }
            } catch let e as APIError {
                await MainActor.run {
                    messages.append(ChatMessage(role: "assistant", text: "⚠️ \(e.message)"))
                    isAnalyzing = false
                }
            } catch {
                await MainActor.run {
                    messages.append(ChatMessage(role: "assistant", text: "⚠️ Errore di connessione. Riprova."))
                    isAnalyzing = false
                }
            }
        }
    }

    private func sendImage(_ image: UIImage) {
        guard !isAnalyzing else { return }

        // Compress to JPEG — max 1.5 MB
        let maxBytes = 1_500_000
        var quality: CGFloat = 0.8
        var imageData = image.jpegData(compressionQuality: quality) ?? Data()
        while imageData.count > maxBytes && quality > 0.2 {
            quality -= 0.15
            imageData = image.jpegData(compressionQuality: quality) ?? Data()
        }

        let base64 = imageData.base64EncodedString()
        let userMsg = ChatMessage(role: "user", text: "📷 Immagine inviata per analisi", imageData: imageData)
        messages.append(userMsg)
        isAnalyzing = true
        errorMessage = nil

        Task {
            do {
                let resp = try await APIService.shared.analyzeImage(imageBase64: base64)
                await MainActor.run {
                    let reply = resp.conversationalMessage ?? resp.analysis?.explanation ?? "Analisi completata."
                    messages.append(ChatMessage(role: "assistant", text: reply, analysis: resp.analysis))
                    dailyUsed = resp.dailyUsed ?? dailyUsed
                    dailyLimit = resp.dailyLimit ?? dailyLimit
                    isAnalyzing = false
                    if let a = resp.analysis { analyses.insert(a, at: 0) }
                }
            } catch let e as APIError {
                await MainActor.run {
                    messages.append(ChatMessage(role: "assistant", text: "⚠️ \(e.message)"))
                    isAnalyzing = false
                }
            } catch {
                await MainActor.run {
                    messages.append(ChatMessage(role: "assistant", text: "⚠️ Errore di connessione. Riprova."))
                    isAnalyzing = false
                }
            }
        }
    }

    private func injectWelcomeMessage() {
        guard !welcomeInjected else { return }
        welcomeInjected = true
        let firstName = authState.currentUser?.name.components(separatedBy: " ").first ?? ""
        let greeting = firstName.isEmpty ? "Ciao! 👋" : "Ciao \(firstName)! 👋"
        let text = "\(greeting) Sono Cyben, il tuo assistente personale di sicurezza informatica.\n\nSono qui per aiutarti a verificare qualsiasi contenuto sospetto: SMS, email, link o testi strani. Puoi anche mandarmi una foto o scansionare un QR code.\n\nCosa vuoi verificare oggi?"
        messages.append(ChatMessage(role: "assistant", text: text))
    }

    private func loadAnalyses() async {
        await MainActor.run { injectWelcomeMessage() }
        guard !loadedScore else { return }
        do {
            let list = try await APIService.shared.getAnalyses()
            await MainActor.run {
                analyses = list
                loadedScore = true
            }
        } catch {}
    }
}

// MARK: - Security Score Card
struct SecurityScoreCard: View {
    let score: Int
    let total: Int
    let analyses: [GuardAnalysis]

    var scoreColor: Color {
        if score >= 75 { return .green }
        if score >= 45 { return .orange }
        return .red
    }

    var safeCount: Int { analyses.filter { $0.riskLevel == "safe" }.count }
    var suspiciousCount: Int { analyses.filter { $0.riskLevel == "suspicious" }.count }
    var dangerousCount: Int { analyses.filter { $0.riskLevel == "dangerous" }.count }

    var body: some View {
        HStack(spacing: 16) {
            ZStack {
                Circle()
                    .stroke(Color(.systemFill), lineWidth: 8)
                    .frame(width: 72, height: 72)
                Circle()
                    .trim(from: 0, to: CGFloat(score) / 100.0)
                    .stroke(scoreColor, style: StrokeStyle(lineWidth: 8, lineCap: .round))
                    .frame(width: 72, height: 72)
                    .rotationEffect(.degrees(-90))
                    .animation(.easeOut(duration: 1.0), value: score)
                VStack(spacing: 0) {
                    Text("\(score)")
                        .font(.system(size: 20, weight: .bold, design: .rounded))
                        .foregroundColor(scoreColor)
                    Text("/ 100").font(.system(size: 9)).foregroundColor(.secondary)
                }
            }

            VStack(alignment: .leading, spacing: 8) {
                HStack {
                    Text("Punteggio sicurezza")
                        .font(.caption.bold())
                    Spacer()
                    Text(score >= 75 ? "Ottimo" : score >= 45 ? "Attenzione" : "A rischio")
                        .font(.caption2.bold())
                        .padding(.horizontal, 7).padding(.vertical, 2)
                        .background(scoreColor.opacity(0.15))
                        .foregroundColor(scoreColor)
                        .cornerRadius(5)
                }

                HStack(spacing: 12) {
                    ScoreStat(count: safeCount, label: "Sicuri", color: .green)
                    ScoreStat(count: suspiciousCount, label: "Sospetti", color: .orange)
                    ScoreStat(count: dangerousCount, label: "Pericolosi", color: .red)
                }
            }
        }
        .padding(14)
        .background(Color(.secondarySystemGroupedBackground))
        .cornerRadius(14)
    }
}

struct ScoreStat: View {
    let count: Int
    let label: String
    let color: Color
    var body: some View {
        VStack(spacing: 2) {
            Text("\(count)").font(.system(size: 15, weight: .bold, design: .rounded)).foregroundColor(color)
            Text(label).font(.caption2).foregroundColor(.secondary)
        }
    }
}

// MARK: - Message Bubble
struct MessageBubble: View {
    let message: DashboardView.ChatMessage
    var onSendImage: (() -> Void)? = nil
    var onSOS: (() -> Void)? = nil
    var isUser: Bool { message.role == "user" }

    var body: some View {
        HStack(alignment: .bottom, spacing: 8) {
            if isUser { Spacer(minLength: 48) }

            if !isUser {
                Image("CbyAvatar")
                    .resizable()
                    .aspectRatio(contentMode: .fit)
                    .frame(width: 32, height: 32)
                    .clipShape(Circle())
                    .overlay(Circle().stroke(Color.cyan.opacity(0.3), lineWidth: 1))
            }

            VStack(alignment: isUser ? .trailing : .leading, spacing: 6) {
                // Image thumbnail (user messages with photo)
                if let imgData = message.imageData, let uiImg = UIImage(data: imgData) {
                    Image(uiImage: uiImg)
                        .resizable()
                        .aspectRatio(contentMode: .fill)
                        .frame(maxWidth: 200, maxHeight: 160)
                        .cornerRadius(14)
                        .clipped()
                }

                // Text bubble — hidden when the message is image-only
                if message.imageData == nil || !message.text.isEmpty {
                    let displayText = message.imageData != nil ? message.text.replacingOccurrences(of: "📷 Immagine inviata per analisi", with: "") : message.text
                    if !displayText.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty {
                        Text(displayText)
                            .font(.subheadline)
                            .foregroundColor(isUser ? .white : .primary)
                            .padding(.horizontal, 14).padding(.vertical, 10)
                            .background(
                                isUser
                                ? AnyShapeStyle(LinearGradient(colors: [.cyan, Color(red:0.4,green:0,blue:0.9)],
                                                               startPoint: .leading, endPoint: .trailing))
                                : AnyShapeStyle(Color(.secondarySystemGroupedBackground))
                            )
                            .cornerRadius(18)
                    }
                }

                if let analysis = message.analysis, analysis.riskLevel != "unknown" {
                    AnalysisResultCard(
                        analysis: analysis,
                        onSendImage: onSendImage,
                        onSOS: onSOS
                    )
                }
            }

            if !isUser { Spacer(minLength: 48) }
        }
    }
}

// MARK: - Analysis Result Card
struct AnalysisResultCard: View {
    let analysis: GuardAnalysis
    var onSendImage: (() -> Void)? = nil
    var onSOS: (() -> Void)? = nil

    var isRisky: Bool { analysis.riskLevel == "suspicious" || analysis.riskLevel == "dangerous" }

    var color: Color {
        switch analysis.riskLevel {
        case "safe": return .green
        case "suspicious": return .orange
        case "dangerous": return .red
        default: return .gray
        }
    }

    var body: some View {
        VStack(alignment: .leading, spacing: 8) {
            HStack(spacing: 8) {
                Image(systemName: analysis.riskIcon)
                    .font(.subheadline.bold())
                    .foregroundColor(color)
                Text(analysis.riskLabel)
                    .font(.subheadline.bold())
                    .foregroundColor(color)
                Spacer()
                if let score = analysis.riskScore {
                    ZStack {
                        Circle()
                            .stroke(color.opacity(0.2), lineWidth: 3)
                        Circle()
                            .trim(from: 0, to: CGFloat(score) / 100.0)
                            .stroke(color, style: StrokeStyle(lineWidth: 3, lineCap: .round))
                            .rotationEffect(.degrees(-90))
                        Text("\(score)%")
                            .font(.system(size: 9, weight: .bold))
                            .foregroundColor(color)
                    }
                    .frame(width: 34, height: 34)
                }
            }

            if let indicators = analysis.indicators, !indicators.isEmpty {
                VStack(alignment: .leading, spacing: 4) {
                    ForEach(indicators.prefix(3), id: \.self) { indicator in
                        HStack(alignment: .top, spacing: 6) {
                            Image(systemName: "exclamationmark.circle.fill")
                                .font(.caption2).foregroundColor(color.opacity(0.7))
                            Text(indicator).font(.caption).foregroundColor(.secondary)
                        }
                    }
                }
            }

            if let rec = analysis.recommendation {
                HStack(alignment: .top, spacing: 6) {
                    Image(systemName: "lightbulb.fill")
                        .font(.caption2).foregroundColor(.yellow)
                    Text(rec).font(.caption).foregroundColor(.secondary)
                }
                .padding(8)
                .background(Color.yellow.opacity(0.07))
                .cornerRadius(8)
            }

            // Contextual action buttons for risky content
            if isRisky {
                VStack(alignment: .leading, spacing: 6) {
                    Text("Azioni suggerite:")
                        .font(.caption2.bold())
                        .foregroundColor(.secondary)

                    ScrollView(.horizontal, showsIndicators: false) {
                        HStack(spacing: 6) {
                            if let onSendImage {
                                Button(action: onSendImage) {
                                    HStack(spacing: 4) {
                                        Image(systemName: "photo.badge.plus")
                                            .font(.caption2)
                                        Text("Invia screenshot")
                                            .font(.caption.bold())
                                    }
                                    .padding(.horizontal, 10).padding(.vertical, 6)
                                    .background(Color.blue.opacity(0.12))
                                    .foregroundColor(.blue)
                                    .cornerRadius(12)
                                    .overlay(RoundedRectangle(cornerRadius: 12).stroke(Color.blue.opacity(0.3), lineWidth: 1))
                                }
                            }

                            if let onSOS {
                                Button(action: onSOS) {
                                    HStack(spacing: 4) {
                                        Image(systemName: "sos.circle.fill")
                                            .font(.caption2)
                                        Text("SOS Urgente")
                                            .font(.caption.bold())
                                    }
                                    .padding(.horizontal, 10).padding(.vertical, 6)
                                    .background(Color.red.opacity(0.12))
                                    .foregroundColor(.red)
                                    .cornerRadius(12)
                                    .overlay(RoundedRectangle(cornerRadius: 12).stroke(Color.red.opacity(0.3), lineWidth: 1))
                                }
                            }
                        }
                    }
                }
                .padding(.top, 2)
            }
        }
        .padding(12)
        .background(color.opacity(0.06))
        .cornerRadius(14)
        .overlay(RoundedRectangle(cornerRadius: 14).stroke(color.opacity(0.2), lineWidth: 1))
    }
}

// MARK: - Thinking Bubble
struct ThinkingBubble: View {
    @State private var phase = 0
    var body: some View {
        HStack(alignment: .bottom, spacing: 8) {
            Image("CbyAvatar")
                .resizable()
                .aspectRatio(contentMode: .fit)
                .frame(width: 32, height: 32)
                .clipShape(Circle())
                .overlay(Circle().stroke(Color.cyan.opacity(0.3), lineWidth: 1))
            HStack(spacing: 5) {
                ForEach(0..<3) { i in
                    Circle().fill(Color.secondary.opacity(0.5)).frame(width: 8, height: 8)
                        .scaleEffect(phase == i ? 1.4 : 0.8)
                        .animation(.easeInOut(duration: 0.45).repeatForever().delay(Double(i) * 0.18), value: phase)
                }
            }
            .padding(.horizontal, 14).padding(.vertical, 12)
            .background(Color(.secondarySystemGroupedBackground))
            .cornerRadius(18)
            Spacer(minLength: 48)
        }
        .onAppear { phase = 1 }
    }
}

// MARK: - Premium Banner
struct PremiumBanner: View {
    @State private var showSub = false
    var body: some View {
        Button { showSub = true } label: {
            HStack(spacing: 10) {
                Image(systemName: "star.fill").foregroundColor(.yellow)
                VStack(alignment: .leading, spacing: 1) {
                    Text("Passa a Base o Premium").font(.caption.bold()).foregroundColor(.primary)
                    Text("Sblocca tutte le funzionalità · 7 giorni gratis")
                        .font(.caption2).foregroundColor(.secondary)
                }
                Spacer()
                Image(systemName: "chevron.right").font(.caption).foregroundColor(.secondary)
            }
            .padding(.horizontal, 14).padding(.vertical, 10)
            .background(Color.yellow.opacity(0.08))
        }
        .sheet(isPresented: $showSub) { SubscriptionView() }
    }
}

// MARK: - Corner radius helper
extension View {
    func cornerRadius(_ radius: CGFloat, corners: UIRectCorner) -> some View {
        clipShape(RoundedCorner(radius: radius, corners: corners))
    }
}

struct RoundedCorner: Shape {
    var radius: CGFloat = .infinity
    var corners: UIRectCorner = .allCorners
    func path(in rect: CGRect) -> Path {
        let path = UIBezierPath(roundedRect: rect, byRoundingCorners: corners,
                                cornerRadii: CGSize(width: radius, height: radius))
        return Path(path.cgPath)
    }
}
