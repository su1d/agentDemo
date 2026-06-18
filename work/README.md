# LangGraph Agent - Full Stack

AI agent built with **LangGraph** (Python), **Java 21** (Spring Boot backend), and **Vue 3** (frontend).

## Architecture

```
User Browser (Vue 3, port 5173)
    |  HTTP REST
    v
Java Spring Boot Backend (port 8080)
    |  stdin/stdout JSON protocol
    v
Python Agent Worker (LangGraph)
```

## Project Structure

```
??? backend/                    # Java 21 Spring Boot
?   ??? pom.xml
?   ??? src/main/java/com/agent/
?       ??? AgentApplication.java
?       ??? controller/AgentController.java   # REST API
?       ??? service/AgentWorkerService.java   # Manages Python subprocess
?       ??? model/ (ChatRequest, ChatResponse, AgentInfo)
??? frontend/                   # Vue 3 + Vite
?   ??? src/App.vue             # Chat UI
?   ??? package.json
??? work/
    ??? agent.py                # LangGraph agent (core logic)
    ??? agent_worker.py         # Python worker process (stdin/stdout protocol)
    ??? demo.py                 # Demo script
    ??? README.md               # Agent details
```

## How It Works

1. **Java backend starts** ? spawns Python `agent_worker.py` as a subprocess
2. **Python worker** loads the LangGraph agent and signals "ready"
3. **Frontend sends chat message** ? POST /api/chat ? Java ? stdin ? Python ? LLM/tools ? stdout ? Java ? Frontend

## Quick Start

```powershell
# 1. Set OpenAI API Key (optional, mock mode works without it)
$env:OPENAI_API_KEY = "sk-..."

# 2. Start Java backend (this will auto-start the Python worker)
mvn -f backend/pom.xml spring-boot:run

# 3. In another terminal, start frontend
cd frontend && npm run dev

# 4. Open http://localhost:5173
```

## API Endpoints

| Method | Path | Description |
|--------|------|-------------|
| POST | /api/chat | Send message `{"message": "..."}` |
| GET | /api/info | Agent info |
| GET | /api/health | Health check |
