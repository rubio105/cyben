# Come risolvere "No schemes are available"

Se Xcode mostra **No schemes are available**, segui uno di questi metodi:

## Metodo 1 — Autocreazione (30 secondi)

1. In Xcode, apri il menu **Product**
2. Vai su **Scheme → Manage Schemes...**
3. In basso a sinistra nella finestra che si apre, clicca **"Autocreate Schemes Now"**
4. Lo schema CybenDefender comparirà nella lista → clicca **Close**

## Metodo 2 — Crea schema manuale (1 minuto)

1. Clicca sul selettore schema nella **toolbar** di Xcode (vicino al pulsante ▶)
2. Seleziona **"New Scheme..."**
3. Dal menu a tendina seleziona il target **CybenDefender**
4. Clicca **OK**

## Metodo 3 — XcodeGen (tool automatico)

Se hai Homebrew installato, dalla cartella `cyben-mobile-ios/` esegui in Terminale:

```bash
brew install xcodegen
xcodegen generate
open CybenDefender.xcodeproj
```

Questo rigenera il progetto Xcode da zero partendo dal file `project.yml`.
