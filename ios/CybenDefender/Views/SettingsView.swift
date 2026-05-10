import SwiftUI
import CryptoKit
import UserNotifications

// MARK: - HIBP Password Check (k-anonymity, no API key needed)
func checkPasswordPwned(password: String) async throws -> Int {
    let data = Data(password.utf8)
    let hash = Insecure.SHA1.hash(data: data)
    let hashHex = hash.compactMap { String(format: "%02X", $0) }.joined()
    let prefix = String(hashHex.prefix(5))
    let suffix = String(hashHex.dropFirst(5))

    guard let url = URL(string: "https://api.pwnedpasswords.com/range/\(prefix)") else {
        throw APIError(message: "URL non valida")
    }
    var req = URLRequest(url: url)
    req.setValue("CybenGuard-iOS/1.0", forHTTPHeaderField: "User-Agent")
    req.setValue("false", forHTTPHeaderField: "Add-Padding")
    let (data2, _) = try await URLSession.shared.data(for: req)
    let text = String(decoding: data2, as: UTF8.self)
    for line in text.components(separatedBy: "\n") {
        let parts = line.trimmingCharacters(in: .whitespacesAndNewlines).components(separatedBy: ":")
        if parts.count == 2 && parts[0].uppercased() == suffix {
            return Int(parts[1]) ?? 0
        }
    }
    return 0
}

// MARK: - Main Settings View
struct SettingsView: View {
    @EnvironmentObject var authState: AuthState
    @EnvironmentObject var notificationService: NotificationService
    @State private var showSubscription = false
    @State private var showSOS = false
    @State private var isSyncing = false
    @State private var syncMessage: String?
    @State private var isSendingReset = false
    @State private var passwordResetError: String?
    @State private var showCancelConfirm = false
    @State private var showPasswordCheck = false
    @State private var notifStatus: UNAuthorizationStatus = .notDetermined

    var user: GuardUser? { authState.currentUser }
    var isPremium: Bool { user?.isPremium == true }
    var isPaid: Bool { user?.isPaid == true }

    var body: some View {
        NavigationView {
            List {
                profileHeaderSection
                subscriptionSection
                sosSection
                securityToolsSection
                notificationsSection
                accountSection
                appInfoSection
                logoutSection
            }
            .navigationTitle("Impostazioni")
            .listStyle(.insetGrouped)
            .sheet(isPresented: $showSubscription) { SubscriptionView() }
            .sheet(isPresented: $showSOS) { SOSSheet() }
            .sheet(isPresented: $showPasswordCheck) { PasswordCheckView() }
            .task { await refreshNotifStatus() }
            .safeAreaInset(edge: .top) {
                if let msg = syncMessage {
                    HStack(spacing: 6) {
                        Image(systemName: "checkmark.circle.fill").foregroundColor(.green).font(.caption)
                        Text(msg).font(.caption.bold()).foregroundColor(.green)
                    }
                    .padding(.horizontal, 14).padding(.vertical, 8)
                    .background(.ultraThinMaterial)
                    .cornerRadius(20)
                    .padding()
                    .onAppear {
                        DispatchQueue.main.asyncAfter(deadline: .now() + 3) { syncMessage = nil }
                    }
                }
            }
        }
    }

    // MARK: - Profile Header
    private var profileHeaderSection: some View {
        Section {
            HStack(spacing: 16) {
                ZStack {
                    Circle()
                        .fill(LinearGradient(colors: [.cyan, Color(red:0.4,green:0,blue:0.9)],
                                             startPoint: .topLeading, endPoint: .bottomTrailing))
                        .frame(width: 58, height: 58)
                        .shadow(color: .cyan.opacity(0.35), radius: 8, x: 0, y: 4)
                    Text(user?.name.prefix(1).uppercased() ?? "?")
                        .font(.title2.bold()).foregroundColor(.white)
                }
                VStack(alignment: .leading, spacing: 5) {
                    Text(user?.name ?? "—").font(.headline)
                    Text(user?.email ?? "—").font(.caption).foregroundColor(.secondary)
                    PlanBadge(plan: user?.plan ?? "none")
                }
                Spacer()
            }
            .padding(.vertical, 6)
        }
    }

