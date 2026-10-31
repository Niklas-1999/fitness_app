#!/usr/bin/env python3
"""Synthesizes IRONCORE's UI sound effects (original, no samples): hacker / cyberpunk terminal style.
Output: app/src/main/res/raw/*.wav   Run: python3 tools/gen_sfx.py"""
import math, random, struct, wave, os

SR = 22050
OUT = os.path.join(os.path.dirname(__file__), '..', 'app', 'src', 'main', 'res', 'raw')
random.seed(2077)


def n(sec): return int(sec * SR)


def silence(sec): return [0.0] * n(sec)


def osc(kind, ph):
    x = ph % 1.0
    if kind == 'sin': return math.sin(2 * math.pi * x)
    if kind == 'sq': return 1.0 if x < .5 else -1.0
    if kind == 'saw': return 2 * x - 1
    if kind == 'tri': return 4 * abs(x - .5) - 1
    return random.uniform(-1, 1)


def tone(dur, f0, f1=None, kind='sq', vol=.5, attack=.002, curve=1.5, fm=0.0, fm_ratio=2.0):
    """exponential pitch glide f0->f1, optional FM, decaying envelope"""
    f1 = f1 or f0
    out, ph, mph = [], 0.0, 0.0
    N = n(dur)
    for i in range(N):
        t = i / N
        f = f0 * (f1 / f0) ** t
        mph += f * fm_ratio / SR
        ph += f * (1 + fm * math.sin(2 * math.pi * mph)) / SR
        env = min(1, i / (attack * SR + 1)) * (1 - t) ** curve
        out.append(osc(kind, ph) * env * vol)
    return out


def noise(dur, vol=.5, curve=2, attack=.001):
    N = n(dur)
    return [random.uniform(-1, 1) * vol * min(1, i / (attack * SR + 1)) * (1 - i / N) ** curve for i in range(N)]


def lowpass(buf, cutoff):
    a = 1 - math.exp(-2 * math.pi * cutoff / SR); y = 0.0; out = []
    for v in buf: y += a * (v - y); out.append(y)
    return out


def highpass(buf, cutoff):
    lp = lowpass(buf, cutoff)
    return [v - l for v, l in zip(buf, lp)]


def sweep_bandpass(buf, f0, f1):
    """noise riser: one-pole low+high pass with moving cutoff"""
    out, y1, y2 = [], 0.0, 0.0
    N = len(buf)
    for i, v in enumerate(buf):
        f = f0 * (f1 / f0) ** (i / N)
        a = 1 - math.exp(-2 * math.pi * f * 1.6 / SR)
        b = 1 - math.exp(-2 * math.pi * f / 1.6 / SR)
        y1 += a * (v - y1); y2 += b * (y1 - y2)
        out.append(y1 - y2)
    return out


def crush(buf, levels=8, hold=1):
    out, last = [], 0.0
    for i, v in enumerate(buf):
        if i % hold == 0: last = round(v * levels) / levels
        out.append(last)
    return out


def drive(buf, k=3.0): return [math.tanh(v * k) / math.tanh(k) for v in buf]


def stutter(buf, start, slice_len, times):
    s, L = n(start), n(slice_len)
    piece = buf[s:s + L]
    return buf[:s] + piece * times + buf[s + L:]


def mix(*parts):
    """parts: (offset_seconds, buffer)"""
    N = max(n(o) + len(b) for o, b in parts); out = [0.0] * N
    for o, b in parts:
        s = n(o)
        for i, v in enumerate(b): out[s + i] += v
    return out


def save(name, buf, gain=.9):
    peak = max(1e-9, max(abs(v) for v in buf))
    with wave.open(os.path.join(OUT, name + '.wav'), 'w') as w:
        w.setnchannels(1); w.setsampwidth(2); w.setframerate(SR)
        w.writeframes(b''.join(struct.pack('<h', int(max(-1, min(1, v / peak * gain)) * 32000)) for v in buf))


