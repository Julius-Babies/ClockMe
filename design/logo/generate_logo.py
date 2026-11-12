"""ClockMe logo generator: static logo, outline logo, SVG animations and Android resources.

usage (from the repo root): python3 design/logo/generate_logo.py design/logo app/src
"""
import math, os, sys

SVG_OUT, APP_SRC = sys.argv[1], sys.argv[2]

# ---------------------------------------------------------------- geometry
C = 54.0                    # Mittelpunkt im 108er-Raster
R = 38.0                    # Ring-Mittellinie
S0 = 9.0                    # Strichstärke Logo (Ring und Zeiger)
G0 = 4.0                    # einheitlicher Abstand
RC0 = 0.2 * S0              # Eckenradius an Segmentschnitten
ROT = -20                   # Drehung des L
LEAD = 6                    # C beginnt so viele Grad vor dem kurzen Zeiger
SPLIT = [0.38, 0.72]        # Segmentgrenzen als Anteil der C-Länge
SF = 2 * R / 9              # Endstrichstärke: M3-Indicator hat stroke = Durchmesser / 10
OUTLINE = 2.0               # Strichstärke der Outline-Variante (dev)

A1 = -90 + ROT              # langer Zeiger
A2 = A1 + 90                # kurzer Zeiger
L1 = R                      # langer Zeiger reicht bis auf den Radius
L2 = R - S0 / 2 - G0 - S0 / 2
START0 = A2 - LEAD
END0 = A1 + 360 - math.degrees(2 * math.asin((S0 + G0) / (2 * R)))
CUTS = [START0 + (END0 - START0) * k for k in SPLIT]
MID = (END0 + START0 + 360) / 2      # Mitte der C-Öffnung
START1, END1 = MID - 360, MID         # geschlossener Ring

KAPPA = 0.5522847498

def V(a):
    a = math.radians(a); return (math.cos(a), math.sin(a))
def T(a):
    a = math.radians(a); return (-math.sin(a), math.cos(a))
def add(*vs): return tuple(sum(c) for c in zip(*vs))
def mul(v, k): return (v[0] * k, v[1] * k)
def ang(p): return math.degrees(math.atan2(p[1], p[0]))
def unwrap(a, ref): return a + 360 * round((ref - a) / 360)
def P(p): return f"{C + p[0]:.2f},{C + p[1]:.2f}"
def lerp(a, b, t): return a + (b - a) * t

def arc_cubics(ctr, r, a0, a1, n):
    out, d = [], (a1 - a0) / n
    k = 4 / 3 * math.tan(math.radians(d) / 4)
    for i in range(n):
        t0, t1 = a0 + d * i, a0 + d * (i + 1)
        p0, p1 = add(ctr, mul(V(t0), r)), add(ctr, mul(V(t1), r))
        out.append((add(p0, mul(T(t0), k * r)), add(p1, mul(T(t1), -k * r)), p1))
    return out

def fillet(ctr, r, a, b):
    if r < 1e-6:
        return [(a, a, b)]
    aa, ab = ang((a[0] - ctr[0], a[1] - ctr[1])), ang((b[0] - ctr[0], b[1] - ctr[1]))
    d = (ab - aa + 180) % 360 - 180
    return arc_cubics(ctr, r, aa, aa + d, 1)

def cut_end(phi, side, g, rc, ro, ri):
    u = V(phi); m = mul((-u[1], u[0]), side)
    a = g / 2 + rc
    res = {}
    for key, rr, k in (("out", ro, -1), ("in", ri, +1)):
        rcr = rr + k * rc
        b = math.sqrt(rcr * rcr - a * a)
        ctr = add(mul(m, a), mul(u, b))
        res[key] = dict(ctr=ctr, line=add(mul(m, g / 2), mul(u, b)), arc=mul(ctr, rr / rcr))
    return res

def cubic_cmds(cs):
    return " ".join(f"C{P(c1)} {P(c2)} {P(p)}" for c1, c2, p in cs)

