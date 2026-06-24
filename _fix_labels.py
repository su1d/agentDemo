with open("D:\\pj\\frontend\\src\\components\\IsometricOffice.vue", "r", encoding="utf-8") as f:
    content = f.read()

content = content.replace("Agent 动物办公室", "Agent 生态办公室")

content = content.replace(
    "{ role: 'orchestrator', x: 0, z: 0, label: 'Orchestrator' },",
    "{ role: 'orchestrator', x: 0, z: 0, label: '协调官' },"
)
content = content.replace(
    "{ role: 'calculator', x: -3, z: 2.5, label: 'Calculator' },",
    "{ role: 'calculator', x: -3, z: 2.5, label: '计算师' },"
)
content = content.replace(
    "{ role: 'weather', x: 3, z: 2.5, label: 'Weather' },",
    "{ role: 'weather', x: 3, z: 2.5, label: '气象员' },"
)
content = content.replace(
    "{ role: 'searcher', x: -3, z: -2.5, label: 'Searcher' },",
    "{ role: 'searcher', x: -3, z: -2.5, label: '探索者' },"
)
content = content.replace(
    "{ role: 'summarizer', x: 3, z: -2.5, label: 'Summarizer' },",
    "{ role: 'summarizer', x: 3, z: -2.5, label: '摘要师' },"
)

old_colors = """const AGENT_COLORS = {
  orchestrator: '#6c5ce7',
  calculator: '#60a5fa',
  weather: '#4ade80',
  searcher: '#fb923c',
  summarizer: '#f472b6',
}"""

new_colors = """const AGENT_COLORS = {
  orchestrator: '#7c3aed',
  calculator: '#3b82f6',
  weather: '#10b981',
  searcher: '#f97316',
  summarizer: '#ec4899',
}"""

content = content.replace(old_colors, new_colors)

with open("D:\\pj\\frontend\\src\\components\\IsometricOffice.vue", "w", encoding="utf-8") as f:
    f.write(content)
print("Labels updated!")
