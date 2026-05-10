import SwiftUI

struct ContentView: View {
    @EnvironmentObject var authState: AuthState
    @EnvironmentObject var vpnManager: VPNManager
    @State private var selectedTab: Tab = .analyze

    enum Tab { case analyze, breach, vpn, settings }

    /// True ONLY when the backend has confirmed the account has no paid plan.
    /// Uses normalizedPlan (lowercased + trimmed) to avoid raw-string edge cases.
    /// Requires isAuthenticated=true so the cover never flashes during load/logout.
    private var mustChoosePlan: Bool {
        guard authState.isAuthenticated, let user = authState.currentUser else { return false }
        return user.hasNoPlan
    }

    /// Reactive binding derived from backend account state, not from StoreKit.
    /// The setter is intentionally a no-op: dismissal is driven exclusively by
    /// authState.currentUser.plan transitioning to "basic" or "premium".
    private var planSelectionBinding: Binding<Bool> {
        Binding(
            get: { mustChoosePlan },
            set: { _ in }
        )
    }

    var body: some View {
        TabView(selection: $selectedTab) {
            DashboardView()
                .tabItem { Label("Analizza", systemImage: "sparkle.magnifyingglass") }
                .tag(Tab.analyze)

            BreachMonitorView()
                .tabItem { Label("Violazioni", systemImage: "eye.slash.fill") }
                .tag(Tab.breach)

            VPNView()
                .tabItem { Label("VPN", systemImage: "lock.shield.fill") }
                .tag(Tab.vpn)

            SettingsView()
                .tabItem { Label("Impostazioni", systemImage: "gearshape.fill") }
                .tag(Tab.settings)
        }
        .tint(.cyan)
        .fullScreenCover(isPresented: planSelectionBinding) {
            NavigationStack {
                SubscriptionView(isDismissable: false)
            }
        }
    }
}