def segment(a_s, a_e, kind_s, kind_e, s, g, rc, k):
    """Ringsegment als Fläche; Befehlsstruktur ist unabhängig von den Parametern (morphbar)."""
    ro, ri, h = R + s / 2, R - s / 2, s / 2
    if kind_s == "cap":
        ps_o, ps_i = mul(V(a_s), ro), mul(V(a_s), ri)
    else:
        cs = cut_end(a_s, +1, g, rc, ro, ri); ps_o, ps_i = cs["out"]["arc"], cs["in"]["arc"]
    if kind_e == "cap":
        pe_o, pe_i = mul(V(a_e), ro), mul(V(a_e), ri)
    else:
        ce = cut_end(a_e, -1, g, rc, ro, ri); pe_o, pe_i = ce["out"]["arc"], ce["in"]["arc"]
    d = [f"M{P(ps_o)}"]
    d.append(cubic_cmds(arc_cubics((0, 0), ro, unwrap(ang(ps_o), a_s), unwrap(ang(pe_o), a_e), 4)))
    if kind_e == "cap":
        ctr, t, r = mul(V(a_e), R), T(a_e), V(a_e)
        tip = add(ctr, mul(t, k * h))
        d.append(f"C{P(add(pe_o, mul(t, KAPPA * k * h)))} {P(add(tip, mul(r, KAPPA * h)))} {P(tip)}")
        d.append(f"L{P(tip)}")
        d.append(f"C{P(add(tip, mul(r, -KAPPA * h)))} {P(add(pe_i, mul(t, KAPPA * k * h)))} {P(pe_i)}")
    else:
        d.append(cubic_cmds(fillet(ce["out"]["ctr"], rc, pe_o, ce["out"]["line"])))
        d.append(f"L{P(ce['in']['line'])}")
        d.append(cubic_cmds(fillet(ce["in"]["ctr"], rc, ce["in"]["line"], pe_i)))
    d.append(cubic_cmds(arc_cubics((0, 0), ri, unwrap(ang(pe_i), a_e), unwrap(ang(ps_i), a_s), 4)))
    if kind_s == "cap":
        ctr, t, r = mul(V(a_s), R), T(a_s), V(a_s)
        tip = add(ctr, mul(t, -k * h))
        d.append(f"C{P(add(ps_i, mul(t, -KAPPA * k * h)))} {P(add(tip, mul(r, -KAPPA * h)))} {P(tip)}")
        d.append(f"L{P(tip)}")
        d.append(f"C{P(add(tip, mul(r, KAPPA * h)))} {P(add(ps_o, mul(t, -KAPPA * k * h)))} {P(ps_o)}")
    else:
        d.append(cubic_cmds(fillet(cs["in"]["ctr"], rc, ps_i, cs["in"]["line"])))
        d.append(f"L{P(cs['out']['line'])}")
        d.append(cubic_cmds(fillet(cs["out"]["ctr"], rc, cs["out"]["line"], ps_o)))
    return " ".join(d) + " Z"

def ring_segments(c):
    """c = 0: Logo-C, c = 1: geschlossener Ring in Endstrichstärke."""
    st, en = lerp(START0, START1, c), lerp(END0, END1, c)
    b = [st] + CUTS + [en]
    s, g, rc, k = lerp(S0, SF, c), G0 * (1 - c), RC0 * (1 - c), 1 - c
    return [segment(b[i], b[i + 1], "cap" if i == 0 else "cut", "cap" if i == len(b) - 2 else "cut", s, g, rc, k)
            for i in range(len(b) - 1)]

HANDS = f"M{P(mul(V(A1), L1))} L{P((0, 0))} L{P(mul(V(A2), L2))}"

def hands_outline():
    u1, u2, h = V(A1), V(A2), S0 / 2
    p1, p2 = mul(u1, L1), mul(u2, L2)
    q = add(mul(u1, h), mul(u2, h))
    return (f"M{P(q)} L{P(add(p1, mul(u2, h)))} A{h},{h} 0 0 0 {P(add(p1, mul(u2, -h)))} "
            f"L{P(mul(u2, -h))} A{h},{h} 0 0 0 {P(mul(u1, -h))} "
            f"L{P(add(p2, mul(u1, -h)))} A{h},{h} 0 0 0 {P(add(p2, mul(u1, h)))} Z")

