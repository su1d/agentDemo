# -*- coding: utf-8 -*-
"""LangGraph Agent - 多智能体基类，支持按角色创建不同专长的 Agent"""
import os, re, json
from typing import Dict, Any, List, Optional
from dataclasses import dataclass, field
from copy import deepcopy
from langgraph.graph import Graph, END
from langchain_openai import ChatOpenAI

OPENAI_API_KEY = os.environ.get("OPENAI_API_KEY", "")
OPENAI_MODEL = os.environ.get("OPENAI_MODEL", "gpt-4o-mini")

# ========== 工具函数 ==========

def calculator(expr):
    try:
        allowed = {"abs": abs, "round": round, "min": min, "max": max, "sum": sum, "pow": pow, "int": int, "float": float}
        safe_dict = {"__builtins__": {}}
        result = eval(expr, safe_dict, allowed)
        return "结果: " + str(result)
    except Exception as e:
        return "错误: " + str(e)

def get_weather(city):
    data = {"Beijing": "晴, 25°C", "Shanghai": "多云, 28°C", "Shenzhen": "小雨, 30°C"}
    return data.get(city, "没有" + city + "的天气数据")

def search_info(topic):
    """模拟搜索功能"""
    data = {
        "python": "Python 是一种高级编程语言，由 Guido van Rossum 创建。",
        "langgraph": "LangGraph 是 LangChain 的图框架，用于构建多步 Agent 工作流。",
    }
    for k, v in data.items():
        if k.lower() in topic.lower():
            return v
    return "搜索到关于 '" + topic + "' 的信息（模拟结果）"

def summarize(text):
    """模拟总结功能"""
    if len(text) > 50:
        return "总结: " + text[:50] + "…"
    return "总结: " + text

# ========== 角色定义 ==========

ROLE_CONFIGS = {
    "assistant": {
        "name": "通用助手",
        "description": "通用对话助手",
        "tools": {},
        "system_prompt": "你是一个有帮助的通用AI助手。请始终用中文回复。",
    },
    "calculator": {
        "name": "计算专家",
        "description": "擅长数学计算",
        "tools": {"calculator": {"func": calculator, "desc": "计算数学表达式"}},
        "system_prompt": "你是计算专家。可用工具：\n- calculator: 计算数学表达式\n请按格式：TOOL: calculator | 表达式，或 ANSWER: 回答。始终用中文。",
    },
    "weather": {
        "name": "天气专家",
        "description": "查询城市天气",
        "tools": {"get_weather": {"func": get_weather, "desc": "查询城市天气"}},
        "system_prompt": "你是天气专家。可用工具：\n- get_weather: 查询城市天气\n请按格式：TOOL: get_weather | 城市名，或 ANSWER: 回答。始终用中文。",
    },
    "searcher": {
        "name": "搜索专家",
        "description": "信息搜索和查找",
        "tools": {"search_info": {"func": search_info, "desc": "搜索信息"}},
        "system_prompt": "你是搜索专家。可用工具：\n- search_info: 搜索信息\n请按格式：TOOL: search_info | 关键词，或 ANSWER: 回答。始终用中文。",
    },
    "summarizer": {
        "name": "总结专家",
        "description": "总结和归纳信息",
        "tools": {"summarize": {"func": summarize, "desc": "总结文本"}},
        "system_prompt": "你是总结专家。可用工具：\n- summarize: 总结文本\n请按格式：TOOL: summarize | 文本，或 ANSWER: 回答。始终用中文。",
    },
}

TOOL_LIST = []
for role_name, cfg in ROLE_CONFIGS.items():
    for tname, tinfo in cfg["tools"].items():
        TOOL_LIST.append("- " + tname + ": " + tinfo["desc"])
TOOL_DESCS = "\n".join(TOOL_LIST)

# ========== Agent State ==========

@dataclass
class AgentState:
    messages: List[Dict[str, str]] = field(default_factory=list)
    next_action: str = ""
    tool_name: str = ""
    tool_input: str = ""
    tool_result: str = ""
    final_answer: str = ""
    iteration: int = 0

