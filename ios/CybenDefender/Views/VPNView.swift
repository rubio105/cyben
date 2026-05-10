import SwiftUI
import NetworkExtension
import UserNotifications

// MARK: - Server model
struct CybenVPNServer: Identifiable, Hashable {
    let id = UUID()
    let flag: String
    let country: String
    let city: String
    let address: String
    let load: Int
    let available: Bool   // false = "presto disponibile"
}

let cybenVPNServers: [CybenVPNServer] = [
    // ── ATTIVO ─────────────────────────────────────────────────────────────
    CybenVPNServer(flag: "🇩🇪", country: "Germania",      city: "Francoforte",   address: "de.vpn.cyben.eu", load: 41, available: true),
    // ── PRESTO DISPONIBILI ─────────────────────────────────────────────────
    CybenVPNServer(flag: "🇮🇹", country: "Italia",        city: "Milano",        address: "it.vpn.cyben.eu", load: 0,  available: false),
    CybenVPNServer(flag: "🇳🇱", country: "Paesi Bassi",   city: "Amsterdam",     address: "nl.vpn.cyben.eu", load: 0,  available: false),
    CybenVPNServer(flag: "🇫🇷", country: "Francia",       city: "Parigi",        address: "fr.vpn.cyben.eu", load: 0,  available: false),
    CybenVPNServer(flag: "🇬🇧", country: "Regno Unito",   city: "Londra",        address: "gb.vpn.cyben.eu", load: 0,  available: false),
    CybenVPNServer(flag: "🇺🇸", country: "Stati Uniti",   city: "New York",      address: "us.vpn.cyben.eu", load: 0,  available: false),
    CybenVPNServer(flag: "🇨🇭", country: "Svizzera",      city: "Zurigo",        address: "ch.vpn.cyben.eu", load: 0,  available: false),
    CybenVPNServer(flag: "🇸🇪", country: "Svezia",        city: "Stoccolma",     address: "se.vpn.cyben.eu", load: 0,  available: false),
]

private var cybenActiveServer: CybenVPNServer { cybenVPNServers.first(where: { $0.available })! }

// MARK: - VPN View
struct VPNView: View {
    @EnvironmentObject var vpnManager: VPNManager
    @EnvironmentObject var authState: AuthState

    @State private var showServerPicker = false
    @State private var showAdvancedConfig = false
    @State private var selectedServer: CybenVPNServer? = cybenVPNServers.first
    @State private var connectError: String? = nil
    @State private var dnsStats: VPNDnsStats? = nil
    @State private var lastPollTime = Date()
    @State private var pollTask: Task<Void, Never>? = nil
    @State private var recentBlocked: [VPNBlockedQuery] = []

    var isPremium: Bool { authState.currentUser?.isPremium == true }

    var body: some View {
        NavigationView {
            Group {
                if !isPremium {
                    PremiumGateView(
                        icon: "lock.shield.fill",
                        title: "VPN IKEv2",
                        description: "Connessione VPN cifrata AES-256 per proteggere il tuo traffico ovunque. Disponibile con piano Premium.",
                        color: .blue
                    )
                } else {
                    ScrollView {
                        VStack(spacing: 20) {

                            // Entitlement setup banner (shown until profile configured)
                            if !vpnManager.isProfileConfigured {
                                entitlementBanner
                            }

                            // Error banner
                            if let err = connectError {
                                errorBanner(err)
                            }

                            // 1 — Status card
                            VPNStatusCard(vpnManager: vpnManager)

                            // 2 — Location picker
                            locationCard

                            // 3 — Connect / Disconnect button
                            connectButton

                            // 4 — Features list
                            featuresList

                            // 5 — DNS protection stats
                            dnsProtectionCard

                            // 6 — Advanced config (collapsed)
                            advancedConfigRow
                        }
                        .padding()
                    }
                }
            }
            .navigationTitle("VPN")
            .sheet(isPresented: $showServerPicker) {
                VPNServerPickerSheet(selected: $selectedServer) { server in
                    vpnManager.config.serverAddress = server.address
                    vpnManager.saveConfig()
                    // Changing server requires re-setup
                    UserDefaults.standard.set(false, forKey: "vpnProfileConfigured")
                }
            }
            .sheet(isPresented: $showAdvancedConfig) {
                VPNConfigSheet(vpnManager: vpnManager)
            }
            .onAppear {
                // Always default to the active (Germany) server
                let active = cybenActiveServer
                if vpnManager.config.serverAddress.isEmpty || vpnManager.config.serverAddress != active.address {
                    selectedServer = active
                    vpnManager.config.serverAddress = active.address
                    vpnManager.saveConfig()
                } else {
                    selectedServer = active
                }
                // Load DNS stats
                Task { dnsStats = try? await APIService.shared.fetchVPNDnsStats() }
                // Request notification permission
                UNUserNotificationCenter.current().requestAuthorization(options: [.alert, .sound, .badge]) { _, _ in }
                // Start polling if already connected
                if vpnManager.isConnected { startBlockedQueryPolling() }
            }
            .onDisappear { stopBlockedQueryPolling() }
            .onChange(of: vpnManager.isConnected) { connected in
                if connected { startBlockedQueryPolling() } else { stopBlockedQueryPolling() }
            }
        }
    }

