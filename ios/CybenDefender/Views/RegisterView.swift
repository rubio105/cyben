import SwiftUI

struct RegisterView: View {
    @EnvironmentObject var authState: AuthState
    let onSwitchToLogin: () -> Void

    @State private var name = ""
    @State private var email = ""
    @State private var password = ""
    @State private var confirmPassword = ""
    @State private var showPassword = false
    @State private var acceptTerms = false
    @State private var acceptPrivacy = false
    @State private var marketingConsent = false
    @State private var partnerCode = ""
    @State private var isLoading = false
    @State private var errorMessage: String?
    @State private var showVerifyEmail = false

    private var passwordStrength: PasswordStrength {
        evaluateStrength(password)
    }

    private var isValid: Bool {
        !name.isEmpty && !email.isEmpty && password.count >= 8
        && password == confirmPassword && acceptTerms && acceptPrivacy
    }

    var body: some View {
        ZStack {
            guardGradient.ignoresSafeArea()
            if showVerifyEmail {
                VerifyEmailPromptView(email: email, onBackToLogin: {
                    showVerifyEmail = false
                    onSwitchToLogin()
                })
                .transition(.opacity)
            } else {
            ScrollView(showsIndicators: false) {
                VStack(spacing: 0) {
                    Spacer().frame(height: 56)

                    VStack(spacing: 14) {
                        ZStack {
                            Circle()
                                .fill(.white.opacity(0.06))
                                .frame(width: 96, height: 96)
                            Circle()
                                .stroke(LinearGradient(colors: [.cyan.opacity(0.5), Color(red:0.4,green:0,blue:0.9).opacity(0.5)],
                                                       startPoint: .topLeading, endPoint: .bottomTrailing),
                                        lineWidth: 1.5)
                                .frame(width: 96, height: 96)
                            Image("CbyAvatar")
                                .resizable()
                                .aspectRatio(contentMode: .fit)
                                .frame(width: 90, height: 90)
                                .clipShape(Circle())
                        }
                        Text("Crea il tuo account").font(.title2.bold()).foregroundColor(.white)
                        Text("7 giorni gratis • nessuna carta subito")
                            .font(.caption).foregroundColor(.white.opacity(0.5))
                    }

                    Spacer().frame(height: 36)

                    VStack(spacing: 12) {
                        GuardField(icon: "person", placeholder: "Nome completo *", text: $name)
                        GuardField(icon: "envelope", placeholder: "Email *", text: $email, keyboardType: .emailAddress)

                        VStack(spacing: 6) {
                            GuardSecureField(placeholder: "Password * (min. 8 caratteri)", text: $password, show: $showPassword)

                            if !password.isEmpty {
                                PasswordStrengthBar(strength: passwordStrength)
                            }
                        }

                        VStack(spacing: 4) {
                            HStack {
                                Image(systemName: "lock.rotation")
                                    .foregroundColor(.white.opacity(0.55)).frame(width: 20)
                                SecureField("", text: $confirmPassword,
                                            prompt: Text("Conferma password *").foregroundColor(.white.opacity(0.45)))
                                    .autocapitalization(.none).autocorrectionDisabled()
                                    .foregroundColor(.white)
                                if !confirmPassword.isEmpty {
                                    Image(systemName: password == confirmPassword ? "checkmark.circle.fill" : "xmark.circle.fill")
                                        .foregroundColor(password == confirmPassword ? .green : .red)
                                        .font(.caption)
                                }
                            }
                            .padding().background(.white.opacity(0.10)).cornerRadius(13)
                            .overlay(RoundedRectangle(cornerRadius: 13)
                                .stroke(confirmPassword.isEmpty ? .white.opacity(0.18)
                                        : password == confirmPassword ? Color.green.opacity(0.5) : Color.red.opacity(0.5)))

                            if !confirmPassword.isEmpty && password != confirmPassword {
                                HStack {
                                    Image(systemName: "exclamationmark.triangle.fill")
                                        .font(.caption2).foregroundColor(.red.opacity(0.8))
                                    Text("Le password non coincidono")
                                        .font(.caption2).foregroundColor(.red.opacity(0.8))
                                    Spacer()
                                }
                                .padding(.leading, 4)
                            }
                        }
                    }
                    .padding(.horizontal, 28)

                    
                    HStack {
                        Image(systemName: "key.fill")
                            .foregroundColor(.white.opacity(0.4))
                            .frame(width: 20)
                        TextField("Codice partner (facoltativo)", text: $partnerCode)
                            .autocapitalization(.allCharacters)
                            .disableAutocorrection(true)
                            .foregroundColor(.white)
                    }
                    .padding(14)
                    .background(Color.white.opacity(0.07))
                    .cornerRadius(12)
                    .padding(.horizontal, 28)
                    .padding(.top, 8)

                    VStack(spacing: 10) {
                        GuardConsentRow(isOn: $acceptPrivacy, label: "Accetto la",
                                        linkText: "Privacy Policy", url: "https://cyben.eu/privacy")
                        GuardConsentRow(isOn: $acceptTerms, label: "Accetto i",
                                        linkText: "Termini di Servizio", url: "https://cyben.eu/terms")
                        GuardConsentRow(isOn: $marketingConsent, label: "Acconsento al marketing (facoltativo)",
                                        linkText: "", url: "")
                    }
                    .padding(.horizontal, 28).padding(.top, 18)

                    if let err = errorMessage {
                        ErrorLabel(message: err).padding(.horizontal, 28).padding(.top, 8)
                    }

                    Button(action: performRegister) {
                        HStack(spacing: 8) {
                            if isLoading { ProgressView().progressViewStyle(.circular).tint(.white).scaleEffect(0.85) }
                            Text(isLoading ? "Creazione account..." : "Crea account").fontWeight(.semibold)
                        }
                        .frame(maxWidth: .infinity).padding(16)
                        .background(isValid && !isLoading
                                    ? AnyShapeStyle(LinearGradient(colors: [.cyan, Color(red:0.4,green:0,blue:0.9)],
                                                                   startPoint: .leading, endPoint: .trailing))
                                    : AnyShapeStyle(Color.white.opacity(0.15)))
                        .foregroundColor(.white).cornerRadius(14)
                    }
                    .disabled(!isValid || isLoading)
                    .padding(.horizontal, 28).padding(.top, 24)

                    HStack(spacing: 4) {
                        Text("Hai già un account?").foregroundColor(.white.opacity(0.5)).font(.subheadline)
                        Button("Accedi") { onSwitchToLogin() }.foregroundColor(.cyan).font(.subheadline.bold())
                    }
                    .padding(.top, 18).padding(.bottom, 48)
                }
            }
            } // else
        }
    }

