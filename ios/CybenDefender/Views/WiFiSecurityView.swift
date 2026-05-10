import SwiftUI

// Repurposed as SMS Protection view — analyzes SMS messages via Guard AI
struct SMSProtectionView: View {
    @EnvironmentObject var authState: AuthState
    @State private var smsText = ""
    @State private var result: GuardAnalysis?
    @State private var conversationalMessage: String?
    @State private var isAnalyzing = false
    @State private var showHistory = false
    @State private var analysisHistory: [GuardAnalysis] = []

    var isPremium: Bool { authState.currentUser?.isPremium == true }

    var body: some View {
        NavigationView {
            Group {
                if !isPremium {
                    PremiumGateView(
                        icon: "message.badge.shield.half.filled.fill",
                        title: "Protezione SMS",
                        description: "Analisi automatica dei messaggi SMS e WhatsApp per rilevare truffe e phishing. Disponibile con piano Premium.",
                        color: .green
                    )
                } else {
                    ScrollView(showsIndicators: false) {
                        VStack(spacing: 20) {

                            // Header
                            HStack {
                                VStack(alignment: .leading, spacing: 4) {
                                    Text("Analizza un messaggio").font(.headline)
                                    Text("Incolla SMS, WhatsApp o qualsiasi testo sospetto").font(.caption).foregroundColor(.secondary)
                                }
                                Spacer()
                                if !analysisHistory.isEmpty {
                                    Button { showHistory = true } label: {
                                        Label("Storico", systemImage: "clock")
                                            .font(.caption).foregroundColor(.cyan)
                                    }
                                }
                            }

                            // Input
                            VStack(spacing: 12) {
                                TextEditor(text: $smsText)
                                    .frame(minHeight: 120)
                                    .padding(10)
                                    .background(Color(.secondarySystemGroupedBackground))
                                    .cornerRadius(12)
                                    .overlay(
                                        Group {
                                            if smsText.isEmpty {
                                                Text("Es: «Hai vinto un premio, clicca qui: bit.ly/xxx»")
                                                    .foregroundColor(.secondary).font(.subheadline)
                                                    .padding(16).frame(maxWidth: .infinity, maxHeight: .infinity, alignment: .topLeading)
                                                    .allowsHitTesting(false)
                                            }
                                        }
                                    )

                                Button(action: analyzeMessage) {
                                    HStack(spacing: 8) {
                                        if isAnalyzing {
                                            ProgressView().progressViewStyle(.circular).tint(.white).scaleEffect(0.85)
                                        } else {
                                            Image(systemName: "sparkle.magnifyingglass")
                                        }
                                        Text(isAnalyzing ? "Analisi in corso..." : "Analizza messaggio")
                                            .fontWeight(.semibold)
                                    }
                                    .frame(maxWidth: .infinity).padding(14)
                                    .background(smsText.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty || isAnalyzing
                                                ? AnyShapeStyle(Color.secondary.opacity(0.3))
                                                : AnyShapeStyle(LinearGradient(colors: [.green, .teal],
                                                                               startPoint: .leading, endPoint: .trailing)))
                                    .foregroundColor(.white).cornerRadius(12)
                                }
                                .disabled(smsText.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty || isAnalyzing)
                            }

                            // Result
                            if let analysis = result {
                                SMSResultCard(analysis: analysis, message: conversationalMessage)
                            }

                            // Quick tips
                            SMSTipsCard()
                        }
                        .padding()
                    }
                }
            }
            .navigationTitle("Protezione SMS")
            .sheet(isPresented: $showHistory) { SMSHistorySheet(history: analysisHistory) }
            .task { if isPremium { await loadHistory() } }
        }
    }

    private func analyzeMessage() {
        let text = smsText.trimmingCharacters(in: .whitespacesAndNewlines)
        guard !text.isEmpty, !isAnalyzing else { return }
        isAnalyzing = true
        result = nil
        Task {
            do {
                let resp = try await APIService.shared.analyze(text: text, type: "sms")
                await MainActor.run {
                    result = resp.analysis
                    conversationalMessage = resp.conversationalMessage
                    if let a = resp.analysis { analysisHistory.insert(a, at: 0) }
                    isAnalyzing = false
                }
            } catch {
                await MainActor.run { isAnalyzing = false }
            }
        }
    }