    // MARK: - Entitlement setup banner
    private var entitlementBanner: some View {
        VStack(alignment: .leading, spacing: 10) {
            HStack(spacing: 8) {
                Image(systemName: "exclamationmark.triangle.fill")
                    .foregroundColor(.orange)
                Text("Primo accesso — configurazione richiesta")
                    .font(.caption.bold())
                    .foregroundColor(.orange)
            }
            Text("Per usare la VPN è necessario:")
                .font(.caption2.bold())
                .foregroundColor(.primary)
            VStack(alignment: .leading, spacing: 4) {
                setupStep(num: "1", text: "Su developer.apple.com → Identifiers → \(Bundle.main.bundleIdentifier ?? "eu.cyben.guard.ios") → abilita **Personal VPN**")
                setupStep(num: "2", text: "Rigenera il provisioning profile e reimportalo in Xcode")
                setupStep(num: "3", text: "Tocca **Connetti VPN** — le credenziali vengono scaricate automaticamente")
            }
        }
        .padding(12)
        .background(Color.orange.opacity(0.08))
        .cornerRadius(12)
        .overlay(RoundedRectangle(cornerRadius: 12).stroke(Color.orange.opacity(0.3), lineWidth: 1))
    }

    @ViewBuilder
    private func setupStep(num: String, text: String) -> some View {
        HStack(alignment: .top, spacing: 8) {
            Text(num)
                .font(.system(size: 10, weight: .bold))
                .frame(width: 16, height: 16)
                .background(Color.orange.opacity(0.2))
                .clipShape(Circle())
                .foregroundColor(.orange)
            Text(LocalizedStringKey(text))
                .font(.caption2)
                .foregroundColor(.secondary)
                .fixedSize(horizontal: false, vertical: true)
        }
    }

    @ViewBuilder
    private func errorBanner(_ message: String) -> some View {
        HStack(alignment: .top, spacing: 8) {
            Image(systemName: "xmark.octagon.fill")
                .foregroundColor(.red)
                .font(.subheadline)
            Text(message)
                .font(.caption)
                .foregroundColor(.red)
            Spacer()
            Button { connectError = nil } label: {
                Image(systemName: "xmark").font(.caption2).foregroundColor(.secondary)
            }
        }
        .padding(12)
        .background(Color.red.opacity(0.08))
        .cornerRadius(12)
        .overlay(RoundedRectangle(cornerRadius: 12).stroke(Color.red.opacity(0.25), lineWidth: 1))
    }