    // MARK: - Subscription
    private var subscriptionSection: some View {
        Section {
            if !isPaid {
                Button { showSubscription = true } label: {
                    HStack(spacing: 14) {
                        ZStack {
                            RoundedRectangle(cornerRadius: 10).fill(Color.yellow.opacity(0.15)).frame(width: 36, height: 36)
                            Image(systemName: "star.fill").foregroundColor(.yellow)
                        }
                        VStack(alignment: .leading, spacing: 2) {
                            Text("Attiva un piano").font(.subheadline.bold())
                            Text("7 giorni gratis · nessuna carta subito")
                                .font(.caption).foregroundColor(.secondary)
                        }
                        Spacer()
                        Image(systemName: "chevron.right").font(.caption).foregroundColor(.secondary)
                    }
                }
                .foregroundColor(.primary)
            } else {
                subscriptionActiveRow
            }
        } header: {
            Text("Abbonamento")
        }
    }

    private var subscriptionActiveRow: some View {
        VStack(spacing: 12) {
            HStack(spacing: 12) {
                ZStack {
                    RoundedRectangle(cornerRadius: 10)
                        .fill(isPremium ? Color.purple.opacity(0.12) : Color.cyan.opacity(0.12))
                        .frame(width: 36, height: 36)
                    Image(systemName: isPremium ? "star.circle.fill" : "checkmark.circle.fill")
                        .foregroundColor(isPremium ? .purple : .cyan)
                }
                VStack(alignment: .leading, spacing: 2) {
                    Text("Piano \(user?.planLabel ?? "—")").font(.subheadline.bold())
                    Text(isPremium ? "Tutte le funzionalità attive" : "Analisi AI · Storico")
                        .font(.caption).foregroundColor(.secondary)
                }
                Spacer()
                if isSyncing { ProgressView().scaleEffect(0.75) }
            }

            HStack(spacing: 8) {
                // Sync via StoreKit restore
                Button {
                    isSyncing = true
                    Task {
                        await StoreKitManager.shared.restorePurchases(authState: authState)
                        await MainActor.run {
                            syncMessage = "Piano sincronizzato: \(authState.currentUser?.planLabel ?? "—")"
                            isSyncing = false
                        }
                    }
                } label: {
                    Label("Aggiorna", systemImage: "arrow.clockwise")
                        .font(.caption.bold())
                        .frame(maxWidth: .infinity)
                        .padding(.vertical, 8)
                        .background(Color(.secondarySystemGroupedBackground))
                        .cornerRadius(10)
                }
                .foregroundColor(.primary)
                .disabled(isSyncing)

                // Cancel subscription: Apple requires this to happen in System Settings
                Button {
                    if let url = URL(string: "https://apps.apple.com/account/subscriptions") {
                        UIApplication.shared.open(url)
                    }
                } label: {
                    Label("Gestisci piano", systemImage: "arrow.up.right.square")
                        .font(.caption.bold())
                        .frame(maxWidth: .infinity)
                        .padding(.vertical, 8)
                        .background(Color(.secondarySystemGroupedBackground))
                        .cornerRadius(10)
                }
                .foregroundColor(.secondary)
            }
            .padding(.top, 2)
        }
        .padding(.vertical, 4)
    }

    // MARK: - SOS
    private var sosSection: some View {
        Section {
            if isPaid {
                Button { showSOS = true } label: {
                    HStack(spacing: 14) {
                        ZStack {
                            RoundedRectangle(cornerRadius: 10).fill(Color.red.opacity(0.1)).frame(width: 36, height: 36)
                            Image(systemName: "sos").foregroundColor(.red)
                        }
                        VStack(alignment: .leading, spacing: 2) {
                            Text("SOS Incidente Critico").font(.subheadline.bold()).foregroundColor(.red)
                            Text("Risposta da esperto umano entro 4 ore")
                                .font(.caption).foregroundColor(.secondary)
                        }
                        Spacer()
                        Image(systemName: "chevron.right").font(.caption).foregroundColor(.secondary)
                    }
                }
            } else {
                HStack(spacing: 14) {
                    ZStack {
                        RoundedRectangle(cornerRadius: 10).fill(Color(.tertiarySystemGroupedBackground)).frame(width: 36, height: 36)
                        Image(systemName: "lock.fill").foregroundColor(.secondary).font(.caption)
                    }
                    VStack(alignment: .leading, spacing: 2) {
                        Text("SOS Incidente Critico").font(.subheadline).foregroundColor(.secondary)
                        Text("Disponibile con piano Base o Premium")
                            .font(.caption).foregroundColor(.secondary)
                    }
                    Spacer()
                    Button("Attiva") { showSubscription = true }
                        .font(.caption.bold()).foregroundColor(.cyan)
                }
            }
        } header: {
            Text("Emergenza Sicurezza")
        }
    }

