import StoreKit
import Foundation

// MARK: - StoreKit 2 Manager
@MainActor
final class StoreKitManager: ObservableObject {
    static let shared = StoreKitManager()

    // Product IDs that must match App Store Connect configuration
    static let productIdBasicYearly    = "cyben_base_yearly"
    static let productIdPremiumMonthly = "cyben_premium_monthly"
    static let productIdPremiumYearly  = "cyben_premium_yearly"
    static let allProductIds: Set<String> = [productIdBasicYearly, productIdPremiumMonthly, productIdPremiumYearly]

    @Published var products: [Product] = []
    @Published var isPurchasing = false
    @Published var isRestoring = false

    private var listenerTask: Task<Void, Never>?

    private init() {}

    // MARK: - Load Products
    func loadProducts() async {
        do {
            let loaded = try await Product.products(for: Self.allProductIds)
            products = loaded.sorted { $0.price < $1.price }
            print("[StoreKit] Loaded \(products.count) products: \(products.map(\.id))")
        } catch {
            print("[StoreKit] Failed to load products: \(error)")
        }
    }

    func product(for id: String) -> Product? {
        products.first { $0.id == id }
    }

    // MARK: - Transaction Listener
    func startListening(authState: AuthState) {
        listenerTask?.cancel()
        listenerTask = Task { [weak self] in
            for await result in Transaction.updates {
                await self?.handle(verificationResult: result, authState: authState)
            }
        }
        print("[StoreKit] Transaction listener started")
    }

    func stopListening() {
        listenerTask?.cancel()
        listenerTask = nil
    }

    // MARK: - Purchase
    func purchase(productId: String, authState: AuthState) async throws -> PurchaseResult {
        guard let product = product(for: productId) else {
            throw StoreKitPurchaseError.productNotFound
        }
        isPurchasing = true
        defer { isPurchasing = false }

        let result = try await product.purchase()

        switch result {
        case .success(let verificationResult):
            let verifyOk = await handle(verificationResult: verificationResult, authState: authState)
            // If Apple accepted the purchase but our server verify failed, signal the caller
            // so the UI can show a helpful recovery message instead of silently dismissing.
            return verifyOk ? .purchased : .verifyFailed
        case .userCancelled:
            return .cancelled
        case .pending:
            return .pending
        @unknown default:
            return .cancelled
        }
    }

    enum PurchaseResult { case purchased, cancelled, pending, verifyFailed }

    // MARK: - Restore Purchases
    func restorePurchases(authState: AuthState) async {
        isRestoring = true
        defer { isRestoring = false }

        var jwsTransactions: [String] = []

        for await result in Transaction.currentEntitlements {
            if case .verified(let tx) = result {
                jwsTransactions.append(tx.jwsRepresentation)
                print("[StoreKit] Entitlement found: \(tx.productID) expires=\(String(describing: tx.expirationDate))")
                await tx.finish()
            }
        }

        print("[StoreKit] Restoring \(jwsTransactions.count) entitlements")

        do {
            let resp = try await APIService.shared.restoreStoreKit(transactions: jwsTransactions)
            // After restore, refresh from server to get the complete and authoritative user state
            await refreshUserFromServer(authState: authState, fallbackPlan: resp.plan,
                                        fallbackStatus: resp.subscriptionStatus,
                                        fallbackInterval: resp.subscriptionInterval)
            print("[StoreKit] Restore completato → plan=\(resp.plan ?? "?")")
        } catch {
            print("[StoreKit] Restore API error: \(error)")
        }
    }

    // MARK: - Handle Transaction
    // Returns true if the server verify succeeded and the plan was updated; false otherwise.
    // The transaction is finished ONLY on success so it is re-delivered on the next launch
    // if the server was temporarily unreachable.
    @discardableResult
    private func handle(verificationResult: VerificationResult<Transaction>, authState: AuthState) async -> Bool {
        guard case .verified(let transaction) = verificationResult else {
            print("[StoreKit] Unverified transaction — ignored")
            return false
        }

        print("[StoreKit] Processing transaction: productId=\(transaction.productID) txId=\(transaction.id) origTxId=\(transaction.originalID) env=\(transaction.environment.rawValue) expires=\(String(describing: transaction.expirationDate))")

        do {
            let resp = try await APIService.shared.verifyStoreKit(signedTransaction: transaction.jwsRepresentation)
            // Finish ONLY after a successful server verify.
            await transaction.finish()
            print("[StoreKit] Transaction finished: \(transaction.id)")

            // After verify, always refresh from server to get the complete user state
            await refreshUserFromServer(authState: authState, fallbackPlan: resp.plan,
                                        fallbackStatus: resp.subscriptionStatus,
                                        fallbackInterval: resp.subscriptionInterval)
            return true
        } catch {
            // Do NOT finish the transaction — it will be re-delivered by Transaction.updates
            // on the next launch so the server gets another chance to verify it.
            print("[StoreKit] ❌ Verify API error — transaction NOT finished, will retry: \(error.localizedDescription)")
            return false
        }
    }

    // Fetches the current user from /api/guard/auth/me and updates authState.
    // Falls back to the provided values if the network call fails.
    private func refreshUserFromServer(authState: AuthState,
                                       fallbackPlan: String?,
                                       fallbackStatus: String?,
                                       fallbackInterval: String?) async {
        do {
            let freshUser = try await APIService.shared.getMe()
            authState.updateUser(freshUser)
            print("[StoreKit] Auth refreshed from server → plan=\(freshUser.plan) status=\(freshUser.subscriptionStatus ?? "?") interval=\(freshUser.subscriptionInterval ?? "?")")
        } catch {
            // Fallback: update with what the verify response gave us
            print("[StoreKit] getMe() failed (\(error.localizedDescription)), applying verify response as fallback")
            if let plan = fallbackPlan, let user = authState.currentUser {
                let updated = GuardUser(id: user.id, name: user.name, email: user.email,
                                       plan: plan, subscriptionStatus: fallbackStatus,
                                       subscriptionInterval: fallbackInterval)
                authState.updateUser(updated)
            }
        }
    }
}

// MARK: - Error
enum StoreKitPurchaseError: LocalizedError {
    case productNotFound
    var errorDescription: String? {
        switch self {
        case .productNotFound: return "Prodotto non trovato su App Store. Riprova più tardi."
        }
    }
}