def state_to_dict(state):
    return {"messages": deepcopy(state.messages), "next_action": state.next_action,
            "tool_name": state.tool_name, "tool_input": state.tool_input,
            "tool_result": state.tool_result, "final_answer": state.final_answer,
            "iteration": state.iteration}

def dict_to_state(d):
    if isinstance(d, AgentState): return d
    return AgentState(messages=d.get("messages", []), next_action=d.get("next_action", ""),
                      tool_name=d.get("tool_name", ""), tool_input=d.get("tool_input", ""),
                      tool_result=d.get("tool_result", ""), final_answer=d.get("final_answer", ""),
                      iteration=d.get("iteration", 0))

# ========== LLM ==========

def create_llm():
    if not OPENAI_API_KEY:
        return None
    return ChatOpenAI(model=OPENAI_MODEL, temperature=0, api_key=OPENAI_API_KEY)

# ========== Mock 回复 ==========

def _mock(user_input, iteration, tools_dict):
    if iteration >= 3:
        return "FINAL: 推理结束（模拟模式）"
    prompt = user_input.lower()
    for name, tinfo in tools_dict.items():
        desc = tinfo["desc"].lower()
        if any(kw in prompt for kw in desc.split()):
            sample_input = user_input
            for kw in ["计算", "加", "减", "乘", "除", "天气", "搜索", "总结"]:
                if kw in prompt:
                    break
            result = tinfo["func"](sample_input)
            return "TOOL_CALL: " + name + " | " + sample_input + " -> " + result
    return "ANSWER: 您好！我是 " + (list(tools_dict.values())[0]["desc"] if tools_dict else "通用助手") + "。请问有什么可以帮助您的？"

# ========== Graph Node 函数 ==========

def node_input(state_dict):
    s = dict_to_state(state_dict)
    return state_to_dict(s)

def node_llm_reason(state_dict):
    s = dict_to_state(state_dict)
    user_msg = s.messages[-1]["content"] if s.messages else ""
    tools_dict = {}
    # will be set by build_agent_graph via closure
    return state_to_dict(s)

def node_use_tool(state_dict):
    s = dict_to_state(state_dict)
    return state_to_dict(s)

def node_output(state_dict):
    s = dict_to_state(state_dict)
    return state_to_dict(s)

# ========== 构建 Agent Graph ==========

