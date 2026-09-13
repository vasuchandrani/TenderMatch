#!/usr/bin/env bash
# ==============================================================================
# TenderMatch - Unified Shutdown Script
# ==============================================================================

echo "Stopping TenderMatch services..."

# Kill frontend on port 3000
PIDS_3000=$(lsof -ti :3000 2>/dev/null)
if [ -n "$PIDS_3000" ]; then
  echo "• Stopping Frontend (port 3000)..."
  kill -9 $PIDS_3000 2>/dev/null || true
fi

# Kill backend on port 8080
PIDS_8080=$(lsof -ti :8080 2>/dev/null)
if [ -n "$PIDS_8080" ]; then
  echo "• Stopping Backend (port 8080)..."
  kill -9 $PIDS_8080 2>/dev/null || true
fi

# Kill AI service on port 8000
PIDS_8000=$(lsof -ti :8000 2>/dev/null)
if [ -n "$PIDS_8000" ]; then
  echo "• Stopping AI Service (port 8000)..."
  kill -9 $PIDS_8000 2>/dev/null || true
fi

echo ""
read -p "Do you also want to stop Docker containers (Postgres & MinIO)? (y/N): " -r response
if [[ "$response" =~ ^([yY][eE][sS]|[yY])$ ]]; then
  cd "$(dirname "$0")"
  docker compose stop postgres minio
  echo "• Docker containers stopped."
fi

echo "✓ All TenderMatch local services stopped successfully."
