#!/bin/bash
# build.sh - Build-Script für das dog-seek Spiel
# Baut wahlweise die JVM-JAR und/oder eine native GraalVM-Binary.
#
# Verwendung:
#   ./build.sh                 # baut beides (JVM + native)
#   ./build.sh jvm             # nur JVM-JAR
#   ./build.sh native          # nur native Binary (GraalVM + native-image nötig)
#   ./build.sh all run         # baut beides und startet danach die native Binary
#   ./build.sh clean           # räumt target/ auf
#
# Optionaler zweiter Parameter:
#   tests   - Tests mit ausführen (Standard: übersprungen)

set -e

# Ins Projektverzeichnis wechseln (wo dieses Script liegt)
cd "$(dirname "$0")"

MODE="${1:-all}"
RUN_AFTER="false"
SKIP_FLAG="-DskipTests"

# Zweites Argument verarbeiten
if [ -n "${2:-}" ]; then
    if [ "$2" = "tests" ]; then
        SKIP_FLAG=""
    elif [ "$2" = "run" ]; then
        RUN_AFTER="true"
    fi
fi
# Wenn "run" als Teil von MODE kam (z. B. "all run")
if [ "${2:-}" = "run" ]; then
    RUN_AFTER="true"
fi

# --- Hilfsfunktionen ---
print_header() {
    echo ""
    echo "==============================================================="
    echo " $1"
    echo "==============================================================="
}

check_mvn() {
    if ! command -v mvn >/dev/null 2>&1; then
        echo "FEHLER: Maven (mvn) ist nicht installiert oder nicht im PATH."
        exit 1
    fi
}

check_native_image() {
    if [ "$MODE" = "native" ] || [ "$MODE" = "all" ]; then
        if ! command -v native-image >/dev/null 2>&1 && [ ! -x "$JAVA_HOME/bin/native-image" ]; then
            echo "WARNUNG: native-image wurde nicht gefunden."
            echo "Stelle sicher, dass GraalVM mit native-image installiert ist und JAVA_HOME darauf zeigt."
            echo "Unter sdkman:    sdk install java 25-graal && sdk use java 25-graal"
            echo ""
            read -p "Trotzdem versuchen? (j/n) " -n 1 -r
            echo
            if [[ ! $REPLY =~ ^[Jj]$ ]]; then
                exit 1
            fi
        fi
    fi
}

# --- Hauptlogik ---
check_mvn

case "$MODE" in
    clean)
        print_header "Aufräumen"
        mvn clean
        echo "target/ wurde entfernt."
        ;;
    jvm)
        check_native_image
        print_header "JVM-Build (Spring Boot Fat-JAR)"
        mvn clean package $SKIP_FLAG
        echo ""
        echo "Fertig: target/dog-seek-1.0-SNAPSHOT.jar"
        echo "Starten mit:  java -jar target/dog-seek-1.0-SNAPSHOT.jar"
        if [ "$RUN_AFTER" = "true" ]; then
            java -jar target/dog-seek-1.0-SNAPSHOT.jar
        fi
        ;;
    native)
        check_native_image
        print_header "Native-Build (GraalVM Native Image)"
        mvn clean $SKIP_FLAG native:compile
        echo ""
        echo "Fertig: target/dog-seek"
        echo "Starten mit:  ./target/dog-seek"
        if [ "$RUN_AFTER" = "true" ]; then
            ./target/dog-seek
        fi
        ;;
    all)
        check_native_image
        print_header "1/2 JVM-Build"
        mvn clean package $SKIP_FLAG
        echo ""
        echo "JVM-Artefakt: target/dog-seek-1.0-SNAPSHOT.jar"

        print_header "2/2 Native-Build"
        mvn clean $SKIP_FLAG native:compile
        echo ""
        echo "Native-Binary: target/dog-seek"

        if [ "$RUN_AFTER" = "true" ]; then
            print_header "Starte native Binary"
            ./target/dog-seek
        fi
        ;;
    help|--help|-h)
        sed -n '2,15p' "$0"
        ;;
    *)
        echo "Unbekannter Modus: $MODE"
        echo "Verwendung: $0 [jvm|native|all|clean|help]"
        exit 1
        ;;
esac

echo ""
echo "Fertig."
