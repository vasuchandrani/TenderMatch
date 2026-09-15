import pytest
from httpx import AsyncClient, ASGITransport
from app.main import app


@pytest.mark.asyncio
async def test_health_endpoint_returns_200():
    transport = ASGITransport(app=app)
    async with AsyncClient(transport=transport, base_url="http://test") as ac:
        response = await ac.get("/health")
    assert response.status_code == 200
    data = response.json()
    assert data["status"] == "healthy"
    assert "service" in data
    assert "ollama_endpoint" in data


@pytest.mark.asyncio
async def test_process_document_stub_returns_stub_response():
    transport = ASGITransport(app=app)
    async with AsyncClient(transport=transport, base_url="http://test") as ac:
        payload = {
            "minio_file_key": "requirements/123/tender.pdf",
            "document_type_hint": "requirement",
            "tender_id": "123",
        }
        response = await ac.post("/api/v1/process-document", json=payload)
    assert response.status_code == 200
    data = response.json()
    assert data["status"] in ["SUCCESS", "STUB_PROCESSED"]
    assert data["minio_file_key"] == "requirements/123/tender.pdf"
    assert data["document_type"] == "requirement"
    assert "message" in data


@pytest.mark.asyncio
async def test_process_document_validates_input():
    transport = ASGITransport(app=app)
    async with AsyncClient(transport=transport, base_url="http://test") as ac:
        # Missing required field `minio_file_key`
        response = await ac.post("/api/v1/process-document", json={})
    assert response.status_code == 422
