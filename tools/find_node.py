#!/usr/bin/env python3
"""Prints the centre of the [idx]-th UI node whose text equals argv[2] (trailing * = prefix match). XML file in argv[1]."""
import sys, re, html
xml = open(sys.argv[1]).read(); want = sys.argv[2]; idx = int(sys.argv[3]) if len(sys.argv) > 3 and sys.argv[3] else 0
hits = []
for m in re.finditer(r'<node [^>]*?text="([^"]*)"[^>]*?bounds="\[(\d+),(\d+)\]\[(\d+),(\d+)\]"', xml):
    t = html.unescape(m.group(1))
    if t == want or (want.endswith('*') and t.startswith(want[:-1])): hits.append(m)
if len(hits) > idx:
    x1, y1, x2, y2 = map(int, hits[idx].groups()[1:]); print((x1 + x2) // 2, (y1 + y2) // 2)
