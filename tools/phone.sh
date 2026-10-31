#!/usr/bin/env bash
# Phone automation helpers for testing: ./tools/phone.sh tap "TEXT" | type "text" | shot file.png | texts | back
ADB="${ANDROID_TOOLS:-$HOME/android-tools}/sdk/platform-tools/adb"
dump() { "$ADB" shell uiautomator dump /sdcard/ui.xml >/dev/null 2>&1; "$ADB" shell cat /sdcard/ui.xml; }
case "$1" in
  tap)   # tap the first node whose text equals (or contains, with -c) $2
    dump > /tmp/ironcore_ui.xml
    b=$(python3 "$(dirname "$0")/find_node.py" /tmp/ironcore_ui.xml "$2" "$3")
    [ -z "$b" ] && { echo "NOT FOUND: $2"; exit 1; }
    y=${b#* }
    if [ "$y" -gt 1900 ] || [ "$y" -lt 250 ]; then   # off-screen: scroll it into the middle and retry
      d=$(( y - 1100 )); "$ADB" shell input swipe 540 $(( 1100 + d/2 > 1700 ? 1700 : 1100 )) 540 $(( 1100 - (d > 900 ? 900 : d) )) 400; sleep .5
      dump > /tmp/ironcore_ui.xml; b=$(python3 "$(dirname "$0")/find_node.py" /tmp/ironcore_ui.xml "$2" "$3")
    fi
    "$ADB" shell input tap $b ;;
  type)  "$ADB" shell input text "$(echo "$2" | sed 's/ /%s/g')" ;;
  shot)  "$ADB" exec-out screencap -p > "$2" ;;
  texts) dump | grep -o 'text="[^"]*"' | grep -v 'text=""' ;;
  back)  "$ADB" shell input keyevent 4 ;;
  edit)  # edit N VALUE: focus the N-th (0-based) input field, clear it, type VALUE
    dump > /tmp/ironcore_ui.xml
    b=$(grep -o 'class="android.widget.EditText"[^>]*bounds="[^"]*"' /tmp/ironcore_ui.xml | sed -n "$(( $2 + 1 ))p" | grep -o '\[[0-9]*,[0-9]*\]\[[0-9]*,[0-9]*\]' | tr '[],' '   ' | awk '{print int(($1+$3)/2), int(($2+$4)/2)}')
    [ -z "$b" ] && { echo "NO FIELD $2"; exit 1; }
    "$ADB" shell input tap $b; sleep .3
    "$ADB" shell input keyevent 123; for i in 1 2 3 4 5 6; do "$ADB" shell input keyevent 67; done
    "$ADB" shell input text "$3"; sleep .2
    "$ADB" shell dumpsys input_method | grep -q "mInputShown=true" && "$ADB" shell input keyevent 4; sleep .3 ;;
  hidekb) "$ADB" shell dumpsys input_method | grep -q "mInputShown=true" && "$ADB" shell input keyevent 4; sleep .3 ;;
  swipeup) "$ADB" shell input swipe 540 1800 540 700 300 ;;
esac