    private func performRegister() {
        guard isValid, !isLoading else { return }
        isLoading = true; errorMessage = nil
        Task {
            do {
                let resp = try await APIService.shared.register(
                    name: name, email: email, password: password,
                    termsAccepted: acceptTerms, privacyAccepted: acceptPrivacy,
                    marketingConsent: marketingConsent,
                    partnerCode: partnerCode
                )
                await MainActor.run {
                    isLoading = false
                    if resp.needsVerification == true {
                        // Email verification required — show confirmation screen
                        showVerifyEmail = true
                    } else if let token = resp.token, let user = resp.user {
                        authState.login(token: token, user: user)
                    } else {
                        errorMessage = resp.error ?? "Registrazione non riuscita"
                    }
                }
            } catch let e as APIError {
                await MainActor.run { errorMessage = e.message; isLoading = false }
            } catch {
                await MainActor.run { errorMessage = "Errore di connessione"; isLoading = false }
            }
        }
    }
}

// MARK: - Password Strength

enum PasswordStrength: Int {
    case empty = 0, veryWeak = 1, weak = 2, medium = 3, strong = 4

    var label: String {
        switch self {
        case .empty: return ""
        case .veryWeak: return "Molto debole"
        case .weak: return "Debole"
        case .medium: return "Discreta"
        case .strong: return "Forte"
        }
    }

    var color: Color {
        switch self {
        case .empty: return .clear
        case .veryWeak: return .red
        case .weak: return .orange
        case .medium: return Color(red: 0.9, green: 0.7, blue: 0)
        case .strong: return .green
        }
    }
}

func evaluateStrength(_ password: String) -> PasswordStrength {
    guard !password.isEmpty else { return .empty }
    if password.count < 6 { return .veryWeak }
    var score = 0
    if password.count >= 8 { score += 1 }
    if password.count >= 12 { score += 1 }
    if password.rangeOfCharacter(from: .uppercaseLetters) != nil { score += 1 }
    if password.rangeOfCharacter(from: .decimalDigits) != nil { score += 1 }
    let symbols = CharacterSet(charactersIn: "!@#$%^&*()_+-=[]{}|;':\",./<>?")
    if password.rangeOfCharacter(from: symbols) != nil { score += 1 }
    switch score {
    case 0...1: return .veryWeak
    case 2: return .weak
    case 3: return .medium
    default: return .strong
    }
}

