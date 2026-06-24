# -*- coding: utf-8 -*-
import sys
sys.stdout.reconfigure(encoding="utf-8")

path = "D:\\pj\\frontend\\src\\components\\IsometricOffice.vue"

with open(path, "rb") as f:
    raw = f.read()

# Replace label values
raw = raw.replace(b"label: 'Orchestrator'", b"label: '\xe5\x8d\x8f\xe8\xb0\x83\xe5\xae\x98'")
raw = raw.replace(b"label: 'Calculator'", b"label: '\xe8\xae\xa1\xe7\xae\x97\xe5\xb8\x88'")  
raw = raw.replace(b"label: 'Weather'", b"label: '\xe6\xb0\x94\xe8\xb1\xa1\xe5\x91\x98'")
raw = raw.replace(b"label: 'Searcher'", b"label: '\xe6\x8e\xa2\xe7\xb4\xa2\xe8\x80\x85'")
raw = raw.replace(b"label: 'Summarizer'", b"label: '\xe6\x91\x98\xe8\xa6\x81\xe5\xb8\x88'")

with open(path, "wb") as f:
    f.write(raw)

print("Done! Labels replaced.")
print("???, ???, ???, ???, ???")
