with open("D:\\pj\\frontend\\src\\components\\IsometricOffice.vue", "r", encoding="utf-8") as f:
    data = f.read()

import re
data = re.sub(r'(const AGENT_COLORS = \{\n)\n', r'\1', data)
data = re.sub(r"(#[a-f0-9]+',)\n\n", r'\1\n', data)

colors = {"#6c5ce7": "#a855f7", "#60a5fa": "#3b82f6", "#4ade80": "#22c55e", "#fb923c": "#f97316", "#f472b6": "#ec4899"}
for old, new in colors.items():
    data = data.replace(old, new)

data = data.replace("emissiveIntensity: 0.35,", "emissiveIntensity: 0.5,")
data = data.replace("emissiveIntensity: 0.30,", "emissiveIntensity: 0.45,")
data = data.replace("emissiveIntensity: 0.28,", "emissiveIntensity: 0.4,")
data = data.replace("emissiveIntensity: 0.25,", "emissiveIntensity: 0.35,")
data = data.replace("emissiveIntensity: 0.22,", "emissiveIntensity: 0.3,")
data = data.replace("emissiveIntensity: 0.20,", "emissiveIntensity: 0.3,")
data = data.replace("emissiveIntensity: 0.18,", "emissiveIntensity: 0.25,")

with open("D:\\pj\\frontend\\src\\components\\IsometricOffice.vue", "w", encoding="utf-8") as f:
    f.write(data)
print("Done!")