def circle(a0, r=R):
    return f"M{P(mul(V(a0), r))} A{r},{r} 0 1 1 {P(mul(V(a0 + 180), r))} A{r},{r} 0 1 1 {P(mul(V(a0), r))}"

# ---------------------------------------------------------------- timing (ms)
def bezier(x1, y1, x2, y2):
    def f(x):
        lo, hi = 0.0, 1.0
        for _ in range(60):
            t = (lo + hi) / 2
            bx = 3 * (1 - t) ** 2 * t * x1 + 3 * (1 - t) * t * t * x2 + t ** 3
            lo, hi = (t, hi) if bx < x else (lo, t)
        t = (lo + hi) / 2
        return 3 * (1 - t) ** 2 * t * y1 + 3 * (1 - t) * t * t * y2 + t ** 3
    return f

HANDS_END = 400                          # L blendet aus, Strich schrumpft
CLOSE_START, CLOSE_END = 150, 800        # Segmente wachsen zum Ring
UNWIND_END = 1200                        # Ring läuft bis auf den ersten Indicator-Frame ab
SPIN = 6000                              # ein voller M3-Zyklus
HANDS_EASE = (0.3, 0.0, 1.0, 1.0)
CLOSE_EASE = (0.2, 0.0, 0.0, 1.0)
# endet mit der Geschwindigkeit, mit der der Spinner das Bogenende bewegt (~0.48°/ms)
UNWIND_EASE = (0.4, 0.0, 0.6, 0.764)
FIRST_PROGRESS, MAX_PROGRESS = 0.1, 0.87
STANDARD = (0.2, 0.0, 0.0, 1.0)

N_SAMPLES = 12
close_times = [CLOSE_START + round((CLOSE_END - CLOSE_START) * i / N_SAMPLES) for i in range(N_SAMPLES + 1)]
close_ease = bezier(*CLOSE_EASE)
close_frames = [ring_segments(close_ease(i / N_SAMPLES)) for i in range(N_SAMPLES + 1)]

def spin_rotation(t):
    """M3 1.4: globale 1080°/6 s linear + 90°-Sprünge (300 ms) alle 1500 ms."""
    extra = 0.0
    for i in range(4):
        t0 = 1500 * i
        extra += 90 * min(max((t - t0) / 300, 0), 1)
    return 1080 * t / SPIN + extra

SPIN_KEYS = [0, 300, 1500, 1800, 3000, 3300, 4500, 4800, 6000]
UNWIND_TRIM = 1 - FIRST_PROGRESS      # Ring startet bei 36°-360°, Kopf bleibt bei 36°
RING_START = 360 * FIRST_PROGRESS

# ---------------------------------------------------------------- SVG
def ks(e): return " ".join(f"{v:g}" for v in e)

def svg_static(color="#000", outline=False):
    segs = ring_segments(0)
    if outline:
        body = "".join(f'<path d="{d}" fill="none" stroke="{color}" stroke-width="{OUTLINE}" stroke-linejoin="round"/>' for d in segs)
        body += f'<path d="{hands_outline()}" fill="none" stroke="{color}" stroke-width="{OUTLINE}" stroke-linejoin="round"/>'
    else:
        body = "".join(f'<path d="{d}" fill="{color}"/>' for d in segs)
        body += f'<path d="{HANDS}" fill="none" stroke="{color}" stroke-width="{S0}" stroke-linecap="round" stroke-linejoin="round"/>'
    return f'<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 108 108">{body}</svg>'

