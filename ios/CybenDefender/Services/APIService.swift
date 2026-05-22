import Foundation

final class APIService: ObservableObject {
    static let shared = APIService()
    private init() {}

    let baseURL = "https://cyben.eu"

    private var authToken: String? {
        KeychainService.shared.load(for: "guard_token")
    }

    // MARK: - Generic request
    func request<T: Decodable>(
        path: String,
        method: String = "GET",
        body: Encodable? = nil,
        authenticated: Bool = true,
        timeoutInterval: TimeInterval = 30
    ) async throws -> T {
        guard let url = URL(string: "\(baseURL)\(path)") else {
            throw APIError(message: "URL non valida")
        }
        var req = URLRequest(url: url)
        req.httpMethod = method
        req.setValue("application/json", forHTTPHeaderField: "Content-Type")
        req.setValue("CybenGuard-iOS/1.0", forHTTPHeaderField: "User-Agent")
        req.timeoutInterval = timeoutInterval

        if authenticated, let token = authToken {
            req.setValue("Bearer \(token)", forHTTPHeaderField: "Authorization")
        }
        if let body { req.httpBody = try JSONEncoder().encode(body) }

        let (data, response) = try await URLSession.shared.data(for: req)
        guard let http = response as? HTTPURLResponse else {
            throw APIError(message: "Risposta non valida dal server")
        }

        // Detect HTML response (endpoint not found / catch-all served index.html)
        let contentType = http.value(forHTTPHeaderField: "Content-Type") ?? ""
        let bodyPrefix = String(data: data.prefix(64), encoding: .utf8) ?? ""
        if contentType.contains("text/html") || bodyPrefix.lowercased().hasPrefix("<!doctype") || bodyPrefix.lowercased().hasPrefix("<html") {
            throw APIError(message: "Endpoint non disponibile sul server. Aggiorna l'app o riprova più tardi.")
        }

        if http.statusCode == 401 { throw APIError(message: "Sessione scaduta. Effettua di nuovo il login.") }
        if http.statusCode == 402 { throw APIError(message: "Abbonamento non attivo.") }
        if http.statusCode == 403 { throw APIError(message: "Funzione riservata al Piano Premium.") }
        if http.statusCode >= 400 {
            if let e = try? JSONDecoder().decode(ErrorResponse.self, from: data) {
                throw APIError(message: e.display)
            }
            throw APIError(message: "Errore HTTP \(http.statusCode)")
        }

        return try JSONDecoder().decode(T.self, from: data)
    }

    // MARK: - Auth
    func register(name: String, email: String, password: String,
                  termsAccepted: Bool, privacyAccepted: Bool, marketingConsent: Bool,
                  partnerCode: String? = nil) async throws -> GuardAuthResponse {
        struct Body: Encodable {
            let name, email, password: String
            let termsAccepted, privacyAccepted, marketingConsent: Bool
            let preferredLanguage: String
            let partnerCode: String?
        }
        let lang = Locale.current.language.languageCode?.identifier ?? "it"
        return try await request(path: "/api/guard/auth/register", method: "POST",
                                 body: Body(name: name, email: email, password: password,
                                            termsAccepted: termsAccepted, privacyAccepted: privacyAccepted,
                                            marketingConsent: marketingConsent, preferredLanguage: lang,
                                            partnerCode: partnerCode.flatMap { $0.isEmpty ? nil : $0 }),
                                 authenticated: false)
    }

    func login(email: String, password: String) async throws -> GuardAuthResponse {
        struct Body: Encodable { let email, password: String }
        return try await request(path: "/api/guard/auth/login", method: "POST",
                                 body: Body(email: email, password: password), authenticated: false)
    }

    func resendVerification(email: String) async throws -> Bool {
        struct Body: Encodable { let email: String }
        struct Resp: Decodable { let ok: Bool? }
        let resp: Resp = try await request(path: "/api/guard/auth/resend-verification", method: "POST",
                                           body: Body(email: email), authenticated: false)
        return resp.ok == true
    }

    func getMe() async throws -> GuardUser {
        return try await request(path: "/api/guard/auth/me")
    }

    func forgotPassword(email: String) async throws {
        struct Body: Encodable { let email: String }
        struct Resp: Decodable { let message: String? }
        let _: Resp = try await request(path: "/api/guard/auth/forgot-password", method: "POST",
                                        body: Body(email: email), authenticated: false)
    }

    // MARK: - Phone Checker
    func checkPhone(phone: String) async throws -> PhoneCheckResult {
        struct Body: Encodable { let phone: String }
        return try await request(path: "/api/guard/check-phone", method: "POST",
                                 body: Body(phone: phone))
    }

    func reportPhone(phone: String, category: String, description: String) async throws -> Int {
        struct Body: Encodable { let phone, category, description: String }
        struct Resp: Decodable { let communityReports: Int? }
        let resp: Resp = try await request(path: "/api/guard/report-phone", method: "POST",
                                           body: Body(phone: phone, category: category, description: description))
        return resp.communityReports ?? 0
    }

