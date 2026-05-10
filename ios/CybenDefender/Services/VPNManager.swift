import Foundation
import NetworkExtension
import Combine

@MainActor
final class VPNManager: ObservableObject {
    static let shared = VPNManager()
    private init() {}

    @Published var status: NEVPNStatus = .disconnected
    @Published var config: VPNConfig = VPNConfig.default
    @Published var lastError: String?

    private let manager = NEVPNManager.shared()
    private var statusObserver: AnyCancellable?

    // MARK: - Lifecycle
    func initialize() async {
        loadConfig()
        await loadPreferences()
        observeStatus()
    }

    private func observeStatus() {
        statusObserver = NotificationCenter.default
            .publisher(for: .NEVPNStatusDidChange)
            .receive(on: RunLoop.main)
            .sink { [weak self] _ in
                self?.status = self?.manager.connection.status ?? .disconnected
            }
    }

    private func loadPreferences() async {
        do {
            try await manager.loadFromPreferences()
            status = manager.connection.status
        } catch {
            status = .disconnected
        }
    }

    // MARK: - Config Persistence
    func loadConfig() {
        let keychain = KeychainService.shared
        config.serverAddress = keychain.load(for: "vpnServer") ?? ""
        config.username = keychain.load(for: "vpnUsername") ?? ""
        config.remoteIdentifier = keychain.load(for: "vpnRemoteId") ?? ""
        config.localIdentifier = keychain.load(for: "vpnLocalId") ?? ""
        if let autoStr = keychain.load(for: "vpnAutoConnect") {
            config.autoConnect = autoStr == "true"
        }
        if let wifiStr = keychain.load(for: "vpnConnectOnUntrustedWifi") {
            config.connectOnUntrustedWifi = wifiStr == "true"
        }
    }

    func saveConfig() {
        let keychain = KeychainService.shared
        keychain.save(config.serverAddress, for: "vpnServer")
        keychain.save(config.username, for: "vpnUsername")
        keychain.save(config.remoteIdentifier, for: "vpnRemoteId")
        keychain.save(config.localIdentifier, for: "vpnLocalId")
        keychain.save(config.autoConnect ? "true" : "false", for: "vpnAutoConnect")
        keychain.save(config.connectOnUntrustedWifi ? "true" : "false", for: "vpnConnectOnUntrustedWifi")
    }

    // MARK: - Save VPN Profile
    func saveVPNProfile(password: String) async throws {
        guard !config.serverAddress.isEmpty else {
            throw AppError.network("Indirizzo server VPN mancante")
        }
        try await manager.loadFromPreferences()

        let proto = NEVPNProtocolIKEv2()
        proto.serverAddress = config.serverAddress
        proto.username = config.username.isEmpty ? "cyben" : config.username
        proto.remoteIdentifier = config.remoteIdentifier.isEmpty ? config.serverAddress : config.remoteIdentifier
        proto.localIdentifier = config.localIdentifier.isEmpty ? proto.username : config.localIdentifier
        proto.useExtendedAuthentication = true
        proto.authenticationMethod = .none
        proto.disconnectOnSleep = false

        // IKE Security
        proto.ikeSecurityAssociationParameters.encryptionAlgorithm = .algorithmAES256
        proto.ikeSecurityAssociationParameters.integrityAlgorithm = .SHA256
        proto.ikeSecurityAssociationParameters.diffieHellmanGroup = .group14
        proto.ikeSecurityAssociationParameters.lifetimeMinutes = 1440

        // Child SA Security
        proto.childSecurityAssociationParameters.encryptionAlgorithm = .algorithmAES256
        proto.childSecurityAssociationParameters.integrityAlgorithm = .SHA256
        proto.childSecurityAssociationParameters.diffieHellmanGroup = .group14
        proto.childSecurityAssociationParameters.lifetimeMinutes = 60

        // Store password in Keychain for VPN
        if !password.isEmpty {
            let keychainRef = try saveVPNPassword(password)
            proto.passwordReference = keychainRef
        }

        manager.protocolConfiguration = proto
        manager.localizedDescription = "Cyben VPN"
        manager.isEnabled = true
        manager.isOnDemandEnabled = config.connectOnUntrustedWifi

        if config.connectOnUntrustedWifi {
            let rule = NEOnDemandRuleConnect()
            rule.interfaceTypeMatch = .wiFi
            manager.onDemandRules = [rule]
        }

        try await manager.saveToPreferences()
        try await manager.loadFromPreferences()
    }