    // MARK: - Location card
    private var locationCard: some View {
        Button { showServerPicker = true } label: {
            HStack(spacing: 14) {
                // Flag circle
                ZStack {
                    Circle()
                        .fill(Color.blue.opacity(0.12))
                        .frame(width: 52, height: 52)
                    Text(selectedServer?.flag ?? "🌍")
                        .font(.system(size: 28))
                }

                VStack(alignment: .leading, spacing: 3) {
                    Text("Posizione selezionata")
                        .font(.caption)
                        .foregroundColor(.secondary)
                    Text(selectedServer.map { "\($0.country) — \($0.city)" } ?? "Seleziona server")
                        .font(.subheadline.bold())
                        .foregroundColor(.primary)
                    if let srv = selectedServer {
                        HStack(spacing: 4) {
                            Image(systemName: "circle.fill")
                                .font(.system(size: 6))
                                .foregroundColor(loadColor(srv.load))
                            Text("Carico: \(srv.load)%")
                                .font(.caption2)
                                .foregroundColor(.secondary)
                        }
                    }
                }

                Spacer()

                HStack(spacing: 4) {
                    Text("Cambia")
                        .font(.caption.bold())
                        .foregroundColor(.blue)
                    Image(systemName: "chevron.right")
                        .font(.caption.bold())
                        .foregroundColor(.blue)
                }
            }
            .padding(14)
            .background(Color(.secondarySystemGroupedBackground))
            .cornerRadius(14)
            .overlay(
                RoundedRectangle(cornerRadius: 14)
                    .stroke(Color.blue.opacity(0.25), lineWidth: 1)
            )
        }
        .buttonStyle(.plain)
    }

    private func loadColor(_ load: Int) -> Color {
        if load < 35 { return .green }
        if load < 65 { return .orange }
        return .red
    }

    // MARK: - Connect button
    private var connectButton: some View {
        Button {
            connectError = nil
            if vpnManager.isConnected {
                vpnManager.disconnect()
            } else {
                // Always fetch fresh credentials from server — no manual password needed
                Task {
                    do {
                        let creds = try await APIService.shared.fetchVPNCredentials()
                        try await vpnManager.setupAndConnect(username: creds.username, password: creds.password)
                    } catch {
                        connectError = error.localizedDescription
                    }
                }
            }
        } label: {
            HStack(spacing: 10) {
                if vpnManager.isTransitioning {
                    ProgressView()
                        .progressViewStyle(.circular)
                        .tint(.white)
                        .scaleEffect(0.9)
                } else {
                    Image(systemName: vpnManager.isConnected ? "stop.circle.fill" : "play.circle.fill")
                        .font(.title3)
                }
                VStack(alignment: .leading, spacing: 1) {
                    Text(vpnManager.isTransitioning
                         ? (vpnManager.status == .connecting ? "Connessione in corso..." : "Disconnessione in corso...")
                         : vpnManager.isConnected ? "Disconnetti VPN" : "Connetti VPN")
                    .fontWeight(.semibold)
                    if !vpnManager.isTransitioning, let srv = selectedServer {
                        Text("\(srv.flag) \(srv.city)")
                            .font(.caption)
                            .opacity(0.8)
                    }
                }
                Spacer()
            }
            .frame(maxWidth: .infinity)
            .padding(16)
            .background(vpnManager.isConnected ? Color.red.opacity(0.85) : Color.blue)
            .foregroundColor(.white)
            .cornerRadius(14)
        }
        .disabled(vpnManager.isTransitioning)
    }

    // MARK: - Features list
    private var featuresList: some View {
        VStack(alignment: .leading, spacing: 10) {
            featureRow(icon: "lock.shield.fill", color: .blue, text: "Protocollo IKEv2 con AES-256")
            featureRow(icon: "shield.lefthalf.filled", color: .purple, text: "DNS sicuro: blocco malware, phishing e adware")
            featureRow(icon: "wifi.exclamationmark", color: .orange, text: "Protezione automatica su reti WiFi pubbliche")
            featureRow(icon: "eye.slash.fill", color: .secondary, text: "Nessun log delle attività di navigazione")
            featureRow(icon: "globe", color: .green, text: "1 posizione attiva · altre in arrivo")
        }
        .padding(14)
        .background(Color(.secondarySystemGroupedBackground))
        .cornerRadius(12)
    }