    // MARK: - AI Analysis
    func analyze(text: String, type: String, chatHistory: [[String: String]] = []) async throws -> AnalyzeResponse {
        struct Body: Encodable {
            let text, type: String
            let chatHistory: [[String: String]]
            let preferredLanguage: String
        }
        let lang = Locale.current.language.languageCode?.identifier ?? "it"
        return try await request(path: "/api/guard/analyze", method: "POST",
                                 body: Body(text: text, type: type, chatHistory: chatHistory, preferredLanguage: lang))
    }

    func analyzeImage(imageBase64: String, mimeType: String = "image/jpeg") async throws -> AnalyzeResponse {
        struct Body: Encodable { let imageBase64, mimeType, type, preferredLanguage: String }
        let lang = Locale.current.language.languageCode?.identifier ?? "it"
        return try await request(path: "/api/guard/analyze-image", method: "POST",
                                 body: Body(imageBase64: imageBase64, mimeType: mimeType, type: "email", preferredLanguage: lang),
                                 timeoutInterval: 90)
    }

    func getAnalyses() async throws -> [GuardAnalysis] {
        return try await request(path: "/api/guard/analyses")
    }

    // MARK: - Breach Monitoring
    func getMonitoredEmails() async throws -> [GuardMonitoredEmail] {
        return try await request(path: "/api/guard/monitored-emails")
    }

    func addMonitoredEmail(email: String, label: String?) async throws -> GuardMonitoredEmail {
        struct Body: Encodable { let email: String; let label: String? }
        return try await request(path: "/api/guard/monitored-emails", method: "POST",
                                 body: Body(email: email, label: label))
    }

    func deleteMonitoredEmail(id: Int) async throws {
        struct Empty: Decodable {}
        let _: Empty = try await request(path: "/api/guard/monitored-emails/\(id)", method: "DELETE")
    }

    func getBreachAlerts() async throws -> [GuardBreachAlert] {
        return try await request(path: "/api/guard/breach-alerts")
    }

    func checkBreach(emailId: Int) async throws -> BreachCheckResponse {
        return try await request(path: "/api/guard/breach-check/\(emailId)", method: "POST")
    }

    // MARK: - Subscription
    func subscribe(plan: String, billingPeriod: String?) async throws -> SubscribeResponse {
        struct Body: Encodable { let plan: String; let billingPeriod: String? }
        return try await request(path: "/api/guard/subscribe", method: "POST",
                                 body: Body(plan: plan, billingPeriod: billingPeriod))
    }

    func syncSubscription() async throws -> SyncResponse {
        return try await request(path: "/api/guard/sync-subscription", method: "POST")
    }

    func getBillingPortal() async throws -> BillingPortalResponse {
        return try await request(path: "/api/guard/billing-portal", method: "POST")
    }

    // MARK: - Human on the Loop
    func requestHuman(text: String, analysisId: Int?) async throws -> HumanRequestResponse {
        struct Body: Encodable { let text: String; let analysisId: Int? }
        return try await request(path: "/api/guard/human-request", method: "POST",
                                 body: Body(text: text, analysisId: analysisId))
    }

    // MARK: - VPN
    func fetchVPNCredentials() async throws -> VPNServerCredentials {
        return try await request(path: "/api/guard/vpn/credentials")
    }

    func fetchVPNDnsStats() async throws -> VPNDnsStats {
        return try await request(path: "/api/guard/vpn/dns-stats")
    }

    func fetchVPNBlockedQueries(since: Date) async throws -> VPNBlockedQueriesResponse {
        let sinceMs = Int(since.timeIntervalSince1970 * 1000)
        return try await request(path: "/api/guard/vpn/blocked-queries?since=\(sinceMs)")
    }

    // MARK: - StoreKit
    func verifyStoreKit(signedTransaction: String) async throws -> StoreKitVerifyResponse {
        struct Body: Encodable { let signedTransaction: String }
        return try await request(path: "/api/guard/storekit/verify", method: "POST",
                                 body: Body(signedTransaction: signedTransaction))
    }

    func restoreStoreKit(transactions: [String]) async throws -> StoreKitVerifyResponse {
        struct TransactionItem: Encodable { let signedTransaction: String }
        struct Body: Encodable { let transactions: [TransactionItem] }
        let items = transactions.map { TransactionItem(signedTransaction: $0) }
        return try await request(path: "/api/guard/storekit/restore", method: "POST",
                                 body: Body(transactions: items))
    }

    // MARK: - SOS
    func sendSOS(description: String, incidentType: String = "other") async throws -> CriticalRequestResponse {
        struct Body: Encodable { let description, incidentType: String }
        return try await request(path: "/api/guard/critical-requests", method: "POST",
                                 body: Body(description: description, incidentType: incidentType))
    }
}
