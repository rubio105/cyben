import SwiftUI

// MARK: - Plan Feature Model

struct PlanFeature: Identifiable {
    let id = UUID()
    let icon: String
    let iconColor: Color
    let title: String
    let description: String
}

// MARK: - Feature Data

enum PlanFeatures {

    static let basic: [PlanFeature] = [
        PlanFeature(
            icon: "bubble.left.and.bubble.right.fill",
            iconColor: Color(hex: "#06b6d4"),
            title: "Agente AI Cyben",
            description: "Analizza manualmente testi, email, SMS, link, messaggi WhatsApp o contenuti sospetti incollati dall'utente."
        ),
        PlanFeature(
            icon: "photo.badge.magnifyingglass",
            iconColor: Color(hex: "#06b6d4"),
            title: "Analisi screenshot e immagini",
            description: "Carica screenshot o immagini sospette e chiedi a Cyben se sembrano phishing, truffe o tentativi di furto dati."
        ),
        PlanFeature(
            icon: "ellipsis.message.fill",
            iconColor: Color(hex: "#06b6d4"),
            title: "Chat di supporto AI",
            description: "Fai domande all'agente AI e ricevi spiegazioni semplici sul rischio rilevato e sui passi da seguire."
        ),
        PlanFeature(
            icon: "clock.arrow.circlepath",
            iconColor: Color(hex: "#06b6d4"),
            title: "Storico analisi",
            description: "Consulta tutte le analisi effettuate con l'agente AI, con data e risultato."
        ),
    ]

    static let premiumExtras: [PlanFeature] = [
        PlanFeature(
            icon: "shield.lefthalf.filled.badge.checkmark",
            iconColor: Color(hex: "#a855f7"),
            title: "Monitoraggio violazioni email",
            description: "Controlla se i tuoi indirizzi email risultano coinvolti in data breach o violazioni note."
        ),
        PlanFeature(
            icon: "key.horizontal.fill",
            iconColor: Color(hex: "#a855f7"),
            title: "Verifica password compromesse",
            description: "Verifica se una password è comparsa in database pubblici di violazioni, senza inviare la password completa."
        ),
        PlanFeature(
            icon: "lock.shield.fill",
            iconColor: Color(hex: "#06b6d4"),
            title: "VPN cifrata IKEv2",
            description: "Protegge la connessione Internet con tunnel VPN cifrato, utile su reti WiFi pubbliche o non affidabili."
        ),
        PlanFeature(
            icon: "wifi.exclamationmark",
            iconColor: Color(hex: "#a855f7"),
            title: "Protezione WiFi",
            description: "Analizza la rete a cui sei connesso e segnala configurazioni deboli, reti pubbliche o potenzialmente rischiose."
        ),
        PlanFeature(
            icon: "message.badge.waveform.fill",
            iconColor: Color(hex: "#a855f7"),
            title: "Protezione SMS",
            description: "Abilita il filtro SMS iOS con liste locali sincronizzate per identificare messaggi sospetti o rischiosi."
        ),
        PlanFeature(
            icon: "phone.badge.waveform.fill",
            iconColor: Color(hex: "#a855f7"),
            title: "Protezione chiamate",
            description: "Abilita l'identificazione chiamate iOS con liste locali sincronizzate per segnalare numeri sospetti."
        ),
        PlanFeature(
            icon: "phone.badge.xmark.fill",
            iconColor: Color(hex: "#a855f7"),
            title: "Controllo numeri sospetti",
            description: "Verifica numeri di telefono associati a truffe, spam o segnalazioni della community."
        ),
        PlanFeature(
            icon: "bolt.shield.fill",
            iconColor: Color(hex: "#06b6d4"),
            title: "Protezione immediata",
            description: "Sincronizza liste locali di numeri, domini e frasi sospette per una protezione aggiornata e sempre attiva."
        ),
    ]
}

// MARK: - Basic Plan Card

struct BasicPlanCard: View {
    var onSelect: (() -> Void)? = nil

    var body: some View {
        PlanCard(
            name: "Basic",
            tagline: "Pensato per chi vuole usare Cyben come assistente AI per controllare manualmente messaggi, email, link o contenuti sospetti.",
            price: "€19,90",
            period: "/anno",
            sub: "= €1,66/mese · 7 giorni gratis",
            accentColor: Color(hex: "#06b6d4"),
            badgeText: nil,
            features: PlanFeatures.basic,
            buttonLabel: "Prova gratis 7 giorni",
            useGradientButton: false,
            onSelect: onSelect
        )
    }
}

// MARK: - Premium Plan Card

struct PremiumPlanCard: View {
    @State private var billing: Billing = .yearly
    var onSelect: ((_ billing: Billing) -> Void)? = nil

