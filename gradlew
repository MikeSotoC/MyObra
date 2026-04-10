#!/usr/bin/env sh

# Lightweight fallback wrapper for environments where Gradle wrapper artifacts
# are not yet generated. Delegates to installed `gradle`.

if ! command -v gradle >/dev/null 2>&1; then
  echo "Error: gradle no está instalado en el sistema." >&2
  exit 1
fi

if [ -z "$JAVA_HOME" ] || echo "$JAVA_HOME" | grep -Eq '/2(5|6|7)(\.|$)'; then
  for CANDIDATE in \
    "$HOME/.local/share/mise/installs/java/21.0.2" \
    "$HOME/.local/share/mise/installs/java/21" \
    "$HOME/.sdkman/candidates/java/21"; do
    if [ -d "$CANDIDATE" ]; then
      export JAVA_HOME="$CANDIDATE"
      export PATH="$JAVA_HOME/bin:$PATH"
      break
    fi
  done
fi

exec gradle "$@"