    // MARK: - Security Tools
    private var securityToolsSection: some View {
        Section {
            Button { showPasswordCheck = true } label: {
                HStack(spacing: 14) {
                    ZStack {
                        RoundedRectangle(cornerRadius: 10).fill(Color.orange.opacity(0.12)).frame(width: 36, height: 36)
                        Image(systemName: "key.viewfinder").foregroundColor(.orange)
                    }
                    VStack(alignment: .leading, spacing: 2) {
                        Text("Verifica Password").font(.subheadline.bold())
                        Text("Controlla se la tua password è stata violata")
                            .font(.caption).foregroundColor(.secondary)
                    }
                    Spacer()
                    Image(systemName: "chevron.right").font(.caption).foregroundColor(.secondary)
                }
            }
            .foregroundColor(.primary)
        } header: {
            Text("Strumenti di Sicurezza")
        }
    }

    // MARK: - Notifications
    private func refreshNotifStatus() async {
        let settings = await UNUserNotificationCenter.current().notificationSettings()
        await MainActor.run { notifStatus = settings.authorizationStatus }
    }

    private var notificationsSection: some View {
        Section {
            switch notifStatus {
            case .authorized, .provisional, .ephemeral:
                HStack(spacing: 14) {
                    ZStack {
                        RoundedRectangle(cornerRadius: 10).fill(Color.green.opacity(0.12)).frame(width: 36, height: 36)
                        Image(systemName: "bell.badge.fill").foregroundColor(.green)
                    }
                    VStack(alignment: .leading, spacing: 2) {
                        Text("Notifiche attive").font(.subheadline.bold())
                        Text("Ricevi avvisi di sicurezza in tempo reale")
                            .font(.caption).foregroundColor(.secondary)
                    }
                    Spacer()
                    Image(systemName: "checkmark.circle.fill").foregroundColor(.green).font(.subheadline)
                }

            case .denied:
                Button {
                    if let url = URL(string: UIApplication.openSettingsURLString) {
                        UIApplication.shared.open(url)
                    }
                } label: {
                    HStack(spacing: 14) {
                        ZStack {
                            RoundedRectangle(cornerRadius: 10).fill(Color.red.opacity(0.1)).frame(width: 36, height: 36)
                            Image(systemName: "bell.slash.fill").foregroundColor(.red)
                        }
                        VStack(alignment: .leading, spacing: 2) {
                            Text("Notifiche disabilitate").font(.subheadline.bold())
                            Text("Tocca per abilitarle nelle Impostazioni di sistema")
                                .font(.caption).foregroundColor(.secondary)
                        }
                        Spacer()
                        Image(systemName: "arrow.up.right.square").font(.caption).foregroundColor(.secondary)
                    }
                }
                .foregroundColor(.primary)

            default: // .notDetermined
                Button {
                    Task {
                        await NotificationService.shared.requestPermission()
                        await refreshNotifStatus()
                    }
                } label: {
                    HStack(spacing: 14) {
                        ZStack {
                            RoundedRectangle(cornerRadius: 10).fill(Color.cyan.opacity(0.12)).frame(width: 36, height: 36)
                            Image(systemName: "bell.fill").foregroundColor(.cyan)
                        }
                        VStack(alignment: .leading, spacing: 2) {
                            Text("Abilita notifiche").font(.subheadline.bold())
                            Text("Avvisi in tempo reale su violazioni e analisi")
                                .font(.caption).foregroundColor(.secondary)
                        }
                        Spacer()
                        Image(systemName: "chevron.right").font(.caption).foregroundColor(.secondary)
                    }
                }
                .foregroundColor(.primary)
            }
        } header: {
            Text("Notifiche")
        }
    }

