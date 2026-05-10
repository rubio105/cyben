import IdentityLookup
import Foundation

// MARK: - SMS Filter Extension
// Target: CybenSMSFilter (bundle: eu.cyben.guard.ios.SMSFilter)
// In Xcode: File → New Target → Message Filter Extension
// Aggiungi App Group "group.eu.cyben.guard.ios" anche a questo target.
// IMPORTANTE: Aggiungi NSExtension → ILMessageFilterIndirectFilteringEnabled = NO in Info.plist
// (filtro locale, nessuna richiesta al server).

final class SMSFilterExtension: ILMessageFilterExtension {}

extension SMSFilterExtension: ILMessageFilterQueryHandling {

    func handle(
        _ queryRequest: ILMessageFilterQueryRequest,
        context: ILMessageFilterExtensionContext,
        completion: @escaping (ILMessageFilterQueryResponse) -> Void
    ) {
        let response = ILMessageFilterQueryResponse()
        let feed = loadFeed()
        let messageBody = (queryRequest.messageBody ?? "").lowercased()
        let sender = queryRequest.sender ?? ""

        // 1. Controlla se il mittente è un numero bloccato
        if let feed, isBlockedNumber(sender, in: feed.blockedPhones) {
            response.action = .filter
            completion(response)
            return
        }

        // 2. Controlla frasi bloccate (blocco diretto)
        if let feed {
            for phrase in feed.blockedPhrases {
                if messageBody.contains(phrase.lowercased()) {
                    response.action = .filter
                    completion(response)
                    return
                }
            }
        }

        // 3. Controlla domini sospetti nel testo
        if let feed {
            for domain in feed.suspiciousDomains {
                if messageBody.contains(domain.lowercased()) {
                    response.action = .filter
                    completion(response)
                    return
                }
            }
        }

        // 4. Controlla frasi sospette (filtra come junk)
        if let feed {
            for phrase in feed.suspiciousPhrases {
                if messageBody.contains(phrase.lowercased()) {
                    response.action = .filter
                    completion(response)
                    return
                }
            }
        }

        // 5. Controlla numero sospetto (non blocca, ma filtra come junk)
        if let feed, isBlockedNumber(sender, in: feed.suspiciousPhones) {
            response.action = .filter
            completion(response)
            return
        }

        response.action = .allow
        completion(response)
    }

    // MARK: - Helpers

    private func loadFeed() -> ProtectionFeed? {
        guard let defaults = UserDefaults(suiteName: "group.eu.cyben.guard.ios"),
              let data = defaults.data(forKey: "protection_feed_cache") else { return nil }
        return try? JSONDecoder().decode(ProtectionFeed.self, from: data)
    }

    private func isBlockedNumber(_ sender: String, in list: [String]) -> Bool {
        let clean = sender.replacingOccurrences(of: "+", with: "")
                          .filter(\.isNumber)
        return list.contains { number in
            let n = number.replacingOccurrences(of: "+", with: "").filter(\.isNumber)
            return n == clean || clean.hasSuffix(n) || n.hasSuffix(clean)
        }
    }
}

// MARK: - Shared model (copia locale per il target Extension)
private struct ProtectionFeed: Codable {
    var blockedPhones:     [String]
    var suspiciousPhones:  [String]
    var trustedPhones:     [String]
    var suspiciousDomains: [String]
    var blockedPhrases:    [String]
    var suspiciousPhrases: [String]
    var lastUpdatedAt:     String
}
