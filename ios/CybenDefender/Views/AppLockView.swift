import SwiftUI
import LocalAuthentication

// MARK: - AppLockView
// Schermata di sblocco mostrata all'avvio e al ritorno dal background.
// Il PIN non lascia mai il dispositivo — nessuna comunicazione con il server.

struct AppLockView: View {
    @State private var enteredPin = ""
    @State private var shake = false
    @State private var errorMessage = ""
    let onUnlocked: () -> Void

    private let maxDigits = 6

    var body: some View {
        ZStack {
            Color(red: 0.04, green: 0.08, blue: 0.14).ignoresSafeArea()

            VStack(spacing: 32) {
                Spacer()

                Image(systemName: "lock.shield.fill")
                    .font(.system(size: 56))
                    .foregroundStyle(Color(red: 0.02, green: 0.71, blue: 0.83))

                Text("Sblocca Cyben Guard")
                    .font(.title2).fontWeight(.bold)
                    .foregroundColor(.white)

                // Indicatori PIN
                HStack(spacing: 16) {
                    ForEach(0..<maxDigits, id: \.self) { i in
                        Circle()
                            .fill(i < enteredPin.count ? Color(red: 0.02, green: 0.71, blue: 0.83) : Color.white.opacity(0.15))
                            .frame(width: 14, height: 14)
                    }
                }
                .offset(x: shake ? -8 : 0)
                .animation(shake ? .default.repeatCount(4, autoreverses: true).speed(4) : .default, value: shake)

                if !errorMessage.isEmpty {
                    Text(errorMessage)
                        .font(.caption)
                        .foregroundColor(.red)
                        .transition(.opacity)
                }

                Spacer()

                // Tastiera numerica
                LazyVGrid(columns: Array(repeating: GridItem(.flexible()), count: 3), spacing: 16) {
                    ForEach(1...9, id: \.self) { digit in
                        PinButton(label: "\(digit)") { appendDigit("\(digit)") }
                    }
                    // Biometria (se disponibile)
                    if AppLockService.shared.isBiometricEnabled {
                        PinButton(icon: biometricIcon) { triggerBiometric() }
                    } else {
                        Color.clear.frame(height: 64)
                    }
                    PinButton(label: "0") { appendDigit("0") }
                    PinButton(icon: "delete.left") { deleteDigit() }
                }
                .padding(.horizontal, 40)

                Spacer().frame(height: 24)
            }
            .padding()
        }
        .task { await tryBiometricOnAppear() }
    }

    // MARK: - Actions

    private func appendDigit(_ digit: String) {
        guard enteredPin.count < maxDigits else { return }
        enteredPin += digit
        errorMessage = ""
        if enteredPin.count == maxDigits { verify() }
    }

    private func deleteDigit() {
        guard !enteredPin.isEmpty else { return }
        enteredPin.removeLast()
        errorMessage = ""
    }

    private func verify() {
        if AppLockService.shared.verifyPin(enteredPin) {
            onUnlocked()
        } else {
            withAnimation { shake = true }
            DispatchQueue.main.asyncAfter(deadline: .now() + 0.4) { shake = false }
            errorMessage = "PIN non corretto"
            enteredPin = ""
        }
    }

    private func triggerBiometric() {
        Task {
            let ok = await AppLockService.shared.authenticateWithBiometrics(
                reason: "Sblocca Cyben Guard"
            )
            if ok { await MainActor.run { onUnlocked() } }
        }
    }

    private func tryBiometricOnAppear() async {
        guard AppLockService.shared.isBiometricEnabled else { return }
        let ok = await AppLockService.shared.authenticateWithBiometrics(reason: "Sblocca Cyben Guard")
        if ok { await MainActor.run { onUnlocked() } }
    }

    private var biometricIcon: String {
        switch AppLockService.shared.biometricType {
        case .faceID:  return "faceid"
        case .touchID: return "touchid"
        default:       return "faceid"
        }
    }
}

// MARK: - AppLockSetupView
// Proposta post-abbonamento per impostare il PIN.

struct AppLockSetupView: View {
    @Environment(\.dismiss) private var dismiss
    @State private var pin = ""
    @State private var confirmPin = ""
    @State private var step: Step = .enter
    @State private var errorMessage = ""
    @State private var biometricOffered = false

    enum Step { case enter, confirm, biometric }

