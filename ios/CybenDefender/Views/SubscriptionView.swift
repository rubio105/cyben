import SwiftUI
import StoreKit

struct SubscriptionView: View {
    /// When false, the user cannot dismiss the screen (mandatory plan selection).
    var isDismissable: Bool = true

    @EnvironmentObject var authState: AuthState
    @StateObject private var sk = StoreKitManager.shared
    @State private var selectedPlan: PlanOption = .premium
    @State private var selectedCycle: BillingCycle = .yearly
    @State private var purchaseError: String?
    @State private var isRestoring = false
    @Environment(\.dismiss) var dismiss

    enum PlanOption: String, CaseIterable {
        case basic, premium
        var label: String { self == .basic ? "Base" : "Premium" }
        var description: String {
            self == .basic
            ? "Solo analisi AI testuale di SMS, email e link sospetti"
            : "Tutto in un'unica protezione: AI, breach monitor, VPN, password checker, telefono, SOS e supporto esperto"
        }
        var color: Color { self == .basic ? .cyan : .purple }
        var icon: String { self == .basic ? "shield.lefthalf.filled" : "shield.lefthalf.filled.badge.checkmark" }
    }

    enum BillingCycle: String, CaseIterable {
        case yearly, monthly
        var label: String { self == .yearly ? "Annuale" : "Mensile" }
        var saving: String? { self == .yearly ? "Risparmia vs mensile" : nil }
    }

    var currentPlan: String { authState.currentUser?.plan ?? "none" }

    private func planPriority(_ p: String) -> Int {
        switch p.lowercased() {
        case "premium": return 2
        case "basic":   return 1
        default:        return 0
        }
    }

    /// True if the user already has the selected plan OR a better one.
    /// Prevents showing a purchase button for a lower-tier plan (e.g. premium user looking at basic).
    var isCurrentPlanOrBetter: Bool {
        planPriority(currentPlan) >= planPriority(selectedPlan.rawValue)
    }

    var selectedProductId: String {
        switch (selectedPlan, selectedCycle) {
        case (.basic, _):         return StoreKitManager.productIdBasicYearly
        case (.premium, .yearly): return StoreKitManager.productIdPremiumYearly
        case (.premium, .monthly):return StoreKitManager.productIdPremiumMonthly
        }
    }

    var selectedProduct: Product? { sk.product(for: selectedProductId) }

    var priceLabel: String {
        guard let p = selectedProduct else {
            switch (selectedPlan, selectedCycle) {
            case (.basic, _):          return "€11,90/anno"
            case (.premium, .yearly):  return "€24,90/anno"
            case (.premium, .monthly): return "€3,99/mese"
            }
        }
        return p.displayPrice + (selectedCycle == .yearly ? "/anno" : "/mese")
    }

