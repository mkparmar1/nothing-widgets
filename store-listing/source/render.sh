#!/usr/bin/env bash
# render.sh <page.html> <out.png> [width] [height] — headless Chrome screenshot of a local page.
CH="/c/Program Files/Google/Chrome/Application/chrome.exe"
html=$(cygpath -m "$(realpath "$1")")
out=$(cygpath -w "$(realpath -m "$2")")
w=${3:-1080}; h=${4:-1920}
timeout 90 "$CH" --headless=new --disable-gpu --hide-scrollbars --force-device-scale-factor=1 \
  --allow-file-access-from-files --virtual-time-budget=4000 \
  --screenshot="$out" --window-size="$w,$h" "file:///$html" >/dev/null 2>&1
[ -f "$2" ] && echo "rendered $(basename "$2")" || echo "FAILED $1"