def build_agent_graph(config):
    tools_dict = config["tools"]

    def node_llm_reason_with_tools(sd):
        s = dict_to_state(sd)
        user_msg = s.messages[-1]["content"] if s.messages else ""
        llm = create_llm()

        if llm:
            system_prompt = config["system_prompt"]
            msgs = [{"role": "system", "content": system_prompt},
                    {"role": "user", "content": user_msg}]
            response = llm.invoke(msgs)
            content = response.content.strip()

            tool_call_pattern = r"TOOL:\s*(\w+)\s*\|\s*(.+)"
            match = re.search(tool_call_pattern, content, re.DOTALL)
            if match:
                s.next_action = "use_tool"
                s.tool_name = match.group(1).strip()
                s.tool_input = match.group(2).strip()
            else:
                s.next_action = "answer"
                s.final_answer = re.sub(r"^ANSWER:\s*", "", content)
        else:
            result = _mock(user_msg, s.iteration, tools_dict)
            if result.startswith("TOOL_CALL:"):
                parts = result[len("TOOL_CALL:"):].split("->", 1)
                call_part = parts[0].strip()
                call_parts = call_part.split("|", 1)
                s.tool_name = call_parts[0].strip()
                s.tool_input = call_parts[1].strip() if len(call_parts) > 1 else ""
                s.next_action = "use_tool"
                if len(parts) > 1:
                    s.tool_result = parts[1].strip()
            else:
                s.next_action = "answer"
                s.final_answer = result.replace("ANSWER: ", "")

        s.iteration += 1
        return state_to_dict(s)

    def node_use_tool_with_tools(sd):
        s = dict_to_state(sd)
        tool_func = tools_dict.get(s.tool_name, {}).get("func")
        if tool_func:
            try:
                result = tool_func(s.tool_input)
                s.tool_result = str(result)
            except Exception as e:
                s.tool_result = "工具调用失败: " + str(e)
        else:
            s.tool_result = "未知工具: " + s.tool_name
        s.messages.append({"role": "assistant", "content": s.tool_result})
        return state_to_dict(s)

    def node_output_with_tools(sd):
        s = dict_to_state(sd)
        s.final_answer = s.final_answer or s.tool_result or "无法生成回答"
        return state_to_dict(s)

    def route_after_llm(sd):
        s = dict_to_state(sd)
        if s.next_action == "use_tool":
            return "use_tool"
        return "output"

    def route_after_tool(sd):
        s = dict_to_state(sd)
        if s.iteration >= 3:
            s.final_answer = "已达到最大调用次数。最后结果: " + s.tool_result
            return "output"
        return "llm_reason"

    w = Graph()
    w.add_node("input", node_input)
    w.add_node("llm_reason", node_llm_reason_with_tools)
    w.add_node("use_tool", node_use_tool_with_tools)
    w.add_node("output", node_output_with_tools)
    w.set_entry_point("input")
    w.add_edge("input", "llm_reason")
    w.add_conditional_edges("llm_reason", route_after_llm,
                            {"use_tool": "use_tool", "output": "output"})
    w.add_conditional_edges("use_tool", route_after_tool,
                            {"llm_reason": "llm_reason", "output": "output"})
    w.set_finish_point("output")
    return w.compile()

# ========== Agent 工厂 ==========

class LangGraphAgent:
    """通用 Agent，根据 role 创建不同专长"""
    def __init__(self, role="assistant"):
        self.role = role
        self.config = ROLE_CONFIGS.get(role, ROLE_CONFIGS["assistant"])
        self.graph = build_agent_graph(self.config)
        self.has_key = bool(OPENAI_API_KEY)

    def get_info(self):
        return {"role": self.role, "name": self.config["name"],
                "description": self.config["description"],
                "tools": list(self.config["tools"].keys()),
                "has_api_key": self.has_key}

    def run(self, user_input):
        init = state_to_dict(AgentState(
            messages=[{"role": "user", "content": user_input}]
        ))
        result = self.graph.invoke(init)
        return dict_to_state(result).final_answer


# ========== 编排 Agent ==========

