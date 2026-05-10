import SwiftUI

struct LoginView: View {
    @EnvironmentObject var authState: AuthState
    let onSwitchToRegister: () -> Void

    @State private var email = ""
    @State private var password = ""
    @State private var showPassword = false
    @State private var isLoading = false
    @State private var errorMessage: String?

    var body: some View {
        ZStack {
            guardGradient.ignoresSafeArea()

            ScrollView(showsIndicators: false) {
                VStack(spacing: 0) {
                    Spacer().frame(height: 72)

                    // Logo
                    VStack(spacing: 14) {
                        ZStack {
                            Circle()
                                .fill(.white.opacity(0.06))
                                .frame(width: 110, height: 110)
                            Circle()
                                .stroke(LinearGradient(colors: [.cyan.opacity(0.5), Color(red:0.4,green:0,blue:0.9).opacity(0.5)],
                                                       startPoint: .topLeading, endPoint: .bottomTrailing),
                                        lineWidth: 1.5)
                                .frame(width: 110, height: 110)
                            Image("CbyAvatar")
                                .resizable()
                                .aspectRatio(contentMode: .fit)
                                .frame(width: 104, height: 104)
                                .clipShape(Circle())
                        }
                        Text("Cyben Guard")
                            .font(.largeTitle.bold())
                            .foregroundColor(.white)
                        Text("La tua guardia digitale personale")
                            .font(.subheadline)
                            .foregroundColor(.white.opacity(0.55))
                    }

                    Spacer().frame(height: 52)

                    // Form
                    VStack(spacing: 14) {
                        GuardField(icon: "envelope", placeholder: "Email", text: $email, keyboardType: .emailAddress)
                        GuardSecureField(placeholder: "Password", text: $password, show: $showPassword)
                    }
                    .padding(.horizontal, 28)

                    // Error
                    if let err = errorMessage {
                        ErrorLabel(message: err).padding(.horizontal, 28).padding(.top, 8)
                    }

                    // CTA
                    Button(action: performLogin) {
                        HStack(spacing: 8) {
                            if isLoading { ProgressView().progressViewStyle(.circular).tint(.white).scaleEffect(0.85) }
                            Text(isLoading ? "Accesso..." : "Accedi").fontWeight(.semibold)
                        }
                        .frame(maxWidth: .infinity).padding(16)
                        .background(isLoading || !canSubmit ? AnyShapeStyle(Color.white.opacity(0.2))
                                    : AnyShapeStyle(LinearGradient(colors: [.cyan, Color(red:0.4,green:0,blue:0.9)],
                                                                   startPoint: .leading, endPoint: .trailing)))
                        .foregroundColor(.white).cornerRadius(14)
                    }
                    .disabled(isLoading || !canSubmit)
                    .padding(.horizontal, 28).padding(.top, 24)

                    HStack(spacing: 4) {
                        Text("Non hai un account?").foregroundColor(.white.opacity(0.5)).font(.subheadline)
                        Button("Registrati") { onSwitchToRegister() }
                            .foregroundColor(.cyan).font(.subheadline.bold())
                    }
                    .padding(.top, 20).padding(.bottom, 48)
                }
            }
        }
    }

    private var canSubmit: Bool { !email.isEmpty && password.count >= 6 }

    private func performLogin() {
        guard canSubmit, !isLoading else { return }
        isLoading = true; errorMessage = nil
        Task {
            do {
                let resp = try await APIService.shared.login(email: email, password: password)
                await MainActor.run {
                    if let token = resp.token, let user = resp.user {
                        authState.login(token: token, user: user)
                    } else {
                        errorMessage = resp.error ?? "Credenziali non valide"
                        isLoading = false
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

// MARK: - Shared style helpers
var guardGradient: LinearGradient {
    LinearGradient(colors: [Color(red:0.04,green:0.04,blue:0.14), Color(red:0.07,green:0.05,blue:0.20)],
                   startPoint: .topLeading, endPoint: .bottomTrailing)
}

struct GuardField: View {
    let icon: String; let placeholder: String
    @Binding var text: String
    var keyboardType: UIKeyboardType = .default
    var body: some View {
        HStack {
            Image(systemName: icon)
                .foregroundColor(.white.opacity(0.55))
                .frame(width: 20)
            TextField("", text: $text,
                      prompt: Text(placeholder).foregroundColor(.white.opacity(0.45)))
                .keyboardType(keyboardType)
                .autocapitalization(keyboardType == .emailAddress ? .none : .words)
                .autocorrectionDisabled()
                .foregroundColor(.white)
        }
        .padding()
        .background(.white.opacity(0.10))
        .cornerRadius(13)
        .overlay(RoundedRectangle(cornerRadius: 13).stroke(.white.opacity(0.18)))
    }
}

struct GuardSecureField: View {
    let placeholder: String
    @Binding var text: String
    @Binding var show: Bool
    var body: some View {
        HStack {
            Image(systemName: "lock")
                .foregroundColor(.white.opacity(0.55))
                .frame(width: 20)
            Group {
                if show {
                    TextField("", text: $text,
                              prompt: Text(placeholder).foregroundColor(.white.opacity(0.45)))
                    .autocapitalization(.none)
                } else {
                    SecureField("", text: $text,
                                prompt: Text(placeholder).foregroundColor(.white.opacity(0.45)))
                }
            }
            .autocorrectionDisabled()
            .foregroundColor(.white)
            Button { show.toggle() } label: {
                Image(systemName: show ? "eye.slash" : "eye")
                    .foregroundColor(.white.opacity(0.45))
            }
        }
        .padding()
        .background(.white.opacity(0.10))
        .cornerRadius(13)
        .overlay(RoundedRectangle(cornerRadius: 13).stroke(.white.opacity(0.18)))
    }
}

struct ErrorLabel: View {
    let message: String
    var body: some View {
        HStack(spacing: 6) {
            Image(systemName: "exclamationmark.triangle.fill").font(.caption)
            Text(message).font(.caption)
        }
        .foregroundColor(Color(red:1,green:0.4,blue:0.4))
        .frame(maxWidth: .infinity, alignment: .leading)
    }
}