    enum Billing { case monthly, yearly }

    var price: String { billing == .yearly ? "€49,90" : "€4,99" }
    var period: String { billing == .yearly ? "/anno" : "/mese" }
    var sub: String {
        billing == .yearly
            ? "7 giorni gratis, poi €49,90/anno · Risparmi €9,98"
            : "7 giorni gratis, poi €4,99/mese"
    }

    var body: some View {
        PlanCard(
            name: "Premium",
            tagline: "Pensato per chi vuole protezione completa: analisi AI, monitoraggio dati, VPN, sicurezza WiFi, protezione SMS/chiamate e supporto umano.",
            price: price,
            period: period,
            sub: sub,
            accentColor: Color(hex: "#a855f7"),
            badgeText: "PIÙ SCELTO",
            features: PlanFeatures.basic + PlanFeatures.premiumExtras,
            buttonLabel: "Prova Premium gratis 7 giorni",
            useGradientButton: true,
            header: {
                AnyView(BillingToggle(billing: $billing))
            },
            onSelect: { onSelect?(billing) }
        )
    }
}

// MARK: - Generic Plan Card

struct PlanCard: View {
    let name: String
    let tagline: String
    let price: String
    let period: String
    let sub: String
    let accentColor: Color
    let badgeText: String?
    let features: [PlanFeature]
    let buttonLabel: String
    var useGradientButton: Bool = false
    var header: (() -> AnyView)? = nil
    var onSelect: (() -> Void)? = nil

    var body: some View {
        VStack(alignment: .leading, spacing: 0) {

            // Header
            ZStack(alignment: .topTrailing) {
                VStack(alignment: .leading, spacing: 8) {
                    Text(name)
                        .font(.title2).fontWeight(.black)
                        .foregroundColor(.white)

                    Text(tagline)
                        .font(.caption)
                        .foregroundColor(.secondary)
                        .fixedSize(horizontal: false, vertical: true)

                    header?()

                    HStack(alignment: .lastTextBaseline, spacing: 2) {
                        Text(price)
                            .font(.system(size: 36, weight: .black))
                            .foregroundColor(.white)
                        Text(period)
                            .font(.subheadline)
                            .foregroundColor(.secondary)
                    }

                    Text(sub)
                        .font(.caption2)
                        .foregroundColor(.secondary)
                }
                .padding(20)
                .frame(maxWidth: .infinity, alignment: .leading)

                if let badge = badgeText {
                    Text(badge)
                        .font(.system(size: 9, weight: .black))
                        .foregroundColor(.white)
                        .padding(.horizontal, 10).padding(.vertical, 4)
                        .background(
                            LinearGradient(
                                colors: [Color(hex: "#a855f7"), Color(hex: "#06b6d4")],
                                startPoint: .leading, endPoint: .trailing
                            )
                        )
                        .clipShape(Capsule())
                        .padding(14)
                }
            }

            Divider().background(Color.white.opacity(0.08))

            // Feature list
            VStack(alignment: .leading, spacing: 0) {
                ForEach(features) { feature in
                    FeatureRow(feature: feature)
                    if feature.id != features.last?.id {
                        Divider().background(Color.white.opacity(0.05)).padding(.leading, 52)
                    }
                }
            }
            .padding(.vertical, 8)

            Divider().background(Color.white.opacity(0.08))

            // CTA
            Button(action: { onSelect?() }) {
                Text(buttonLabel)
                    .font(.headline).fontWeight(.black)
                    .foregroundColor(.white)
                    .frame(maxWidth: .infinity)
                    .padding(.vertical, 14)
                    .background(
                        Group {
                            if useGradientButton {
                                LinearGradient(
                                    colors: [Color(hex: "#a855f7"), Color(hex: "#06b6d4")],
                                    startPoint: .leading, endPoint: .trailing
                                )
                            } else {
                                accentColor
                            }
                        }
                    )
                    .clipShape(RoundedRectangle(cornerRadius: 14))
            }
            .padding(16)
        }
        .background(
            RoundedRectangle(cornerRadius: 20)
                .fill(Color(red: 0.05, green: 0.09, blue: 0.16))
                .overlay(
                    RoundedRectangle(cornerRadius: 20)
                        .stroke(accentColor.opacity(0.25), lineWidth: 1)
                )
        )
    }
}

// MARK: - Feature Row

struct FeatureRow: View {
    let feature: PlanFeature