    private func loadHistory() async {
        do { analysisHistory = try await APIService.shared.getAnalyses().filter { $0.inputType == "sms" } } catch {}
    }
}

struct SMSResultCard: View {
    let analysis: GuardAnalysis
    let message: String?

    var color: Color {
        switch analysis.riskLevel {
        case "safe": return .green
        case "suspicious": return .orange
        case "dangerous": return .red
        default: return .gray
        }
    }

    var body: some View {
        VStack(alignment: .leading, spacing: 14) {
            // Risk level header
            HStack {
                Image(systemName: analysis.riskIcon).font(.title2).foregroundColor(color)
                VStack(alignment: .leading, spacing: 2) {
                    Text(analysis.riskLabel).font(.headline).foregroundColor(color)
                    if let score = analysis.riskScore {
                        Text("Punteggio di rischio: \(score)/100").font(.caption).foregroundColor(.secondary)
                    }
                }
                Spacer()
            }

            if let msg = message {
                Text(msg).font(.subheadline).foregroundColor(.primary)
            }

            if let explanation = analysis.explanation {
                Text(explanation).font(.caption).foregroundColor(.secondary)
            }

            if let indicators = analysis.indicators, !indicators.isEmpty {
                VStack(alignment: .leading, spacing: 4) {
                    Text("Segnali rilevati:").font(.caption.bold())
                    ForEach(indicators, id: \.self) { indicator in
                        HStack(alignment: .top, spacing: 6) {
                            Image(systemName: "exclamationmark.circle.fill").font(.caption2).foregroundColor(color)
                            Text(indicator).font(.caption)
                        }
                    }
                }
            }

            if let rec = analysis.recommendation {
                HStack(alignment: .top, spacing: 6) {
                    Image(systemName: "lightbulb.fill").font(.caption).foregroundColor(.yellow)
                    Text(rec).font(.caption.bold())
                }
                .padding(10).background(Color.yellow.opacity(0.08)).cornerRadius(8)
            }
        }
        .padding()
        .background(color.opacity(0.06))
        .cornerRadius(14)
        .overlay(RoundedRectangle(cornerRadius: 14).stroke(color.opacity(0.2)))
    }
}

struct SMSTipsCard: View {
    var body: some View {
        VStack(alignment: .leading, spacing: 10) {
            Text("Come riconoscere una truffa SMS").font(.caption.bold()).foregroundColor(.secondary)
            ForEach([
                ("⚠️", "Link abbreviati (bit.ly, tinyurl) che non sai dove portano"),
                ("⚠️", "Urgenza eccessiva: «Risposta entro 24 ore»"),
                ("⚠️", "Premi o vincite inaspettate"),
                ("⚠️", "Richieste di dati personali o bancari"),
            ], id: \.1) { emoji, tip in
                HStack(alignment: .top, spacing: 6) {
                    Text(emoji).font(.caption)
                    Text(tip).font(.caption).foregroundColor(.secondary)
                }
            }
        }
        .padding()
        .background(Color(.secondarySystemGroupedBackground))
        .cornerRadius(12)
    }
}

struct SMSHistorySheet: View {
    let history: [GuardAnalysis]
    @Environment(\.dismiss) var dismiss
    var body: some View {
        NavigationView {
            List(history) { analysis in
                VStack(alignment: .leading, spacing: 4) {
                    HStack {
                        Image(systemName: analysis.riskIcon).font(.caption)
                            .foregroundColor(analysis.riskLevel == "safe" ? .green : analysis.riskLevel == "dangerous" ? .red : .orange)
                        Text(analysis.riskLabel).font(.caption.bold())
                        Spacer()
                        if let date = analysis.createdAt { Text(date.prefix(10)).font(.caption2).foregroundColor(.secondary) }
                    }
                    Text(analysis.inputText ?? "").font(.caption).foregroundColor(.secondary).lineLimit(2)
                }
                .padding(.vertical, 2)
            }
            .navigationTitle("Storico analisi SMS").navigationBarTitleDisplayMode(.inline)
            .toolbar { ToolbarItem(placement: .navigationBarTrailing) { Button("Chiudi") { dismiss() } } }
        }
    }
}
