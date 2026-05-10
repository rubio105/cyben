# Cyben Guard — iOS App

## Come aprire il progetto in Xcode

### Metodo consigliato — XcodeGen (2 comandi in Terminale)

XcodeGen genera un progetto Xcode **garantito valido** dal file `project.yml`.

```bash
# 1. Installa XcodeGen (una tantum, richiede Homebrew)
brew install xcodegen

# 2. Dalla cartella cyben-mobile-ios/, genera il progetto
cd cyben-mobile-ios
xcodegen generate

# 3. Apri il progetto
open CybenDefender.xcodeproj
```

> Se non hai Homebrew: vai su https://brew.sh e segui le istruzioni (1 minuto).

### Metodo alternativo — crea il progetto manualmente in Xcode

1. Apri **Xcode** → **New Project**
2. Scegli **iOS → App** → Next
3. Compila così:
   - Product Name: `CybenDefender`
   - Bundle Identifier: `eu.cyben.guard.ios`
   - Interface: `SwiftUI`
   - Language: `Swift`
4. Salva nella cartella `cyben-mobile-ios/`
5. **Sostituisci** il `CybenDefender/` che Xcode ha creato con quello del ZIP
6. In **Signing & Capabilities** aggiungi il capability **Personal VPN**

---

## Funzionalità dell'app

### Piano Base (€11,90/anno · 7 giorni gratis)
- Chat AI agente anti-phishing (analisi SMS, email, URL, testi sospetti)
- Storico analisi

### Piano Premium (€3,99/mese o €24,90/anno · 7 giorni gratis)
- Tutto il piano Base incluso
- Analisi immagini sospette
- Monitoraggio violazioni dati (2 email)
- **VPN IKEv2** integrata (Personal VPN iOS nativo)
- **Protezione SMS** avanzata
- Human on the Loop — revisione da esperto
- SOS Incidente Critico (risposta entro 4 ore)
- Security Awareness Training

---

## Struttura file

| File | Ruolo |
|------|-------|
| `App/CybenDefenderApp.swift` | Entry point, AuthState + JWT Keychain |
| `Services/APIService.swift` | Client HTTP `/api/guard/...` |
| `Services/VPNManager.swift` | VPN IKEv2 via NEVPNManager |
| `Services/KeychainService.swift` | Keychain wrapper |
| `Models/Models.swift` | Modelli Guard + VPNConfig |
| `Views/LoginView.swift` | Login JWT |
| `Views/RegisterView.swift` | Registrazione con consensi GDPR |
| `Views/DashboardView.swift` | Chat AI (Base+) |
| `Views/BreachMonitorView.swift` | Monitoraggio violazioni (Premium) |
| `Views/VPNView.swift` | VPN IKEv2 (Premium) |
| `Views/WiFiSecurityView.swift` | Protezione SMS — `SMSProtectionView` (Premium) |
| `Views/SubscriptionView.swift` | Scelta piano, checkout Stripe in-app |
| `Views/SettingsView.swift` | Profilo, abbonamento, Human on the Loop, SOS |

## API Backend

Tutte le chiamate puntano a `https://cyben.eu/api/guard/...` — stesso backend del sito web Guard.

**Auth:** `Authorization: Bearer <JWT>`  
**Token storage:** Keychain, chiave `guard_token`

## Setup Xcode (dopo apertura progetto)

1. **Signing & Capabilities** → seleziona il tuo Apple Developer Team
2. Aggiungi capability **Personal VPN** (se non già presente)
3. Bundle ID: `eu.cyben.guard.ios`
4. iOS 16+ — simulatore o dispositivo fisico