    var body: some View {
        HStack(alignment: .top, spacing: 14) {
            ZStack {
                RoundedRectangle(cornerRadius: 10)
                    .fill(feature.iconColor.opacity(0.12))
                    .frame(width: 36, height: 36)
                Image(systemName: feature.icon)
                    .font(.system(size: 16))
                    .foregroundColor(feature.iconColor)
            }

            VStack(alignment: .leading, spacing: 3) {
                Text(feature.title)
                    .font(.subheadline).fontWeight(.bold)
                    .foregroundColor(.white)
                Text(feature.description)
                    .font(.caption)
                    .foregroundColor(.secondary)
                    .fixedSize(horizontal: false, vertical: true)
            }
        }
        .padding(.horizontal, 16)
        .padding(.vertical, 12)
    }
}

// MARK: - Billing Toggle

struct BillingToggle: View {
    @Binding var billing: PremiumPlanCard.Billing

    var body: some View {
        HStack(spacing: 2) {
            BillingButton(label: "Mensile", selected: billing == .monthly) {
                billing = .monthly
            }
            BillingButton(label: "Annuale", badge: "-17%", selected: billing == .yearly) {
                billing = .yearly
            }
        }
        .padding(4)
        .background(Color.white.opacity(0.06))
        .clipShape(RoundedRectangle(cornerRadius: 12))
    }
}

private struct BillingButton: View {
    let label: String
    var badge: String? = nil
    let selected: Bool
    let action: () -> Void

    var body: some View {
        Button(action: action) {
            HStack(spacing: 4) {
                Text(label)
                    .font(.caption).fontWeight(.semibold)
                    .foregroundColor(selected ? .white : .secondary)
                if let badge {
                    Text(badge)
                        .font(.system(size: 9, weight: .black))
                        .foregroundColor(Color(hex: "#4ade80"))
                }
            }
            .padding(.horizontal, 12).padding(.vertical, 7)
            .background(
                selected
                ? AnyView(RoundedRectangle(cornerRadius: 9).fill(Color(hex: "#7c3aed")))
                : AnyView(Color.clear)
            )
        }
    }
}

// MARK: - Plans Comparison Screen (full-screen sheet)

struct PlansComparisonView: View {
    var onSelectBasic: (() -> Void)? = nil
    var onSelectPremium: ((_ billing: PremiumPlanCard.Billing) -> Void)? = nil
    @Environment(\.dismiss) private var dismiss

    var body: some View {
        ZStack {
            Color(red: 0.04, green: 0.08, blue: 0.14).ignoresSafeArea()

            ScrollView {
                VStack(spacing: 20) {
                    // Title
                    VStack(spacing: 6) {
                        Text("Scegli il tuo piano")
                            .font(.title).fontWeight(.black)
                            .foregroundColor(.white)
                        Text("7 giorni gratis · Carta richiesta · Cancella quando vuoi")
                            .font(.caption)
                            .foregroundColor(.secondary)
                    }
                    .padding(.top, 28)

                    // Basic
                    BasicPlanCard(onSelect: onSelectBasic)
                        .padding(.horizontal, 16)

                    // Premium
                    PremiumPlanCard(onSelect: onSelectPremium)
                        .padding(.horizontal, 16)

                    // Trust badges
                    HStack(spacing: 20) {
                        TrustBadge(icon: "lock.fill", label: "Stripe")
                        TrustBadge(icon: "checkmark.shield.fill", label: "GDPR")
                        TrustBadge(icon: "arrow.counterclockwise", label: "Cancella sempre")
                    }
                    .padding(.bottom, 32)
                }
            }
        }
        .navigationTitle("")
        .toolbar {
            ToolbarItem(placement: .topBarTrailing) {
                Button("Chiudi") { dismiss() }
                    .foregroundColor(.secondary)
            }
        }
    }
}

private struct TrustBadge: View {
    let icon: String
    let label: String

    var body: some View {
        HStack(spacing: 5) {
            Image(systemName: icon)
                .font(.caption)
                .foregroundColor(.secondary)
            Text(label)
                .font(.caption2)
                .foregroundColor(.secondary)
        }
    }
}

// MARK: - Color hex helper

extension Color {
    init(hex: String) {
        let h = hex.trimmingCharacters(in: .init(charactersIn: "#"))
        var rgb: UInt64 = 0
        Scanner(string: h).scanHexInt64(&rgb)
        let r = Double((rgb >> 16) & 0xFF) / 255
        let g = Double((rgb >> 8)  & 0xFF) / 255
        let b = Double(rgb & 0xFF) / 255
        self.init(red: r, green: g, blue: b)
    }
}

// MARK: - Preview

#Preview("Basic") { BasicPlanCard() }
#Preview("Premium") { PremiumPlanCard() }
#Preview("Comparison") {
    NavigationStack { PlansComparisonView() }
}