    var body: some View {
        NavigationView {
            ScrollView(showsIndicators: false) {
                VStack(spacing: 24) {

                    // Header
                    VStack(spacing: 6) {
                        Image(systemName: "shield.lefthalf.filled.badge.checkmark")
                            .font(.system(size: 44))
                            .foregroundStyle(LinearGradient(colors: [.cyan, Color(red:0.4,green:0,blue:0.9)],
                                                            startPoint: .topLeading, endPoint: .bottomTrailing))
                        Text("Scegli il tuo piano").font(.title2.bold())
                        Text("7 giorni di prova gratuita inclusi").font(.subheadline).foregroundColor(.secondary)
                    }
                    .padding(.top, 8)

                    // Plan selector
                    VStack(spacing: 12) {
                        ForEach(PlanOption.allCases, id: \.self) { plan in
                            PlanCard(plan: plan, isSelected: selectedPlan == plan,
                                     currentPlan: currentPlan, billingCycle: selectedCycle,
                                     priceLabel: priceLabelFor(plan: plan, cycle: selectedCycle))
                            .onTapGesture {
                                withAnimation(.easeInOut(duration: 0.2)) { selectedPlan = plan }
                            }
                        }
                    }

                    // Billing cycle (only for premium)
                    if selectedPlan == .premium {
                        Picker("Fatturazione", selection: $selectedCycle) {
                            ForEach(BillingCycle.allCases, id: \.self) { c in
                                Text(c.label).tag(c)
                            }
                        }
                        .pickerStyle(.segmented)
                    }

                    // CTA
                    if isCurrentPlanOrBetter {
                        Label("Piano attivo", systemImage: "checkmark.circle.fill")
                            .font(.subheadline.bold()).foregroundColor(.green)
                            .frame(maxWidth: .infinity).padding(16)
                            .background(Color.green.opacity(0.1)).cornerRadius(14)
                    } else {
                        Button(action: startPurchase) {
                            HStack(spacing: 8) {
                                if sk.isPurchasing {
                                    ProgressView().progressViewStyle(.circular).tint(.white).scaleEffect(0.85)
                                }
                                VStack(spacing: 2) {
                                    Text(sk.isPurchasing ? "Elaborazione..." : "Inizia 7 giorni gratis")
                                        .fontWeight(.semibold)
                                    if !sk.isPurchasing {
                                        Text(priceLabel).font(.caption).opacity(0.85)
                                    }
                                }
                            }
                            .frame(maxWidth: .infinity).padding(16)
                            .background(LinearGradient(colors: [selectedPlan.color, selectedPlan.color.opacity(0.7)],
                                                       startPoint: .leading, endPoint: .trailing))
                            .foregroundColor(.white).cornerRadius(14)
                        }
                        .disabled(sk.isPurchasing)
                    }

                    if let err = purchaseError {
                        Label(err, systemImage: "exclamationmark.triangle.fill")
                            .font(.caption).foregroundColor(.red).multilineTextAlignment(.center)
                    }

                    // Restore purchases
                    Button {
                        purchaseError = nil
                        Task { await sk.restorePurchases(authState: authState) }
                    } label: {
                        Text("Ripristina acquisti")
                            .font(.footnote).foregroundColor(.secondary).underline()
                    }
                    .disabled(sk.isRestoring)

                    // Feature table
                    FeatureTable()

                    Text("L'abbonamento si rinnova automaticamente. Puoi annullare in qualsiasi momento dalle Impostazioni di sistema → [Il tuo nome] → Abbonamenti.")
                        .font(.caption2).foregroundColor(.secondary).multilineTextAlignment(.center)
                        .padding(.horizontal).padding(.bottom, 24)
                }
                .padding(.horizontal)
            }
            .navigationTitle(isDismissable ? "Piani" : "Scegli il tuo piano")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                if isDismissable {
                    ToolbarItem(placement: .navigationBarLeading) {
                        Button("Chiudi") { dismiss() }
                    }
                } else {
                    ToolbarItem(placement: .navigationBarLeading) {
                        Button("Chiudi") {
                            authState.logout()
                        }
                    }
                }
            }
            .task {
                if sk.products.isEmpty { await sk.loadProducts() }
            }
            // Auto-dismiss when plan improves (e.g. purchase processed via Transaction.updates
            // listener while the sheet was still open, which can happen in Sandbox).
            .onChange(of: authState.currentUser?.plan) { oldPlan, newPlan in
                guard let newP = newPlan, let oldP = oldPlan, isDismissable else { return }
                if planPriority(newP) > planPriority(oldP) {
                    dismiss()
                }
            }
        }
    }

    // MARK: - Purchase via StoreKit
    private func startPurchase() {
        purchaseError = nil
        Task {
            do {
                let result = try await sk.purchase(productId: selectedProductId, authState: authState)
                switch result {
                case .purchased:
                    await MainActor.run { dismiss() }
                case .cancelled:
                    break
                case .pending:
                    await MainActor.run {
                        purchaseError = "Acquisto in attesa di approvazione (Family Sharing o Ask to Buy)."
                    }
                case .verifyFailed:
                    // Apple accepted the payment but our server couldn't confirm it.
                    // The transaction is NOT finished so it will be re-delivered on the next launch.
                    // Guide the user to recover without losing the purchase.
                    await MainActor.run {
                        purchaseError = "L'acquisto è stato elaborato da Apple ma il server non ha risposto. Il tuo abbonamento verrà attivato automaticamente alla prossima apertura dell'app. Puoi anche toccare \"Ripristina acquisti\" per forzare la sincronizzazione subito."
                    }
                }
            } catch {
                await MainActor.run { purchaseError = error.localizedDescription }
            }
        }
    }

    // MARK: - Price label helper
    private func priceLabelFor(plan: PlanOption, cycle: BillingCycle) -> String {
        let id: String
        switch (plan, cycle) {
        case (.basic, _):          id = StoreKitManager.productIdBasicYearly
        case (.premium, .yearly):  id = StoreKitManager.productIdPremiumYearly
        case (.premium, .monthly): id = StoreKitManager.productIdPremiumMonthly
        }
        if let p = sk.product(for: id) {
            let suffix = (plan == .basic || cycle == .yearly) ? "/anno" : "/mese"
            return p.displayPrice + suffix
        }
        switch (plan, cycle) {
        case (.basic, _):          return "€11,90/anno"
        case (.premium, .yearly):  return "€24,90/anno"
        case (.premium, .monthly): return "€3,99/mese"
        }
    }
}

