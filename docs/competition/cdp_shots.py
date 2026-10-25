"""企开得胜 — UI 外观图批截（CDP + 9222，登录 + 7 路由 + 全景大屏）。"""
import json, time, base64, os, urllib.request
import websocket

OUT = r"D:\unibrain\docs\competition\screenshots"
os.makedirs(OUT, exist_ok=True)

targets = json.load(urllib.request.urlopen("http://127.0.0.1:9222/json", timeout=5))
ws_url = [t for t in targets if t.get("type") == "page"][0]["webSocketDebuggerUrl"]
ws = websocket.create_connection(ws_url, timeout=30)
_mid = [0]
def cdp(method, params=None):
    _mid[0] += 1
    ws.send(json.dumps({"id": _mid[0], "method": method, "params": params or {}}))
    while True:
        m = json.loads(ws.recv())
        if m.get("id") == _mid[0]:
            return m.get("result", {})
def ev(expr):
    return cdp("Runtime.evaluate", {"expression": expr, "returnByValue": True, "awaitPromise": True}).get("result", {}).get("value")
def shot(fname, wait=2.0):
    time.sleep(wait)
    r = cdp("Page.captureScreenshot", {"format": "png"})
    p = os.path.join(OUT, fname)
    open(p, "wb").write(base64.b64decode(r["data"]))
    print(f"{fname}  {os.path.getsize(p)//1024}KB")
def wait_until(expr, tries=60, every=1.0):
    for _ in range(tries):
        time.sleep(every)
        try:
            if ev(expr): return True
        except Exception:
            pass
    return False

cdp("Page.enable"); cdp("Runtime.enable")
cdp("Page.navigate", {"url": "http://localhost:8888/"})
print("login form ready:", wait_until("document.querySelectorAll('input').length >= 2"))
shot("01-login.png")

ev(r"""
(() => {
  const setV = (el, v) => {
    const s = Object.getOwnPropertyDescriptor(HTMLInputElement.prototype, 'value').set;
    s.call(el, v); el.dispatchEvent(new Event('input', { bubbles: true }));
  };
  setV(document.querySelectorAll('input')[0], 'admin');
  setV(document.querySelector('input[type="password"]'), '123456');
  const btn = [...document.querySelectorAll('button')].find(b => b.type === 'submit' || /登\s*录/.test(b.innerText));
  if (btn) btn.click();
  return 'ok';
})()
""")
print("home reached:", wait_until("location.hash === '#/home' && document.querySelectorAll('input').length === 0", 40))
shot("02-home.png", 4)

for route, fname, w in [
    ("industry",   "03-industry.png",   8),
    ("diagnosis",  "04-diagnosis.png",  5),
    ("investment", "05-investment.png", 5),
    ("enterprise", "06-enterprise.png", 5),
    ("reports",    "07-reports.png",    5),
]:
    ev(f"location.hash = '#/{route}'")
    time.sleep(2)
    shot(fname, w)

ev("location.hash = '#/home'"); time.sleep(3)
print("fullscreen btn:", ev(r"""
(() => {
  const btn = [...document.querySelectorAll('button')].find(b => b.innerText.includes('全景大屏'));
  if (btn) { btn.click(); return 'clicked'; }
  return 'no-btn';
})()
"""))
shot("08-dashboard-full.png", 5)

print("loggedIn:", ev("!!(localStorage.getItem('token')||localStorage.getItem('Admin-Token')||sessionStorage.getItem('token'))"))
ws.close()
print("DONE")