def svg_anim(color, spinner):
    total = UNWIND_END + (SPIN if spinner else 0)
    s = lambda ms: f"{ms / 1000:g}s"
    out = []
    for i in range(3):
        vals = [close_frames[0][i]] + [f[i] for f in close_frames]
        times = [0] + close_times
        kt = ";".join(f"{t / CLOSE_END:.4f}" for t in times)
        out.append(f'<path d="{vals[0]}" fill="{color}">'
                   f'<animate attributeName="d" dur="{s(CLOSE_END)}" fill="freeze" keyTimes="{kt}" values="{";".join(vals)}"/>'
                   f'<set attributeName="opacity" to="0" begin="{s(CLOSE_END)}" fill="freeze"/></path>')
    out.append(f'<path d="{HANDS}" fill="none" stroke="{color}" stroke-width="{S0}" stroke-linecap="round" stroke-linejoin="round">'
               f'<animate attributeName="stroke-width" values="{S0};0" dur="{s(HANDS_END)}" calcMode="spline" keyTimes="0;1" keySplines="{ks(HANDS_EASE)}" fill="freeze"/>'
               f'<animate attributeName="opacity" values="1;0" dur="{s(HANDS_END)}" calcMode="spline" keyTimes="0;1" keySplines="{ks(HANDS_EASE)}" fill="freeze"/></path>')
    un = UNWIND_END - CLOSE_END
    out.append(f'<path d="{circle(RING_START - 360)}" pathLength="100" fill="none" stroke="{color}" stroke-width="{SF:.3f}" stroke-linecap="round" '
               f'stroke-dasharray="100 200" opacity="0">'
               f'<set attributeName="opacity" to="1" begin="{s(CLOSE_END)}" fill="freeze"/>'
               f'<animate attributeName="stroke-dasharray" values="100 200;{100 * (1 - UNWIND_TRIM):g} 200" begin="{s(CLOSE_END)}" dur="{s(un)}" calcMode="spline" keyTimes="0;1" keySplines="{ks(UNWIND_EASE)}" fill="freeze"/>'
               f'<animate attributeName="stroke-dashoffset" values="0;{-100 * UNWIND_TRIM:g}" begin="{s(CLOSE_END)}" dur="{s(un)}" calcMode="spline" keyTimes="0;1" keySplines="{ks(UNWIND_EASE)}" fill="freeze"/>'
               + (f'<set attributeName="opacity" to="0" begin="{s(UNWIND_END)}" fill="freeze"/>' if spinner else "") + '</path>')
    if spinner:
        rot_vals = ";".join(f"{spin_rotation(t):g} {C:g} {C:g}" for t in SPIN_KEYS)
        rot_kt = ";".join(f"{t / SPIN:g}" for t in SPIN_KEYS)
        out.append(f'<g><animateTransform attributeName="transform" type="rotate" begin="{s(UNWIND_END)}" dur="{s(SPIN)}" values="{rot_vals}" keyTimes="{rot_kt}" fill="freeze"/>'
                   f'<path d="{circle(0)}" pathLength="100" fill="none" stroke="{color}" stroke-width="{SF:.3f}" stroke-linecap="round" '
                   f'stroke-dasharray="{100 * FIRST_PROGRESS:g} 200" opacity="0">'
                   f'<set attributeName="opacity" to="1" begin="{s(UNWIND_END)}" fill="freeze"/>'
                   f'<animate attributeName="stroke-dasharray" begin="{s(UNWIND_END)}" dur="{s(SPIN)}" calcMode="spline" keyTimes="0;0.5;1" '
                   f'values="{100 * FIRST_PROGRESS:g} 200;{100 * MAX_PROGRESS:g} 200;{100 * FIRST_PROGRESS:g} 200" keySplines="0 0 1 1;{ks(STANDARD)}" fill="freeze"/>'
                   f'</path></g>')
    return f'<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 108 108">{"".join(out)}</svg>'

os.makedirs(SVG_OUT, exist_ok=True)
files = {
    "clockme.svg": svg_static(),
    "clockme-outline.svg": svg_static(outline=True),
    "clockme-intro.svg": svg_anim("#000", spinner=False),
    "clockme-splash.svg": svg_anim("#000", spinner=True),
}
for name, content in files.items():
    open(os.path.join(SVG_OUT, name), "w").write(content + "\n")

