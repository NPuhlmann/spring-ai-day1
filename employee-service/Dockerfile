# Python 3.11 slim image als Basis
FROM python:3.11-slim

# Arbeitsverzeichnis im Container
WORKDIR /app

# Kopiere requirements.txt und installiere Dependencies
COPY requirements.txt .
RUN pip install --no-cache-dir -r requirements.txt

# Kopiere den Applikations-Code
COPY main.py .

# Exponiere Port 50000
EXPOSE 50000

# Starte Uvicorn Server
CMD ["uvicorn", "main:app", "--host", "0.0.0.0", "--port", "50000"]
