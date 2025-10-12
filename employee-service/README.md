# Employee Service API

Ein einfacher FastAPI-Service, der Mitarbeiterdaten über eine REST-API bereitstellt. Der Service ist mit API-Key Authentifizierung gesichert und läuft in einem Docker-Container.

## Features

- HTTP GET Endpoint für Mitarbeiterdaten
- API-Key Authentifizierung via Header
- 3 Beispiel-Mitarbeiter mit vollständigen Informationen
- Docker-Container Support
- Port 50000

## API-Key

Der Service verwendet einen festen API-Key für die Authentifizierung:

```
workshop-secret-key-2024
```

Der API-Key muss bei jedem Request im Header `X-API-Key` mitgesendet werden.

## Endpoints

### GET /employees

Gibt eine Liste aller Mitarbeiter zurück.

**Authentifizierung:** Erforderlich

**Beispiel Request:**

```bash
curl -H "X-API-Key: workshop-secret-key-2024" http://localhost:50000/employees
```

**Beispiel Response:**

```json
[
  {
    "id": 1,
    "name": "Anna Schmidt",
    "email": "anna.schmidt@example.com",
    "position": "Software Engineer",
    "department": "Engineering",
    "salary": 75000,
    "hire_date": "2020-03-15"
  },
  {
    "id": 2,
    "name": "Max Müller",
    "email": "max.mueller@example.com",
    "position": "Product Manager",
    "department": "Product",
    "salary": 85000,
    "hire_date": "2019-07-22"
  },
  {
    "id": 3,
    "name": "Sarah Weber",
    "email": "sarah.weber@example.com",
    "position": "DevOps Engineer",
    "department": "Engineering",
    "salary": 80000,
    "hire_date": "2021-01-10"
  }
]
```

### GET /

Service-Informationen (keine Authentifizierung erforderlich)

```bash
curl http://localhost:50000/
```

### GET /health

Health-Check Endpoint (keine Authentifizierung erforderlich)

```bash
curl http://localhost:50000/health
```

## Docker

### Container bauen

```bash
cd employee-service
docker build -t employee-service:latest .
```

### Container starten

```bash
docker run -d -p 50000:50000 --name employee-service employee-service:latest
```

**Optionen:**
- `-d`: Container im Hintergrund laufen lassen
- `-p 50000:50000`: Port-Mapping (Host:Container)
- `--name employee-service`: Name für den Container

### Container stoppen

```bash
docker stop employee-service
```

### Container entfernen

```bash
docker rm employee-service
```

### Logs anzeigen

```bash
docker logs employee-service
```

### Container Status prüfen

```bash
docker ps
```

## Lokale Entwicklung (ohne Docker)

Falls du den Service lokal ohne Docker ausführen möchtest:

### Installation

```bash
cd employee-service
pip install -r requirements.txt
```

### Starten

```bash
uvicorn main:app --host 0.0.0.0 --port 50000
```

### API-Dokumentation

FastAPI generiert automatisch eine interaktive API-Dokumentation:

- Swagger UI: http://localhost:50000/docs
- ReDoc: http://localhost:50000/redoc

## Fehlerbehandlung

### 403 Forbidden - API-Key fehlt

```bash
curl http://localhost:50000/employees
```

Response:
```json
{
  "detail": "API-Key fehlt. Bitte X-API-Key Header setzen."
}
```

### 403 Forbidden - Ungültiger API-Key

```bash
curl -H "X-API-Key: wrong-key" http://localhost:50000/employees
```

Response:
```json
{
  "detail": "Ungültiger API-Key."
}
```

## Integration mit Spring AI Workshop

Dieser Service kann parallel zum Spring AI Workshop Projekt laufen und z.B. als externe Datenquelle für RAG oder Tool-Integration verwendet werden.

**Beispiel:** Der Spring Service könnte den Employee Service als Tool verwenden, um Mitarbeiterdaten abzurufen.