// MARK: - Plan Card
struct PlanCard: View {
    let plan: SubscriptionView.PlanOption
    let isSelected: Bool
    let currentPlan: String
    let billingCycle: SubscriptionView.BillingCycle
    let priceLabel: String

    var isActive: Bool { currentPlan == plan.rawValue }

    var body: some View {
        HStack(spacing: 14) {
            Image(systemName: plan.icon).font(.title2).foregroundColor(plan.color).frame(width: 36)
            VStack(alignment: .leading, spacing: 4) {
                HStack(spacing: 6) {
                    Text("Piano \(plan.label)").font(.subheadline.bold())
                    if isActive {
                        Text("ATTIVO").font(.caption2.bold()).padding(.horizontal, 6).padding(.vertical, 2)
                            .background(plan.color).foregroundColor(.white).cornerRadius(4)
                    }
                }
                Text(plan.description).font(.caption).foregroundColor(.secondary).lineLimit(2)
            }
            Spacer()
            VStack(alignment: .trailing, spacing: 2) {
                Text(priceLabel).font(.caption.bold()).foregroundColor(plan.color)
                if plan == .basic { Text("solo annuale").font(.caption2).foregroundColor(.secondary) }
            }
        }
        .padding(14)
        .background(isSelected ? plan.color.opacity(0.08) : Color(.secondarySystemGroupedBackground))
        .cornerRadius(14)
        .overlay(RoundedRectangle(cornerRadius: 14).stroke(isSelected ? plan.color : Color.clear, lineWidth: 2))
        .animation(.easeInOut(duration: 0.2), value: isSelected)
    }
}

// MARK: - Feature Table
struct FeatureTable: View {
    struct Feat { let name: String; let basic: Bool; let premium: Bool }
    let features: [Feat] = [
        Feat(name: "Analisi AI SMS, email, URL (chat)", basic: true,  premium: true),
        Feat(name: "Panoramica e statistiche",           basic: true,  premium: true),
        Feat(name: "Analisi immagini sospette",          basic: false, premium: true),
        Feat(name: "Storico analisi",                    basic: false, premium: true),
        Feat(name: "Verifica password (HIBP)",           basic: false, premium: true),
        Feat(name: "Verifica numeri di telefono",        basic: false, premium: true),
        Feat(name: "Monitoraggio breach (2 email)",      basic: false, premium: true),
        Feat(name: "Digital Footprint",                  basic: false, premium: true),
        Feat(name: "VPN IKEv2 AES-256",                  basic: false, premium: true),
        Feat(name: "Protezione SMS avanzata",            basic: false, premium: true),
        Feat(name: "Human on the Loop",                  basic: false, premium: true),
        Feat(name: "SOS Incidente Critico (4h)",         basic: false, premium: true),
        Feat(name: "Security Awareness Training",        basic: false, premium: true),
    ]
    var body: some View {
        VStack(alignment: .leading, spacing: 0) {
            HStack {
                Text("Funzionalità").font(.caption.bold()).foregroundColor(.secondary)
                Spacer()
                Text("Base").font(.caption.bold()).foregroundColor(.cyan).frame(width: 48, alignment: .center)
                Text("Premium").font(.caption.bold()).foregroundColor(.purple).frame(width: 72, alignment: .center)
            }
            .padding(.horizontal).padding(.vertical, 10)
            .background(Color(.tertiarySystemGroupedBackground))
            Divider()
            ForEach(Array(features.enumerated()), id: \.offset) { idx, f in
                HStack {
                    Text(f.name).font(.caption)
                    Spacer()
                    Image(systemName: f.basic ? "checkmark" : "minus")
                        .font(.caption.bold()).foregroundColor(f.basic ? .green : .secondary.opacity(0.3))
                        .frame(width: 48, alignment: .center)
                    Image(systemName: f.premium ? "checkmark.circle.fill" : "minus")
                        .font(.caption.bold()).foregroundColor(f.premium ? .purple : .secondary.opacity(0.3))
                        .frame(width: 72, alignment: .center)
                }
                .padding(.horizontal).padding(.vertical, 8)
                .background(idx.isMultiple(of: 2) ? Color.clear : Color(.tertiarySystemGroupedBackground).opacity(0.5))
                if idx < features.count - 1 { Divider() }
            }
        }
        .background(Color(.secondarySystemGroupedBackground))
        .cornerRadius(14)
    }
}

// MARK: - Safari View (kept for potential Stripe billing portal use)
import SafariServices
struct SafariView: UIViewControllerRepresentable {
    let url: URL
    func makeUIViewController(context: Context) -> SFSafariViewController {
        let vc = SFSafariViewController(url: url)
        vc.preferredControlTintColor = .systemCyan
        return vc
    }
    func updateUIViewController(_ uiViewController: SFSafariViewController, context: Context) {}
}