    // MARK: - Account
    private var accountSection: some View {
        Section {
            Button {
                guard let email = user?.email, !isSendingReset else { return }
                isSendingReset = true
                passwordResetError = nil
                Task {
                    do {
                        try await APIService.shared.forgotPassword(email: email)
                        await MainActor.run {
                            syncMessage = "Email di reset inviata a \(email)"
                            isSendingReset = false
                        }
                    } catch let e as APIError {
                        await MainActor.run { passwordResetError = e.message; isSendingReset = false }
                    } catch {
                        await MainActor.run { passwordResetError = "Errore di connessione"; isSendingReset = false }
                    }
                }
            } label: {
                HStack(spacing: 14) {
                    ZStack {
                        RoundedRectangle(cornerRadius: 10).fill(Color.blue.opacity(0.1)).frame(width: 36, height: 36)
                        Image(systemName: "key.fill").foregroundColor(.blue)
                        if isSendingReset {
                            ProgressView().scaleEffect(0.65)
                        }
                    }
                    Text("Cambia password").font(.subheadline)
                }
            }
            .foregroundColor(.primary)
            .disabled(isSendingReset)

            if let err = passwordResetError {
                Text(err).font(.caption).foregroundColor(.red)
            }
        } header: {
            Text("Account")
        }
    }

    // MARK: - App Info
    private var appInfoSection: some View {
        Section {
            HStack {
                Label("Versione", systemImage: "info.circle")
                Spacer()
                Text("1.0.0").foregroundColor(.secondary).font(.subheadline)
            }
            Link(destination: URL(string: "https://cyben.eu/privacy")!) {
                Label("Privacy Policy", systemImage: "hand.raised")
            }
            Link(destination: URL(string: "https://cyben.eu/terms")!) {
                Label("Termini di Servizio", systemImage: "doc.text")
            }
        } header: {
            Text("App")
        }
    }

    // MARK: - Logout
    private var logoutSection: some View {
        Section {
            Button(role: .destructive) { authState.logout() } label: {
                HStack {
                    Label("Esci dall'account", systemImage: "arrow.backward.square")
                    Spacer()
                }
            }
        }
    }
}

// MARK: - Plan Badge
struct PlanBadge: View {
    let plan: String
    var color: Color { plan == "premium" ? .purple : plan == "basic" ? .cyan : .secondary }
    var label: String { plan == "premium" ? "✦ Premium" : plan == "basic" ? "✓ Base" : "Free" }
    var body: some View {
        Text(label).font(.caption2.bold())
            .padding(.horizontal, 8).padding(.vertical, 3)
            .background(color.opacity(0.15)).foregroundColor(color).cornerRadius(6)
    }
}

// MARK: - SOS Sheet
struct SOSSheet: View {
    @Environment(\.dismiss) var dismiss
    @State private var description = ""
    @State private var incidentType = "other"
    @State private var isSending = false
    @State private var sent = false
    @State private var errorMessage: String?

    let incidentTypes: [(String, String, String)] = [
        ("ransomware", "Ransomware", "lock.trianglebadge.exclamationmark.fill"),
        ("hack", "Account violato", "person.badge.minus"),
        ("scam", "Truffa / Frode", "exclamationmark.shield.fill"),
        ("phishing", "Phishing riuscito", "envelope.badge.shield.half.filled.fill"),
        ("breach", "Dati rubati", "externaldrive.badge.xmark"),
        ("other", "Altro incidente", "questionmark.circle.fill"),
    ]

    var body: some View {
        NavigationView {
            ScrollView {
                VStack(spacing: 0) {
                    sosHero
                    if sent { sentConfirmation } else { sosForm }
                }
                .padding(.bottom, 40)
            }
            .background(Color(.systemGroupedBackground))
            .navigationBarTitleDisplayMode(.inline)
            .toolbar { ToolbarItem(placement: .navigationBarTrailing) { Button("Chiudi") { dismiss() } } }
        }
    }

