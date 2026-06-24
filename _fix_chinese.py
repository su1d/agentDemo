# -*- coding: utf-8 -*-
import sys, os
sys.stdout.reconfigure(encoding="utf-8")

bak_path = "D:\\pj\\frontend\\src\\components\\IsometricOffice.vue.bak"
cur_path = "D:\\pj\\frontend\\src\\components\\IsometricOffice.vue"

with open(bak_path, 'rb') as f:
    bak = f.read()
text = bak.decode('gbk')
utf8 = text.encode('utf-8')

utf8 = utf8.replace(b"label: 'Orchestrator'", '???'.encode('utf-8'))
utf8 = utf8.replace(b"label: 'Calculator'", '???'.encode('utf-8'))
utf8 = utf8.replace(b"label: 'Weather'", '???'.encode('utf-8'))
utf8 = utf8.replace(b"label: 'Searcher'", '???'.encode('utf-8'))
utf8 = utf8.replace(b"label: 'Summarizer'", '???'.encode('utf-8'))

idx = utf8.find(b'<template>', 100)
if idx >= 0:
    utf8 = utf8[:idx]

with open(cur_path, 'wb') as f:
    f.write(utf8)

with open(cur_path, 'r', encoding='utf-8') as f:
    content = f.read()
for label in ['???', '???', '???', '???', '???']:
    print('OK: ' + label if label in content else 'FAIL: ' + label)
