#!/usr/bin/env python3
"""Simulates the XP/level curve: how long does each level take for a given training pattern?"""
import math

# --- XP per quest, diminishing within a day (anti-overtraining) ---
BASE = 30                       # typical quest base XP (scaled by quest size in the app)
DAILY_MULT = [1.0, 1.0, 1.0, 0.9, 0.8, 0.6, 0.3, 0.15]   # k-th quest of the day; afterwards FLOOR
FLOOR = 0.05
CLEAR_BONUS = 40                # directive clear, once per day

def day_xp(quests, clear=True, streak=0):
    xp = sum(BASE * (DAILY_MULT[k] if k < len(DAILY_MULT) else FLOOR) for k in range(quests))
    if clear: xp += CLEAR_BONUS
    return xp * (1 + min(streak * 0.02, 0.30))

# --- level requirement: XP needed to go from L to L+1 (no cap) ---
def need(L):
    return max(60, round(84 * 1.075 ** L - 10))

def simulate(days_per_week, quests, weeks=104):
    lvl, xp, streak, out = 1, 0, 0, {}
    for d in range(weeks * 7):
        if d % 7 < days_per_week:
            streak += 1
            xp += day_xp(quests, streak=streak)
            while xp >= need(lvl):
                xp -= need(lvl); lvl += 1
        out[d + 1] = lvl
    return out

if __name__ == '__main__':
    print('need(L):', {L: need(L) for L in [1, 2, 3, 5, 10, 15, 20, 30, 40, 50, 75, 100]})
    print('one full day (5 quests) =', round(day_xp(5)), 'XP; 8 quests =', round(day_xp(8)), 'XP; 12 quests =', round(day_xp(12)), 'XP')
    for dpw in (3, 4, 5):
        s = simulate(dpw, 5)
        print(f'{dpw}x/week:', ', '.join(f'd{d}: L{s[d]}' for d in (1, 2, 7, 14, 30, 60, 90, 180, 365, 728)))
    # days per level-up at various levels for 4x/week
    s = simulate(4, 5); first = {}
    for d, l in s.items(): first.setdefault(l, d)
    for L in (3, 5, 10, 15, 20, 25, 30, 40):
        if L + 1 in first: print(f'L{L}->{L+1}: {first[L+1]-first[L]} days')

REST_XP = 20  # REST PROTOCOL check-in on non-training days

def days_to_level(dpw, quests=5):
    """first day each level is reached, training dpw days/week + rest check-ins on the other days"""
    lvl, xp, streak, first = 1, 0, 0, {1: 0}
    for d in range(1, 3 * 365):
        streak += 1
        xp += day_xp(quests, streak=streak) if (d - 1) % 7 < dpw else REST_XP
        while xp >= need(lvl):
            xp -= need(lvl); lvl += 1; first[lvl] = d
    return first

RANKS = [('GHOST', 1), ('RUNNER', 10), ('OPERATIVE', 22), ('ENFORCER', 33), ('VANGUARD', 40), ('APEX', 46), ('LEGEND', 53)]

if __name__ == '__main__':
    print('\nrank thresholds (days to reach):')
    tables = {d: days_to_level(d) for d in (3, 4, 5)}
    for name, L in RANKS:
        print(f'  {name:10s} L{L:<3d}', '  '.join(f'{d}x/wk: day {tables[d].get(L, "-")}' for d in (3, 4, 5)))
