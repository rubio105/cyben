import Foundation
import CallKit

// MARK: - Call Directory Extension
// Target: CybenCallDirectory (bundle: eu.cyben.guard.ios.CallDirectory)
// In Xcode: File → New Target → Call Directory Extension
// Aggiungi App Group "group.eu.cyben.guard.ios" anche a questo target.

final class CallDirectoryHandler: CXCallDirectoryProvider {

    override func beginRequest(with context: CXCallDirectoryExtensionContext) {
        let feed = loadFeed()

        if context.isIncremental {
            context.removeAllBlockingEntries()
            context.removeAllIdentificationEntries()
        }

        // Numeri da bloccare completamente
        let blocked = normalizedNumbers(feed?.blockedPhones ?? [])
        for number in blocked {
            context.addBlockingEntry(withNextSequentialPhoneNumber: number)
        }

        // Numeri sospetti — identificati come "Cyben: Sospetto"
        let suspicious = normalizedNumbers(feed?.suspiciousPhones ?? [])
        for number in suspicious {
            context.addIdentificationEntry(
                withNextSequentialPhoneNumber: number,
                label: "Cyben: Numero Sospetto"
            )
        }

        context.completeRequest()
    }

    // Legge il feed dalla cache condivisa (App Group, offline)
    private func loadFeed() -> ProtectionFeed? {
        guard let defaults = UserDefaults(suiteName: "group.eu.cyben.guard.ios"),
              let data = defaults.data(forKey: "protection_feed_cache") else { return nil }
        return try? JSONDecoder().decode(ProtectionFeed.self, from: data)
    }

    // Converte i numeri in CXCallDirectoryPhoneNumber (Int64, ordinati)
    private func normalizedNumbers(_ raw: [String]) -> [CXCallDirectoryPhoneNumber] {
        raw
            .compactMap { s -> CXCallDirectoryPhoneNumber? in
                let digits = s.replacingOccurrences(of: "+", with: "")
                              .filter(\.isNumber)
                return CXCallDirectoryPhoneNumber(digits)
            }
            .sorted()
    }
}

// MARK: - Shared model (copia locale — Swift non condivide file tra target)
private struct ProtectionFeed: Codable {
    var blockedPhones:     [String]
    var suspiciousPhones:  [String]
    var trustedPhones:     [String]
    var suspiciousDomains: [String]
    var blockedPhrases:    [String]
    var suspiciousPhrases: [String]
    var lastUpdatedAt:     String
}