struct PasswordStrengthBar: View {
    let strength: PasswordStrength

    var body: some View {
        VStack(alignment: .leading, spacing: 4) {
            HStack(spacing: 4) {
                ForEach(1...4, id: \.self) { segment in
                    RoundedRectangle(cornerRadius: 3)
                        .fill(segment <= strength.rawValue ? strength.color : Color.white.opacity(0.15))
                        .frame(height: 4)
                        .animation(.easeInOut(duration: 0.25), value: strength.rawValue)
                }
            }
            if strength != .empty {
                Text(strength.label)
                    .font(.caption2).foregroundColor(strength.color)
                    .animation(.easeInOut, value: strength.rawValue)
            }
        }
        .padding(.horizontal, 4)
    }
}

// MARK: - Reusable field components
struct GuardConsentRow: View {
    @Binding var isOn: Bool
    let label: String; let linkText: String; let url: String
    var isRequired: Bool { !url.isEmpty }
    var body: some View {
        HStack(alignment: .top, spacing: 12) {
            Button { isOn.toggle() } label: {
                Image(systemName: isOn ? "checkmark.square.fill" : "square")
                    .foregroundColor(isOn ? .cyan : .white.opacity(0.4))
                    .font(.system(size: 20))
            }.buttonStyle(.plain)
            HStack(spacing: 4) {
                Text(label).font(.caption).foregroundColor(.white.opacity(0.6))
                if !linkText.isEmpty, let destination = URL(string: url) {
                    Link(linkText, destination: destination).font(.caption.bold()).foregroundColor(.cyan)
                }
                if isRequired {
                    Text("*").font(.caption).foregroundColor(.red)
                }
            }
            Spacer()
        }
    }
}

// MARK: - Verify Email Prompt
struct VerifyEmailPromptView: View {
    let email: String
    let onBackToLogin: () -> Void
    @State private var resendLoading = false
    @State private var resendMessage: String?

    var body: some View {
        VStack(spacing: 28) {
            Spacer()
            ZStack {
                Circle().fill(.white.opacity(0.06)).frame(width: 100, height: 100)
                Image(systemName: "envelope.badge.shield.half.filled")
                    .font(.system(size: 44))
                    .foregroundStyle(
                        LinearGradient(colors: [.cyan, Color(red:0.4,green:0,blue:0.9)],
                                       startPoint: .top, endPoint: .bottom)
                    )
            }

            VStack(spacing: 10) {
                Text("Conferma la tua email")
                    .font(.title2.bold()).foregroundColor(.white)
                Text("Abbiamo inviato un link di verifica a:")
                    .font(.subheadline).foregroundColor(.white.opacity(0.6))
                Text(email)
                    .font(.subheadline.bold()).foregroundColor(.cyan)
                Text("Clicca il link nell'email per attivare il tuo account.")
                    .font(.caption).foregroundColor(.white.opacity(0.5))
                    .multilineTextAlignment(.center)
                    .padding(.horizontal, 24)
            }

            VStack(spacing: 14) {
                Button(action: {
                    resendLoading = true; resendMessage = nil
                    Task {
                        do {
                            _ = try await APIService.shared.resendVerification(email: email)
                            await MainActor.run { resendMessage = "Email inviata! Controlla la posta." }
                        } catch {
                            await MainActor.run { resendMessage = "Errore, riprova più tardi." }
                        }
                        await MainActor.run { resendLoading = false }
                    }
                }) {
                    HStack(spacing: 8) {
                        if resendLoading { ProgressView().progressViewStyle(.circular).tint(.white).scaleEffect(0.8) }
                        Text(resendLoading ? "Invio..." : "Invia di nuovo")
                    }
                    .frame(maxWidth: .infinity).padding(14)
                    .background(.white.opacity(0.12)).foregroundColor(.white).cornerRadius(12)
                }
                .disabled(resendLoading)

                Button("Vai al login") { onBackToLogin() }
                    .frame(maxWidth: .infinity).padding(14)
                    .background(LinearGradient(colors: [.cyan, Color(red:0.4,green:0,blue:0.9)],
                                               startPoint: .leading, endPoint: .trailing))
                    .foregroundColor(.white).cornerRadius(12)
            }
            .padding(.horizontal, 36)

            if let msg = resendMessage {
                Text(msg)
                    .font(.caption).foregroundColor(.cyan)
                    .multilineTextAlignment(.center)
                    .padding(.horizontal, 24)
            }

            Spacer()
        }
    }
}