    private var sosHero: some View {
        ZStack {
            LinearGradient(colors: [Color.red.opacity(0.08), Color(.systemGroupedBackground)],
                           startPoint: .top, endPoint: .bottom)
            VStack(spacing: 10) {
                ZStack {
                    Circle().fill(Color.red.opacity(0.1)).frame(width: 80, height: 80)
                    Circle().stroke(Color.red.opacity(0.2), lineWidth: 1.5).frame(width: 80, height: 80)
                    Image(systemName: "sos").font(.system(size: 38, weight: .bold)).foregroundColor(.red)
                }
                .padding(.top, 28)

                Text("SOS Incidente Critico").font(.title3.bold())

                Text("Un esperto di sicurezza Cyben analizza il tuo caso e ti risponde entro 4 ore. Include il servizio Human on the Loop.")
                    .font(.subheadline).foregroundColor(.secondary).multilineTextAlignment(.center)
                    .padding(.horizontal, 24)
                    .padding(.bottom, 20)
            }
        }
    }

    private var sentConfirmation: some View {
        VStack(spacing: 20) {
            ZStack {
                Circle().fill(Color.green.opacity(0.12)).frame(width: 80, height: 80)
                Image(systemName: "checkmark.circle.fill").font(.system(size: 44)).foregroundColor(.green)
            }
            .padding(.top, 32)

            VStack(spacing: 8) {
                Text("SOS Inviato!").font(.title3.bold()).foregroundColor(.green)
                Text("Il tuo caso è stato ricevuto. Un esperto di sicurezza Cyben ti contatterà all'email del tuo account entro 4 ore.")
                    .font(.subheadline).foregroundColor(.secondary).multilineTextAlignment(.center)
                    .padding(.horizontal, 32)
            }

            Button { dismiss() } label: {
                Text("Chiudi").fontWeight(.semibold)
                    .frame(maxWidth: .infinity).padding(14)
                    .background(Color.green).foregroundColor(.white).cornerRadius(14)
            }
            .padding(.horizontal, 28).padding(.top, 8)
        }
    }

    private var sosForm: some View {
        VStack(spacing: 20) {
            VStack(alignment: .leading, spacing: 10) {
                Text("Tipo di incidente")
                    .font(.caption.bold()).foregroundColor(.secondary)
                    .padding(.horizontal, 16)

                LazyVGrid(columns: [GridItem(.flexible()), GridItem(.flexible())], spacing: 8) {
                    ForEach(incidentTypes, id: \.0) { id, label, icon in
                        Button { incidentType = id } label: {
                            HStack(spacing: 8) {
                                Image(systemName: icon)
                                    .font(.caption.bold())
                                    .foregroundColor(incidentType == id ? .red : .secondary)
                                    .frame(width: 18)
                                Text(label)
                                    .font(.caption.bold())
                                    .foregroundColor(incidentType == id ? .red : .primary)
                                    .multilineTextAlignment(.leading)
                                Spacer()
                            }
                            .padding(.horizontal, 10).padding(.vertical, 10)
                            .background(incidentType == id ? Color.red.opacity(0.08) : Color(.secondarySystemGroupedBackground))
                            .cornerRadius(10)
                            .overlay(RoundedRectangle(cornerRadius: 10)
                                .stroke(incidentType == id ? Color.red.opacity(0.3) : Color.clear, lineWidth: 1.5))
                        }
                    }
                }
                .padding(.horizontal, 16)
            }

            VStack(alignment: .leading, spacing: 8) {
                Text("Descrivi il problema")
                    .font(.caption.bold()).foregroundColor(.secondary)
                    .padding(.horizontal, 16)
                TextEditor(text: $description)
                    .frame(minHeight: 120)
                    .padding(10)
                    .background(Color(.secondarySystemGroupedBackground))
                    .cornerRadius(12)
                    .padding(.horizontal, 16)
            }

            if let err = errorMessage {
                HStack(spacing: 6) {
                    Image(systemName: "exclamationmark.triangle.fill").foregroundColor(.red).font(.caption)
                    Text(err).font(.caption).foregroundColor(.red)
                }
                .padding(.horizontal, 16)
            }

            HStack(spacing: 6) {
                Image(systemName: "clock.fill").font(.caption2).foregroundColor(.secondary)
                Text("Risposta entro 4 ore · Human on the Loop incluso")
                    .font(.caption2).foregroundColor(.secondary)
            }
            .padding(.horizontal, 16)

            Button {
                isSending = true; errorMessage = nil
                Task {
                    do {
                        _ = try await APIService.shared.sendSOS(description: description, incidentType: incidentType)
                        await MainActor.run { withAnimation { sent = true } }
                    } catch let e as APIError {
                        await MainActor.run { errorMessage = e.message }
                    } catch {
                        await MainActor.run { errorMessage = "Errore di connessione. Riprova." }
                    }
                    await MainActor.run { isSending = false }
                }
            } label: {
                HStack(spacing: 8) {
                    if isSending { ProgressView().progressViewStyle(.circular).tint(.white).scaleEffect(0.85) }
                    Image(systemName: "sos").font(.subheadline.bold())
                    Text(isSending ? "Invio in corso..." : "Invia SOS").fontWeight(.semibold)
                }
                .frame(maxWidth: .infinity).padding(16)
                .background(description.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty || isSending
                            ? Color.secondary.opacity(0.25) : Color.red)
                .foregroundColor(.white).cornerRadius(14)
                .shadow(color: description.isEmpty ? .clear : .red.opacity(0.3), radius: 8, x: 0, y: 4)
            }
            .disabled(description.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty || isSending)
            .padding(.horizontal, 16)
        }
        .padding(.top, 4)
    }
}

