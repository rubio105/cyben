import SwiftUI

// MARK: - Model
struct PhoneCheckResult: Decodable {
    let phone: String
    let riskLevel: String
    let riskScore: Int
    let numberType: String?
    let country: String?
    let `operator`: String?
    let lineType: String?
    let summary: String?
    let indicators: [String]
    let recommendation: String?
    let scamCategory: String?
    let communityReports: Int
    let communityEscalated: Bool
}

// MARK: - View
struct PhoneCheckerView: View {
    @EnvironmentObject var authState: AuthState
    @Environment(\.dismiss) private var dismiss

    @State private var phone = "+39 "
    @State private var isLoading = false
    @State private var result: PhoneCheckResult?
    @State private var errorMessage: String?

    // Report form
    @State private var showReportForm = false
    @State private var reportCategory = "altro"
    @State private var reportDesc = ""
    @State private var reporting = false
    @State private var reportMsg: String?

    private var isPremium: Bool { authState.user?.isPremium == true }

    var body: some View {
        NavigationView {
            ZStack {
                guardGradient.ignoresSafeArea()
                ScrollView(showsIndicators: false) {
                    VStack(spacing: 20) {
                        Spacer().frame(height: 4)

                        // Header
                        VStack(spacing: 6) {
                            Image(systemName: "phone.badge.checkmark.fill")
                                .font(.system(size: 44))
                                .foregroundStyle(LinearGradient(
                                    colors: [.cyan, Color(red:0.4,green:0,blue:0.9)],
                                    startPoint: .top, endPoint: .bottom))
                            Text("Verifica Numero")
                                .font(.title2.bold()).foregroundColor(.white)
                            Text("Analisi AI + segnalazioni community")
                                .font(.caption).foregroundColor(.white.opacity(0.5))
                        }

                        // Input
                        VStack(spacing: 12) {
                            HStack {
                                Image(systemName: "phone")
                                    .foregroundColor(.white.opacity(0.55)).frame(width: 20)
                                TextField("", text: $phone,
                                          prompt: Text("+39 349 1234567").foregroundColor(.white.opacity(0.35)))
                                    .keyboardType(.phonePad)
                                    .foregroundColor(.white)
                                    .autocorrectionDisabled()
                                if !phone.trimmingCharacters(in: .whitespaces).isEmpty && phone != "+39 " {
                                    Button { phone = "+39 " } label: {
                                        Image(systemName: "xmark.circle.fill")
                                            .foregroundColor(.white.opacity(0.35))
                                    }
                                }
                            }
                            .padding()
                            .background(.white.opacity(0.10))
                            .cornerRadius(13)
                            .overlay(RoundedRectangle(cornerRadius: 13).stroke(.white.opacity(0.18)))

                            Text("Includi il prefisso internazionale: +39 per Italia, +1 per USA, ecc.")
                                .font(.caption2).foregroundColor(.white.opacity(0.4))
                                .frame(maxWidth: .infinity, alignment: .leading)

                            if !isPremium {
                                HStack(spacing: 6) {
                                    Image(systemName: "lock.fill").font(.caption)
                                    Text("Disponibile con Piano Premium")
                                        .font(.caption.bold())
                                }
                                .foregroundColor(.orange)
                                .padding(.vertical, 6)
                            }

                            Button(action: performCheck) {
                                HStack(spacing: 8) {
                                    if isLoading {
                                        ProgressView().progressViewStyle(.circular).tint(.white).scaleEffect(0.85)
                                    }
                                    Text(isLoading ? "Analisi in corso..." : "Analizza numero")
                                        .fontWeight(.semibold)
                                }
                                .frame(maxWidth: .infinity).padding(15)
                                .background(canCheck
                                            ? AnyShapeStyle(LinearGradient(colors: [.cyan, Color(red:0.4,green:0,blue:0.9)],
                                                                           startPoint: .leading, endPoint: .trailing))
                                            : AnyShapeStyle(Color.white.opacity(0.12)))
                                .foregroundColor(.white).cornerRadius(14)
                            }
                            .disabled(!canCheck)
                        }
                        .padding(.horizontal, 24)

                        // Error
                        if let err = errorMessage {
                            ErrorLabel(message: err).padding(.horizontal, 24)
                        }

                        // Result
                        if let r = result {
                            PhoneResultCard(result: r,
                                           showReportForm: $showReportForm,
                                           reportCategory: $reportCategory,
                                           reportDesc: $reportDesc,
                                           reporting: $reporting,
                                           reportMsg: $reportMsg,
                                           onReport: submitReport)
                                .padding(.horizontal, 24)
                        }

                        Spacer().frame(height: 32)
                    }
                }
            }
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .cancellationAction) {
                    Button("Chiudi") { dismiss() }.foregroundColor(.cyan)
                }
            }
        }
    }

    private var canCheck: Bool {
        isPremium && !isLoading && phone.trimmingCharacters(in: .whitespaces).count >= 6
    }

    private func performCheck() {
        guard canCheck else { return }
        let trimmed = phone.trimmingCharacters(in: .whitespaces)
        guard trimmed.hasPrefix("+") else {
            errorMessage = "Inserisci il prefisso internazionale (es. +39 per l'Italia)"
            return
        }
        isLoading = true; errorMessage = nil; result = nil
        showReportForm = false; reportMsg = nil
        Task {
            do {
                let r = try await APIService.shared.checkPhone(phone: trimmed)
                await MainActor.run { result = r; isLoading = false }
            } catch let e as APIError {
                await MainActor.run { errorMessage = e.message; isLoading = false }
            } catch {
                await MainActor.run { errorMessage = "Errore di connessione"; isLoading = false }
            }
        }
    }

    private func submitReport() {
        guard let r = result else { return }
        reporting = true; reportMsg = nil
        Task {
            do {
                let count = try await APIService.shared.reportPhone(
                    phone: r.phone, category: reportCategory, description: reportDesc)
                await MainActor.run {
                    reportMsg = "✓ Segnalazione inviata. Segnalazioni community: \(count)"
                    showReportForm = false
                }
            } catch let e as APIError {
                await MainActor.run { reportMsg = "⚠ \(e.message)" }
            } catch {
                await MainActor.run { reportMsg = "⚠ Errore di connessione" }
            }
            await MainActor.run { reporting = false }
        }
    }
}

