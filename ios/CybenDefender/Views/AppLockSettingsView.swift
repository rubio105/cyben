import SwiftUI

// MARK: - AppLockSettingsView
// Raggiungibile da Impostazioni > Sicurezza app
// Permette di abilitare/disabilitare PIN e biometria.
// Il PIN non viene mai inviato al server.

struct AppLockSettingsView: View {
    @Environment(\.dismiss) private var dismiss
    @State private var isEnabled       = AppLockService.shared.isAppLockEnabled
    @State private var isBioEnabled    = AppLockService.shared.isBiometricEnabled
    @State private var showSetup       = false
    @State private var showDisableAlert = false

    private let lock = AppLockService.shared

    var body: some View {
        ZStack {
            Color(red: 0.04, green: 0.08, blue: 0.14).ignoresSafeArea()
            List {
                Section {
                    Toggle(isOn: $isEnabled) {
                        Label("Protezione app", systemImage: "lock.shield")
                            .foregroundColor(.white)
                    }
                    .tint(Color(red: 0.02, green: 0.71, blue: 0.83))
                    .onChange(of: isEnabled) { _, newVal in
                        if newVal {
                            if !lock.isPinSet { showSetup = true }
                        } else {
                            showDisableAlert = true
                        }
                    }
                    .listRowBackground(Color.white.opacity(0.05))

                    if isEnabled && lock.biometricType != .none {
                        Toggle(isOn: $isBioEnabled) {
                            Label(lock.biometricLabel, systemImage: lock.biometricType == .touchID ? "touchid" : "faceid")
                                .foregroundColor(.white)
                        }
                        .tint(Color(red: 0.02, green: 0.71, blue: 0.83))
                        .onChange(of: isBioEnabled) { _, newVal in
                            lock.isBiometricEnabled = newVal
                            syncToServer()
                        }
                        .listRowBackground(Color.white.opacity(0.05))
                    }

                    if isEnabled {
                        Button {
                            showSetup = true
                        } label: {
                            Label("Cambia codice", systemImage: "key.fill")
                                .foregroundColor(Color(red: 0.02, green: 0.71, blue: 0.83))
                        }
                        .listRowBackground(Color.white.opacity(0.05))
                    }
                } header: {
                    Text("PROTEZIONE LOCALE").font(.caption).foregroundColor(.secondary)
                }

                Section {
                    Text("Il codice di sblocco e la biometria restano esclusivamente sul tuo dispositivo. Cyben non li riceve né li archivia.")
                        .font(.caption)
                        .foregroundColor(.secondary)
                        .listRowBackground(Color.white.opacity(0.03))
                }
            }
            .scrollContentBackground(.hidden)
        }
        .navigationTitle("Sicurezza app")
        .navigationBarTitleDisplayMode(.inline)
        .sheet(isPresented: $showSetup, onDismiss: {
            isEnabled   = lock.isAppLockEnabled
            isBioEnabled = lock.isBiometricEnabled
        }) {
            AppLockSetupView()
        }
        .alert("Disattivare la protezione?", isPresented: $showDisableAlert) {
            Button("Disattiva", role: .destructive) {
                lock.disableAppLock()
                isEnabled    = false
                isBioEnabled = false
                syncToServer()
            }
            Button("Annulla", role: .cancel) { isEnabled = true }
        } message: {
            Text("Il codice di sblocco verrà rimosso dal dispositivo.")
        }
    }

    private func syncToServer() {
        if let token = KeychainService.shared.load(for: "guard_token") {
            Task { await AppLockService.shared.syncPreferencesToServer(token: token) }
        }
    }
}

#Preview { NavigationStack { AppLockSettingsView() } }
