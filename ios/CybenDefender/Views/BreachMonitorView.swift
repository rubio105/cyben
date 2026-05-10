import SwiftUI

struct BreachMonitorView: View {
    @EnvironmentObject var authState: AuthState
    @State private var monitoredEmails: [GuardMonitoredEmail] = []
    @State private var breachAlerts: [GuardBreachAlert] = []
    @State private var isLoading = false
    @State private var showAddEmail = false
    @State private var newEmail = ""
    @State private var newLabel = ""
    @State private var isAdding = false
    @State private var checkingId: Int?

    var isPremium: Bool { authState.currentUser?.isPremium == true }

    var body: some View {
        NavigationView {
            Group {
                if !isPremium {
                    PremiumGateView(
                        icon: "eye.slash.fill",
                        title: "Monitoraggio Violazioni",
                        description: "Tieni sotto controllo se le tue email compaiono in data breach. Disponibile con piano Premium.",
                        color: .purple
                    )
                } else {
                    List {
                        Section {
                            ForEach(monitoredEmails) { entry in
                                MonitoredEmailRow(entry: entry, isChecking: checkingId == entry.id) {
                                    await checkBreaches(emailId: entry.id)
                                } onDelete: {
                                    await deleteEmail(id: entry.id)
                                }
                            }
                            if monitoredEmails.count < 2 {
                                Button { showAddEmail = true } label: {
                                    Label("Aggiungi email", systemImage: "plus.circle.fill").foregroundColor(.cyan)
                                }
                            }
                        } header: { Text("Email monitorate (\(monitoredEmails.count)/2)") }

                        if !breachAlerts.isEmpty {
                            Section("Violazioni rilevate") {
                                ForEach(breachAlerts) { alert in BreachAlertRow(alert: alert) }
                            }
                        } else if !isLoading {
                            Section {
                                HStack {
                                    Image(systemName: "checkmark.shield.fill").foregroundColor(.green)
                                    Text("Nessuna violazione rilevata").font(.subheadline).foregroundColor(.secondary)
                                }
                            }
                        }
                    }
                    .refreshable { await loadData() }
                    .sheet(isPresented: $showAddEmail) { addEmailSheet }
                }
            }
            .navigationTitle("Violazioni Dati")
            .task { if isPremium { await loadData() } }
        }
    }

    private var addEmailSheet: some View {
        NavigationView {
            Form {
                Section("Nuova email da monitorare") {
                    TextField("Email", text: $newEmail).keyboardType(.emailAddress).autocapitalization(.none)
                    TextField("Etichetta (es. Lavoro)", text: $newLabel)
                }
            }
            .navigationTitle("Aggiungi Email").navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .navigationBarLeading) { Button("Annulla") { showAddEmail = false } }
                ToolbarItem(placement: .navigationBarTrailing) {
                    Button("Aggiungi") { Task { await addEmail() } }.disabled(newEmail.isEmpty || isAdding)
                }
            }
        }
    }

    private func loadData() async {
        isLoading = true
        async let emails = APIService.shared.getMonitoredEmails()
        async let alerts = APIService.shared.getBreachAlerts()
        do { monitoredEmails = try await emails; breachAlerts = try await alerts } catch {}
        isLoading = false
    }

    private func addEmail() async {
        isAdding = true
        do {
            let entry = try await APIService.shared.addMonitoredEmail(
                email: newEmail, label: newLabel.isEmpty ? nil : newLabel)
            await MainActor.run { monitoredEmails.append(entry); newEmail = ""; newLabel = ""; showAddEmail = false }
        } catch {}
        isAdding = false
    }

    private func checkBreaches(emailId: Int) async {
        checkingId = emailId
        do { _ = try await APIService.shared.checkBreach(emailId: emailId); await loadData() } catch {}
        checkingId = nil
    }

    private func deleteEmail(id: Int) async {
        do {
            try await APIService.shared.deleteMonitoredEmail(id: id)
            await MainActor.run { monitoredEmails.removeAll { $0.id == id } }
        } catch {}
    }
}

struct MonitoredEmailRow: View {
    let entry: GuardMonitoredEmail; let isChecking: Bool
    let onCheck: () async -> Void; let onDelete: () async -> Void
    var body: some View {
        HStack {
            VStack(alignment: .leading, spacing: 3) {
                Text(entry.email).font(.subheadline.bold())
                if let label = entry.label { Text(label).font(.caption).foregroundColor(.secondary) }
                if let count = entry.breachCount, count > 0 {
                    Label("\(count) violazion\(count == 1 ? "e" : "i")", systemImage: "exclamationmark.triangle.fill")
                        .font(.caption).foregroundColor(.orange)
                } else {
                    Label("Nessuna violazione", systemImage: "checkmark.circle").font(.caption).foregroundColor(.green)
                }
            }
            Spacer()
            if isChecking { ProgressView().scaleEffect(0.8) }
            else {
                Button { Task { await onCheck() } } label: {
                    Image(systemName: "arrow.clockwise.circle.fill").foregroundColor(.cyan).font(.title3)
                }.buttonStyle(.plain)
                Button { Task { await onDelete() } } label: {
                    Image(systemName: "trash.circle.fill").foregroundColor(.red.opacity(0.7)).font(.title3)
                }.buttonStyle(.plain)
            }
        }.padding(.vertical, 4)
    }
}

struct BreachAlertRow: View {
    let alert: GuardBreachAlert
    var body: some View {
        VStack(alignment: .leading, spacing: 4) {
            HStack {
                Image(systemName: "exclamationmark.shield.fill").foregroundColor(.red).font(.caption)
                Text(alert.breachName ?? "Violazione rilevata").font(.subheadline.bold())
                Spacer()
                if let date = alert.breachDate { Text(date.prefix(10)).font(.caption2).foregroundColor(.secondary) }
            }
            if let classes = alert.dataClasses, !classes.isEmpty {
                Text(classes.joined(separator: ", ")).font(.caption).foregroundColor(.secondary)
            }
        }.padding(.vertical, 4)
    }
}

// MARK: - Premium Gate (shared across views)
struct PremiumGateView: View {
    let icon: String; let title: String; let description: String; let color: Color
    @State private var showSub = false
    var body: some View {
        VStack(spacing: 24) {
            Spacer()
            Image(systemName: icon).font(.system(size: 56)).foregroundColor(color.opacity(0.8))
            VStack(spacing: 8) {
                Text(title).font(.title3.bold())
                Text(description).font(.subheadline).foregroundColor(.secondary)
                    .multilineTextAlignment(.center).padding(.horizontal, 32)
            }
            Button { showSub = true } label: {
                Label("Scopri i piani", systemImage: "star.fill")
                    .fontWeight(.semibold).padding(.horizontal, 28).padding(.vertical, 14)
                    .background(color).foregroundColor(.white).cornerRadius(14)
            }
            Spacer()
        }
        .sheet(isPresented: $showSub) { SubscriptionView() }
    }
}