# ---------------------------------------------------------------- Android
NS = 'xmlns:android="http://schemas.android.com/apk/res/android"'
ICON_SCALE = 0.66           # Launcher: Logo deutlich innerhalb der Safe Zone (r 33)
SPLASH_SCALE = 0.75         # Splash: Außenradius 31.9 < 36 (2/3-Kreis der Icon-Fläche)

def write(rel, content):
    path = os.path.join(APP_SRC, rel)
    os.makedirs(os.path.dirname(path), exist_ok=True)
    open(path, "w").write('<?xml version="1.0" encoding="utf-8"?>\n' + content.strip() + "\n")

def logo_paths(color, outline, indent="        "):
    segs = ring_segments(0)
    if outline:
        lines = [f'<path android:pathData="{d}" android:strokeColor="{color}" android:strokeWidth="{OUTLINE}" android:strokeLineJoin="round" />' for d in segs]
        lines.append(f'<path android:pathData="{hands_outline()}" android:strokeColor="{color}" android:strokeWidth="{OUTLINE}" android:strokeLineJoin="round" />')
    else:
        lines = [f'<path android:pathData="{d}" android:fillColor="{color}" />' for d in segs]
        lines.append(f'<path android:pathData="{HANDS}" android:strokeColor="{color}" android:strokeWidth="{S0}" '
                     f'android:strokeLineCap="round" android:strokeLineJoin="round" />')
    return "\n".join(indent + l for l in lines)

def vector(body, size=108, comment=""):
    return f"""{comment}<vector {NS}
    android:width="{size}dp"
    android:height="{size}dp"
    android:viewportWidth="108"
    android:viewportHeight="108">
{body}
</vector>"""

def scaled(body, scale):
    return f"""    <group android:pivotX="54" android:pivotY="54" android:scaleX="{scale}" android:scaleY="{scale}">
{body}
    </group>"""

GEN = "<!-- Generated by design/logo/generate_logo.py, do not edit by hand. -->\n"

for src, outline in (("main", False), ("debug", True)):
    res = f"{src}/res"
    write(f"{res}/drawable/ic_launcher_foreground.xml", vector(scaled(logo_paths("#FFFFFFFF", outline), ICON_SCALE), comment=GEN))
    write(f"{res}/drawable/ic_launcher_monochrome.xml", vector(scaled(logo_paths("#FFFFFFFF", outline), ICON_SCALE), comment=GEN))
    # Legacy-Icons (API 24/25)
    for name, shape, scale in (("ic_launcher", "M24,0h60a24,24 0 0 1 24,24v60a24,24 0 0 1 -24,24h-60a24,24 0 0 1 -24,-24v-60a24,24 0 0 1 24,-24z", 0.9),
                               ("ic_launcher_round", "M54,0a54,54 0 1 1 0,108a54,54 0 1 1 0,-108z", 0.85)):
        body = f'    <path android:pathData="{shape}" android:fillColor="@color/brand" />\n' + scaled(logo_paths("#FFFFFFFF", outline), scale)
        write(f"{res}/mipmap-anydpi/{name}.xml", vector(body, size=48, comment=GEN))

# Splash: animierte Variante mit Spinner
def anim(prop, frm, to, start, dur, vtype="floatType", interp=None):
    i = (f'\n                    <aapt:attr name="android:interpolator">\n                        <pathInterpolator android:controlX1="{interp[0]}" android:controlY1="{interp[1]}" '
         f'android:controlX2="{interp[2]}" android:controlY2="{interp[3]}" />\n                    </aapt:attr>\n                ') if interp else ""
    lin = "" if interp else ' android:interpolator="@android:interpolator/linear"'
    close = f">{i}</objectAnimator>" if interp else " />"
    return (f'                <objectAnimator android:propertyName="{prop}" android:valueType="{vtype}" '
            f'android:valueFrom="{frm}" android:valueTo="{to}" android:startOffset="{start}" android:duration="{dur}"{lin}{close}')

