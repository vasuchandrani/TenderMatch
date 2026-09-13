#!/usr/bin/env bash
# ==============================================================================
# TenderMatch - Unified Master Startup Script
# ==============================================================================
# Starts all services (Docker, Ollama, AI Service, Spring Backend, Frontend)
# ==============================================================================

PROJECT_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
cd "$PROJECT_ROOT"

mkdir -p "$PROJECT_ROOT/logs"

echo ""
echo "============================================================="
echo "  🚀 STARTING TENDERMATCH FULL-STACK PLATFORM"
echo "============================================================="
echo "Project Root: $PROJECT_ROOT"
echo "Logs Folder:  $PROJECT_ROOT/logs"
echo ""

# Terminal colors
GREEN="\033[0;32m"
BLUE="\033[0;34m"
YELLOW="\033[1;33m"
RED="\033[0;31m"
NC="\033[0m"

# Helper: check if a port is in use
is_port_in_use() {
  local port="$1"
  lsof -i :"$port" >/dev/null 2>&1
}

# Helper: wait for an HTTP URL to return 200 OK
wait_for_url() {
  local url="$1"
  local max_wait="${2:-30}"
  local count=0
  while ! curl -s -f "$url" >/dev/null 2>&1; do
    sleep 1
    count=$((count + 1))
    if [ "$count" -ge "$max_wait" ]; then
      return 1
    fi
  done
  return 0
}

# ------------------------------------------------------------------------------
# 1. Docker Infrastructure (Postgres on 5433 & MinIO on 9000/9001)
# ------------------------------------------------------------------------------
echo -e "${BLUE}[1/5] Checking Docker Infrastructure (Postgres & MinIO)...${NC}"
if ! docker info >/dev/null 2>&1; then
  echo -e "${RED}✗ Docker is not running. Please start Docker Desktop and retry.${NC}"
  exit 1
fi

docker compose up -d postgres minio >/dev/null 2>&1
echo -e "${GREEN}✓ Docker containers running (PostgreSQL: 5433, MinIO: 9000/9001)${NC}"

# ------------------------------------------------------------------------------
# 2. Ollama Local LLM (Port 11434)
# ------------------------------------------------------------------------------
echo -e "${BLUE}[2/5] Checking Ollama LLM Service (Port 11434)...${NC}"
if is_port_in_use 11434; then
  echo -e "${GREEN}✓ Ollama is already running on port 11434${NC}"
else
  echo "  Starting Ollama in background..."
  nohup ollama serve > "$PROJECT_ROOT/logs/ollama.log" 2>&1 &
  if wait_for_url "http://localhost:11434/api/tags" 15; then
    echo -e "${GREEN}✓ Ollama started successfully${NC}"
  else
    echo -e "${YELLOW}⚠ Ollama started (see logs/ollama.log)${NC}"
  fi
fi

# ------------------------------------------------------------------------------
# 3. AI Microservice (FastAPI on Port 8000)
# ------------------------------------------------------------------------------
echo -e "${BLUE}[3/5] Checking AI Microservice (Port 8000)...${NC}"
if is_port_in_use 8000; then
  echo -e "${GREEN}✓ AI service is already running on port 8000${NC}"
else
  echo "  Starting AI microservice (FastAPI)..."
  cd "$PROJECT_ROOT/tendermatch-ai"
  
  if [ -f ".venv/bin/activate" ]; then
    source .venv/bin/activate
  elif [ -f "venv/bin/activate" ]; then
    source venv/bin/activate
  fi
  
  nohup uvicorn app.main:app --host 0.0.0.0 --port 8000 --reload > "$PROJECT_ROOT/logs/ai-service.log" 2>&1 &
  cd "$PROJECT_ROOT"
  
  if wait_for_url "http://localhost:8000/health" 20; then
    echo -e "${GREEN}✓ AI service is healthy on http://localhost:8000${NC}"
  else
    echo -e "${YELLOW}⚠ AI service launching... check logs/ai-service.log${NC}"
  fi
fi

# ------------------------------------------------------------------------------
# 4. Backend Microservice (Spring Boot on Port 8080)
# ------------------------------------------------------------------------------
echo -e "${BLUE}[4/5] Checking Backend Service (Port 8080)...${NC}"
if is_port_in_use 8080; then
  echo -e "${GREEN}✓ Backend is already running on port 8080${NC}"
else
  echo "  Starting Spring Boot backend (this may take 15-20 seconds)..."
  cd "$PROJECT_ROOT/tendermatch-backend"
  
  if [ -x "./mvnw" ]; then
    nohup ./mvnw spring-boot:run > "$PROJECT_ROOT/logs/backend.log" 2>&1 &
  else
    nohup mvn spring-boot:run > "$PROJECT_ROOT/logs/backend.log" 2>&1 &
  fi
  cd "$PROJECT_ROOT"
  
  if wait_for_url "http://localhost:8080/api/health" 40; then
    echo -e "${GREEN}✓ Backend is healthy on http://localhost:8080${NC}"
  else
    echo -e "${YELLOW}⚠ Backend is starting up... check logs/backend.log${NC}"
  fi
fi

# ------------------------------------------------------------------------------
# 5. Frontend Web Application (Vite on Port 3000)
# ------------------------------------------------------------------------------
echo -e "${BLUE}[5/5] Checking Frontend Application (Port 3000)...${NC}"
if is_port_in_use 3000; then
  echo -e "${GREEN}✓ Frontend is already running on port 3000${NC}"
else
  echo "  Starting Frontend (Vite dev server)..."
  cd "$PROJECT_ROOT/tendermatch-frontend"
  nohup npm run dev > "$PROJECT_ROOT/logs/frontend.log" 2>&1 &
  cd "$PROJECT_ROOT"
  
  if wait_for_url "http://localhost:3000" 15; then
    echo -e "${GREEN}✓ Frontend is live at http://localhost:3000${NC}"
  else
    echo -e "${YELLOW}⚠ Frontend starting... check logs/frontend.log${NC}"
  fi
fi

echo ""
echo "============================================================="
echo -e "${GREEN}  🎉 TENDERMATCH PLATFORM IS FULLY OPERATIONAL!${NC}"
echo "============================================================="
echo "  🌐 Frontend Web App:     http://localhost:3000"
echo "  ☕ Backend API:          http://localhost:8080/api/health"
echo "  🤖 AI Service & Docs:    http://localhost:8000/docs"
echo "  📦 MinIO Storage UI:     http://localhost:9001 (minioadmin/minioadmin)"
echo "  🗄️ PostgreSQL Database:  localhost:5433 (tendermatch/tendermatch_secret)"
echo "============================================================="
echo ""
echo "Live logs can be viewed with:"
echo "  • AI Service:  tail -f logs/ai-service.log"
echo "  • Backend:     tail -f logs/backend.log"
echo "  • Frontend:    tail -f logs/frontend.log"
echo ""
echo "To shut down all services anytime, run:"
echo "  ./stop_project.sh"
echo "============================================================="

# Automatically open browser
if command -v open >/dev/null 2>&1; then
  open http://localhost:3000
fi
