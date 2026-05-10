import Foundation
import LocalAuthentication

// MARK: - AppLockService
// Gestisce PIN locale (Keychain) + Face ID / Touch ID (LocalAuthentication).
// Il server NON riceve né il PIN né dati biometrici — tutto locale.

final class AppLockService: ObservableObject {
    static let shared = AppLockService()
    private init() {}

    private let pinKey        = "cyben_app_lock_pin"
    private let enabledKey    = "cyben_app_lock_enabled"
    private let biometricKey  = "cyben_app_lock_biometric"

    // MARK: - PIN setup

    var isPinSet: Bool {
        KeychainService.shared.load(for: pinKey) != nil
    }

    var isAppLockEnabled: Bool {
        get { UserDefaults.standard.bool(forKey: enabledKey) }
        set { UserDefaults.standard.set(newValue, forKey: enabledKey) }
    }

    var isBiometricEnabled: Bool {
        get { UserDefaults.standard.bool(forKey: biometricKey) }
        set { UserDefaults.standard.set(newValue, forKey: biometricKey) }
    }

    // Salva il PIN in Keychain (mai inviato al server)
    func setPin(_ pin: String) -> Bool {
        guard pin.count == 6, pin.allSatisfy(\.isNumber) else { return false }
        KeychainService.shared.save(pin, for: pinKey)
        isAppLockEnabled = true
        return true
    }

    // Verifica il PIN inserito dall'utente
    func verifyPin(_ input: String) -> Bool {
        guard let stored = KeychainService.shared.load(for: pinKey) else { return false }
        return input == stored
    }

    // Rimuove PIN e disattiva tutto
    func disableAppLock() {
        KeychainService.shared.delete(for: pinKey)
        isAppLockEnabled = false
        isBiometricEnabled = false
    }

    // MARK: - Biometria

    var biometricType: LABiometryType {
        let ctx = LAContext()
        var error: NSError?
        guard ctx.canEvaluatePolicy(.deviceOwnerAuthenticationWithBiometrics, error: &error) else {
            return .none
        }
        return ctx.biometryType
    }

    var biometricLabel: String {
        switch biometricType {
        case .faceID:   return "Face ID"
        case .touchID:  return "Touch ID"
        default:        return "Biometria"
        }
    }

    // Tenta autenticazione biometrica — ritorna true se riuscita
    func authenticateWithBiometrics(reason: String) async -> Bool {
        guard isBiometricEnabled else { return false }
        let ctx = LAContext()
        var error: NSError?
        guard ctx.canEvaluatePolicy(.deviceOwnerAuthenticationWithBiometrics, error: &error) else {
            return false
        }
        do {
            return try await ctx.evaluatePolicy(
                .deviceOwnerAuthenticationWithBiometrics,
                localizedReason: reason
            )
        } catch {
            return false
        }
    }

    // MARK: - Sync preferenze (solo flags, niente segreti)

    func syncPreferencesToServer(token: String) async {
        guard let url = URL(string: "https://cyben.eu/api/guard/me/preferences") else { return }
        var req = URLRequest(url: url)
        req.httpMethod = "PATCH"
        req.setValue("Bearer \(token)", forHTTPHeaderField: "Authorization")
        req.setValue("application/json", forHTTPHeaderField: "Content-Type")
        let body: [String: Any] = [
            "appLockEnabled": isAppLockEnabled,
            "biometricUnlockEnabled": isBiometricEnabled,
        ]
        req.httpBody = try? JSONSerialization.data(withJSONObject: body)
        _ = try? await URLSession.shared.data(for: req)
        // Fallimento silenzioso — lo sblocco resta comunque locale
    }
}
