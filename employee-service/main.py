from fastapi import FastAPI, Security, HTTPException, status
from fastapi.security import APIKeyHeader
from typing import List, Dict

app = FastAPI(
    title="Employee Service API",
    description="Ein einfacher Service für Mitarbeiterdaten mit API-Key Authentifizierung",
    version="1.0.0",
)

# API Key Configuration
API_KEY = "workshop-secret-key-2024"
api_key_header = APIKeyHeader(name="X-API-Key", auto_error=False)

# Mitarbeiterdaten
EMPLOYEES = [
    {
        "id": 1,
        "name": "Anna Schmidt",
        "email": "anna.schmidt@example.com",
        "position": "Software Engineer",
        "department": "Engineering",
        "salary": 75000,
        "hire_date": "2020-03-15",
    },
    {
        "id": 2,
        "name": "Max Müller",
        "email": "max.mueller@example.com",
        "position": "Product Manager",
        "department": "Product",
        "salary": 85000,
        "hire_date": "2019-07-22",
    },
    {
        "id": 3,
        "name": "Sarah Weber",
        "email": "sarah.weber@example.com",
        "position": "DevOps Engineer",
        "department": "Engineering",
        "salary": 80000,
        "hire_date": "2021-01-10",
    },
]


async def verify_api_key(api_key: str = Security(api_key_header)):
    """Verifiziert den API-Key aus dem Request-Header."""
    if api_key is None:
        raise HTTPException(
            status_code=status.HTTP_403_FORBIDDEN,
            detail="API-Key fehlt. Bitte X-API-Key Header setzen.",
        )
    if api_key != API_KEY:
        raise HTTPException(
            status_code=status.HTTP_403_FORBIDDEN, detail="Ungültiger API-Key."
        )
    return api_key


@app.get("/")
async def root():
    """Root-Endpoint mit Service-Informationen."""
    return {
        "service": "Employee Service API",
        "version": "1.0.0",
        "endpoints": {
            "/employees": "GET - Liste aller Mitarbeiter (benötigt API-Key)",
            "/employees/{id}": "GET - Einzelner Mitarbeiter per ID (benötigt API-Key)",
        },
    }


@app.get("/employees", response_model=List[Dict])
async def get_employees(api_key: str = Security(verify_api_key)):
    """
    Gibt eine Liste aller Mitarbeiter zurück.

    Benötigt einen gültigen API-Key im X-API-Key Header.
    """
    return EMPLOYEES


@app.get("/employees/{employee_id}", response_model=Dict)
async def get_employee_by_id(employee_id: int, api_key: str = Security(verify_api_key)):
    """
    Gibt die Daten eines einzelnen Mitarbeiters anhand der ID zurück.

    Benötigt einen gültigen API-Key im X-API-Key Header.
    """
    for employee in EMPLOYEES:
        if employee["id"] == employee_id:
            return employee

    raise HTTPException(
        status_code=status.HTTP_404_NOT_FOUND,
        detail=f"Mitarbeiter mit ID {employee_id} nicht gefunden.",
    )


@app.get("/health")
async def health_check():
    """Health-Check Endpoint ohne Authentifizierung."""
    return {"status": "healthy"}
