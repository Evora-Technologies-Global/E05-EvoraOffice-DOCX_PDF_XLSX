#!/usr/bin/env python3
"""Verify ELF64 PT_LOAD 16 KB (0x4000) page alignment on .so files, APKs, AABs, or directories."""

from __future__ import annotations

import argparse
import os
import struct
import sys
import zipfile
from pathlib import Path
from typing import Iterable, Tuple

ELF_MAGIC = b"\x7fELF"
ELFCLASS32 = 1
ELFCLASS64 = 2
PT_LOAD = 1
PAGE_16KB = 0x4000


def inspect_elf_bytes(data: bytes, label: str) -> Tuple[bool, list[str]]:
    """Inspect an ELF binary buffer for 16 KB PT_LOAD segment alignment."""
    if len(data) < 64 or data[:4] != ELF_MAGIC:
        return False, [f"{label}: not a valid ELF binary"]

    ei_class = data[4]
    ei_data = data[5]
    if ei_data not in (1, 2):
        return False, [f"{label}: unsupported ELF endianness ({ei_data})"]
    endian = "<" if ei_data == 1 else ">"

    if ei_class == ELFCLASS32:
        return False, [f"{label}: 32-bit ELF32 found (only 64-bit 16 KB aligned ABIs permitted)"]
    if ei_class != ELFCLASS64:
        return False, [f"{label}: unknown ELF class ({ei_class})"]

    e_phoff, = struct.unpack_from(f"{endian}Q", data, 32)
    e_phentsize, e_phnum = struct.unpack_from(f"{endian}HH", data, 54)
    if e_phoff == 0 or e_phnum == 0:
        return False, [f"{label}: missing ELF64 program headers"]

    bad_segments: list[str] = []
    load_count = 0
    for idx in range(e_phnum):
        offset = e_phoff + idx * e_phentsize
        if offset + 56 > len(data):
            return False, [f"{label}: truncated program header {idx}"]
        p_type, = struct.unpack_from(f"{endian}I", data, offset)
        if p_type == PT_LOAD:
            load_count += 1
            p_align, = struct.unpack_from(f"{endian}Q", data, offset + 48)
            if p_align < PAGE_16KB or (p_align % PAGE_16KB) != 0:
                bad_segments.append(f"PT_LOAD[{idx}] p_align=0x{p_align:x} (< 0x4000)")

    if load_count == 0:
        return False, [f"{label}: zero PT_LOAD segments"]
    if bad_segments:
        return False, [f"{label}: {', '.join(bad_segments)}"]
    return True, [f"OK 0x4000 ({load_count} PT_LOAD): {label}"]


def iter_targets(paths: Iterable[Path]) -> Iterable[Tuple[str, bytes]]:
    for target in paths:
        if not target.exists():
            raise FileNotFoundError(f"Path not found: {target}")
        if target.is_dir():
            for root, _, files in os.walk(target):
                for name in sorted(files):
                    if name.endswith(".so"):
                        file_path = Path(root) / name
                        yield str(file_path), file_path.read_bytes()
        elif target.suffix.lower() in {".apk", ".aab", ".aar", ".zip"}:
            with zipfile.ZipFile(target, "r") as zf:
                for info in sorted(zf.infolist(), key=lambda i: i.filename):
                    if info.filename.endswith(".so"):
                        yield f"{target}!/{info.filename}", zf.read(info.filename)
        elif target.name.endswith(".so"):
            yield str(target), target.read_bytes()


def main() -> int:
    parser = argparse.ArgumentParser(description="Verify 16 KB (0x4000) ELF alignment.")
    parser.add_argument("inputs", nargs="+", type=Path, help=".so, directory, .apk, .aab, or .aar")
    parser.add_argument("--allow-empty", action="store_true", help="Exit 0 if no .so files exist")
    args = parser.parse_args()

    total = 0
    failures = 0
    for label, payload in iter_targets(args.inputs):
        total += 1
        ok, messages = inspect_elf_bytes(payload, label)
        for msg in messages:
            print(msg)
        if not ok:
            failures += 1

    if total == 0:
        msg = "No .so files found in inputs."
        if args.allow_empty:
            print(f"WARN: {msg} (--allow-empty set)")
            return 0
        print(f"ERROR: {msg}", file=sys.stderr)
        return 1

    print(f"Summary: {total - failures}/{total} .so binaries passed 16 KB (0x4000) alignment.")
    return 0 if failures == 0 else 1


if __name__ == "__main__":
    sys.exit(main())
