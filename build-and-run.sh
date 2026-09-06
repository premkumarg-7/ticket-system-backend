#!/usr/bin/env bash
# Build and run all ticket-system service JARs.
# Usage:
#   ./build-and-run.sh            # build + run all
#   ./build-and-run.sh build      # build only
#   ./build-and-run.sh run        # run only (assumes JARs already built)
#   ./build-and-run.sh stop       # stop running services
#   SKIP_TESTS=false ./build-and-run.sh   # include tests during build

set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
LOG_DIR="${ROOT_DIR}/logs"
PID_DIR="${ROOT_DIR}/.pids"
SPRING_PROFILE="${SPRING_PROFILE:-dev}"
SKIP_TESTS="${SKIP_TESTS:-true}"

# When the project lives on /mnt/ (Windows drive via WSL), Gradle's lock files
# fail with I/O errors. Redirect Gradle build caches to the Linux filesystem.
BUILD_CACHE_ROOT="${BUILD_CACHE_ROOT:-${HOME}/.cache/ticket-system}"

mkdir -p "${LOG_DIR}" "${PID_DIR}" "${BUILD_CACHE_ROOT}"

# name | dir | builder (gradle|maven) | port
SERVICES=(
  "auth|auth|gradle|8081"
  "ticketService|ticketService|maven|8088"
  "notification-service|notification-service|gradle|9000"
  "gateway|gateway|gradle|8001"
)

log()  { printf '\033[1;34m[%s]\033[0m %s\n' "$(date +%H:%M:%S)" "$*"; }
warn() { printf '\033[1;33m[%s]\033[0m %s\n' "$(date +%H:%M:%S)" "$*"; }
err()  { printf '\033[1;31m[%s]\033[0m %s\n' "$(date +%H:%M:%S)" "$*" >&2; }

if [[ "${ROOT_DIR}" == /mnt/* ]]; then
  log "Project is on ${ROOT_DIR} (WSL mount) — Gradle project cache redirected to ${BUILD_CACHE_ROOT}."
fi

build_service() {
  local name="$1" dir="$2" builder="$3"
  log "Building ${name} (${builder})..."
  local proj_cache="${BUILD_CACHE_ROOT}/${name}/.gradle"
  pushd "${ROOT_DIR}/${dir}" >/dev/null
  case "${builder}" in
    gradle)
      local args=("bootJar" "--no-daemon" "--project-cache-dir=${proj_cache}")
      [[ "${SKIP_TESTS}" == "true" ]] && args+=("-x" "test")
      ./gradlew "${args[@]}"
      ;;
    maven)
      local args=("clean" "package")
      [[ "${SKIP_TESTS}" == "true" ]] && args+=("-DskipTests")
      ./mvnw "${args[@]}"
      ;;
    *) err "Unknown builder: ${builder}"; exit 1 ;;
  esac
  popd >/dev/null
}

find_jar() {
  local dir="$1" builder="$2" jar
  case "${builder}" in
    gradle) jar=$(find "${ROOT_DIR}/${dir}/build/libs" -maxdepth 1 -name '*.jar' ! -name '*-plain.jar' | head -n1) ;;
    maven)  jar=$(find "${ROOT_DIR}/${dir}/target"     -maxdepth 1 -name '*.jar' ! -name 'original-*' ! -name '*-sources.jar' ! -name '*-javadoc.jar' | head -n1) ;;
  esac
  echo "${jar}"
}

run_service() {
  local name="$1" dir="$2" builder="$3" port="$4"
  local jar
  jar=$(find_jar "${dir}" "${builder}")
  if [[ -z "${jar}" || ! -f "${jar}" ]]; then
    err "JAR not found for ${name} — run build first."
    return 1
  fi
  local pid_file="${PID_DIR}/${name}.pid"
  if [[ -f "${pid_file}" ]] && kill -0 "$(cat "${pid_file}")" 2>/dev/null; then
    warn "${name} already running (PID $(cat "${pid_file}"))."
    return 0
  fi
  log "Starting ${name} on port ${port} → ${LOG_DIR}/${name}.log"
  nohup java -jar "${jar}" \
    --spring.profiles.active="${SPRING_PROFILE}" \
    --server.port="${port}" \
    > "${LOG_DIR}/${name}.log" 2>&1 &
  echo $! > "${pid_file}"
  log "${name} started (PID $(cat "${pid_file}"))."
}

stop_service() {
  local name="$1"
  local pid_file="${PID_DIR}/${name}.pid"
  if [[ ! -f "${pid_file}" ]]; then
    warn "${name}: no pid file."
    return 0
  fi
  local pid; pid=$(cat "${pid_file}")
  if kill -0 "${pid}" 2>/dev/null; then
    log "Stopping ${name} (PID ${pid})..."
    kill "${pid}" || true
    for _ in 1 2 3 4 5 6 7 8 9 10; do
      kill -0 "${pid}" 2>/dev/null || break
      sleep 1
    done
    kill -0 "${pid}" 2>/dev/null && kill -9 "${pid}" || true
  fi
  rm -f "${pid_file}"
}

cmd_build() {
  for entry in "${SERVICES[@]}"; do
    IFS='|' read -r name dir builder _port <<< "${entry}"
    build_service "${name}" "${dir}" "${builder}"
  done
  log "All builds complete."
}

cmd_run() {
  for entry in "${SERVICES[@]}"; do
    IFS='|' read -r name dir builder port <<< "${entry}"
    run_service "${name}" "${dir}" "${builder}" "${port}"
    sleep 3   # small stagger so dependents come up after auth/ticketService
  done
  log "All services launched. Tail logs with: tail -f ${LOG_DIR}/*.log"
}

cmd_stop() {
  for entry in "${SERVICES[@]}"; do
    IFS='|' read -r name _dir _builder _port <<< "${entry}"
    stop_service "${name}"
  done
  log "All services stopped."
}

case "${1:-all}" in
  build) cmd_build ;;
  run)   cmd_run ;;
  stop)  cmd_stop ;;
  all)   cmd_build; cmd_run ;;
  *) err "Usage: $0 [build|run|stop|all]"; exit 1 ;;
esac