    @ViewBuilder
    private func featureRow(icon: String, color: Color, text: String) -> some View {
        HStack(spacing: 10) {
            Image(systemName: icon)
                .font(.caption)
                .foregroundColor(color)
                .frame(width: 20)
            Text(text)
                .font(.caption)
                .foregroundColor(.secondary)
        }
    }

    // MARK: - DNS Protection card
    private var dnsProtectionCard: some View {
        VStack(alignment: .leading, spacing: 12) {
            // Header
            HStack(spacing: 8) {
                Image(systemName: "shield.lefthalf.filled")
                    .font(.subheadline.bold())
                    .foregroundColor(.purple)
                Text("Protezione DNS attiva")
                    .font(.subheadline.bold())
                    .foregroundColor(.primary)
                Spacer()
                if let stats = dnsStats {
                    Image(systemName: stats.active ? "checkmark.circle.fill" : "minus.circle.fill")
                        .foregroundColor(stats.active ? .green : .secondary)
                        .font(.subheadline)
                } else {
                    ProgressView().scaleEffect(0.7)
                }
            }

            Divider()

            // Stats row
            HStack(spacing: 0) {
                dnsStatCell(
                    value: dnsStats.map { formatNumber($0.blockedDomains) } ?? "—",
                    label: "Domini bloccati",
                    icon: "xmark.shield.fill",
                    color: .red
                )
                Divider().frame(height: 36)
                dnsStatCell(
                    value: dnsStats?.lastUpdated.map { formatRelativeDate($0) } ?? "—",
                    label: "Aggiornata",
                    icon: "arrow.clockwise.circle.fill",
                    color: .blue
                )
                Divider().frame(height: 36)
                dnsStatCell(
                    value: "Lun 03:00",
                    label: "Prossimo update",
                    icon: "calendar.circle.fill",
                    color: .orange
                )
            }

            // Source note
            if let stats = dnsStats {
                Text(stats.feedSource)
                    .font(.caption2)
                    .foregroundColor(.secondary)
                    .lineLimit(1)
            }
        }
        .padding(14)
        .background(Color(.secondarySystemGroupedBackground))
        .cornerRadius(12)
        .overlay(
            RoundedRectangle(cornerRadius: 12)
                .stroke(Color.purple.opacity(0.2), lineWidth: 1)
        )
    }

    @ViewBuilder
    private func dnsStatCell(value: String, label: String, icon: String, color: Color) -> some View {
        VStack(spacing: 4) {
            Image(systemName: icon)
                .font(.caption)
                .foregroundColor(color)
            Text(value)
                .font(.caption.bold())
                .foregroundColor(.primary)
                .minimumScaleFactor(0.7)
                .lineLimit(1)
            Text(label)
                .font(.system(size: 9))
                .foregroundColor(.secondary)
                .multilineTextAlignment(.center)
        }
        .frame(maxWidth: .infinity)
    }

    private func formatNumber(_ n: Int) -> String {
        if n >= 1_000_000 { return String(format: "%.1fM", Double(n) / 1_000_000) }
        if n >= 1_000 { return String(format: "%.0fk", Double(n) / 1_000) }
        return "\(n)"
    }

    private func formatRelativeDate(_ iso: String) -> String {
        let fmt = ISO8601DateFormatter()
        fmt.formatOptions = [.withInternetDateTime, .withFractionalSeconds]
        guard let date = fmt.date(from: iso) ?? ISO8601DateFormatter().date(from: iso) else { return "—" }
        let days = Calendar.current.dateComponents([.day], from: date, to: Date()).day ?? 0
        if days == 0 { return "Oggi" }
        if days == 1 { return "Ieri" }
        return "\(days)gg fa"
    }

    // MARK: - Blocked query polling
    private func startBlockedQueryPolling() {
        stopBlockedQueryPolling()
        lastPollTime = Date()
        pollTask = Task {
            while !Task.isCancelled {
                await checkAndNotifyBlocked()
                try? await Task.sleep(nanoseconds: 30_000_000_000) // 30s
            }
        }
    }

