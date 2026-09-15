import pytest
from httpx import AsyncClient, ASGITransport
from app.main import app


@pytest.mark.asyncio
async def test_list_models():
    transport = ASGITransport(app=app)
    async with AsyncClient(transport=transport, base_url="http://test") as ac:
        res = await ac.get("/api/v1/models")
    assert res.status_code == 200
    models = res.json()
    assert isinstance(models, list)
    assert len(models) >= 5
    model_names = [m["model_name"] for m in models]
    assert "confidence_model" in model_names
    assert "operator_model" in model_names


@pytest.mark.asyncio
async def test_get_model_versions_and_promote():
    transport = ASGITransport(app=app)
    async with AsyncClient(transport=transport, base_url="http://test") as ac:
        # Get versions
        res = await ac.get("/api/v1/models/confidence_model/versions")
        assert res.status_code == 200
        versions = res.json()
        assert len(versions) >= 1

        # Promote version 1
        promote_res = await ac.post("/api/v1/models/confidence_model/1/promote")
        assert promote_res.status_code == 200
        assert promote_res.json()["status"] == "promoted"

        # Rollback version 1
        rollback_res = await ac.post("/api/v1/models/confidence_model/1/rollback")
        assert rollback_res.status_code == 200
        assert rollback_res.json()["status"] in ["shadow", "trained"]


@pytest.mark.asyncio
async def test_shadow_stats():
    transport = ASGITransport(app=app)
    async with AsyncClient(transport=transport, base_url="http://test") as ac:
        res = await ac.get("/api/v1/models/confidence_model/shadow-stats")
    assert res.status_code == 200
    stats = res.json()
    assert "agreement_rate" in stats
    assert "total_predictions" in stats


@pytest.mark.asyncio
async def test_retrain_check():
    transport = ASGITransport(app=app)
    async with AsyncClient(transport=transport, base_url="http://test") as ac:
        res = await ac.get("/api/v1/training/retrain-check")
    assert res.status_code == 200
    checks = res.json()
    assert isinstance(checks, list)
    assert len(checks) >= 5


@pytest.mark.asyncio
async def test_trigger_train_model():
    transport = ASGITransport(app=app)
    async with AsyncClient(transport=transport, base_url="http://test") as ac:
        res = await ac.post("/api/v1/training/train/operator_model")
    assert res.status_code == 200
    data = res.json()
    assert data["status"] == "trained"
    assert data["version"] >= 1
    assert "metrics" in data
