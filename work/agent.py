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
    if iteration >= 1:
        return "ANSWER: 结果已处理完成。（模拟回复）"
    # 尝试匹配工具
    for tname, tinfo in tools_dict.items():
        if tname == "calculator":
            if any(op in user_input for op in ["+", "-", "*", "/", "calc", "计算"]):
                nums = re.findall(r"[\d+\-*/().]", user_input)
                expr = "".join(nums).strip("+-*/.")
                if expr:
                    return "TOOL: calculator | " + expr
        if tname == "get_weather":
            if "上海" in user_input or "shanghai" in user_input:
                return "TOOL: get_weather | Shanghai"
        if tname == "search_info":
            if "搜索" in user_input or "查找" in user_input or "什么是" in user_input:
                keyword = user_input.replace("搜索", "").replace("查找", "").replace("什么是", "").strip()
                if keyword:
                    return "TOOL: search_info | " + keyword
        if tname == "summarize":
            if "总结" in user_input or "归纳" in user_input:
                return "TOOL: summarize | " + user_input
    return "ANSWER: 关于 " + user_input + "，好问题！（模拟回复）"

# ========== 构建 Agent 图 ==========

def build_agent_graph(role_config):
    """根据角色配置构建 LangGraph"""
    tools_dict = role_config["tools"]
    system_prompt = role_config["system_prompt"]

    def node_input(sd):
        s = dict_to_state(sd)
        msg = s.messages[-1]["content"]
        # print suppressed
        s.iteration = 0
        return state_to_dict(s)

    def node_llm_reason(sd):
        s = dict_to_state(sd)
        llm = create_llm()
        msgs = [{"role": "system", "content": system_prompt}]
        for m in s.messages:
            msgs.append(m)
        if s.tool_result:
            msgs.append({"role": "system", "content": "工具 " + s.tool_name + " 返回: " + s.tool_result})
            s.tool_result = ""
            s.tool_name = ""
        # print suppressed
        user_input = s.messages[-1]["content"]
        if llm:
            content = llm.invoke(msgs).content.strip()
        else:
            content = _mock(user_input, s.iteration, tools_dict)
        pass # print suppressed
        u = content.upper()
        if u.startswith("TOOL:"):
            s.next_action = "use_tool"
            rest = content[5:].strip()
            if "|" in rest:
                parts = rest.split("|", 1)
                s.tool_name = parts[0].strip()
                s.tool_input = parts[1].strip()
            else:
                parts = rest.split(None, 1)
                s.tool_name = parts[0] if parts else ""
                s.tool_input = parts[1] if len(parts) > 1 else ""
        elif u.startswith("FINAL:"):
            s.next_action = "end"
            s.final_answer = content[6:].strip()
        else:
            if u.startswith("ANSWER:"):
                ans = content[7:].strip()
            else:
                ans = content
            s.messages.append({"role": "assistant", "content": ans})
            s.next_action = "end"
            s.final_answer = ans
        return state_to_dict(s)

    def node_use_tool(sd):
        s = dict_to_state(sd)
        # print suppressed
        func = tools_dict.get(s.tool_name, {}).get("func", lambda x: "未知工具: " + x)
        result = func(s.tool_input)
        # print suppressed
        s.tool_result = result
        s.iteration += 1
        return state_to_dict(s)

    def node_output(sd):
        s = dict_to_state(sd)
        # print suppressed
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
    w.add_node("llm_reason", node_llm_reason)
    w.add_node("use_tool", node_use_tool)
    w.add_node("output", node_output)
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
    """
    def __init__(self):
        self.agents = {}
        # 创建所有角色的 Agent 实例
        for role in ROLE_CONFIGS:
            self.agents[role] = LangGraphAgent(role)
        # 编排器本身的 LLM（用来做路由判断）
        self.llm = create_llm()

    def list_agents(self):
        return {role: agent.get_info() for role, agent in self.agents.items()}

    def route(self, user_input):
        """判断应该用哪个 Agent"""
        if not self.llm:
            # 模拟模式：关键词匹配
            return self._mock_route(user_input)
        # 真实模式：让 LLM 判断
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
            if "上海" in u or "shanghai" in u:
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
        编排模式：指定 Agent 链，前一个输出作为下一个输入
        roles: ["calculator", "summarizer"] 等
        """
        if not roles:
            roles = list(self.agents.keys())
        current_input = user_input
        chain_results = []
        for role in roles:
            pass # print suppressed
            reply = self.agents[role].run(current_input)
            chain_results.append({"role": role, "name": ROLE_CONFIGS[role]["name"], "reply": reply})
            current_input = reply
        return chain_results

if __name__ == "__main__":
    # 服务器模式 - 不运行交互测试
    pass
