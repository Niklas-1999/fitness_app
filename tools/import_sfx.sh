#!/usr/bin/env bash
# Import custom sound effects: put files named by role into sounds/ (any format ffmpeg reads:
# mp3, wav, ogg, flac, m4a …), e.g. sounds/levelup.mp3, then run ./tools/import_sfx.sh
# Each file is trimmed of leading silence (MP3s usually have some), loudness-normalized,
# converted to OGG Vorbis mono and written to app/src/main/res/raw/sfx_<role>.ogg,
# replacing the generated default. Roles without a custom file keep the default.
set -e
cd "$(dirname "$0")/.."
RAW=app/src/main/res/raw
ROLES="tap tick boot complete levelup streak error glitch"
for role in $ROLES; do
  src=$(ls sounds/"$role".* 2>/dev/null | head -1 || true)
  [ -z "$src" ] && continue
  rm -f "$RAW/sfx_$role".*
  ffmpeg -loglevel error -y -i "$src" \
    -af "silenceremove=start_periods=1:start_threshold=-50dB,loudnorm=I=-16:TP=-1.5:LRA=11" \
    -ac 1 -ar 44100 -c:a libvorbis -q:a 5 "$RAW/sfx_$role.ogg"
  echo "imported $src -> $RAW/sfx_$role.ogg ($(ffprobe -v error -show_entries format=duration -of csv=p=0 "$RAW/sfx_$role.ogg")s)"
done
