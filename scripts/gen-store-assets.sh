#!/usr/bin/env bash
# Generate the Play Store submission graphics (512x512 icon + 1024x500 feature
# graphic) from a source image. The app's actual launcher icon stays the hand-drawn
# vector at app/src/main/res/drawable/ic_launcher.xml (referenced directly by the
# manifest) — this script only produces the PNG-only assets the Play Console
# requires, in the same Windows-95 grey/mine palette. Requires ImageMagick. No
# secrets. Re-runnable.
#
# Env (optional):
#   ICON_SRC - 1024x1024 source PNG (default: store-assets/icon-source.png; created
#              as a Win95-style mine placeholder if absent)
set -euo pipefail
ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
ASSETS="$ROOT/store-assets"
SRC="${ICON_SRC:-$ASSETS/icon-source.png}"
GREY='#C0C0C0'; LIGHT='#FFFFFF'; DARK='#808080'; BLACK='#000000'
FONT="$(fc-match -f '%{file}' 'DejaVu Sans:bold' 2>/dev/null || echo DejaVuSans-Bold)"
mkdir -p "$ASSETS"

# Win95-bezel mine icon placeholder (mirrors the raised-tile look of ic_launcher.xml):
# grey tile, white top/left highlight, dark bottom/right shadow, black mine (circle +
# 4 spikes) centered.
if [ ! -f "$SRC" ]; then
  convert -size 1024x1024 "xc:$GREY" \
    -fill "$LIGHT"  -draw "polygon 76,76 950,76 950,132 132,132 132,950 76,950" \
    -fill "$DARK"   -draw "polygon 950,76 950,950 76,950 76,894 894,894 894,76" \
    -fill none -stroke "$BLACK" -strokewidth 76 \
      -draw "line 512,246 512,778" \
      -draw "line 246,512 778,512" \
      -draw "line 322,322 702,702" \
      -draw "line 702,322 322,702" \
    -fill "$BLACK" -stroke none -draw "circle 512,512 512,304" \
    "$SRC"
fi

convert "$SRC" -resize 512x512 "$ASSETS/play-icon-512.png"

# Feature graphic (1024x500): a row of classic minesweeper cells (raised/flagged/
# sunken-numbered/exploded-mine) plus the game name, Win95 grey background.
FEAT="$ASSETS/play-feature-1024x500.png"
TMPD="$(mktemp -d)"; trap 'rm -rf "$TMPD"' EXIT
convert -size 1024x500 "xc:$GREY" "$FEAT"

# draw_cell x y size kind [label] [labelColor]
draw_cell() {
  local x=$1 y=$2 s=$3 kind=$4 label="${5:-}" lc="${6:-#000000}"
  local out="$TMPD/cell_${x}_${y}.png"
  local bevel=$((s/9))
  case "$kind" in
    raised)
      convert -size "${s}x${s}" "xc:$GREY" \
        -fill "$LIGHT" -draw "polygon 0,0 $s,0 $s,$bevel $bevel,$bevel $bevel,$s 0,$s" \
        -fill "$DARK"  -draw "polygon $s,0 $s,$s 0,$s 0,$((s-bevel)) $((s-bevel)),$((s-bevel)) $((s-bevel)),0" \
        "$out" ;;
    flag)
      convert -size "${s}x${s}" "xc:$GREY" \
        -fill "$LIGHT" -draw "polygon 0,0 $s,0 $s,$bevel $bevel,$bevel $bevel,$s 0,$s" \
        -fill "$DARK"  -draw "polygon $s,0 $s,$s 0,$s 0,$((s-bevel)) $((s-bevel)),$((s-bevel)) $((s-bevel)),0" \
        -fill "#000000" -draw "line $((s/2)),$((s*3/10)) $((s/2)),$((s*8/10))" \
        -fill "#C00000" -draw "polygon $((s/2)),$((s*25/100)) $((s/2)),$((s*5/10)) $((s*8/10)),$((s*37/100))" \
        "$out" ;;
    sunken)
      convert -size "${s}x${s}" "xc:#BDBDBD" \
        -fill "$DARK"  -draw "polygon 0,0 $s,0 $s,$bevel $bevel,$bevel $bevel,$s 0,$s" \
        -font "$FONT" -fill "$lc" -gravity center -pointsize $((s*55/100)) -annotate 0 "$label" \
        "$out" ;;
    mine)
      convert -size "${s}x${s}" "xc:#C00000" \
        -fill "$DARK"  -draw "polygon 0,0 $s,0 $s,$bevel $bevel,$bevel $bevel,$s 0,$s" \
        -fill "#000000" -draw "circle $((s/2)),$((s/2)) $((s/2)),$((s*30/100))" \
        "$out" ;;
  esac
  convert "$FEAT" "$out" -geometry "+${x}+${y}" -composite "$FEAT"
}

# A short diagonal band of tiles, top-left to bottom-right, evoking a real board.
sz=112
draw_cell 40  40  $sz raised
draw_cell 152 40  $sz sunken "" "#0000FF"
draw_cell 40  152 $sz sunken 1 "#0000FF"
draw_cell 152 152 $sz sunken 2 "#008000"
draw_cell 264 152 $sz flag
draw_cell 264 264 $sz mine
draw_cell 40  264 $sz sunken 1 "#0000FF"
draw_cell 152 264 $sz sunken 3 "#FF0000"

# Game title, Win95-style raised text (white drop offset behind black fill),
# right of the tile band.
convert "$FEAT" \
  -font "$FONT" -pointsize 100 -gravity West \
  -fill "$LIGHT" -annotate +422-3 "Démineur" \
  -fill "#000000" -annotate +420-5 "Démineur" \
  "$FEAT"

echo "Generated store-assets: play-icon-512.png, play-feature-1024x500.png (from $SRC)."
