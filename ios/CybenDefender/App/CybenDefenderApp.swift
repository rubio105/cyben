import SwiftUI
import UserNotifications
import OneSignalFramework

// MARK: - App Delegate
class AppDelegate: NSObject, UIApplicationDelegate, UNUserNotificationCenterDelegate {

    func application(_ application: UIApplication,
                     didFinishLaunchingWithOptions launchOptions: [UIApplication.LaunchOptionsKey: Any]? = nil) -> Bool {
        UNUserNotificationCenter.current().delegate = self
        OneSignal.Debug.logLevel = .verbose
        OneSignal.initialize("31270916-7dbe-4d64-817f-cbb7aa808060", withLaunchOptions: launchOptions)
        print("[OneSignal] Initialized. Subscription ID: \(OneSignal.User.pushSubscription.id ?? \"nil\")")
        OneSignal.Notifications.requestPermission({ accepted in
            print("[OneSignal] Permesso: \(accepted) | ID: \(OneSignal.User.pushSubscription.id ?? \"nil\") | token: \(OneSignal.User.pushSubscription.token ?? \"nil\")")
        }, fallbackToSettings: true)
        return true
    }

    func application(_ application: UIApplication,
                     didRegisterForRemoteNotificationsWithDeviceToken deviceToken: Data) {
        let token = deviceToken.map { String(format: "%02.2hhx", $0) }.joined()
        print("[APNs] Device token: \(token)")
        UserDefaults.standard.set(token, forKey: "apns_device_token")
        NotificationService.shared.deviceToken = token
        Task { await NotificationService.shared.registerTokenIfNeeded() }
    }

    func application(_ application: UIApplication,
                     didFailToRegisterForRemoteNotificationsWithError error: Error) {
        print("[APNs] Registrazione FALLITA: \(error.localizedDescription)")
    }

    func userNotificationCenter(_ center: UNUserNotificationCenter,
                                willPresent notification: UNNotification,
                                withCompletionHandler completionHandler: @escaping (UNNotificationPresentationOptions) -> Void) {
        completionHandler([.banner, .badge, .sound])
    }

    func userNotificationCenter(_ center: UNUserNotificationCenter,
                                didReceive response: UNNotificationResponse,
                                withCompletionHandler completionHandler: @escaping () -> Void) {
        completionHandler()
    }
}

// MARK: - Notification Service
final class NotificationService: ObservableObject {
    static let shared = NotificationService()
    private init() {}

    @Published var permissionStatus: UNAuthorizationStatus = .notDetermined
    var deviceToken: String?

    @MainActor
    func requestPermission() async {
        let center = UNUserNotificationCenter.current()
        let current = await center.notificationSettings()
        await MainActor.run { permissionStatus = current.authorizationStatus }
        if current.authorizationStatus == .authorized {
            await UIApplication.shared.registerForRemoteNotifications()
        }
    }

    func registerTokenIfNeeded() async {
        guard let token = deviceToken ?? UserDefaults.standard.string(forKey: "apns_device_token"),
              KeychainService.shared.load(for: "guard_token") != nil else { return }
        struct Body: Encodable { let deviceToken: String }
        struct Resp: Decodable { let ok: Bool? }
        do {
            let _: Resp = try await APIService.shared.request(
                path: "/api/guard/apns/register-token", method: "POST",
                body: Body(deviceToken: token))
            print("[Cyben] APNs device token registrato sul backend")
        } catch {
            print("[Cyben] Impossibile registrare token APNs: \(error)")
        }
    }

    func scheduleLocalNotification(title: String, body: String, identifier: String = UUID().uuidString) {
        let content = UNMutableNotificationContent()
        content.title = title
        content.body = body
        content.sound = .default
        let trigger = UNTimeIntervalNotificationTrigger(timeInterval: 1, repeats: false)
        let request = UNNotificationRequest(identifier: identifier, content: content, trigger: trigger)
        UNUserNotificationCenter.current().add(request)
    }
}