    private func stopBlockedQueryPolling() {
        pollTask?.cancel()
        pollTask = nil
    }

    private func checkAndNotifyBlocked() async {
        guard let resp = try? await APIService.shared.fetchVPNBlockedQueries(since: lastPollTime),
              resp.vpnConnected else { return }

        let newQueries = resp.blockedQueries
        lastPollTime = Date()

        guard !newQueries.isEmpty else { return }
        recentBlocked = newQueries

        // Show local notification for each new blocked domain
        let center = UNUserNotificationCenter.current()
        for query in newQueries.prefix(3) {
            let content = UNMutableNotificationContent()
            content.title = "Cyben Guard ha bloccato un sito"
            content.body = "\(query.domain) è un dominio pericoloso — accesso bloccato dal DNS sicuro."
            content.sound = .default
            content.categoryIdentifier = "VPN_BLOCK"

            let request = UNNotificationRequest(
                identifier: "vpn-block-\(query.domain)-\(Int(query.blockedAt))",
                content: content,
                trigger: nil
            )
            try? await center.add(request)
        }

        if newQueries.count > 3 {
            let content = UNMutableNotificationContent()
            content.title = "Cyben Guard — \(newQueries.count) minacce bloccate"
            content.body = "Il DNS sicuro ha bloccato \(newQueries.count) domini pericolosi nell'ultimo minuto."
            content.sound = .default
            let request = UNNotificationRequest(
                identifier: "vpn-block-batch-\(Int(Date().timeIntervalSince1970))",
                content: content,
                trigger: nil
            )
            try? await center.add(request)
        }
    }

    // MARK: - Advanced config row
    private var advancedConfigRow: some View {
        Button { showAdvancedConfig = true } label: {
            HStack {
                Image(systemName: "gearshape.fill").foregroundColor(.secondary)
                Text("Configurazione avanzata").font(.subheadline).foregroundColor(.primary)
                Spacer()
                Image(systemName: "chevron.right").font(.caption).foregroundColor(.secondary)
            }
            .padding(14)
            .background(Color(.secondarySystemGroupedBackground))
            .cornerRadius(12)
        }
        .buttonStyle(.plain)
    }
}

// MARK: - Password Sheet (first-time setup)
struct VPNPasswordSheet: View {
    let selectedServer: CybenVPNServer?
    let onConnect: (String) -> Void

    @Environment(\.dismiss) var dismiss
    @State private var password = ""
    @State private var isConnecting = false

    var body: some View {
        NavigationView {
            VStack(spacing: 24) {
                // Header
                VStack(spacing: 10) {
                    ZStack {
                        Circle().fill(Color.blue.opacity(0.12)).frame(width: 64, height: 64)
                        Text(selectedServer?.flag ?? "🌍").font(.system(size: 34))
                    }
                    Text(selectedServer.map { "\($0.country) — \($0.city)" } ?? "Server VPN")
                        .font(.headline)
                    Text("Inserisci la password IKEv2 per configurare la connessione VPN. Verrà salvata in modo sicuro nel Keychain del dispositivo.")
                        .font(.caption)
                        .foregroundColor(.secondary)
                        .multilineTextAlignment(.center)
                        .padding(.horizontal)
                }
                .padding(.top, 24)

                // Password field
                VStack(alignment: .leading, spacing: 6) {
                    Text("Password VPN").font(.caption.bold()).foregroundColor(.secondary)
                    SecureField("", text: $password, prompt: Text("Password IKEv2").foregroundColor(.secondary))
                        .padding(12)
                        .background(Color(.secondarySystemGroupedBackground))
                        .cornerRadius(10)
                        .overlay(RoundedRectangle(cornerRadius: 10).stroke(Color(.separator), lineWidth: 1))
                        .textContentType(.password)
                }
                .padding(.horizontal)

                // Note
                HStack(alignment: .top, spacing: 8) {
                    Image(systemName: "info.circle.fill").foregroundColor(.blue).font(.caption)
                    Text("La password è quella del tuo account VPN Cyben, non quella dell'app.")
                        .font(.caption2).foregroundColor(.secondary)
                }
                .padding(.horizontal)

                Spacer()

                // Connect button
                Button {
                    isConnecting = true
                    dismiss()
                    onConnect(password)
                } label: {
                    HStack(spacing: 8) {
                        if isConnecting {
                            ProgressView().progressViewStyle(.circular).tint(.white).scaleEffect(0.85)
                        }
                        Text("Configura e connetti").fontWeight(.semibold)
                    }
                    .frame(maxWidth: .infinity).padding(16)
                    .background(password.isEmpty ? Color.blue.opacity(0.4) : Color.blue)
                    .foregroundColor(.white).cornerRadius(14)
                }
                .disabled(password.isEmpty || isConnecting)
                .padding(.horizontal)
                .padding(.bottom, 8)
            }
            .navigationTitle("Password VPN")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .navigationBarLeading) {
                    Button("Annulla") { dismiss() }
                }
            }
        }
    }
}

