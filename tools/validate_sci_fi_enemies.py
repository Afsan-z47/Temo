#!/usr/bin/env python3
"""Validate the sci-fi enemy sprite contracts consumed by UtopiaAssets."""

from __future__ import annotations

import json
from pathlib import Path

from PIL import Image, ImageChops


ROOT = Path(__file__).resolve().parent.parent
ASSET_ROOT = ROOT / "assets" / "utopia"
IDS = ("cave_slime", "bone_sentinel", "abyss_mage", "floor_warden")
DISPLAY_NAMES = {
    "cave_slime": "Recon Drone",
    "bone_sentinel": "Aegis Robot",
    "abyss_mage": "Helix Cyborg",
    "floor_warden": "Enhanced Warden",
}


def alpha_bbox(frame: Image.Image):
    return frame.getchannel("A").getbbox()


def assert_two_by_two_pixels(image: Image.Image, label: str) -> None:
    down = image.resize((image.width // 2, image.height // 2), Image.Resampling.NEAREST)
    restored = down.resize(image.size, Image.Resampling.NEAREST)
    if ImageChops.difference(image, restored).getbbox() is not None:
        raise AssertionError(f"{label} contains off-grid or smoothed pixels")


def check_sheet(path: Path, frame_size: tuple[int, int], rows: int,
                columns: int, used_per_row: tuple[int, ...]) -> None:
    with Image.open(path) as source:
        image = source.convert("RGBA")
    expected_size = (frame_size[0] * columns, frame_size[1] * rows)
    if image.size != expected_size:
        raise AssertionError(f"{path.name}: expected {expected_size}, got {image.size}")
    if source.mode != "RGBA":
        raise AssertionError(f"{path.name}: expected RGBA, got {source.mode}")
    assert_two_by_two_pixels(image, path.name)

    for row in range(rows):
        for column in range(columns):
            left = column * frame_size[0]
            top = row * frame_size[1]
            frame = image.crop((left, top, left + frame_size[0], top + frame_size[1]))
            occupied = alpha_bbox(frame) is not None
            should_be_occupied = column < used_per_row[row]
            if occupied != should_be_occupied:
                state = "occupied" if occupied else "empty"
                expected = "occupied" if should_be_occupied else "empty"
                raise AssertionError(
                    f"{path.name} row {row} column {column}: {state}, expected {expected}"
                )


def main() -> None:
    for enemy_id in IDS:
        check_sheet(
            ASSET_ROOT / "enemies" / "topdown" / f"{enemy_id}_topdown_64x96.png",
            (64, 96),
            4,
            4,
            (4, 4, 4, 4),
        )
        check_sheet(
            ASSET_ROOT / "enemies" / "battle" / f"{enemy_id}_battle_128x192.png",
            (128, 192),
            4,
            8,
            (4, 6, 3, 6),
        )

    manifest_path = ASSET_ROOT / "asset_manifest.json"
    manifest = json.loads(manifest_path.read_text(encoding="utf-8"))
    if manifest.get("version") != "1.2.0":
        raise AssertionError("asset_manifest.json version is not 1.2.0")
    for enemy_id, display_name in DISPLAY_NAMES.items():
        entry = manifest["enemies"][enemy_id]
        if entry.get("displayName") != display_name:
            raise AssertionError(f"manifest name mismatch for {enemy_id}")
        if entry.get("compatibilityId") != enemy_id:
            raise AssertionError(f"manifest compatibility ID mismatch for {enemy_id}")

    print("Sci-fi enemy validation passed: 8 exact-grid RGBA sheets, 140 occupied frames, and manifest metadata.")


if __name__ == "__main__":
    main()