// MARK: - Result Card
struct PhoneResultCard: View {
    let result: PhoneCheckResult
    @Binding var showReportForm: Bool
    @Binding var reportCategory: String
    @Binding var reportDesc: String
    @Binding var reporting: Bool
    @Binding var reportMsg: String?
    let onReport: () -> Void

    private var riskColor: Color {
        switch result.riskLevel {
        case "safe":      return .green
        case "suspicious": return .orange
        case "dangerous":  return .red
        default:           return .gray
        }
    }
    private var riskLabel: String {
        switch result.riskLevel {
        case "safe":      return "Sicuro"
        case "suspicious": return "Sospetto"
        case "dangerous":  return "Pericoloso"
        default:           return "Sconosciuto"
        }
    }
    private var riskIcon: String {
        switch result.riskLevel {
        case "safe":      return "checkmark.shield.fill"
        case "suspicious": return "exclamationmark.triangle.fill"
        case "dangerous":  return "xmark.shield.fill"
        default:           return "questionmark.circle.fill"
        }
    }

    private let categories = [
        ("altro", "Altro"),
        ("phishing_bancario", "Phishing bancario"),
        ("finto_corriere", "Finto corriere"),
        ("finto_ente", "Finto ente (INPS, Agenzia Entrate...)"),
        ("truffa_vincite", "Truffa vincite/premi"),
        ("call_center_abusivo", "Call center abusivo"),
    ]

