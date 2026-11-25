#!/usr/bin/env python3
"""Exhaustive WCAG 2.1 AA contrast verification for E05 Evora Office Design System."""

import sys


def hex_to_rgb(hex_color: str) -> tuple[int, int, int]:
    h = hex_color.lstrip("#")
    return tuple(int(h[i : i + 2], 16) for i in (0, 2, 4))


def relative_luminance(hex_color: str) -> float:
    def channel(c: int) -> float:
        s = c / 255.0
        return s / 12.92 if s <= 0.04045 else ((s + 0.055) / 1.055) ** 2.4

    r, g, b = hex_to_rgb(hex_color)
    return 0.2126 * channel(r) + 0.7152 * channel(g) + 0.0722 * channel(b)


def contrast_ratio(fg: str, bg: str) -> float:
    l1 = relative_luminance(fg)
    l2 = relative_luminance(bg)
    lighter, darker = max(l1, l2), min(l1, l2)
    return (lighter + 0.05) / (darker + 0.05)


TOKENS = {
    "light": {
        "bg_canvas": "#F2F4F8",
        "surface_card": "#FFFFFF",
        "surface_subtle": "#E9EEF5",
        "surface_elevated": "#FFFFFF",
        "text_primary": "#0F172A",
        "text_secondary": "#475569",
        "text_muted": "#526075",
        "border_subtle": "#CBD5E1",
        "border_input": "#7C8BA1",
        "primary_btn": "#186FD4",
        "on_primary_btn": "#FFFFFF",
        "brand_accent": "#2489FF",
        "primary_container": "#E6F1FF",
        "on_primary_container": "#0D4A9E",
        "success_fill": "#1E7D5E",
        "on_success_fill": "#FFFFFF",
        "success_container": "#E3F8F0",
        "on_success_container": "#125741",
        "warning_fill": "#B45309",
        "on_warning_fill": "#FFFFFF",
        "warning_container": "#FEF3C7",
        "on_warning_container": "#78350F",
        "error_fill": "#C82828",
        "on_error_fill": "#FFFFFF",
        "error_container": "#FEECEC",
        "on_error_container": "#8C1D1D",
        "pro_gold_container": "#FFF4DB",
        "on_pro_gold_container": "#7A4B00",
        # Format Solid Header (white text) & Badge Container (dark ink on pastel)
        "fmt_word_header": "#1563DB",
        "fmt_word_tint": "#EAF2FF",
        "fmt_word_ink": "#114FB3",
        "fmt_pdf_header": "#D33434",
        "fmt_pdf_tint": "#FFEBEB",
        "fmt_pdf_ink": "#A81E1E",
        "fmt_excel_header": "#1D820E",
        "fmt_excel_tint": "#E6F7E4",
        "fmt_excel_ink": "#146308",
        "fmt_ppt_header": "#B85600",
        "fmt_ppt_tint": "#FFF2E4",
        "fmt_ppt_ink": "#8F4100",
        "fmt_hwp_header": "#0074BF",
        "fmt_hwp_tint": "#E5F5FF",
        "fmt_hwp_ink": "#005894",
        "fmt_odf_header": "#8B1ED9",
        "fmt_odf_tint": "#F5EAFF",
        "fmt_odf_ink": "#6C14AD",
        "fmt_epub_header": "#5A7D0E",
        "fmt_epub_tint": "#F0F8DF",
        "fmt_epub_ink": "#446107",
        "fmt_txt_header": "#1474B0",
        "fmt_txt_tint": "#E6F5FF",
        "fmt_txt_ink": "#0D5887",
    },
    "dark": {
        "bg_canvas": "#101318",
        "surface_card": "#1A1E26",
        "surface_subtle": "#232934",
        "surface_elevated": "#252B36",
        "text_primary": "#F1F5F9",
        "text_secondary": "#B0BAC9",
        "text_muted": "#94A1B5",
        "border_subtle": "#333C4D",
        "border_input": "#68768C",
        "primary_btn": "#4DA1FF",
        "on_primary_btn": "#061D3D",
        "brand_accent": "#4DA1FF",
        "primary_container": "#183359",
        "on_primary_container": "#BFDBFE",
        "success_fill": "#46D8A9",
        "on_success_fill": "#04261B",
        "success_container": "#12382B",
        "on_success_container": "#86EFCD",
        "warning_fill": "#FBBF24",
        "on_warning_fill": "#2D1902",
        "warning_container": "#3B2809",
        "on_warning_container": "#FDE68A",
        "error_fill": "#FF7878",
        "on_error_fill": "#3B0A0A",
        "error_container": "#3F171A",
        "on_error_container": "#FECACA",
        "pro_gold_container": "#3A2B0D",
        "on_pro_gold_container": "#FCD34D",
        "fmt_word_header": "#162642",
        "fmt_word_tint": "#1B2E4F",
        "fmt_word_ink": "#89B7FF",
        "fmt_pdf_header": "#38181A",
        "fmt_pdf_tint": "#401C1F",
        "fmt_pdf_ink": "#FF9494",
        "fmt_excel_header": "#142B16",
        "fmt_excel_tint": "#17331A",
        "fmt_excel_ink": "#75E067",
        "fmt_ppt_header": "#36200D",
        "fmt_ppt_tint": "#3D2510",
        "fmt_ppt_ink": "#FFB067",
        "fmt_hwp_header": "#0F273B",
        "fmt_hwp_tint": "#133047",
        "fmt_hwp_ink": "#66C3FF",
        "fmt_odf_header": "#2B183E",
        "fmt_odf_tint": "#341D4A",
        "fmt_odf_ink": "#D599FF",
        "fmt_epub_header": "#222E10",
        "fmt_epub_tint": "#293814",
        "fmt_epub_ink": "#B5E34F",
        "fmt_txt_header": "#13293B",
        "fmt_txt_tint": "#173247",
        "fmt_txt_ink": "#72C8FF",
    },
    "reader": {
        "sepia_bg": "#F5ECD8",
        "sepia_text": "#3B2F20",
        "sepia_muted": "#6E5A42",
        "night_bg": "#0F1216",
        "night_text": "#E2E8F0",
        "night_muted": "#94A3B8",
    },
}


