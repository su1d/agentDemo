import sys, os, json, io

if sys.stdout.encoding != "utf-8":
    sys.stdout = io.TextIOWrapper(sys.stdout.buffer, encoding="utf-8")
    sys.stderr = io.TextIOWrapper(sys.stderr.buffer, encoding="utf-8")

def safe_json(obj):
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

        elif action == "parallel":
            roles = data.get("roles", None)
            result = agent.parallel_call(msg, roles)
            print(safe_json({"type": "reply", "content": safe_json(result)}))

        elif action == "debate":
            roles = data.get("roles", None)
            rounds = data.get("rounds", 2)
            result = agent.debate(msg, roles, rounds)
            print(safe_json({"type": "reply", "content": safe_json(result)}))

        elif action == "critique_chain":
            generate_role = data.get("generate_role", "searcher")
            critique_role = data.get("critique_role", "summarizer")
            refine_rounds = data.get("refine_rounds", 2)
            result = agent.critique_chain(msg, generate_role, critique_role, refine_rounds)
            print(safe_json({"type": "reply", "content": safe_json(result)}))

        elif action == "voting_consensus":
            roles = data.get("roles", None)
            result = agent.voting_consensus(msg, roles)
            print(safe_json({"type": "reply", "content": safe_json(result)}))

        elif action == "brainstorm":
            roles = data.get("roles", None)
            result = agent.brainstorm(msg, roles)
            print(safe_json({"type": "reply", "content": safe_json(result)}))

        elif action == "list_modes":
            modes = [
                "chat (auto-route)",
                "orchestrate (chain)",
                "parallel (parallel-summarize)",
                "debate (multi-round debate)",
                "critique_chain (generate-criticize-refine)",
                "voting_consensus (compete-evaluate)",
                "brainstorm (divergent-converge)",
                "chat_all (compare all)",
                "list_agents"
            ]
            print(safe_json({"type": "reply", "content": safe_json({"modes": modes})}))

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
