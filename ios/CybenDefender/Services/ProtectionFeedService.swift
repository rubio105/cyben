import Foundation
import CallKit

// MARK: - Shared App Group
// Aggiungi "group.eu.cyben.guard.ios" in Xcode → Signing & Capabilities
// per il target principale E per entrambe le Extension.
let kAppGroup = "group.eu.cyben.guard.ios"

// MARK: - Model
struct ProtectionFeed: Codable {
    var blockedPhones:     [String]
    var suspiciousPhones:  [String]
    var trustedPhones:     [String]
    var suspiciousDomains: [String]
    var blockedPhrases:    [String]
    var suspiciousPhrases: [String]
    var lastUpdatedAt:     String
}

// MARK: - Service
final class ProtectionFeedService {
    static let shared = ProtectionFeedService()
    private init() {}

    private let cacheKey   = "protection_feed_cache"
    private let maxAgeKey  = "protection_feed_fetched_at"
    private let maxAge: TimeInterval = 6 * 3600 // ricarica ogni 6 ore

    // Legge il feed dalla cache condivisa (usato dalle Extension offline)
    func cachedFeed() -> ProtectionFeed? {
        guard let defaults = UserDefaults(suiteName: kAppGroup),
              let data = defaults.data(forKey: cacheKey) else { return nil }
        return try? JSONDecoder().decode(ProtectionFeed.self, from: data)
    }

    // Scarica il feed dal server e aggiorna la cache condivisa
    @discardableResult
    func refresh(token: String, force: Bool = false) async -> Bool {
        let defaults = UserDefaults(suiteName: kAppGroup)
        if !force, let lastFetch = defaults?.double(forKey: maxAgeKey),
           Date().timeIntervalSince1970 - lastFetch < maxAge {
            return true // cache ancora valida
        }

        guard let url = URL(string: "https://cyben.eu/api/guard/protection/feed") else { return false }
        var req = URLRequest(url: url)
        req.setValue("Bearer \(token)", forHTTPHeaderField: "Authorization")
        req.setValue("CybenGuard-iOS/1.0", forHTTPHeaderField: "User-Agent")
        req.timeoutInterval = 15

        do {
            let (data, response) = try await URLSession.shared.data(for: req)
            guard (response as? HTTPURLResponse)?.statusCode == 200 else { return false }
            let feed = try JSONDecoder().decode(ProtectionFeed.self, from: data)
            defaults?.set(data, forKey: cacheKey)
            defaults?.set(Date().timeIntervalSince1970, forKey: maxAgeKey)
            // Notifica la Call Directory Extension di aggiornarsi
            await reloadCallDirectory()
            return true
        } catch {
            return false
        }
    }

    // Forza il reload della Call Directory dopo aggiornamento feed
    private func reloadCallDirectory() async {
        await withCheckedContinuation { continuation in
            CXCallDirectoryManager.sharedInstance.reloadExtension(
                withIdentifier: "eu.cyben.guard.ios.CallDirectory"
            ) { _ in continuation.resume() }
        }
    }
}