    // MARK: - Setup + Connect (call this on first connect or when server changes)
    func setupAndConnect(username: String, password: String) async throws {
        guard !config.serverAddress.isEmpty else {
            throw AppError.network("Seleziona prima un server VPN")
        }
        // Use the server-provided username (email) as both username and local identifier
        config.username = username
        config.localIdentifier = username
        config.remoteIdentifier = config.serverAddress
        saveConfig()
        lastError = nil
        do {
            try await saveVPNProfile(password: password)
            try manager.connection.startVPNTunnel()
            UserDefaults.standard.set(true, forKey: "vpnProfileConfigured")
        } catch let err as NSError {
            lastError = friendlyVPNError(err)
            throw AppError.network(friendlyVPNError(err))
        }
    }

    // MARK: - Connect (uses existing profile)
    func connect() async throws {
        guard !config.serverAddress.isEmpty else {
            throw AppError.network("Seleziona prima un server VPN")
        }
        lastError = nil
        do {
            try manager.connection.startVPNTunnel()
        } catch let err as NSError {
            lastError = friendlyVPNError(err)
            throw AppError.network(friendlyVPNError(err))
        }
    }

    var isProfileConfigured: Bool {
        UserDefaults.standard.bool(forKey: "vpnProfileConfigured")
    }

    private func friendlyVPNError(_ err: NSError) -> String {
        // NEVPNError codes
        switch err.code {
        case 1:  return "VPN non configurata. Inserisci la password e riprova."
        case 4:  return "Entitlement Personal VPN non abilitato sul profilo di firma. Abilita la capability 'Personal VPN' su developer.apple.com e rigenera il provisioning profile."
        case 5:  return "Connessione VPN rifiutata dal server. Controlla l'indirizzo e le credenziali."
        case 8:  return "Timeout di connessione. Verifica la tua connessione internet."
        default: return err.localizedDescription
        }
    }

    func disconnect() {
        manager.connection.stopVPNTunnel()
        lastError = nil
    }

    func toggle() async throws {
        switch status {
        case .connected, .connecting:
            disconnect()
        case .disconnected, .disconnecting, .invalid, .reasserting:
            try await connect()
        @unknown default:
            try await connect()
        }
    }

    // MARK: - Helpers
    var statusLabel: String {
        switch status {
        case .connected: return "Connesso"
        case .connecting: return "Connessione in corso..."
        case .disconnected: return "Disconnesso"
        case .disconnecting: return "Disconnessione in corso..."
        case .reasserting: return "Riconnessione..."
        case .invalid: return "Non configurato"
        @unknown default: return "Sconosciuto"
        }
    }

    var isConnected: Bool { status == .connected }
    var isTransitioning: Bool { status == .connecting || status == .disconnecting || status == .reasserting }

    // MARK: - Keychain VPN Password
    private func saveVPNPassword(_ password: String) throws -> Data {
        guard let passwordData = password.data(using: .utf8) else {
            throw AppError.network("Password non valida")
        }
        let query: [CFString: Any] = [
            kSecClass: kSecClassGenericPassword,
            kSecAttrService: "eu.cyben.guard.vpn",
            kSecAttrAccount: "vpnPassword",
            kSecAttrAccessible: kSecAttrAccessibleAlwaysThisDeviceOnly,
            kSecReturnPersistentRef: true,
            kSecValueData: passwordData
        ]
        SecItemDelete(query as CFDictionary)
        var result: AnyObject?
        let status = SecItemAdd(query as CFDictionary, &result)
        guard status == errSecSuccess, let ref = result as? Data else {
            throw AppError.network("Impossibile salvare la password VPN")
        }
        return ref
    }
}
