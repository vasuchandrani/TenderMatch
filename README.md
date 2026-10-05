# TenderMatch 🚀
**AI-Powered Tender Evaluation & Vendor Matching Platform**

TenderMatch is an end-to-end intelligent procurement platform designed to streamline and automate tender evaluations. It extracts technical specifications from tender and vendor bid documents using computer vision and NLP, normalizes and compares them using semantic embeddings and ML classifiers, and provides procurement officers with an interactive human-in-the-loop review and scoring interface.

---

## 🏛️ System Architecture

The platform is designed as a distributed multi-service architecture:

| Service | Technology | Port | Description |
| :--- | :--- | :--- | :--- |
| **Frontend Web App** | React 18, Vite, Lucide Icons | `3000` | Human-in-the-loop review UI, comparison matrix, scoring leaderboard, and model registry dashboard |
| **Backend API** | Spring Boot 3, Java 17, Spring Security | `8080` | REST API, JWT authentication, business logic, PostgreSQL & MinIO orchestration |
| **AI Microservice** | FastAPI, Python 3.11, PyTorch, Sentence-Transformers | `8000` | Document processing, OCR, NLP extraction, semantic matching engine, ML classification |
| **Relational Database** | PostgreSQL + `pgvector` | `5432` / `5433` | Relational data persistence and vector similarity search |
| **Object Storage** | MinIO (S3-compatible) | `9000` / `9001` | Secure PDF tender and bid document storage |
| **LLM Provider** | Google Gemini / Groq / Ollama | `11434` (local) | Multi-model LLM inference for reasoning and specification extraction |

---

## 📁 Repository Structure

```
TenderMatch/
├── tendermatch-frontend/     # React + Vite web application
├── tendermatch-backend/      # Spring Boot 3 Java backend service
├── tendermatch-ai/           # FastAPI Python AI/ML microservice
├── scripts/                  # Integration test & diagnostic utilities
├── docker-compose.yml        # Docker service definitions (Postgres & MinIO)
├── run_project.sh            # Master unified startup script
├── stop_project.sh           # Master shutdown script
├── .env.example              # Sample environment configuration template
└── .gitignore                # Git ignore configuration
```

---

## 🛠️ Prerequisites

Ensure the following tools are installed on your machine:
- **Docker & Docker Desktop** (for PostgreSQL and MinIO)
- **Java 17+** and **Maven** (for Spring Boot backend)
- **Python 3.11+** with `venv` (for AI microservice)
- **Node.js 18+** and `npm` (for frontend)
- *(Optional)* **Ollama** (if running local LLMs like `llama3.1:8b`)

---

## 🚀 Getting Started

### 1. Environment Configuration
Copy the template environment file and fill in your API credentials:
```bash
cp .env.example .env
```
Key settings to configure in `.env`:
- `GEMINI_API_KEY`: Your Google AI Studio API key (free tier supported)
- `JWT_SECRET`: 256-bit secret key for HMAC authentication

### 2. Quick Start (All Services)
Launch the entire stack using the master startup script:
```bash
chmod +x run_project.sh stop_project.sh
./run_project.sh
```

The script will automatically:
1. Start PostgreSQL and MinIO containers via Docker Compose.
2. Initialize and verify the Ollama / Gemini LLM service.
3. Start the FastAPI AI service on port `8000`.
4. Launch the Spring Boot backend on port `8080`.
5. Start the React frontend on port `3000` and open it in your browser.

### 3. Stopping the Platform
To cleanly stop all background processes and containers:
```bash
./stop_project.sh
```

---

## 🧪 Testing & Verification

Each service includes a complete automated test suite:

- **AI Microservice**:
  ```bash
  cd tendermatch-ai
  source venv/bin/activate
  pytest tests
  ```
- **Backend Service**:
  ```bash
  cd tendermatch-backend
  mvn test
  ```
- **Frontend App**:
  ```bash
  cd tendermatch-frontend
  npm test -- --run
  npm run build
  ```

---

## 📄 License
This project is licensed under the MIT License.