// MARK: - Main App
@main
struct CybenGuardApp: App {
    @UIApplicationDelegateAdaptor(AppDelegate.self) var appDelegate
    @StateObject private var authState = AuthState()
    @StateObject private var vpnManager = VPNManager.shared
    @StateObject private var notificationService = NotificationService.shared
    @StateObject private var storeKitManager = StoreKitManager.shared
    @State private var storeKitInitialized = false

    var body: some Scene {
        WindowGroup {
            Group {
                if authState.isAuthenticated {
                    ContentView()
                        .environmentObject(authState)
                        .environmentObject(vpnManager)
                        .environmentObject(notificationService)
                        .environmentObject(storeKitManager)
                        .task { await vpnManager.initialize() }
                } else {
                    AuthRootView()
                        .environmentObject(authState)
                }
            }
            .animation(.easeInOut(duration: 0.3), value: authState.isAuthenticated)
            .onChange(of: authState.isAuthenticated) { isAuth in
                if isAuth { initializeStoreKit() } else {
                    storeKitInitialized = false
                    storeKitManager.stopListening()
                }
            }
            .task { if authState.isAuthenticated { initializeStoreKit() } }
        }
    }

    private func initializeStoreKit() {
        guard !storeKitInitialized else { return }
        storeKitInitialized = true
        storeKitManager.startListening(authState: authState)
        Task {
            await storeKitManager.loadProducts()
            await storeKitManager.restorePurchases(authState: authState)
        }
    }
}

// MARK: - Auth Root
struct AuthRootView: View {
    @State private var showRegister = false
    @EnvironmentObject var authState: AuthState

    var body: some View {
        if showRegister {
            RegisterView(onSwitchToLogin: { showRegister = false })
                .environmentObject(authState)
        } else {
            LoginView(onSwitchToRegister: { showRegister = true })
                .environmentObject(authState)
        }
    }
}

// MARK: - Auth State
@MainActor
final class AuthState: ObservableObject {
    @Published var isAuthenticated = false
    @Published var currentUser: GuardUser?

    init() {
        isAuthenticated = KeychainService.shared.load(for: "guard_token") != nil
        restoreUser()
        if isAuthenticated, let user = currentUser {
            OneSignal.login(String(user.id))
            OneSignal.User.addEmail(user.email)
        }
        if isAuthenticated {
            Task { await NotificationService.shared.requestPermission() }
        }
    }

    private func restoreUser() {
        guard let idStr = KeychainService.shared.load(for: "guard_userId"),
              let id = Int(idStr),
              let email = KeychainService.shared.load(for: "guard_email"),
              let name = KeychainService.shared.load(for: "guard_name") else { return }
        let plan = KeychainService.shared.load(for: "guard_plan") ?? "none"
        let status = KeychainService.shared.load(for: "guard_subStatus")
        let interval = KeychainService.shared.load(for: "guard_subInterval")
        currentUser = GuardUser(id: id, name: name, email: email, plan: plan,
                                subscriptionStatus: status, subscriptionInterval: interval)
    }

    func login(token: String, user: GuardUser) {
        KeychainService.shared.save(token, for: "guard_token")
        saveUser(user)
        currentUser = user
        isAuthenticated = true
        OneSignal.login(String(user.id))
        OneSignal.User.addEmail(user.email)
        Task { await NotificationService.shared.registerTokenIfNeeded() }
    }

    func updateUser(_ user: GuardUser) { saveUser(user); currentUser = user }

    private func saveUser(_ user: GuardUser) {
        KeychainService.shared.save(String(user.id), for: "guard_userId")
        KeychainService.shared.save(user.email, for: "guard_email")
        KeychainService.shared.save(user.name, for: "guard_name")
        KeychainService.shared.save(user.plan, for: "guard_plan")
        if let s = user.subscriptionStatus { KeychainService.shared.save(s, for: "guard_subStatus") }
        if let i = user.subscriptionInterval { KeychainService.shared.save(i, for: "guard_subInterval") }
    }

    func logout() {
        OneSignal.logout()
        KeychainService.shared.clearAll()
        currentUser = nil
        isAuthenticated = false
    }
}