    var body: some View {
        ZStack {
            Color(red: 0.04, green: 0.08, blue: 0.14).ignoresSafeArea()
            VStack(spacing: 28) {
                Spacer()
                Image(systemName: "lock.shield")
                    .font(.system(size: 48))
                    .foregroundStyle(Color(red: 0.02, green: 0.71, blue: 0.83))

                Text(stepTitle).font(.title2).fontWeight(.bold).foregroundColor(.white)
                Text(stepSubtitle).font(.subheadline).foregroundColor(.secondary).multilineTextAlignment(.center)

                if step != .biometric {
                    HStack(spacing: 16) {
                        ForEach(0..<6, id: \.self) { i in
                            Circle()
                                .fill(i < currentPin.count ? Color(red: 0.02, green: 0.71, blue: 0.83) : Color.white.opacity(0.15))
                                .frame(width: 14, height: 14)
                        }
                    }
                    if !errorMessage.isEmpty {
                        Text(errorMessage).font(.caption).foregroundColor(.red)
                    }
                }

                if step == .biometric {
                    VStack(spacing: 16) {
                        Button {
                            AppLockService.shared.isBiometricEnabled = true
                            syncAndDismiss()
                        } label: {
                            Label("Abilita \(AppLockService.shared.biometricLabel)", systemImage: biometricIcon)
                                .frame(maxWidth: .infinity).padding()
                                .background(Color(red: 0.02, green: 0.71, blue: 0.83))
                                .foregroundColor(.white).cornerRadius(14)
                        }
                        Button("Salta") { syncAndDismiss() }
                            .foregroundColor(.secondary)
                    }.padding(.horizontal, 40)
                } else {
                    LazyVGrid(columns: Array(repeating: GridItem(.flexible()), count: 3), spacing: 16) {
                        ForEach(1...9, id: \.self) { d in
                            PinButton(label: "\(d)") { appendDigit("\(d)") }
                        }
                        Color.clear.frame(height: 64)
                        PinButton(label: "0") { appendDigit("0") }
                        PinButton(icon: "delete.left") { deleteDigit() }
                    }.padding(.horizontal, 40)
                }

                Button("Annulla") { dismiss() }.foregroundColor(.secondary).padding(.bottom)
            }.padding()
        }
    }

    private var currentPin: String { step == .enter ? pin : confirmPin }

    private var stepTitle: String {
        switch step {
        case .enter:    return "Scegli un codice"
        case .confirm:  return "Conferma il codice"
        case .biometric: return "Sblocco rapido"
        }
    }

    private var stepSubtitle: String {
        switch step {
        case .enter:    return "Il codice resta sul tuo dispositivo e non viene mai inviato."
        case .confirm:  return "Inserisci di nuovo il codice per confermarlo."
        case .biometric: return "Usa \(AppLockService.shared.biometricLabel) per sbloccare l'app più velocemente."
        }
    }

    private var biometricIcon: String {
        AppLockService.shared.biometricType == .touchID ? "touchid" : "faceid"
    }

    private func appendDigit(_ digit: String) {
        guard currentPin.count < 6 else { return }
        if step == .enter { pin += digit } else { confirmPin += digit }
        errorMessage = ""
        if currentPin.count == 6 { advance() }
    }

    private func deleteDigit() {
        if step == .enter { if !pin.isEmpty { pin.removeLast() } }
        else { if !confirmPin.isEmpty { confirmPin.removeLast() } }
        errorMessage = ""
    }

    private func advance() {
        switch step {
        case .enter:
            step = .confirm
        case .confirm:
            if pin == confirmPin {
                _ = AppLockService.shared.setPin(pin)
                let hasBio = AppLockService.shared.biometricType != .none
                if hasBio { step = .biometric } else { syncAndDismiss() }
            } else {
                errorMessage = "I codici non coincidono. Riprova."
                pin = ""; confirmPin = ""
                step = .enter
            }
        case .biometric: break
        }
    }

    private func syncAndDismiss() {
        if let token = KeychainService.shared.load(for: "guard_token") {
            Task { await AppLockService.shared.syncPreferencesToServer(token: token) }
        }
        dismiss()
    }
}

// MARK: - PinButton helper

private struct PinButton: View {
    var label: String? = nil
    var icon: String? = nil
    let action: () -> Void

    var body: some View {
        Button(action: action) {
            ZStack {
                Circle()
                    .fill(Color.white.opacity(0.08))
                    .frame(width: 72, height: 72)
                if let label { Text(label).font(.title).fontWeight(.semibold).foregroundColor(.white) }
                else if let icon { Image(systemName: icon).font(.title2).foregroundColor(.white) }
            }
        }
    }
}

#Preview { AppLockView { } }
