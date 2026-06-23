# encoding: utf-8
with open(r"D:\pj\frontend\src\components\IsometricOffice.vue", "rb") as fp:
    raw = fp.read()
if raw[0:3] == b"\xef\xbb\xbf":
    raw = raw[3:]
iso = raw.decode("utf-8", errors="replace")

with open(r"D:\pj\frontend\src\views\app\DashboardView.vue", "rb") as fp:
    raw2 = fp.read()
if raw2[0:3] == b"\xef\xbb\xbf":
    raw2 = raw2[3:]
dash = raw2.decode("utf-8", errors="replace")

print("Read OK")
