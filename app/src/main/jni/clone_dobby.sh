#!/bin/bash
# Clone Dobby source into jni/dobby/ for native ECDH hook
# Run this once before building:
#   cd app/src/main/jni && bash clone_dobby.sh

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
DOBBY_DIR="$SCRIPT_DIR/dobby"

if [ -d "$DOBBY_DIR" ]; then
    echo "Dobby already exists at $DOBBY_DIR"
    echo "To update: cd $DOBBY_DIR && git pull"
    exit 0
fi

echo "Cloning Dobby..."
git clone --depth 1 https://github.com/jmpews/Dobby.git "$DOBBY_DIR"

if [ $? -eq 0 ]; then
    echo "Dobby cloned successfully to $DOBBY_DIR"
else
    echo "Failed to clone Dobby. You can also manually download:"
    echo "  https://github.com/jmpews/Dobby/archive/refs/heads/master.zip"
    echo "  Extract to: $DOBBY_DIR"
    exit 1
fi