// MARK: - Server Picker Sheet
struct VPNServerPickerSheet: View {
    @Binding var selected: CybenVPNServer?
    @Environment(\.dismiss) var dismiss
    let onSelect: (CybenVPNServer) -> Void

    var availableServers: [CybenVPNServer] { cybenVPNServers.filter { $0.available } }
    var comingSoonServers: [CybenVPNServer] { cybenVPNServers.filter { !$0.available } }

    var body: some View {
        NavigationView {
            List {
                // ── Active servers ──────────────────────────────────────
                Section {
                    ForEach(availableServers) { server in
                        Button {
                            selected = server
                            onSelect(server)
                            dismiss()
                        } label: {
                            serverRow(server: server, available: true)
                        }
                        .buttonStyle(.plain)
                        .listRowBackground(
                            selected?.id == server.id
                                ? Color.blue.opacity(0.07)
                                : Color(.secondarySystemGroupedBackground)
                        )
                    }
                } header: {
                    Text("Server disponibili")
                } footer: {
                    Text("Il carico indica il traffico attuale sul server.")
                }

                // ── Coming soon ─────────────────────────────────────────
                Section {
                    ForEach(comingSoonServers) { server in
                        serverRow(server: server, available: false)
                            .listRowBackground(Color(.secondarySystemGroupedBackground))
                    }
                } header: {
                    Text("Presto disponibili")
                }
            }
            .listStyle(.insetGrouped)
            .navigationTitle("Scegli posizione")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .navigationBarTrailing) {
                    Button("Chiudi") { dismiss() }
                }
            }
        }
    }

    @ViewBuilder
    private func serverRow(server: CybenVPNServer, available: Bool) -> some View {
        HStack(spacing: 14) {
            Text(server.flag)
                .font(.title2)
                .frame(width: 36)
                .opacity(available ? 1.0 : 0.45)

            VStack(alignment: .leading, spacing: 2) {
                Text(server.country)
                    .font(.subheadline.bold())
                    .foregroundColor(available ? .primary : .secondary)
                Text(server.city)
                    .font(.caption)
                    .foregroundColor(.secondary)
            }

            Spacer()

            if available {
                HStack(spacing: 4) {
                    Circle()
                        .fill(loadColor(server.load))
                        .frame(width: 7, height: 7)
                    Text("\(server.load)%")
                        .font(.caption2)
                        .foregroundColor(.secondary)
                }
                if selected?.id == server.id {
                    Image(systemName: "checkmark.circle.fill")
                        .foregroundColor(.blue)
                        .font(.subheadline)
                }
            } else {
                Text("Presto disponibile")
                    .font(.caption2.bold())
                    .foregroundColor(.orange)
                    .padding(.horizontal, 8)
                    .padding(.vertical, 3)
                    .background(Color.orange.opacity(0.12))
                    .cornerRadius(6)
            }
        }
        .contentShape(Rectangle())
        .opacity(available ? 1.0 : 0.75)
    }

    private func loadColor(_ load: Int) -> Color {
        if load < 35 { return .green }
        if load < 65 { return .orange }
        return .red
    }
}

