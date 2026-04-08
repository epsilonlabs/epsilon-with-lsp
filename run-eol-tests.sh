#!/usr/bin/env bash
#
# Fast test runner for EolBuildTests and EvlBuildTests.
#
# Rebuilds eol.engine + test module via Maven, then runs tests directly with java.
#
# Usage:
#   ./run-eol-tests.sh                          # run all EolBuildTests + EvlBuildTests
#   ./run-eol-tests.sh -f multiPackage.eol       # filter output to a specific test file
#
set -euo pipefail

PROJECT="$(cd "$(dirname "$0")" && pwd)"
PLUGINS="$PROJECT/plugins"
TESTS="$PROJECT/tests/org.eclipse.epsilon.eol.staticanalyser.tests"
TYCHO_CACHE="$HOME/.m2/repository/.cache/tycho"

# --- Step 1: Rebuild eol.engine (and its deps) + test module ---

echo "=== Rebuilding ==="
mvn package -pl plugins/org.eclipse.epsilon.eol.engine,plugins/org.eclipse.epsilon.emc.emf,plugins/org.eclipse.epsilon.evl.staticanalyser,tests/org.eclipse.epsilon.eol.staticanalyser.tests -am -DskipTests -q
echo "=== Build done ==="

# --- Step 2: Resolve external dependency JARs from Tycho cache ---

EXT_CP=""
for jar in \
  "org.antlr.runtime_3.2.0.*.jar" \
  "org.apache.commons.collections_3.2.2.*.jar" \
  "org.eclipse.emf.common_2.*.jar" \
  "org.eclipse.emf.ecore_2.*.jar" \
  "org.eclipse.emf.ecore.xmi_2.*.jar" \
  "org.eclipse.emf.ecore.change_2.*.jar" \
  "org.eclipse.emf_2.*.jar" \
  "org.eclipse.xsd_2.*.jar"; do
  found=$(find "$TYCHO_CACHE" -name "$jar" -not -name "*.headers" 2>/dev/null | head -1)
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
CP="$CP:$PLUGINS/org.eclipse.epsilon.eol.engine/target/org.eclipse.epsilon.eol.engine-2.9.0-SNAPSHOT.jar"
CP="$CP:$PLUGINS/org.eclipse.epsilon.common/target/org.eclipse.epsilon.common-2.9.0-SNAPSHOT.jar"
CP="$CP:$PLUGINS/org.eclipse.epsilon.emc.emf/target/org.eclipse.epsilon.emc.emf-2.9.0-SNAPSHOT.jar"
CP="$CP:$PLUGINS/org.eclipse.epsilon.evl.engine/target/org.eclipse.epsilon.evl.engine-2.9.0-SNAPSHOT.jar"
CP="$CP:$PLUGINS/org.eclipse.epsilon.evl.staticanalyser/target/org.eclipse.epsilon.evl.staticanalyser-2.9.0-SNAPSHOT.jar"
CP="$CP:$PLUGINS/org.eclipse.epsilon.erl.engine/target/org.eclipse.epsilon.erl.engine-2.9.0-SNAPSHOT.jar"
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
TEST_CLASSES="org.eclipse.epsilon.eol.staticanalyser.tests.EolBuildTests org.eclipse.epsilon.eol.staticanalyser.tests.EvlBuildTests"
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
