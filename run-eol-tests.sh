#!/usr/bin/env bash
#
# Test runner for static analysis and LSP regressions.
#
# Rebuilds the engines, language server and test modules, then runs JUnit directly.
#
# Usage:
#   ./run-eol-tests.sh                          # run analyser, LSP and standalone LSP suites
#   ./run-eol-tests.sh -f multiPackage.eol       # filter output to a specific test file
#
set -euo pipefail

PROJECT="$(cd "$(dirname "$0")" && pwd)"
PLUGINS="$PROJECT/plugins"
TESTS="$PROJECT/tests/org.eclipse.epsilon.eol.staticanalyser.tests"
TYCHO_CACHE="$HOME/.m2/repository/.cache/tycho"
P2_BUNDLES="$HOME/.m2/repository/p2/osgi/bundle"
BUILD_PROJECTS="plugins/org.eclipse.epsilon.common,plugins/org.eclipse.epsilon.eol.engine,plugins/org.eclipse.epsilon.erl.engine,plugins/org.eclipse.epsilon.egl.engine,plugins/org.eclipse.epsilon.emc.emf,plugins/org.eclipse.epsilon.emc.plainxml,plugins/org.eclipse.epsilon.emc.bibtex,plugins/org.eclipse.epsilon.evl.engine,plugins/org.eclipse.epsilon.flexmi,plugins/org.eclipse.epsilon.lsp,tests/org.eclipse.epsilon.eol.staticanalyser.tests,tests/org.eclipse.epsilon.lsp.test"

# --- Step 1: Explicitly rebuild the plugins used by the tests (avoids stale JARs) ---

echo "=== Rebuilding ==="
mvn clean package -pl "$BUILD_PROJECTS,plugins/org.eclipse.epsilon.lsp.standalone,tests/org.eclipse.epsilon.lsp.standalone.test" -am -DskipTests -q
echo "=== Build done ==="

# --- Step 2: Resolve external dependency JARs from Tycho cache ---

EXT_CP=""
for jar in \
  "org.antlr.runtime_3.5.2.*.jar" \
  "org.apache.commons.collections_3.2.2.*.jar" \
  "org.eclipse.emf.common_2.*.jar" \
  "org.eclipse.emf.ecore_2.*.jar" \
  "org.eclipse.emf.ecore.xmi_2.*.jar" \
  "org.eclipse.emf.ecore.change_2.*.jar" \
  "org.eclipse.emf_2.*.jar" \
  "org.eclipse.xsd_2.*.jar" \
  "org.eclipse.emf.emfatic.core_*.jar" \
  "org.eclipse.gymnast.runtime.core_*.jar" \
  "org.apache.commons.io_*.jar" \
  "org.eclipse.core.runtime_*.jar" \
  "org.eclipse.equinox.common_*.jar" \
  "org.eclipse.equinox.registry_*.jar" \
  "org.eclipse.osgi_*.jar" \
  "org.eclipse.lsp4e_0.19.12.*.jar" \
  "org.eclipse.jface_*.jar" \
  "org.eclipse.lsp4j_*.jar" \
  "org.eclipse.lsp4j.jsonrpc_*.jar" \
  "com.google.guava_*.jar" \
  "com.google.guava.failureaccess_*.jar" \
  "com.google.gson_*.jar" \
  "org.apache.commons.lang3_*.jar" \
  "org.yaml.snakeyaml_*.jar"; do
  # Tycho 5 stores resolved bundles in Maven layout; prefer those over older
  # downloads in the transport cache, which can contain stale platform versions.
  found=""
  bundle="${jar%%_*}"
  if [ -d "$P2_BUNDLES/$bundle" ]; then
    found=$(find "$P2_BUNDLES/$bundle" -name "${jar/_/-}" 2>/dev/null | sort -V | tail -1)
  fi
  if [ -z "$found" ]; then
    found=$(find "$TYCHO_CACHE" -name "$jar" -not -name "*.headers" 2>/dev/null | sort -V | tail -1)
  fi
  if [ -z "$found" ]; then
    echo "ERROR: Could not find $jar in Tycho cache" >&2
    exit 1
  fi
  EXT_CP="$EXT_CP:$found"