// MARK: - Status Card
struct VPNStatusCard: View {
    @ObservedObject var vpnManager: VPNManager

    var statusColor: Color {
        switch vpnManager.status {
        case .connected:                          return .green
        case .connecting, .disconnecting, .reasserting: return .orange
        default:                                  return .secondary
        }
    }

    var statusLabel: String {
        switch vpnManager.status {
        case .connected:      return "Connesso"
        case .connecting:     return "Connessione in corso..."
        case .disconnecting:  return "Disconnessione in corso..."
        case .reasserting:    return "Riconnessione..."
        default:              return "Disconnesso"
        }
    }

    var statusSubtitle: String {
        switch vpnManager.status {
        case .connected:                              return "Il tuo traffico è protetto"
        case .connecting, .reasserting:               return "Stabilendo la connessione cifrata..."
        case .disconnecting:                          return "Chiusura del tunnel VPN..."
        default:                                      return "Nessuna connessione VPN attiva"
        }
    }

    var iconName: String {
        switch vpnManager.status {
        case .connected:                          return "lock.shield.fill"
        case .connecting, .reasserting:           return "lock.shield"
        default:                                  return "lock.open.fill"
        }
    }

    var body: some View {
        HStack(spacing: 16) {
            ZStack {
                Circle().fill(statusColor.opacity(0.15)).frame(width: 56, height: 56)
                if vpnManager.isTransitioning {
                    ProgressView()
                        .progressViewStyle(.circular)
                        .tint(statusColor)
                        .scaleEffect(0.9)
                } else {
                    Image(systemName: iconName)
                        .font(.title2).foregroundColor(statusColor)
                }
            }
            VStack(alignment: .leading, spacing: 4) {
                Text(statusLabel).font(.headline)
                Text(statusSubtitle)
                    .font(.caption).foregroundColor(.secondary)
            }
            Spacer()
            Circle().fill(statusColor).frame(width: 10, height: 10)
        }
        .padding()
        .background(Color(.secondarySystemGroupedBackground))
        .cornerRadius(14)
    }
}

// MARK: - Config Sheet (advanced)
struct VPNConfigSheet: View {
    @ObservedObject var vpnManager: VPNManager
    @Environment(\.dismiss) var dismiss
    @State private var serverAddress = ""
    @State private var username = ""
    @State private var remoteId = ""

    var body: some View {
        NavigationView {
            Form {
                Section("Server VPN personalizzato") {
                    TextField("Indirizzo server (es. vpn.cyben.eu)", text: $serverAddress)
                        .autocapitalization(.none).autocorrectionDisabled()
                    TextField("Remote Identifier", text: $remoteId)
                        .autocapitalization(.none).autocorrectionDisabled()
                }
                Section("Credenziali") {
                    TextField("Username", text: $username)
                        .autocapitalization(.none).autocorrectionDisabled()
                }
                Section {
                    Text("La password VPN sarà richiesta al momento della connessione e salvata nel Keychain sicuro del dispositivo.")
                        .font(.caption).foregroundColor(.secondary)
                }
            }
            .navigationTitle("Configurazione avanzata")
            .navigationBarTitleDisplayMode(.inline)
            .onAppear {
                serverAddress = vpnManager.config.serverAddress
                username = vpnManager.config.username
                remoteId = vpnManager.config.remoteIdentifier
            }
            .toolbar {
                ToolbarItem(placement: .navigationBarLeading) { Button("Annulla") { dismiss() } }
                ToolbarItem(placement: .navigationBarTrailing) {
                    Button("Salva") {
                        vpnManager.config.serverAddress = serverAddress
                        vpnManager.config.username = username
                        vpnManager.config.remoteIdentifier = remoteId
                        vpnManager.saveConfig()
                        dismiss()
                    }
                    .fontWeight(.semibold)
                    .disabled(serverAddress.isEmpty)
                }
            }
        }
    }
}