// MARK: - Password Check View
struct PasswordCheckView: View {
    @Environment(\.dismiss) var dismiss
    @State private var password = ""
    @State private var showPassword = false
    @State private var isChecking = false
    @State private var result: PasswordCheckResult?
    @State private var errorMessage: String?

    enum PasswordCheckResult {
        case safe
        case breached(Int)
    }

    var body: some View {
        NavigationView {
            ScrollView {
                VStack(spacing: 0) {
                    checkHero
                    checkContent
                }
                .padding(.bottom, 40)
            }
            .background(Color(.systemGroupedBackground))
            .navigationBarTitleDisplayMode(.inline)
            .toolbar { ToolbarItem(placement: .navigationBarTrailing) { Button("Chiudi") { dismiss() } } }
        }
    }

    private var checkHero: some View {
        ZStack {
            LinearGradient(colors: [Color.orange.opacity(0.08), Color(.systemGroupedBackground)],
                           startPoint: .top, endPoint: .bottom)
            VStack(spacing: 10) {
                ZStack {
                    Circle().fill(Color.orange.opacity(0.1)).frame(width: 80, height: 80)
                    Image(systemName: "key.viewfinder").font(.system(size: 36)).foregroundColor(.orange)
                }
                .padding(.top, 28)

                Text("Verifica Password").font(.title3.bold())
                Text("Controlla se la tua password è presente nei database di violazioni conosciuti tramite Have I Been Pwned.")
                    .font(.subheadline).foregroundColor(.secondary).multilineTextAlignment(.center)
                    .padding(.horizontal, 24).padding(.bottom, 24)
            }
        }
    }

