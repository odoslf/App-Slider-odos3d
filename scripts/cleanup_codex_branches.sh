#!/usr/bin/env bash
# === Limpieza de ramas "codex/*" con backup, dejando solo 'main' ===
# Este script crea un respaldo de cada rama remota que comience con "codex/",
# fusiona sus cambios en la rama principal configurada y posteriormente las
# elimina tanto local como remotamente.
#
# Requisitos:
#   - Acceso de escritura al repositorio remoto.
#   - La herramienta `gh` es opcional; si está disponible intentará fijar la rama
#     principal como predeterminada.
#
# Uso:
#   REPO_DIR="/ruta/al/repositorio" BR_KEEP="main" ./cleanup_codex_branches.sh
#
# Variables de entorno opcionales:
#   REPO_DIR : Directorio del repositorio. Por defecto, el directorio actual.
#   BR_KEEP  : Rama principal que se desea conservar (default: main).

set -euo pipefail

REPO_DIR="${REPO_DIR:-$(pwd)}"
BR_KEEP="${BR_KEEP:-main}"

cd "$REPO_DIR"

echo "[INFO] Limpiando ramas codex/* en $REPO_DIR dejando como principal '$BR_KEEP'"

# 1) Traer todo y limpiar referencias obsoletas
git fetch --all --prune

# 2) Detectar ramas remotas 'codex/*'
mapfile -t CODEX_BRANCHES < <(git branch -r | sed -n 's|  origin/\(codex/.*\)$|\1|p')

if (( ${#CODEX_BRANCHES[@]} == 0 )); then
  echo "[INFO] No hay ramas 'codex/*' remotas que limpiar."
  exit 0
fi

echo "[INFO] Ramas Codex encontradas: ${CODEX_BRANCHES[*]}"

# 3) Bucle: backup + borrado remoto y local
for BR_OLD in "${CODEX_BRANCHES[@]}"; do
  TS=$(date -u +%Y%m%d-%H%M%S)
  SAFE="${BR_OLD//\//-}"
  BACKUP_BRANCH="backup/${SAFE}-$TS"
  BACKUP_TAG="backup/${SAFE}-$TS"

  echo "[INFO] Procesando origin/$BR_OLD"
  echo "[INFO]   - Creando backups en rama '$BACKUP_BRANCH' y tag '$BACKUP_TAG'"

  # 3.1) Crear backups apuntando al commit remoto actual de esa rama
  git branch -f "$BACKUP_BRANCH" "origin/$BR_OLD"
  git tag -a "$BACKUP_TAG" "origin/$BR_OLD" -m "Backup antes de borrar $BR_OLD"

  # 3.2) Subir backups
  git push origin "$BACKUP_BRANCH"
  git push origin "$BACKUP_TAG"

  # 3.3) Intentar fusionar cambios en la rama principal
  echo "[INFO]   - Fusionando origin/$BR_OLD en $BR_KEEP"
  git switch "$BR_KEEP"
  if git merge --no-ff "origin/$BR_OLD" -m "Merge de $BR_OLD antes de limpieza"; then
    git push origin "$BR_KEEP"
  else
    echo "[WARN] Conflicto al fusionar origin/$BR_OLD. Se aborta merge y se continúa."
    git merge --abort || true
  fi

  # 3.4) Borrar rama remota y limpieza local
  echo "[INFO]   - Eliminando rama origin/$BR_OLD y referencias locales"
  git push origin --delete "$BR_OLD" || true
  git branch -D "$BR_OLD" 2>/dev/null || true

done

# 4) Asegurarnos de estar en 'BR_KEEP' y publicar
git switch "$BR_KEEP"
git pull --ff-only origin "$BR_KEEP" || true
git push origin "$BR_KEEP" || true

# 5) Fijar 'BR_KEEP' como rama por defecto si 'gh' está disponible y autenticado
REMOTE_URL="$(git remote get-url origin)"
OWNER_REPO="$(echo "$REMOTE_URL" | sed -E 's#.*github.com[:/]([^/]+/[^/.]+)(\.git)?#\1#')"
if command -v gh >/dev/null 2>&1; then
  echo "[INFO] Ajustando rama predeterminada a '$BR_KEEP' en GitHub"
  gh repo edit "$OWNER_REPO" --default-branch "$BR_KEEP" || true
fi

# 6) Mostrar estado final
echo "=== Ramas remotas tras limpieza ==="
git fetch --all --prune
git branch -r
echo "Listo. Backups creados bajo 'backup/*' (rama + tag) por cada rama codex eliminada."
