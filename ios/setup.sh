#!/bin/bash
# Cyben Guard iOS — Setup script
# Esegui questo script dalla cartella cyben-mobile-ios/ dopo aver dezippato

echo "=== Cyben Guard iOS Setup ==="

PROJ="CybenDefender.xcodeproj"

# Crea cartella xcschemes se mancante
mkdir -p "$PROJ/xcshareddata/xcschemes"
mkdir -p "$PROJ/project.xcworkspace"

echo "✓ Struttura progetto verificata"
echo ""
echo "Apri il progetto in Xcode:"
echo "  open $PROJ"
echo ""
echo "Se Xcode mostra 'No schemes are available':"
echo "  1. Menu Product → Scheme → Manage Schemes"
echo "  2. Clicca 'Autocreate Schemes Now'"
echo "  OPPURE"
echo "  1. Clicca sul selettore scheme nella toolbar"
echo "  2. Scegli 'New Scheme...'"
echo "  3. Seleziona 'CybenDefender' → OK"

# Apre direttamente il progetto
open "$PROJ" 2>/dev/null || echo "Apri manualmente CybenDefender.xcodeproj in Xcode"