    private var checkContent: some View {
        VStack(spacing: 20) {
            VStack(alignment: .leading, spacing: 8) {
                HStack {
                    Image(systemName: showPassword ? "eye.slash" : "eye")
                        .foregroundColor(.secondary).font(.subheadline)
                        .frame(width: 20)
                    if showPassword {
                        TextField("Inserisci password da verificare", text: $password)
                            .autocapitalization(.none).autocorrectionDisabled()
                    } else {
                        SecureField("Inserisci password da verificare", text: $password)
                            .autocapitalization(.none).autocorrectionDisabled()
                    }
                    Button { showPassword.toggle() } label: {
                        Image(systemName: showPassword ? "eye.slash.fill" : "eye.fill")
                            .foregroundColor(.secondary).font(.caption)
                    }
                }
                .padding(14)
                .background(Color(.secondarySystemGroupedBackground))
                .cornerRadius(12)

                HStack(spacing: 4) {
                    Image(systemName: "lock.shield.fill").foregroundColor(.green).font(.caption2)
                    Text("La password viene verificata in modo anonimo. Non viene mai inviata al server.")
                        .font(.caption2).foregroundColor(.secondary)
                }
            }
            .padding(.horizontal, 16)

            if let result = result {
                resultCard(result)
            }

            if let err = errorMessage {
                HStack(spacing: 6) {
                    Image(systemName: "exclamationmark.triangle.fill").foregroundColor(.orange).font(.caption)
                    Text(err).font(.caption).foregroundColor(.orange)
                }
                .padding(.horizontal, 16)
            }

            Button {
                isChecking = true; result = nil; errorMessage = nil
                Task {
                    do {
                        let count = try await checkPasswordPwned(password: password)
                        await MainActor.run {
                            withAnimation(.spring()) {
                                result = count > 0 ? .breached(count) : .safe
                            }
                            isChecking = false
                        }
                    } catch {
                        await MainActor.run {
                            errorMessage = "Impossibile completare la verifica. Controlla la connessione."
                            isChecking = false
                        }
                    }
                }
            } label: {
                HStack(spacing: 8) {
                    if isChecking { ProgressView().progressViewStyle(.circular).tint(.white).scaleEffect(0.85) }
                    Text(isChecking ? "Verifica in corso..." : "Verifica password").fontWeight(.semibold)
                }
                .frame(maxWidth: .infinity).padding(16)
                .background(password.isEmpty || isChecking ? Color.secondary.opacity(0.25) : Color.orange)
                .foregroundColor(.white).cornerRadius(14)
            }
            .disabled(password.isEmpty || isChecking)
            .padding(.horizontal, 16)
        }
        .padding(.top, 8)
    }

    @ViewBuilder
    private func resultCard(_ res: PasswordCheckResult) -> some View {
        switch res {
        case .safe:
            VStack(spacing: 12) {
                ZStack {
                    Circle().fill(Color.green.opacity(0.12)).frame(width: 64, height: 64)
                    Image(systemName: "checkmark.shield.fill").font(.system(size: 30)).foregroundColor(.green)
                }
                Text("Password Sicura").font(.headline).foregroundColor(.green)
                Text("Ottimo! Questa password non risulta in nessun database di violazioni conosciuto.")
                    .font(.subheadline).foregroundColor(.secondary).multilineTextAlignment(.center)
            }
            .padding(20)
            .background(Color.green.opacity(0.07))
            .cornerRadius(16)
            .overlay(RoundedRectangle(cornerRadius: 16).stroke(Color.green.opacity(0.2)))
            .padding(.horizontal, 16)
            .transition(.scale.combined(with: .opacity))

        case .breached(let count):
            VStack(spacing: 12) {
                ZStack {
                    Circle().fill(Color.red.opacity(0.12)).frame(width: 64, height: 64)
                    Image(systemName: "xmark.shield.fill").font(.system(size: 30)).foregroundColor(.red)
                }
                Text("Password Compromessa!").font(.headline).foregroundColor(.red)
                Text("Questa password è stata trovata in **\(count.formatted()) violazioni** di dati. Cambiala immediatamente su tutti i servizi dove la usi.")
                    .font(.subheadline).foregroundColor(.secondary).multilineTextAlignment(.center)

                VStack(alignment: .leading, spacing: 8) {
                    breachTip(icon: "1.circle.fill", text: "Cambia la password su tutti i siti dove la usi")
                    breachTip(icon: "2.circle.fill", text: "Usa una password unica per ogni servizio")
                    breachTip(icon: "3.circle.fill", text: "Attiva l'autenticazione a due fattori (2FA)")
                }
                .padding(.top, 4)
            }
            .padding(20)
            .background(Color.red.opacity(0.06))
            .cornerRadius(16)
            .overlay(RoundedRectangle(cornerRadius: 16).stroke(Color.red.opacity(0.2)))
            .padding(.horizontal, 16)
            .transition(.scale.combined(with: .opacity))
        }
    }

    private func breachTip(icon: String, text: String) -> some View {
        HStack(alignment: .top, spacing: 8) {
            Image(systemName: icon).foregroundColor(.red.opacity(0.7)).font(.caption)
            Text(text).font(.caption).foregroundColor(.secondary).fixedSize(horizontal: false, vertical: true)
        }
    }
}
