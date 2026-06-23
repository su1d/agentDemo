with open('D:\\pj\\frontend\\src\\components\\IsometricOffice.vue', 'rb') as fp:
    raw = fp.read()
if raw[0:3] == b'\xef\xbb\xbf':
    raw = raw[3:]
iso = raw.decode('utf-8')
print('ISO read ok, len=%d' % len(iso))
with open('D:\\pj\\frontend\\src\\views\\app\\DashboardView.vue', 'rb') as fp:
    raw2 = fp.read()
if raw2[0:3] == b'\xef\xbb\xbf':
    raw2 = raw2[3:]
dash = raw2.decode('utf-8')
print('Dash read ok, len=%d' % len(dash))
