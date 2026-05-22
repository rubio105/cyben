import SwiftUI
import UserNotifications
import OneSignalFramework

// MARK: - App Delegate (handles APNs token registration)
class AppDelegate: NSObject, UIApplicationDelegate, UNUserNotificationCenterDelegate {

    func application(_ application: UIApplication,
                     didFinishLaunchingWithOptions launchOptions: [UIApplication.LaunchOptionsKey: Any]? = nil) -> Bool {
        UNUserNotificationCenter.current().delegate = self
        OneSignal.initialize("31270916-7dbe-4d64-817f-cbb7aa808060", withLaunchOptions: launchOptions)
        return true
    }

    func application(_ application: UIApplication,
                     didRegisterForRemoteNotificationsWithDeviceToken deviceToken: Data) {
        let token = deviceToken.map { String(format: "%02.2hhx", $0) }.joined()
        UserDefaults.standard.set(token, forKey: "apns_device_token")
        NotificationService.shared.deviceToken = token
        // Upload token to backend (fire-and-forget)
        Task { await NotificationService.shared.registerTokenIfNeeded() }
    }

    func application(_ application: UIApplication,
                     didFailToRegisterForRemoteNotificationsWithError error: Error) {
        print("[Cyben] APNs registration failed: \(error.localizedDescription)")
    }

    // Handle notification shown while app is in foreground
    func userNotificationCenter(_ center: UNUserNotificationCenter,
                                willPresent notification: UNNotification,
                                withCompletionHandler completionHandler: @escaping (UNNotificationPresentationOptions) -> Void) {
        completionHandler([.banner, .badge, .sound])
    }

    // Handle notification tap
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

    /// Requests notification permission from the user.
    /// Only shows the system dialog once; subsequently respects the user's choice.
    @MainActor
    func requestPermission() async {
        let center = UNUserNotificationCenter.current()
        let current = await center.notificationSettings()

        await MainActor.run { permissionStatus = current.authorizationStatus }

        if current.authorizationStatus == .notDetermined {
            do {
                let granted = try await center.requestAuthorization(options: [.alert, .badge, .sound])
                let updated = await center.notificationSettings()
                await MainActor.run { permissionStatus = updated.authorizationStatus }
                if granted {
                    await UIApplication.shared.registerForRemoteNotifications()
                    OneSignal.Notifications.requestPermission({ _ in }, fallbackToSettings: false)
                }
            } catch {
                print("[Cyben] Notification permission error: \(error)")
            }
        } else if current.authorizationStatus == .authorized {
            // Already authorized — ensure remote registration is active
            await UIApplication.shared.registerForRemoteNotifications()
        }
    }

    /// Upload device token to backend so server can send APNs pushes.
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

    /// Schedules a local notification (used for security alerts).
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
    // Prevents double-initialization when both .task and onChange fire for the same session
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
                if isAuth {
                    initializeStoreKit()
                } else {
                    storeKitInitialized = false
                    storeKitManager.stopListening()
                }
            }
            // Handles cold launch when session is already restored from Keychain
            .task {
                if authState.isAuthenticated {
                    initializeStoreKit()
                }
            }
        }
    }

    private func initializeStoreKit() {
        guard !storeKitInitialized else {
            print("[App] StoreKit già inizializzato — skip")
            return
        }
        storeKitInitialized = true
        storeKitManager.startListening(authState: authState)
        Task {
            await storeKitManager.loadProducts()
            await storeKitManager.restorePurchases(authState: authState)
            try? await Task.sleep(nanoseconds: 1_500_000_000)
            await NotificationService.shared.requestPermission()
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
        // If already authenticated on launch, request permission after a delay
        if isAuthenticated {
            Task {
                try? await Task.sleep(nanoseconds: 2_000_000_000)
                await NotificationService.shared.requestPermission()
            }
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
        // Upload APNs token to backend (now that JWT auth is ready)
        Task { await NotificationService.shared.registerTokenIfNeeded() }
    }

    func updateUser(_ user: GuardUser) {
        saveUser(user)
        currentUser = user
    }

    private func saveUser(_ user: GuardUser) {
        KeychainService.shared.save(String(user.id), for: "guard_userId")
        KeychainService.shared.save(user.email, for: "guard_email")
        KeychainService.shared.save(user.name, for: "guard_name")
        KeychainService.shared.save(user.plan, for: "guard_plan")
        if let s = user.subscriptionStatus { KeychainService.shared.save(s, for: "guard_subStatus") }
        if let i = user.subscriptionInterval { KeychainService.shared.save(i, for: "guard_subInterval") }
    }

    func logout() {
        KeychainService.shared.clearAll()
        currentUser = nil
        isAuthenticated = false
    }
}