class OrchestratorAgent:
    """
    编排器：接收用户问题 → 判断用哪个 Agent → 调用 → 返回结果
    支持多种协作模式：自动路由、链式编排、并行、辩论、批评链、投票共识、头脑风暴
    """
    def __init__(self):
        self.agents = {}
        for role in ROLE_CONFIGS:
            self.agents[role] = LangGraphAgent(role)
        self.llm = create_llm()

    def list_agents(self):
        return {role: agent.get_info() for role, agent in self.agents.items()}

    def route(self, user_input):
        """判断应该用哪个 Agent"""
        if not self.llm:
            return self._mock_route(user_input)
        prompt = (
            "你是一个任务路由助手。根据用户输入，选择最合适的 Agent。可选角色：\n"
            + "\n".join([f"- {r}: {c['description']}" for r, c in ROLE_CONFIGS.items()])
            + "\n请只回复角色名称，不要回复其他内容。"
        )
        msgs = [{"role": "system", "content": prompt},
                {"role": "user", "content": user_input}]
        content = self.llm.invoke(msgs).content.strip().lower()
        return content if content in ROLE_CONFIGS else "assistant"

    def _mock_route(self, user_input):
        u = user_input.lower()
        if any(op in u for op in ["+", "-", "*", "/", "计算", "等于"]):
            return "calculator"
        if "天气" in u or "温度" in u or "weather" in u:
            return "weather"
        if "搜索" in u or "查找" in u or "什么是" in u or "介绍" in u:
            return "searcher"
        if "总结" in u or "归纳" in u:
            return "summarizer"
        return "assistant"

    def chat(self, user_input):
        """路由 + 执行"""
        role = self.route(user_input)
        result = self.agents[role].run(user_input)
        return result

    def chat_with_all(self, user_input):
        """让所有 Agent 回复（用于对比展示）"""
        results = {}
        for role, agent in self.agents.items():
            results[role] = {"name": ROLE_CONFIGS[role]["name"], "reply": agent.run(user_input)}
        return results

    def orchestrate(self, user_input, roles=None):
        """
        链式编排模式：指定 Agent 链，前一个输出作为下一个输入
        roles: ["calculator", "summarizer"] 等
        """
        if not roles:
            roles = list(self.agents.keys())
        current_input = user_input
        chain_results = []
        for role in roles:
            reply = self.agents[role].run(current_input)
            chain_results.append({"role": role, "name": ROLE_CONFIGS[role]["name"], "reply": reply})
            current_input = reply
        return chain_results

    # ========== 高级协作模式 ==========

    def parallel_call(self, user_input, roles=None):
        """
        并行模式：多个 Agent 同时处理同一问题，收集所有结果后汇总
        roles: ["calculator", "searcher"] 等
        """
        if not roles:
            roles = list(self.agents.keys())
        from concurrent.futures import ThreadPoolExecutor, as_completed
        results = {}
        with ThreadPoolExecutor(max_workers=len(roles)) as executor:
            future_map = {executor.submit(self.agents[r].run, user_input): r for r in roles}
            for future in as_completed(future_map):
                role = future_map[future]
                try:
                    reply = future.result()
                except Exception as e:
                    reply = f"错误: {e}"
                results[role] = {"name": ROLE_CONFIGS[role]["name"], "reply": reply}

        summary_input = f"原始问题: {user_input}\n\n各Agent回复:\n"
        for role, data in results.items():
            summary_input += f"\n[{data['name']}]: {data['reply']}\n"
        summary_input += "\n请整合以上所有回复，给出一个统一的综合答案。"
        summary = self.agents["summarizer"].run(summary_input)
        return {
            "mode": "parallel",
            "individual_results": results,
            "summary": summary
        }

    def debate(self, user_input, roles=None, rounds=2):
        """
        辩论模式：多个 Agent 轮流发表观点、互相反驳，最后由总结 Agent 给出最终结论
        roles: 参与辩论的 Agent 角色列表（至少2个）
        rounds: 辩论轮数
        """
        if not roles or len(roles) < 2:
            roles = ["searcher", "calculator"]
        round_history = []
        for rnd in range(rounds):
            round_results = {}
            for role in roles:
                if rnd == 0:
                    prompt = f"请从你专业的角度分析以下问题，给出你的观点:\n{user_input}"
                else:
                    others_view = "\n".join(
                        [f"{ROLE_CONFIGS[r]['name']}: {round_results.get(r, {}).get('reply', '')}"
                         for r in roles if r != role]
                    )
                    prompt = (f"问题: {user_input}\n\n"
                              f"其他专家的观点:\n{others_view}\n\n"
                              f"请基于以上信息，进一步阐述或反驳，完善你的观点。")
                reply = self.agents[role].run(prompt)
                round_results[role] = {"name": ROLE_CONFIGS[role]["name"], "reply": reply}
            round_history.append({"round": rnd + 1, "results": round_results})

        debate_summary = f"问题: {user_input}\n\n辩论记录:\n"
        for rnd_data in round_history:
            debate_summary += f"\n--- 第{rnd_data['round']}轮 ---\n"
            for role, data in rnd_data["results"].items():
                debate_summary += f"[{data['name']}]: {data['reply']}\n"
        debate_summary += "\n请综合所有辩论内容，给出最终结论。"
        conclusion = self.agents["summarizer"].run(debate_summary)
        return {
            "mode": "debate",
            "rounds": round_history,
            "conclusion": conclusion
        }

    def critique_chain(self, user_input, generate_role="searcher", critique_role="summarizer", refine_rounds=2):
        """
        批评链模式：Agent A 生成 → Agent B 批评 → Agent A 改进（可多轮）
        generate_role: 负责生成的 Agent
        critique_role: 负责批评的 Agent
        refine_rounds: 改进轮数
        """
        generate_agent = self.agents[generate_role]
        critique_agent = self.agents[critique_role]
        gen_name = ROLE_CONFIGS[generate_role]["name"]
        crit_name = ROLE_CONFIGS[critique_role]["name"]

        current_output = generate_agent.run(f"请针对以下问题进行详细分析:\n{user_input}")
        history = [{"step": "生成", "role": generate_role, "name": gen_name, "content": current_output}]

        for rnd in range(refine_rounds):
            critique_prompt = (f"原始问题: {user_input}\n\n"
                               f"生成的回答:\n{current_output}\n\n"
                               f"请批评以上回答，指出不足、遗漏或错误之处，"
                               f"并给出具体的改进建议。")
            critique = critique_agent.run(critique_prompt)
            history.append({"step": f"批评#{rnd+1}", "role": critique_role, "name": crit_name, "content": critique})

            refine_prompt = (f"原始问题: {user_input}\n\n"
                             f"你之前的回答:\n{current_output}\n\n"
                             f"收到的批评意见:\n{critique}\n\n"
                             f"请根据批评意见改进你的回答，输出最终版本。")
            current_output = generate_agent.run(refine_prompt)
            history.append({"step": f"改进#{rnd+1}", "role": generate_role, "name": gen_name, "content": current_output})

        return {
            "mode": "critique_chain",
            "history": history,
            "final_answer": current_output
        }

    def voting_consensus(self, user_input, roles=None):
        """
        投票/共识模式：多个 Agent 各自产生方案，然后汇总评估选出最佳
        """
        if not roles:
            roles = [r for r in self.agents.keys() if r != "summarizer"]
        proposals = {}
        for role in roles:
            reply = self.agents[role].run(
                f"请针对以下问题提出你的解决方案:\n{user_input}"
            )
            proposals[role] = {"name": ROLE_CONFIGS[role]["name"], "proposal": reply}

        eval_input = f"问题: {user_input}\n\n各Agent方案:\n"
        for role, data in proposals.items():
            eval_input += f"\n--- [{data['name']}] ---\n{data['proposal']}\n"
        eval_input += ("\n请评估以上所有方案，从准确性、完整性、实用性等角度打分（1-10分），"
                       "选出最佳方案，并说明理由。")
        evaluation = self.agents["summarizer"].run(eval_input)

        return {
            "mode": "voting_consensus",
            "proposals": proposals,
            "evaluation": evaluation
        }

    def brainstorm(self, user_input, roles=None):
        """
        头脑风暴模式：所有 Agent 自由发挥，然后归类整理
        """
        if not roles:
            roles = list(self.agents.keys())
        ideas = {}
        for role in roles:
            reply = self.agents[role].run(
                f"请针对以下主题进行头脑风暴，从你的专业角度提出尽可能多的想法、创意或建议:\n{user_input}"
            )
            ideas[role] = {"name": ROLE_CONFIGS[role]["name"], "ideas": reply}

        organize_input = "头脑风暴主题: " + user_input + "\n\n收集到的想法:\n"
        for role, data in ideas.items():
            organize_input += f"\n--- [{data['name']}] ---\n{data['ideas']}\n"
        organize_input += "\n请将以上所有想法进行归类、整理，去重合并，形成一个结构化的头脑风暴报告。"
        organized = self.agents["summarizer"].run(organize_input)

        return {
            "mode": "brainstorm",
            "ideas": ideas,
            "organized_report": organized
        }

if __name__ == "__main__":
    pass
