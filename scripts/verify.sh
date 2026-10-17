#!/usr/bin/env bash
# One-line verification for a ticket. Usage: scripts/verify.sh [module ...]
#   e.g. scripts/verify.sh domain data feature/booking     (default: domain data)
# Builds the app (Android + iOS compile), runs detekt, runs each module's host AND iOS tests from
# clean results, prints real per-module counts, and greps for the project's red flags.
# Exits non-zero if anything fails. Needs network for Gradle (agents: dangerouslyDisableSandbox).
set -uo pipefail
cd "$(git rev-parse --show-toplevel)"
mods=("$@"); [ ${#mods[@]} -eq 0 ] && mods=(domain data)
log=$(mktemp -t verify-gradle); status=0; tasks=()
for m in "${mods[@]}"; do
  [ -d "$m" ] || { echo "unknown module dir: $m"; exit 2; }
  rm -rf "$m/build/test-results"; g=":${m//\//:}"
  tasks+=("$g:testAndroidHostTest" "$g:iosSimulatorArm64Test")
done
echo "== gradle: ${mods[*]} (tests --rerun) + assembleDebug + iOS compile + detekt"
if ! ./gradlew "${tasks[@]}" --rerun :androidApp:assembleDebug :shared:compileKotlinIosSimulatorArm64 detekt >"$log" 2>&1; then
  echo "GRADLE FAILED — log: $log"; grep -E "^e: |FAILED|What went wrong" -A3 "$log" | head -40; status=1
fi
echo "== tests (real counts from XML)"
python3 - "${mods[@]}" <<'PY' || status=1
import glob, re, sys
bad = False
for m in sys.argv[1:]:
    for t in ("testAndroidHostTest", "iosSimulatorArm64Test"):
        fs = glob.glob(f"{m}/build/test-results/{t}/TEST-*.xml")
        n = f = e = 0
        for p in fs:
            x = re.search(r'tests="(\d+)" skipped="\d+" failures="(\d+)" errors="(\d+)"', open(p).read())
            n += int(x[1]); f += int(x[2]); e += int(x[3])
        flag = "" if (n and not f and not e) else "  <-- CHECK"
        bad |= bool(f or e)
        print(f"{m:20} {t:22} {n:5} tests  {f} failures  {e} errors{flag}")
sys.exit(1 if bad else 0)
PY
echo "== red flags (vs origin/main)"
hard=$(grep -rnE 'Color\(0x|[0-9]\.(dp|sp)\b' feature core/designsystem/src --include='*.kt' 2>/dev/null | grep -v '/build/' | grep -v 'designsystem/theme/' || true)
[ -n "$hard" ] && { echo "hardcoded design values:"; echo "$hard" | head; status=1; } || echo "hardcoded design values: none"
sup=$(git diff origin/main -- '*.kt' | grep -c '^+.*@Suppress' || true)
echo "new @Suppress lines: $sup (each needs a written justification)"
echo "architect-owned files changed: $(git diff --name-only origin/main | grep -E 'App\.kt|InitKoin\.kt|Destination\.kt|SampleData\.kt|settings\.gradle\.kts|shared/build\.gradle\.kts' | tr '\n' ' ')"
[ $status -eq 0 ] && echo "RESULT: OK" || echo "RESULT: FAILED"
exit $status