def target(name, animators):
    return f"""    <target android:name="{name}">
        <aapt:attr name="android:animation">
            <set android:ordering="together">
{chr(10).join(animators)}
            </set>
        </aapt:attr>
    </target>"""

targets = []
for i in range(3):
    a = [anim("pathData", close_frames[j][i], close_frames[j + 1][i], close_times[j], close_times[j + 1] - close_times[j], "pathType")
         for j in range(N_SAMPLES)]
    a.append(anim("fillAlpha", 1, 0, CLOSE_END, 0))
    targets.append(target(f"segment{i}", a))
targets.append(target("hands", [anim("strokeWidth", S0, 0, 0, HANDS_END, interp=HANDS_EASE),
                                anim("strokeAlpha", 1, 0, 0, HANDS_END, interp=HANDS_EASE)]))
targets.append(target("ring", [anim("strokeAlpha", 0, 1, CLOSE_END, 0),
                               anim("trimPathStart", 0, round(UNWIND_TRIM, 4), CLOSE_END, UNWIND_END - CLOSE_END, interp=UNWIND_EASE),
                               anim("strokeAlpha", 1, 0, UNWIND_END, 0)]))
targets.append(target("arc", [anim("strokeAlpha", 0, 1, UNWIND_END, 0),
                              anim("trimPathEnd", FIRST_PROGRESS, MAX_PROGRESS, UNWIND_END, SPIN // 2),
                              anim("trimPathEnd", MAX_PROGRESS, FIRST_PROGRESS, UNWIND_END + SPIN // 2, SPIN // 2, interp=STANDARD)]))
targets.append(target("spinner", [anim("rotation", round(spin_rotation(SPIN_KEYS[j]), 3), round(spin_rotation(SPIN_KEYS[j + 1]), 3),
                                       UNWIND_END + SPIN_KEYS[j], SPIN_KEYS[j + 1] - SPIN_KEYS[j]) for j in range(len(SPIN_KEYS) - 1)]))

segs0 = ring_segments(0)
splash = f"""{GEN}<!--
  Splash icon (API 31+): the logo's L fades out, the ring segments grow into the first frame of
  the Material 3 indeterminate CircularProgressIndicator (36° arc at 3 o'clock, stroke = diameter / 10),
  then one full 6 s indicator cycle follows.
-->
<animated-vector {NS}
    xmlns:aapt="http://schemas.android.com/aapt">
    <aapt:attr name="android:drawable">
        <vector
            android:width="108dp"
            android:height="108dp"
            android:viewportWidth="108"
            android:viewportHeight="108">
            <group android:pivotX="54" android:pivotY="54" android:scaleX="{SPLASH_SCALE}" android:scaleY="{SPLASH_SCALE}">
{chr(10).join(f'                <path android:name="segment{i}" android:pathData="{d}" android:fillColor="#FFFFFFFF" />' for i, d in enumerate(segs0))}
                <path android:name="hands" android:pathData="{HANDS}" android:strokeColor="#FFFFFFFF" android:strokeWidth="{S0}" android:strokeLineCap="round" android:strokeLineJoin="round" />
                <path android:name="ring" android:pathData="{circle(RING_START - 360)}" android:strokeColor="#FFFFFFFF" android:strokeWidth="{SF:.3f}" android:strokeLineCap="round" android:strokeAlpha="0" />
                <group android:name="spinner" android:pivotX="54" android:pivotY="54">
                    <path android:name="arc" android:pathData="{circle(0)}" android:strokeColor="#FFFFFFFF" android:strokeWidth="{SF:.3f}" android:strokeLineCap="round" android:strokeAlpha="0" android:trimPathEnd="{FIRST_PROGRESS}" />
                </group>
            </group>
        </vector>
    </aapt:attr>
{chr(10).join(targets)}
</animated-vector>"""
write("main/res/drawable/splash_animated.xml", splash)
print("total splash ms", UNWIND_END + SPIN)
