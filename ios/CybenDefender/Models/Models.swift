import Foundation

// MARK: - Auth
struct GuardAuthResponse: Decodable {
    let token: String?
    let user: GuardUser?
    let error: String?
    let ok: Bool?
    let needsVerification: Bool?
}

struct GuardUser: Decodable, Identifiable {
    let id: Int
    let name: String
    let email: String
    let plan: String            // "none" | "basic" | "premium"
    let subscriptionStatus: String?
    let subscriptionInterval: String?  // "monthly" | "annual" | nil

    /// Normalized plan string: lowercased and whitespace-trimmed.
    /// Use this for all comparisons instead of the raw `plan` field.
    var normalizedPlan: String { plan.trimmingCharacters(in: .whitespacesAndNewlines).lowercased() }

    var isPaid: Bool    { normalizedPlan == "basic" || normalizedPlan == "premium" }
    var isPremium: Bool { normalizedPlan == "premium" }
    var isBasic: Bool   { normalizedPlan == "basic" }
    var hasNoPlan: Bool { normalizedPlan == "none" || normalizedPlan.isEmpty }

    var planLabel: String {
        switch normalizedPlan {
        case "basic":   return "Base"
        case "premium": return "Premium"
        default:        return "Gratuito"
        }
    }

    init(id: Int, name: String, email: String, plan: String,
         subscriptionStatus: String? = nil, subscriptionInterval: String? = nil) {
        self.id = id
        self.name = name
        self.email = email
        self.plan = plan
        self.subscriptionStatus = subscriptionStatus
        self.subscriptionInterval = subscriptionInterval
    }
}

// MARK: - Analysis
struct GuardAnalysis: Decodable, Identifiable {
    let id: Int
    let inputText: String?
    let inputType: String?
    let riskLevel: String?      // "safe" | "suspicious" | "dangerous" | "unknown"
    let riskScore: Int?
    let explanation: String?
    let recommendation: String?
    let indicators: [String]?
    let createdAt: String?

    var riskColor: String {
        switch riskLevel {
        case "safe": return "green"
        case "suspicious": return "orange"
        case "dangerous": return "red"
        default: return "gray"
        }
    }
    var riskIcon: String {
        switch riskLevel {
        case "safe": return "checkmark.shield.fill"
        case "suspicious": return "exclamationmark.triangle.fill"
        case "dangerous": return "xmark.shield.fill"
        default: return "questionmark.circle.fill"
        }
    }
    var riskLabel: String {
        switch riskLevel {
        case "safe": return "Sicuro"
        case "suspicious": return "Sospetto"
        case "dangerous": return "Pericoloso"
        default: return "Sconosciuto"
        }
    }
}

struct AnalyzeResponse: Decodable {
    let analysis: GuardAnalysis?
    let conversationalMessage: String?
    let dailyUsed: Int?
    let dailyLimit: Int?
    let error: String?
}

// MARK: - Breach Monitoring
struct GuardMonitoredEmail: Decodable, Identifiable {
    let id: Int
    let email: String
    let label: String?
    let lastChecked: String?
    let breachCount: Int?
}

struct GuardBreachAlert: Decodable, Identifiable {
    let id: Int
    let emailId: Int?
    let breachName: String?
    let breachDate: String?
    let dataClasses: [String]?
    let description: String?
    let isRead: Bool?
    let createdAt: String?
}

struct BreachCheckResponse: Decodable {
    let found: Bool?
    let breaches: [BreachInfo]?
    let error: String?
}

struct BreachInfo: Decodable, Identifiable {
    let id = UUID()
    let name: String?
    let domain: String?
    let breachDate: String?
    let dataClasses: [String]?
    let pwnCount: Int?
    enum CodingKeys: String, CodingKey {
        case name, domain, breachDate, dataClasses, pwnCount
    }
}

// MARK: - Subscription
struct SubscribeResponse: Decodable {
    let url: String?
    let error: String?
}

struct SyncResponse: Decodable {
    let plan: String?
    let subscriptionStatus: String?
    let error: String?
}

struct BillingPortalResponse: Decodable {
    let url: String?
    let error: String?
}

// MARK: - Human on the Loop / SOS
struct HumanRequestResponse: Decodable {
    let id: Int?
    let error: String?
    let message: String?
}

struct CriticalRequestResponse: Decodable {
    let id: Int?
    let error: String?
    let message: String?
}

// MARK: - VPN Credentials (from backend)
struct VPNServerCredentials: Decodable {
    let username: String
    let password: String
    let authMethod: String?
}

// MARK: - VPN Blocked Query event
struct VPNBlockedQuery: Decodable, Identifiable {
    var id: String { "\(domain)-\(blockedAt)" }
    let domain: String
    let blockedAt: TimeInterval
}

struct VPNBlockedQueriesResponse: Decodable {
    let blockedQueries: [VPNBlockedQuery]
    let vpnConnected: Bool
    let vpnIp: String?
}

// MARK: - VPN DNS Stats (from backend)
struct VPNDnsStats: Decodable {
    let active: Bool
    let blockedDomains: Int
    let lastUpdated: String?
    let dnsServer: String
    let feedSource: String
    let updateSchedule: String
}

// MARK: - VPN Config (used by VPNManager)
struct VPNConfig: Codable {
    var serverAddress: String
    var serverPort: Int
    var username: String
    var remoteIdentifier: String
    var localIdentifier: String
    var autoConnect: Bool
    var connectOnUntrustedWifi: Bool

    static var `default`: VPNConfig {
        VPNConfig(serverAddress: "", serverPort: 500, username: "",
                  remoteIdentifier: "", localIdentifier: "",
                  autoConnect: false, connectOnUntrustedWifi: true)
    }
}

// MARK: - StoreKit
struct StoreKitVerifyResponse: Decodable {
    let ok: Bool?
    let plan: String?
    let subscriptionStatus: String?
    let subscriptionInterval: String?
    let expiresAt: String?
    let message: String?
}

// MARK: - Errors
struct APIError: LocalizedError {
    let message: String
    var errorDescription: String? { message }
}

/// Legacy error type used by VPNManager
enum AppError: LocalizedError {
    case network(String)
    var errorDescription: String? {
        switch self { case .network(let m): return m }
    }
}

struct ErrorResponse: Decodable {
    let error: String?
    let message: String?
    var display: String { error ?? message ?? "Errore sconosciuto" }
}