def data_burst(count, spacing, dur=.016, lo=900, hi=4200, vol=.3):
    return mix(*[(i * spacing, crush(tone(dur, f := random.uniform(lo, hi), f * random.choice([.8, 1, 1.25]), 'sq', vol, curve=1), 6))
                 for i in range(count)])


# tap: mechanical keystroke (noise transient + tiny click)
save('sfx_tap', mix((0, highpass(noise(.012, .9, 3), 2500)), (0, tone(.006, 3200, 2600, 'sq', .35, curve=2))), .5)

# tick: short FM data chirp
save('sfx_tick', mix((0, crush(tone(.014, 3000, 5200, 'sq', .5, fm=.6, fm_ratio=3.1), 5))), .35)

# boot line: teletype double click + faint carrier
save('sfx_boot', mix((0, highpass(noise(.008, .8, 3), 3000)), (.028, highpass(noise(.006, .6, 3), 3000)),
                     (0, crush(tone(.045, 1180, 1180, 'sq', .12, curve=1), 4))), .45)

# quest complete: "packet uploaded" -> data burst, low thunk, two-beep ACK
save('sfx_complete', mix((0, data_burst(9, .021)),
                         (.2, lowpass(tone(.14, 90, 45, 'sin', .9, curve=2), 400)),
                         (.2, highpass(noise(.03, .4, 3), 1500)),
                         (.27, tone(.035, 1620, 1620, 'sq', .22, curve=.5)),
                         (.33, tone(.05, 1620, 1620, 'sq', .22, curve=.8))), .8)

# level up: "SYSTEM UPGRADE" -> filtered riser + accelerating data, sub-bass impact,
# dark detuned crushed chord, glitch stutter tail
riser = sweep_bandpass(noise(.75, .7, 0), 300, 6000)
riser = [v * (i / len(riser)) ** 1.5 for i, v in enumerate(riser)]
accel = mix(*[(.75 * (1 - (1 - k / 14) ** 1.7), crush(tone(.012, 1800 + 160 * k, 2600 + 160 * k, 'sq', .25), 5)) for k in range(14)])
sub = lowpass(tone(1.1, 70, 38, 'sin', 1.0, attack=.003, curve=1.2), 300)
chord = [0.0] * n(1.1)
for f in (55.0, 82.41, 87.31, 110.0):          # A1, E2, F2 (minor-second tension), A2
    for det in (-.6, .6):
        chord = [a + b for a, b in zip(chord, tone(1.1, f + det, f + det, 'saw', .16, attack=.01, curve=1.6))]
chord = crush(drive(lowpass(chord, 1400), 2.2), 10, hold=2)
impact = mix((0, sub), (0, chord), (0, highpass(noise(.12, .6, 4), 900)))
impact = stutter(impact, .55, .035, 5)
save('sfx_levelup', mix((0, riser), (0, accel), (.75, impact)), .9)

# streak: descending crushed FM shimmer (network ping cascade)
save('sfx_streak', mix(*[(i * .038, crush(tone(.07, f, f * .97, 'sq', .25, fm=.3, fm_ratio=1.5), 6))
                         for i, f in enumerate([2093, 1760, 1568, 1319, 1175])],
                       (0, lowpass(tone(.25, 110, 60, 'sin', .5, curve=2), 300))), .65)

# error: "ACCESS DENIED" distorted low buzz x2
buzz = lambda d: crush(drive(tone(d, 96, 88, 'sq', .7, curve=.4), 4), 6)
save('sfx_error', mix((0, buzz(.11)), (.15, buzz(.16))), .7)

# glitch: crushed noise + falling square, stuttered
g = mix((0, crush(noise(.2, .5, 1.5), 4, hold=3)), (0, crush(tone(.2, 3000, 160, 'sq', .25, curve=1), 4, hold=2)))
save('sfx_glitch', stutter(g, .04, .025, 3), .55)

print('ok', sorted(os.listdir(OUT)))