def run_checks() -> int:
    l = TOKENS["light"]
    d = TOKENS["dark"]
    r = TOKENS["reader"]

    checks: list[tuple[str, str, str, float]] = [
        # Light normal text (>= 4.5)
        ("L: text_primary on bg_canvas", l["text_primary"], l["bg_canvas"], 4.5),
        ("L: text_primary on surface_card", l["text_primary"], l["surface_card"], 4.5),
        ("L: text_secondary on surface_card", l["text_secondary"], l["surface_card"], 4.5),
        ("L: text_secondary on bg_canvas", l["text_secondary"], l["bg_canvas"], 4.5),
        ("L: text_muted on surface_card", l["text_muted"], l["surface_card"], 4.5),
        ("L: text_muted on bg_canvas", l["text_muted"], l["bg_canvas"], 4.5),
        ("L-UI: border_input on surface_card", l["border_input"], l["surface_card"], 3.0),
        ("L-UI: border_input on bg_canvas", l["border_input"], l["bg_canvas"], 3.0),
        ("L: on_primary_btn on primary_btn", l["on_primary_btn"], l["primary_btn"], 4.5),
        ("L: on_primary_container on primary_container", l["on_primary_container"], l["primary_container"], 4.5),
        ("L: primary_btn on surface_card (text CTA)", l["primary_btn"], l["surface_card"], 4.5),
        ("L: on_success_fill on success_fill", l["on_success_fill"], l["success_fill"], 4.5),
        ("L: on_success_container on success_container", l["on_success_container"], l["success_container"], 4.5),
        ("L: on_warning_fill on warning_fill", l["on_warning_fill"], l["warning_fill"], 4.5),
        ("L: on_warning_container on warning_container", l["on_warning_container"], l["warning_container"], 4.5),
        ("L: on_error_fill on error_fill", l["on_error_fill"], l["error_fill"], 4.5),
        ("L: on_error_container on error_container", l["on_error_container"], l["error_container"], 4.5),
        ("L: on_pro_gold on pro_gold_container", l["on_pro_gold_container"], l["pro_gold_container"], 4.5),
        # Light graphic UI (>= 3.0)
        ("L-UI: brand_accent on surface_card", l["brand_accent"], l["surface_card"], 3.0),
        ("L-UI: brand_accent on bg_canvas", l["brand_accent"], l["bg_canvas"], 3.0),
        ("L-UI: white on brand_accent (FAB icon)", "#FFFFFF", l["brand_accent"], 3.0),
        # Light format headers & badges (>= 4.5)
        ("L-Fmt: white on Word header", "#FFFFFF", l["fmt_word_header"], 4.5),
        ("L-Fmt: Word ink on Word tint", l["fmt_word_ink"], l["fmt_word_tint"], 4.5),
        ("L-Fmt: white on PDF header", "#FFFFFF", l["fmt_pdf_header"], 4.5),
        ("L-Fmt: PDF ink on PDF tint", l["fmt_pdf_ink"], l["fmt_pdf_tint"], 4.5),
        ("L-Fmt: white on Excel header", "#FFFFFF", l["fmt_excel_header"], 4.5),
        ("L-Fmt: Excel ink on Excel tint", l["fmt_excel_ink"], l["fmt_excel_tint"], 4.5),
        ("L-Fmt: white on PPT header", "#FFFFFF", l["fmt_ppt_header"], 4.5),
        ("L-Fmt: PPT ink on PPT tint", l["fmt_ppt_ink"], l["fmt_ppt_tint"], 4.5),
        ("L-Fmt: white on HWP header", "#FFFFFF", l["fmt_hwp_header"], 4.5),
        ("L-Fmt: HWP ink on HWP tint", l["fmt_hwp_ink"], l["fmt_hwp_tint"], 4.5),
        ("L-Fmt: white on ODF header", "#FFFFFF", l["fmt_odf_header"], 4.5),
        ("L-Fmt: ODF ink on ODF tint", l["fmt_odf_ink"], l["fmt_odf_tint"], 4.5),
        ("L-Fmt: white on EPUB header", "#FFFFFF", l["fmt_epub_header"], 4.5),
        ("L-Fmt: EPUB ink on EPUB tint", l["fmt_epub_ink"], l["fmt_epub_tint"], 4.5),
        ("L-Fmt: white on TXT header", "#FFFFFF", l["fmt_txt_header"], 4.5),
        ("L-Fmt: TXT ink on TXT tint", l["fmt_txt_ink"], l["fmt_txt_tint"], 4.5),
        # Dark normal text (>= 4.5)
        ("D: text_primary on bg_canvas", d["text_primary"], d["bg_canvas"], 4.5),
        ("D: text_primary on surface_card", d["text_primary"], d["surface_card"], 4.5),
        ("D: text_secondary on surface_card", d["text_secondary"], d["surface_card"], 4.5),
        ("D: text_secondary on bg_canvas", d["text_secondary"], d["bg_canvas"], 4.5),
        ("D: text_muted on surface_card", d["text_muted"], d["surface_card"], 4.5),
        ("D: text_muted on bg_canvas", d["text_muted"], d["bg_canvas"], 4.5),
        ("D-UI: border_input on surface_card", d["border_input"], d["surface_card"], 3.0),
        ("D-UI: border_input on bg_canvas", d["border_input"], d["bg_canvas"], 3.0),
        ("D: on_primary_btn on primary_btn", d["on_primary_btn"], d["primary_btn"], 4.5),
        ("D: on_primary_container on primary_container", d["on_primary_container"], d["primary_container"], 4.5),
        ("D: primary_btn on surface_card (text CTA)", d["primary_btn"], d["surface_card"], 4.5),
        ("D: on_success_fill on success_fill", d["on_success_fill"], d["success_fill"], 4.5),
        ("D: on_success_container on success_container", d["on_success_container"], d["success_container"], 4.5),
        ("D: on_warning_fill on warning_fill", d["on_warning_fill"], d["warning_fill"], 4.5),
        ("D: on_warning_container on warning_container", d["on_warning_container"], d["warning_container"], 4.5),
        ("D: on_error_fill on error_fill", d["on_error_fill"], d["error_fill"], 4.5),
        ("D: on_error_container on error_container", d["on_error_container"], d["error_container"], 4.5),
        ("D: on_pro_gold on pro_gold_container", d["on_pro_gold_container"], d["pro_gold_container"], 4.5),
        # Dark format headers & badges (>= 4.5)
        ("D-Fmt: text_primary on Word header", d["text_primary"], d["fmt_word_header"], 4.5),
        ("D-Fmt: Word ink on Word tint", d["fmt_word_ink"], d["fmt_word_tint"], 4.5),
        ("D-Fmt: text_primary on PDF header", d["text_primary"], d["fmt_pdf_header"], 4.5),
        ("D-Fmt: PDF ink on PDF tint", d["fmt_pdf_ink"], d["fmt_pdf_tint"], 4.5),
        ("D-Fmt: text_primary on Excel header", d["text_primary"], d["fmt_excel_header"], 4.5),
        ("D-Fmt: Excel ink on Excel tint", d["fmt_excel_ink"], d["fmt_excel_tint"], 4.5),
        ("D-Fmt: text_primary on PPT header", d["text_primary"], d["fmt_ppt_header"], 4.5),
        ("D-Fmt: PPT ink on PPT tint", d["fmt_ppt_ink"], d["fmt_ppt_tint"], 4.5),
        ("D-Fmt: text_primary on HWP header", d["text_primary"], d["fmt_hwp_header"], 4.5),
        ("D-Fmt: HWP ink on HWP tint", d["fmt_hwp_ink"], d["fmt_hwp_tint"], 4.5),
        ("D-Fmt: text_primary on ODF header", d["text_primary"], d["fmt_odf_header"], 4.5),
        ("D-Fmt: ODF ink on ODF tint", d["fmt_odf_ink"], d["fmt_odf_tint"], 4.5),
        ("D-Fmt: text_primary on EPUB header", d["text_primary"], d["fmt_epub_header"], 4.5),
        ("D-Fmt: EPUB ink on EPUB tint", d["fmt_epub_ink"], d["fmt_epub_tint"], 4.5),
        ("D-Fmt: text_primary on TXT header", d["text_primary"], d["fmt_txt_header"], 4.5),
        ("D-Fmt: TXT ink on TXT tint", d["fmt_txt_ink"], d["fmt_txt_tint"], 4.5),
        # Reader Sepia & Night
        ("Reader: sepia_text on sepia_bg", r["sepia_text"], r["sepia_bg"], 4.5),
        ("Reader: sepia_muted on sepia_bg", r["sepia_muted"], r["sepia_bg"], 4.5),
        ("Reader: night_text on night_bg", r["night_text"], r["night_bg"], 4.5),
        ("Reader: night_muted on night_bg", r["night_muted"], r["night_bg"], 4.5),
    ]

    failures = 0
    for label, fg, bg, req in checks:
        cr = contrast_ratio(fg, bg)
        ok = cr >= req
        if not ok:
            failures += 1
        mark = "PASS" if ok else "FAIL"
        print(f"[{mark}] {label:45s} {fg} on {bg} = {cr:5.2f}:1 (min {req}:1)")

    print(f"\nSummary: {len(checks) - failures}/{len(checks)} passed, {failures} failed.")
    return 1 if failures else 0


if __name__ == "__main__":
    sys.exit(run_checks())