    var body: some View {
        VStack(alignment: .leading, spacing: 16) {

            // Risk header
            HStack(spacing: 14) {
                ZStack {
                    Circle().fill(riskColor.opacity(0.15)).frame(width: 56, height: 56)
                    Image(systemName: riskIcon).font(.system(size: 26)).foregroundColor(riskColor)
                }
                VStack(alignment: .leading, spacing: 2) {
                    Text(result.phone).font(.subheadline.bold()).foregroundColor(.white)
                    Text(riskLabel).font(.headline.bold()).foregroundColor(riskColor)
                    Text("Score: \(result.riskScore)/100").font(.caption).foregroundColor(.white.opacity(0.5))
                }
                Spacer()
                // Community badge
                if result.communityReports > 0 {
                    VStack(spacing: 2) {
                        Image(systemName: "person.3.fill").font(.caption).foregroundColor(.red)
                        Text("\(result.communityReports)").font(.caption.bold()).foregroundColor(.red)
                        Text("segnalaz.").font(.caption2).foregroundColor(.white.opacity(0.4))
                    }
                }
            }

            // Risk bar
            GeometryReader { geo in
                ZStack(alignment: .leading) {
                    RoundedRectangle(cornerRadius: 4).fill(.white.opacity(0.1)).frame(height: 8)
                    RoundedRectangle(cornerRadius: 4).fill(riskColor)
                        .frame(width: geo.size.width * CGFloat(result.riskScore) / 100, height: 8)
                }
            }
            .frame(height: 8)

            // Meta info
            HStack(spacing: 12) {
                if let country = result.country {
                    metaChip(icon: "globe", label: country)
                }
                if let op = result.operator {
                    metaChip(icon: "antenna.radiowaves.left.and.right", label: op)
                }
                if let lt = result.lineType {
                    metaChip(icon: "phone.circle", label: lt)
                }
            }

            // Summary
            if let summary = result.summary, !summary.isEmpty {
                Text(summary)
                    .font(.subheadline).foregroundColor(.white.opacity(0.85))
                    .padding(12).background(.white.opacity(0.07)).cornerRadius(10)
            }

            // Community escalation banner
            if result.communityEscalated {
                HStack(spacing: 8) {
                    Image(systemName: "person.3.fill").foregroundColor(.red)
                    Text("Questo numero è stato segnalato più volte dalla community come truffa.")
                        .font(.caption.bold()).foregroundColor(.red)
                }
                .padding(10).background(Color.red.opacity(0.12)).cornerRadius(10)
            }

            // Indicators
            if !result.indicators.isEmpty {
                VStack(alignment: .leading, spacing: 6) {
                    Text("Segnali rilevati").font(.caption.bold()).foregroundColor(.white.opacity(0.6))
                    ForEach(result.indicators, id: \.self) { ind in
                        HStack(alignment: .top, spacing: 6) {
                            Image(systemName: "exclamationmark.circle.fill")
                                .font(.caption).foregroundColor(.orange).padding(.top, 1)
                            Text(ind).font(.caption).foregroundColor(.white.opacity(0.8))
                        }
                    }
                }
            }

            // Recommendation
            if let rec = result.recommendation, !rec.isEmpty {
                HStack(alignment: .top, spacing: 8) {
                    Image(systemName: "lightbulb.fill").foregroundColor(.cyan).font(.subheadline)
                    Text(rec).font(.caption).foregroundColor(.white.opacity(0.8))
                }
                .padding(10).background(Color.cyan.opacity(0.08)).cornerRadius(10)
            }

            // Scam category
            if let cat = result.scamCategory {
                HStack(spacing: 6) {
                    Image(systemName: "tag.fill").font(.caption).foregroundColor(.orange)
                    Text(cat.replacingOccurrences(of: "_", with: " ").capitalized)
                        .font(.caption.bold()).foregroundColor(.orange)
                }
            }

            Divider().background(.white.opacity(0.15))

            // Report section
            if let msg = reportMsg {
                Text(msg).font(.caption)
                    .foregroundColor(msg.hasPrefix("✓") ? .cyan : .orange)
            }

            if showReportForm {
                VStack(alignment: .leading, spacing: 10) {
                    Text("Segnala numero truffa").font(.caption.bold()).foregroundColor(.white.opacity(0.7))

                    VStack(alignment: .leading, spacing: 4) {
                        Text("Categoria").font(.caption2).foregroundColor(.white.opacity(0.5))
                        Picker("Categoria", selection: $reportCategory) {
                            ForEach(categories, id: \.0) { cat in
                                Text(cat.1).tag(cat.0)
                            }
                        }
                        .pickerStyle(.menu)
                        .tint(.cyan)
                        .background(.white.opacity(0.08)).cornerRadius(8)
                    }

                    VStack(alignment: .leading, spacing: 4) {
                        Text("Descrizione (opzionale)").font(.caption2).foregroundColor(.white.opacity(0.5))
                        ZStack(alignment: .topLeading) {
                            if reportDesc.isEmpty {
                                Text("Cosa è successo? (es. 'Mi ha chiamato fingendosi INPS')")
                                    .font(.caption).foregroundColor(.white.opacity(0.3))
                                    .padding(8)
                            }
                            TextEditor(text: $reportDesc)
                                .frame(minHeight: 64)
                                .font(.caption)
                                .foregroundColor(.white)
                                .scrollContentBackground(.hidden)
                                .background(.clear)
                        }
                        .padding(4)
                        .background(.white.opacity(0.08)).cornerRadius(8)
                    }

                    HStack(spacing: 10) {
                        Button("Annulla") { showReportForm = false }
                            .font(.caption.bold()).foregroundColor(.white.opacity(0.5))
                        Spacer()
                        Button(action: onReport) {
                            HStack(spacing: 6) {
                                if reporting { ProgressView().scaleEffect(0.7).tint(.white) }
                                Text(reporting ? "Invio..." : "Invia segnalazione").font(.caption.bold())
                            }
                            .padding(.horizontal, 14).padding(.vertical, 8)
                            .background(Color.red.opacity(0.8)).foregroundColor(.white).cornerRadius(8)
                        }
                        .disabled(reporting)
                    }
                }
                .padding(12).background(.white.opacity(0.05)).cornerRadius(10)
            } else if reportMsg == nil {
                Button {
                    showReportForm = true
                    reportCategory = result.scamCategory ?? "altro"
                } label: {
                    HStack(spacing: 6) {
                        Image(systemName: "flag.fill").font(.caption)
                        Text("Segnala come truffa").font(.caption.bold())
                    }
                    .foregroundColor(.red.opacity(0.85))
                }
            }
        }
        .padding(16)
        .background(.white.opacity(0.07))
        .cornerRadius(16)
        .overlay(RoundedRectangle(cornerRadius: 16).stroke(riskColor.opacity(0.3), lineWidth: 1))
    }

    private func metaChip(icon: String, label: String) -> some View {
        HStack(spacing: 4) {
            Image(systemName: icon).font(.caption2).foregroundColor(.white.opacity(0.5))
            Text(label).font(.caption2).foregroundColor(.white.opacity(0.7))
        }
        .padding(.horizontal, 8).padding(.vertical, 4)
        .background(.white.opacity(0.08)).cornerRadius(6)
    }
}
