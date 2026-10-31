#!/usr/bin/env python3
"""Synthesizes the app's UI sound effects (original, no samples). Output: app/src/main/res/raw/*.wav"""
import math, random, struct, wave, os, sys

SR = 22050
OUT = os.path.join(os.path.dirname(__file__), '..', 'app', 'src', 'main', 'res', 'raw')
random.seed(2077)

def osc(kind, ph):
    x = ph % 1.0
    if kind == 'sin': return math.sin(2 * math.pi * x)
    if kind == 'sq': return 1.0 if x < .5 else -1.0
    if kind == 'saw': return 2 * x - 1
    if kind == 'tri': return 4 * abs(x - .5) - 1
    return random.uniform(-1, 1)

def tone(dur, f0, f1=None, kind='sq', vol=.5, attack=.003, decay=None, crush=0):
    n = int(SR * dur); out = []; ph = 0.0; f1 = f1 or f0
    for i in range(n):
        t = i / n
        f = f0 * (f1 / f0) ** t
        ph += f / SR
        env = min(1, i / (attack * SR + 1)) * ((1 - t) ** (decay if decay else 1.5))
        s = osc(kind, ph) * env * vol
        if crush: s = round(s * crush) / crush
        out.append(s)
    return out

def mix(*parts):
    n = max(len(p) + o for o, p in parts); buf = [0.0] * n
    for o, p in parts:
        for i, v in enumerate(p): buf[o + i] += v
    return buf

def at(sec): return int(sec * SR)

def save(name, buf, gain=.9):
    peak = max(1e-9, max(abs(v) for v in buf))
    with wave.open(os.path.join(OUT, name + '.wav'), 'w') as w:
        w.setnchannels(1); w.setsampwidth(2); w.setframerate(SR)
        w.writeframes(b''.join(struct.pack('<h', int(max(-1, min(1, v / peak * gain)) * 32000)) for v in buf))

# tap: tiny digital blip
save('sfx_tap', mix((0, tone(.035, 1900, 1500, 'sq', .4, decay=3))), .55)
# tick: XP counter tick
save('sfx_tick', mix((0, tone(.018, 2600, 2400, 'tri', .5, decay=4))), .45)
# boot line: short beep
save('sfx_boot', mix((0, tone(.05, 980, 990, 'sq', .35, decay=2, crush=6))), .5)
# quest complete: rising 3-note arpeggio + noise burst
notes = [523.25, 659.25, 987.77]
save('sfx_complete', mix(*[(at(i * .07), tone(.16, f, f * 1.01, 'saw', .45, decay=2)) for i, f in enumerate(notes)],
                          (0, tone(.06, 1, 1, 'noise', .12, decay=3))), .8)
# level up: upward sweep + power chord with bitcrush tail
chord = [261.63, 392.0, 523.25, 783.99]
save('sfx_levelup', mix((0, tone(.45, 180, 1400, 'saw', .35, attack=.01, decay=.6)),
                         *[(at(.38), tone(1.1, f, f, 'sq', .18, decay=1.8, crush=10)) for f in chord],
                         (at(.38), tone(.25, 1, 1, 'noise', .2, decay=4))), .85)
# streak / bonus: shimmering fast arpeggio
save('sfx_streak', mix(*[(at(i * .045), tone(.12, f, f, 'tri', .35, decay=2)) for i, f in enumerate([784, 988, 1175, 1568, 1976])]), .7)
# error / failed: two low buzzes
save('sfx_error', mix((0, tone(.09, 140, 120, 'sq', .5, decay=1)), (at(.12), tone(.12, 120, 90, 'sq', .5, decay=1))), .7)
# glitch: crushed noise sweep (theme change, transitions)
save('sfx_glitch', mix((0, tone(.18, 1, 1, 'noise', .4, decay=2, crush=4)), (0, tone(.18, 3000, 200, 'sq', .2, decay=2, crush=4))), .6)
print('ok', sorted(os.listdir(OUT)))
