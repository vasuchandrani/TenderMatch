from pydantic_settings import BaseSettings, SettingsConfigDict


class Settings(BaseSettings):
    # Service
    SERVICE_NAME: str = "tendermatch-ai"
    DEBUG: bool = False

    # Database
    DATABASE_URL: str = "postgresql+asyncpg://tendermatch:tendermatch_secret@localhost:5432/tendermatch"

    # MinIO
    MINIO_ENDPOINT: str = "localhost:9000"
    MINIO_ACCESS_KEY: str = "minioadmin"
    MINIO_SECRET_KEY: str = "minioadmin"
    MINIO_BUCKET_NAME: str = "tender-pdfs"
    MINIO_SECURE: bool = False

    # Google Gemini Cloud LLM (Free via Google AI Studio)
    GEMINI_API_KEY: str = ""
    GEMINI_PRIMARY_MODEL: str = "gemini-3.5-flash-lite"
    GEMINI_FALLBACK_MODEL: str = "gemini-3.5-flash"

    # Groq Cloud LLM
    GROQ_API_KEY: str = ""
    GROQ_API_BASE: str = "https://api.groq.com/openai/v1"
    GROQ_PRIMARY_MODEL: str = "llama-3.3-70b-versatile"
    GROQ_FALLBACK_MODEL: str = "llama-3.1-8b-instant"

    # Ollama Local Fallback
    OLLAMA_API_BASE: str = "http://localhost:11434"
    PRIMARY_LLM_MODEL: str = "llama3.1:8b"
    FALLBACK_LLM_MODEL: str = "llama3.1:8b"


    # Embedding & Cross-Encoder Models
    EMBEDDING_MODEL_NAME: str = "sentence-transformers/all-MiniLM-L6-v2"
    CROSS_ENCODER_MODEL_NAME: str = "cross-encoder/ms-marco-MiniLM-L-6-v2"

    model_config = SettingsConfigDict(env_file=("../.env", ".env"), extra="ignore")


settings = Settings()

