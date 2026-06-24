with open("D:\\pj\\frontend\\src\\components\\IsometricOffice.vue.bak", "rb") as f:
    content = f.read()

# Find connectionText assignments  
for pattern in [b"connectionText", b"label:", b"emoji", b"title="]:
    import re
    lines = content.decode("utf-8", errors="replace").split("\n")
    for i, line in enumerate(lines):
        if pattern.decode() in line:
            print(f"{i}: {line.strip()[:120]}")
