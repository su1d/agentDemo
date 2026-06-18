import sys, os, json, io

# Force UTF-8 encoding for stdout/stderr
if sys.stdout.encoding != "utf-8":
    sys.stdout = io.TextIOWrapper(sys.stdout.buffer, encoding="utf-8")
    sys.stderr = io.TextIOWrapper(sys.stderr.buffer, encoding="utf-8")


def safe_json(obj):
    """JSON 序列化时避免 surrogate 字符导致错误"""
    return json.dumps(obj, ensure_ascii=False, default=str)


sys.path.insert(0, "work")
from agent import OrchestratorAgent, ROLE_CONFIGS

WORKER_ROLE = os.environ.get("WORKER_ROLE", "orchestrator")

if WORKER_ROLE == "orchestrator":
    agent = OrchestratorAgent()
    model_name = "orchestrator"
else:
    from agent import LangGraphAgent
    import contextlib

    class SilentLangGraphAgent(LangGraphAgent):
        def run(self, user_input):
            f = io.StringIO()
            with contextlib.redirect_stdout(f), contextlib.redirect_stderr(f):
                return super().run(user_input)

    agent = SilentLangGraphAgent(WORKER_ROLE)
    model_name = WORKER_ROLE

# Signal ready
print(safe_json({"type": "ready", "model": model_name}))
sys.stdout.flush()

# Process requests
for line in sys.stdin:
    line = line.strip()
    if not line:
        continue
    try:
        data = json.loads(line)
        msg = data.get("message", "").strip()
        action = data.get("action", "chat")
        if action == "orchestrate":
            roles = data.get("roles", None)
            result = agent.orchestrate(msg, roles)
            print(safe_json({"type": "reply", "content": safe_json(result)}))
        elif action == "list_agents":
            result = agent.list_agents()
            print(safe_json({"type": "reply", "content": safe_json(result)}))
        elif action == "chat_all":
            result = agent.chat_with_all(msg)
            print(safe_json({"type": "reply", "content": safe_json(result)}))
        else:
            if msg:
                reply = agent.chat(msg)
                print(safe_json({"type": "reply", "content": reply}))
            else:
                print(safe_json({"type": "error", "content": "Empty message"}))
    except json.JSONDecodeError as e:
        print(safe_json({"type": "error", "content": "Invalid JSON: " + str(e)}))
    except Exception as e:
        print(safe_json({"type": "error", "content": str(e)}))
    sys.stdout.flush()