done
EXT_CP="$EXT_CP:$HOME/.m2/repository/junit/junit/4.13.2/junit-4.13.2.jar"
EXT_CP="$EXT_CP:$HOME/.m2/repository/org/hamcrest/hamcrest-core/1.3/hamcrest-core-1.3.jar"
EXT_CP="${EXT_CP#:}"

# --- Step 3: Build runtime classpath from target/ JARs ---

CP="$TESTS/target/classes"
CP="$CP:$PLUGINS/org.eclipse.epsilon.eol.engine/target/org.eclipse.epsilon.eol.engine-2.10.0-SNAPSHOT.jar"
CP="$CP:$PLUGINS/org.eclipse.epsilon.common/target/org.eclipse.epsilon.common-2.10.0-SNAPSHOT.jar"
CP="$CP:$PLUGINS/org.eclipse.epsilon.emc.emf/target/org.eclipse.epsilon.emc.emf-2.10.0-SNAPSHOT.jar"
CP="$CP:$PLUGINS/org.eclipse.epsilon.emc.plainxml/target/org.eclipse.epsilon.emc.plainxml-2.10.0-SNAPSHOT.jar"
CP="$CP:$PLUGINS/org.eclipse.epsilon.evl.engine/target/org.eclipse.epsilon.evl.engine-2.10.0-SNAPSHOT.jar"
CP="$CP:$PLUGINS/org.eclipse.epsilon.erl.engine/target/org.eclipse.epsilon.erl.engine-2.10.0-SNAPSHOT.jar"
CP="$CP:$PLUGINS/org.eclipse.epsilon.emc.bibtex/target/org.eclipse.epsilon.emc.bibtex-2.10.0-SNAPSHOT.jar"
CP="$CP:$PLUGINS/org.eclipse.epsilon.egl.engine/target/org.eclipse.epsilon.egl.engine-2.10.0-SNAPSHOT.jar"
CP="$CP:$PLUGINS/org.eclipse.epsilon.lsp/target/org.eclipse.epsilon.lsp-2.10.0-SNAPSHOT.jar"
CP="$CP:$PLUGINS/org.eclipse.epsilon.flexmi/target/org.eclipse.epsilon.flexmi-2.10.0-SNAPSHOT.jar"
CP="$CP:$PROJECT/tests/org.eclipse.epsilon.lsp.test/target/classes"
CP="$CP:$PLUGINS/org.eclipse.epsilon.lsp.standalone/target/org.eclipse.epsilon.lsp.standalone-2.10.0-SNAPSHOT.jar"
CP="$CP:$PROJECT/tests/org.eclipse.epsilon.lsp.standalone.test/target/classes"
CP="$CP:$EXT_CP"

# --- Step 4: Parse arguments ---

FILTER_FILE=""
while [[ $# -gt 0 ]]; do
  case "$1" in
    -f) FILTER_FILE="$2"; shift 2 ;;
    *)  echo "Usage: $0 [-f file.eol]"; exit 1 ;;
  esac
done

# --- Step 5: Run tests (from test module dir for resource resolution) ---

cd "$TESTS"

echo "=== Running tests ==="
TEST_CLASSES="org.eclipse.epsilon.eol.staticanalyser.tests.StaticAnalyserTestSuite org.eclipse.epsilon.lsp.test.EpsilonLanguageServerTestSuite org.eclipse.epsilon.lsp.standalone.test.StandaloneLanguageServerTestSuite"
if [ -n "$FILTER_FILE" ]; then
  java -cp "$CP" org.junit.runner.JUnitCore $TEST_CLASSES 2>&1 | \
    awk -v pat="$FILTER_FILE" '
      /^Tests run:/ || /^FAILURES/ || /^OK/ || /^There w/ || /^Time:/ { print; next }
      /^Testing program:/ { show = (index($0, pat) > 0) }
      /^[0-9]+\)/ { show = (index($0, pat) > 0) }
      show { print }
    '
else
  java -cp "$CP" org.junit.runner.JUnitCore $TEST_CLASSES 2>&1
fi
