# Custom sound effects

Drop files here named after the role they replace, then run `./tools/import_sfx.sh` and rebuild (`./dev.sh`).

| File name | When it plays |
|---|---|
| `tap.*` | button taps |
| `tick.*` | XP bar filling / counters |
| `boot.*` | each boot / terminal log line |
| `complete.*` | quest completed |
| `levelup.*` | level-up sequence (can be 1–2 s) |
| `streak.*` | streak increased |
| `error.*` | failed / denied |
| `glitch.*` | screen transitions, theme change |

Any format works (mp3, wav, ogg, flac, m4a). Keep UI sounds short (taps < 0.1 s).
Only use sounds whose licence allows it (e.g. CC0 on freesound.org or Pixabay sounds).
To go back to a generated default: delete the file here, then run `python3 tools/gen_sfx.py && ./tools/import_sfx.sh` (regenerates all defaults, then re-applies the remaining custom files).
