#!/usr/bin/env python3
"""Generate the engine-ready utopian sci-fi enemy animation sheets.

The renderer expects the legacy enum/file identifiers, so this script keeps
those paths stable while replacing their artwork with four new designs:

* cave_slime    -> hovering Recon Drone
* bone_sentinel -> humanoid Aegis Robot
* abyss_mage    -> human Helix Cyborg
* floor_warden  -> human Enhanced Warden

Every sheet is drawn on a half-resolution pixel grid and enlarged with nearest
neighbour sampling.  This keeps the exact libGDX frame contracts while avoiding
anti-aliased or off-grid pixels.
"""

from __future__ import annotations

import json
from pathlib import Path

from PIL import Image, ImageDraw, ImageFont


PROJECT_ROOT = Path(__file__).resolve().parent.parent
ASSET_ROOT = PROJECT_ROOT / "assets" / "utopia"
TOPDOWN_DIR = ASSET_ROOT / "enemies" / "topdown"
BATTLE_DIR = ASSET_ROOT / "enemies" / "battle"
PREVIEW_DIR = PROJECT_ROOT / "previews"


P = {
    "transparent": (0, 0, 0, 0),
    "white": (246, 244, 236, 255),
    "off_white": (222, 221, 212, 255),
    "light": (195, 202, 208, 255),
    "mid": (143, 154, 165, 255),
    "dark": (59, 70, 82, 255),
    "ink": (29, 37, 46, 255),
    "void": (15, 22, 30, 255),
    "red": (224, 55, 49, 255),
    "red_light": (247, 101, 89, 255),
    "red_dark": (163, 38, 35, 255),
    "blue": (33, 112, 208, 255),
    "blue_light": (91, 183, 244, 255),
    "blue_glow": (146, 221, 255, 255),
    "skin_light": (235, 181, 142, 255),
    "skin_tan": (190, 124, 82, 255),
    "skin_shadow": (139, 84, 58, 255),
    "hair": (27, 28, 31, 255),
    "shadow": (26, 33, 40, 105),
}


ENEMIES = [
    {
        "id": "cave_slime",
        "displayName": "Recon Drone",
        "archetype": "aerial combat drone",
        "kind": "drone",
    },
    {
        "id": "bone_sentinel",
        "displayName": "Aegis Robot",
        "archetype": "humanoid security robot",
        "kind": "robot",
    },
    {
        "id": "abyss_mage",
        "displayName": "Helix Cyborg",
        "archetype": "human cyborg operative",
        "kind": "cyborg",
    },
    {
        "id": "floor_warden",
        "displayName": "Enhanced Warden",
        "archetype": "enhanced human elite",
        "kind": "warden",
    },
]


def save(image: Image.Image, path: Path) -> None:
    path.parent.mkdir(parents=True, exist_ok=True)
    image.save(path, optimize=True)


def upscale(image: Image.Image) -> Image.Image:
    return image.resize((image.width * 2, image.height * 2), Image.Resampling.NEAREST)


def outlined_rect(draw: ImageDraw.ImageDraw, box, fill, outline=P["ink"], inset=1) -> None:
    draw.rectangle(box, fill=outline)
    x0, y0, x1, y1 = box
    if x1 - x0 > inset * 2 and y1 - y0 > inset * 2:
        draw.rectangle((x0 + inset, y0 + inset, x1 - inset, y1 - inset), fill=fill)


def outlined_ellipse(draw: ImageDraw.ImageDraw, box, fill, outline=P["ink"], inset=1) -> None:
    draw.ellipse(box, fill=outline)
    x0, y0, x1, y1 = box
    if x1 - x0 > inset * 2 and y1 - y0 > inset * 2:
        draw.ellipse((x0 + inset, y0 + inset, x1 - inset, y1 - inset), fill=fill)


def outlined_polygon(draw: ImageDraw.ImageDraw, points, fill, outline=P["ink"]) -> None:
    draw.polygon(points, fill=outline)
    cx = sum(x for x, _ in points) / len(points)
    cy = sum(y for _, y in points) / len(points)
    inner = []
    for x, y in points:
        inner.append((int(round(x + (cx - x) * 0.14)), int(round(y + (cy - y) * 0.14))))
    draw.polygon(inner, fill=fill)


def tint_alpha(image: Image.Image, color, alpha: int) -> Image.Image:
    overlay = Image.new("RGBA", image.size, (*color[:3], alpha))
    mask = image.getchannel("A")
    overlay.putalpha(Image.eval(mask, lambda a: min(a, alpha)))
    return Image.alpha_composite(image, overlay)


# ---------------------------------------------------------------------------
# Top-down exploration sprites (base frame 32x48, exported frame 64x96)


def draw_topdown_drone(direction: str, frame: int) -> Image.Image:
    im = Image.new("RGBA", (32, 48), P["transparent"])
    d = ImageDraw.Draw(im)
    bob = [0, -1, 0, 1][frame % 4]
    cx = 16
    y = 19 + bob
    d.ellipse((7, 37, 25, 41), fill=P["shadow"])

    # Rear stabilisers and compact graphite propulsion ring.
    if direction in {"down", "up"}:
        d.polygon([(8, y + 5), (2, y + 2), (5, y + 8)], fill=P["red"])
        d.polygon([(24, y + 5), (30, y + 2), (27, y + 8)], fill=P["red"])
        d.rectangle((7, y + 9, 25, y + 14), fill=P["ink"])
        outlined_ellipse(d, (5, y, 27, y + 14), P["off_white"])
        d.rectangle((9, y + 1, 23, y + 4), fill=P["white"])
        if direction == "down":
            d.rectangle((10, y + 7, 22, y + 10), fill=P["ink"])
            d.rectangle((12, y + 7, 20, y + 8), fill=P["blue_light"])
            d.rectangle((15, y + 8, 17, y + 10), fill=P["blue_glow"])
        else:
            d.rectangle((11, y + 3, 21, y + 5), fill=P["light"])
            d.rectangle((14, y + 12, 18, y + 14), fill=P["blue"])
    else:
        # Draw right-facing, then mirror for left.
        d.polygon([(9, y + 3), (3, y), (6, y + 7)], fill=P["red"])
        d.polygon([(10, y + 12), (4, y + 14), (7, y + 8)], fill=P["red_dark"])
        d.rectangle((8, y + 9, 24, y + 14), fill=P["ink"])
        outlined_ellipse(d, (6, y, 27, y + 14), P["off_white"])
        d.polygon([(9, y + 1), (23, y + 2), (26, y + 5), (11, y + 5)], fill=P["white"])
        outlined_ellipse(d, (21, y + 4, 28, y + 11), P["blue_light"])
        d.rectangle((23, y + 6, 27, y + 9), fill=P["blue_glow"])
        if direction == "left":
            im = im.transpose(Image.Transpose.FLIP_LEFT_RIGHT)
            d = ImageDraw.Draw(im)

    # Hover jets animate beneath the shell.
    jet = 2 + (frame % 2)
    d.rectangle((10, y + 15, 12, y + 15 + jet), fill=P["blue"])
    d.rectangle((20, y + 15, 22, y + 15 + jet), fill=P["blue_light"])
    return upscale(im)


def humanoid_step(frame: int) -> tuple[int, int]:
    steps = [(0, 0), (-1, 1), (0, 0), (1, -1)]
    return steps[frame % 4]


def draw_topdown_humanoid(kind: str, direction: str, frame: int) -> Image.Image:
    im = Image.new("RGBA", (32, 48), P["transparent"])
    d = ImageDraw.Draw(im)
    left_step, right_step = humanoid_step(frame)
    bob = 1 if frame % 2 else 0
    x = 16
    d.ellipse((8, 42, 24, 45), fill=P["shadow"])

    if direction == "left":
        right_facing = False
        direction = "right"
    else:
        right_facing = True

    # Legs and boots.
    d.line((x - 4, 31 + bob, x - 5 + left_step, 41), fill=P["ink"], width=5)
    d.line((x + 4, 31 + bob, x + 5 + right_step, 41), fill=P["ink"], width=5)
    d.line((x - 4, 32 + bob, x - 5 + left_step, 39), fill=P["dark"], width=2)
    d.line((x + 4, 32 + bob, x + 5 + right_step, 39), fill=P["dark"], width=2)
    d.rectangle((x - 8 + left_step, 40, x - 2 + left_step, 43), fill=P["ink"])
    d.rectangle((x + 2 + right_step, 40, x + 8 + right_step, 43), fill=P["ink"])

    coat = kind == "cyborg"
    broad = kind == "warden"
    torso_left = x - (7 if broad else 6)
    torso_right = x + (7 if broad else 6)
    if coat:
        d.polygon([(x - 7, 25), (x + 6, 25), (x + 8, 38), (x + 1, 35),
                   (x - 6, 39), (x - 8, 31)], fill=P["ink"])
        d.polygon([(x - 5, 25), (x + 4, 25), (x + 5, 36), (x, 33),
                   (x - 4, 36), (x - 6, 30)], fill=P["off_white"])
    outlined_polygon(d, [(torso_left, 20 + bob), (torso_right, 20 + bob),
                         (x + 6, 32 + bob), (x - 6, 32 + bob)], P["off_white"])
    d.polygon([(x - 4, 21 + bob), (x + 4, 21 + bob), (x + 3, 30 + bob),
               (x - 3, 30 + bob)], fill=P["white"])

    if kind == "robot":
        d.rectangle((x - 5, 25 + bob, x + 5, 29 + bob), fill=P["dark"])
        d.rectangle((x - 1, 25 + bob, x + 1, 29 + bob), fill=P["blue"])
    elif kind == "cyborg":
        d.line((x, 21 + bob, x, 32 + bob), fill=P["blue"], width=1)
        d.rectangle((x - 6, 25 + bob, x - 4, 31 + bob), fill=P["dark"])
    else:
        d.rectangle((x - 6, 23 + bob, x + 6, 25 + bob), fill=P["red"])
        d.rectangle((x - 2, 22 + bob, x + 2, 25 + bob), fill=P["white"])

    # Arms make the categories readable at exploration scale.
    skin = P["skin_tan"] if kind == "warden" else P["skin_light"]
    left_arm = P["light"] if kind == "robot" else (P["dark"] if kind == "cyborg" else skin)
    right_arm = P["light"] if kind == "robot" else skin
    d.line((torso_left + 1, 22 + bob, x - 9, 32 + bob), fill=P["ink"], width=5)
    d.line((torso_right - 1, 22 + bob, x + 9, 32 + bob), fill=P["ink"], width=5)
    d.line((torso_left, 23 + bob, x - 9, 31 + bob), fill=left_arm, width=3)
    d.line((torso_right, 23 + bob, x + 9, 31 + bob), fill=right_arm, width=3)

    # Head: robot visor versus visible human face/hair.
    if kind == "robot":
        outlined_rect(d, (x - 6, 8 + bob, x + 6, 20 + bob), P["off_white"])
        d.rectangle((x - 5, 12 + bob, x + 5, 16 + bob), fill=P["ink"])
        d.rectangle((x - 3, 12 + bob, x + 4, 13 + bob), fill=P["blue_light"])
        d.rectangle((x + 4, 14 + bob, x + 6, 17 + bob), fill=P["red"])
        d.rectangle((x - 4, 9 + bob, x + 4, 10 + bob), fill=P["white"])
    else:
        outlined_ellipse(d, (x - 5, 8 + bob, x + 5, 20 + bob), skin)
        d.rectangle((x - 5, 8 + bob, x + 5, 12 + bob), fill=P["hair"])
        if direction == "down":
            d.point((x - 2, 14 + bob), fill=P["ink"])
            d.point((x + 2, 14 + bob), fill=P["blue_light"] if kind == "cyborg" else P["ink"])
            if kind == "cyborg":
                d.rectangle((x + 2, 13 + bob, x + 5, 15 + bob), fill=P["dark"])
                d.point((x + 3, 14 + bob), fill=P["blue_glow"])
        elif direction == "up":
            d.rectangle((x - 4, 10 + bob, x + 4, 15 + bob), fill=P["hair"])
            if kind == "cyborg":
                d.rectangle((x + 3, 13 + bob, x + 5, 18 + bob), fill=P["dark"])
        else:
            d.point((x + 4, 14 + bob), fill=P["blue_light"] if kind == "cyborg" else P["ink"])

    if kind == "robot":
        d.rectangle((x + 7, 28 + bob, x + 12, 33 + bob), fill=P["red"])
        d.rectangle((x + 10, 29 + bob, x + 13, 31 + bob), fill=P["red_light"])
    elif kind == "cyborg":
        d.rectangle((x - 10, 28 + bob, x - 7, 33 + bob), fill=P["blue"])
        d.point((x - 8, 29 + bob), fill=P["blue_glow"])
    else:
        d.line((x + 9, 30 + bob, x + 14, 21 + bob), fill=P["red_dark"], width=3)
        d.line((x + 10, 29 + bob, x + 14, 21 + bob), fill=P["red_light"], width=1)

    if not right_facing:
        im = im.transpose(Image.Transpose.FLIP_LEFT_RIGHT)
    return upscale(im)


def make_topdown_sheet(enemy: dict) -> Image.Image:
    sheet = Image.new("RGBA", (4 * 64, 4 * 96), P["transparent"])
    for row, direction in enumerate(["down", "left", "right", "up"]):
        for frame in range(4):
            if enemy["kind"] == "drone":
                sprite = draw_topdown_drone(direction, frame)
            else:
                sprite = draw_topdown_humanoid(enemy["kind"], direction, frame)
            sheet.alpha_composite(sprite, (frame * 64, row * 96))
    return sheet


# ---------------------------------------------------------------------------
# Side-view battle sprites (base frame 64x96, exported frame 128x192)


def battle_motion(animation: str, frame: int) -> dict:
    pose = {"x": 0, "y": 0, "lean": 0, "arm": "idle", "step": 0, "state": "stand", "effect": 0}
    if animation == "idle":
        pose["y"] = [0, -1, -1, 0][frame]
    elif animation == "attack":
        pose["x"] = [0, 1, 4, 8, 10, 6][frame]
        pose["lean"] = [0, 0, 1, 3, 2, 0][frame]
        pose["arm"] = "attack"
        pose["step"] = [0, 0, 1, 2, 2, 1][frame]
        pose["effect"] = frame
    elif animation == "hurt":
        pose["x"] = [0, -4, -6][frame]
        pose["lean"] = [0, -3, -5][frame]
        pose["arm"] = "hurt"
        pose["effect"] = frame
    elif animation == "defeat":
        if frame < 2:
            pose["x"] = -frame * 2
            pose["lean"] = -frame * 4
            pose["arm"] = "hurt"
        elif frame < 4:
            pose["state"] = "kneel"
            pose["y"] = 5 + (frame - 2) * 5
            pose["arm"] = "hurt"
        else:
            pose["state"] = "prone"
            pose["effect"] = frame
    return pose


def draw_battle_drone(animation: str, frame: int) -> Image.Image:
    im = Image.new("RGBA", (64, 96), P["transparent"])
    d = ImageDraw.Draw(im)
    pose = battle_motion(animation, frame)

    if animation == "defeat" and frame >= 4:
        d.ellipse((10, 83, 50, 89), fill=P["shadow"])
        d.polygon([(17, 78), (30, 74), (44, 81), (38, 87), (18, 86)], fill=P["ink"])
        d.polygon([(19, 78), (30, 76), (39, 81), (36, 84), (20, 84)], fill=P["off_white"])
        d.rectangle((29, 78, 35, 82), fill=P["blue"] if frame == 4 else P["dark"])
        d.line((15, 80, 8, 74), fill=P["red"], width=2)
        if frame == 4:
            d.point((45, 76), fill=P["red_light"])
            d.point((49, 73), fill=P["blue_light"])
        return upscale(im)

    bob = [0, -1, -1, 0][frame % 4]
    x = 28 + pose["x"]
    y = 42 + bob + pose["y"]
    if animation == "hurt":
        y += frame * 2
    d.ellipse((x - 20, 82, x + 22, 89), fill=P["shadow"])
    d.polygon([(x - 14, y + 7), (x - 25, y + 2), (x - 20, y + 13)], fill=P["red"])
    d.polygon([(x + 10, y + 12), (x + 20, y + 17), (x + 16, y + 8)], fill=P["red_dark"])
    d.rectangle((x - 13, y + 11, x + 15, y + 22), fill=P["ink"])
    outlined_ellipse(d, (x - 17, y, x + 19, y + 23), P["off_white"])
    d.polygon([(x - 11, y + 2), (x + 12, y + 3), (x + 17, y + 8),
               (x - 8, y + 8)], fill=P["white"])
    outlined_ellipse(d, (x + 10, y + 7, x + 23, y + 19), P["blue"])
    d.rectangle((x + 14, y + 10, x + 21, y + 15), fill=P["blue_glow"])
    d.rectangle((x - 8, y + 20, x - 3, y + 25 + (frame % 2)), fill=P["blue"])
    d.rectangle((x + 6, y + 21, x + 10, y + 25 + ((frame + 1) % 2)), fill=P["blue_light"])
    d.rectangle((x - 3, y + 5, x + 4, y + 7), fill=P["light"])

    if animation == "attack":
        barrel_x = min(60, x + 23)
        d.rectangle((barrel_x - 1, y + 12, min(63, barrel_x + 7), y + 15), fill=P["ink"])
        d.rectangle((barrel_x + 1, y + 12, min(63, barrel_x + 7), y + 13), fill=P["red"])
        if frame in {3, 4}:
            d.rectangle((min(63, barrel_x + 7), y + 11, min(63, barrel_x + 12), y + 15), fill=P["red_light"])
            d.point((min(63, barrel_x + 14), y + 13), fill=P["white"])
    if animation == "hurt" and frame == 1:
        im = tint_alpha(im, P["red"], 90)
    return upscale(im)


def draw_prone_humanoid(kind: str, frame: int) -> Image.Image:
    im = Image.new("RGBA", (64, 96), P["transparent"])
    d = ImageDraw.Draw(im)
    skin = P["skin_tan"] if kind == "warden" else P["skin_light"]
    d.ellipse((9, 83, 55, 90), fill=P["shadow"])
    outlined_polygon(d, [(12, 75), (39, 72), (52, 79), (46, 87), (18, 86), (7, 82)], P["off_white"])
    if kind == "robot":
        outlined_rect(d, (45, 73, 57, 85), P["off_white"])
        d.rectangle((49, 77, 56, 79), fill=P["blue"] if frame == 4 else P["dark"])
        d.line((18, 83, 8, 88), fill=P["dark"], width=5)
    else:
        outlined_ellipse(d, (45, 73, 57, 85), skin)
        d.rectangle((48, 73, 56, 76), fill=P["hair"])
        if kind == "cyborg":
            d.point((54, 78), fill=P["blue_light"] if frame == 4 else P["dark"])
        d.line((18, 83, 8, 88), fill=P["dark"], width=5)
    if kind == "warden":
        d.line((13, 77, 2, 88), fill=P["red_dark"], width=3)
        d.line((14, 77, 2, 88), fill=P["red_light"] if frame == 4 else P["red_dark"], width=1)
    return upscale(im)


def draw_battle_humanoid(kind: str, animation: str, frame: int) -> Image.Image:
    pose = battle_motion(animation, frame)
    if pose["state"] == "prone":
        return draw_prone_humanoid(kind, frame)

    im = Image.new("RGBA", (64, 96), P["transparent"])
    d = ImageDraw.Draw(im)
    x = 28 + pose["x"]
    y = pose["y"]
    crouch = 7 if pose["state"] == "kneel" else 0
    lean = pose["lean"]
    broad = 4 if kind == "warden" else 0
    skin = P["skin_tan"] if kind == "warden" else P["skin_light"]

    d.ellipse((x - 17, 84, x + 19, 90), fill=P["shadow"])
    hip = (x, 57 + y + crouch)
    back_knee = (x - 6 - pose["step"], 70 + y + crouch)
    back_foot = (x - 8, 85 + y)
    front_knee = (x + 6 + pose["step"], 69 + y + crouch)
    front_foot = (x + 12 + pose["step"], 85 + y)
    for points in ([hip, back_knee, back_foot], [hip, front_knee, front_foot]):
        d.line(points, fill=P["ink"], width=8, joint="curve")
        d.line(points, fill=P["dark"], width=5, joint="curve")
    d.rectangle((back_foot[0] - 3, back_foot[1] - 2, back_foot[0] + 6, back_foot[1] + 2), fill=P["ink"])
    d.rectangle((front_foot[0] - 3, front_foot[1] - 2, front_foot[0] + 7, front_foot[1] + 2), fill=P["ink"])
    if kind in {"robot", "warden"}:
        d.rectangle((back_knee[0] - 3, back_knee[1] - 2, back_knee[0] + 3, back_knee[1] + 2), fill=P["light"])
        d.rectangle((front_knee[0] - 3, front_knee[1] - 2, front_knee[0] + 3, front_knee[1] + 2), fill=P["off_white"])

    # Cyborg coat tails, clearly distinct from the armored silhouettes.
    if kind == "cyborg":
        d.polygon([(x - 10, 43 + y), (x + 7, 44 + y), (x + 5, 74 + y + crouch),
                   (x - 2, 66 + y), (x - 11, 72 + y + crouch)], fill=P["ink"])
        d.polygon([(x - 8, 44 + y), (x + 5, 45 + y), (x + 3, 70 + y + crouch),
                   (x - 2, 63 + y), (x - 9, 68 + y + crouch)], fill=P["off_white"])
        d.line((x - 5, 48 + y, x - 5, 67 + y), fill=P["blue"], width=2)

    torso = [(x - 10 - broad // 2, 31 + y), (x + 8 + broad // 2, 30 + y + lean),
             (x + 10 + broad // 2, 56 + y + crouch), (x - 8 - broad // 2, 58 + y + crouch)]
    outlined_polygon(d, torso, P["off_white"])
    d.polygon([(x - 7, 34 + y), (x + 5, 33 + y + lean), (x + 7, 52 + y + crouch),
               (x - 6, 54 + y + crouch)], fill=P["white"])
    d.rectangle((x - 8, 53 + y + crouch, x + 9, 58 + y + crouch), fill=P["ink"])

    if kind == "robot":
        d.rectangle((x - 5, 38 + y, x + 6, 49 + y), fill=P["dark"])
        d.rectangle((x - 1, 39 + y, x + 2, 47 + y), fill=P["blue"])
        d.polygon([(x - 12, 31 + y), (x - 5, 27 + y), (x, 34 + y), (x - 8, 39 + y)], fill=P["white"])
        d.polygon([(x + 4, 29 + y), (x + 12, 32 + y), (x + 8, 39 + y), (x + 1, 34 + y)], fill=P["off_white"])
    elif kind == "cyborg":
        d.line((x - 1, 34 + y, x + 1, 54 + y + crouch), fill=P["blue"], width=2)
        d.rectangle((x - 8, 39 + y, x - 5, 52 + y), fill=P["dark"])
    else:
        d.rectangle((x - 7, 38 + y, x + 7, 43 + y), fill=P["light"])
        d.line((x - 5, 36 + y, x + 5, 52 + y), fill=P["red"], width=2)
        d.polygon([(x - 13, 31 + y), (x - 5, 27 + y), (x + 1, 34 + y), (x - 8, 40 + y)], fill=P["white"])
        d.polygon([(x + 4, 28 + y), (x + 13, 32 + y), (x + 8, 40 + y), (x + 1, 34 + y)], fill=P["off_white"])

    # Head and face.
    head_x = x + 1 + lean // 2
    if kind == "robot":
        outlined_polygon(d, [(head_x - 7, 16 + y), (head_x + 5, 15 + y),
                             (head_x + 9, 20 + y), (head_x + 7, 31 + y),
                             (head_x - 5, 31 + y), (head_x - 8, 24 + y)], P["off_white"])
        d.rectangle((head_x - 5, 20 + y, head_x + 7, 25 + y), fill=P["ink"])
        d.rectangle((head_x - 2, 20 + y, head_x + 6, 22 + y), fill=P["blue_light"])
        d.rectangle((head_x + 6, 24 + y, head_x + 8, 27 + y), fill=P["red"])
        d.rectangle((head_x - 4, 17 + y, head_x + 4, 18 + y), fill=P["white"])
    else:
        outlined_polygon(d, [(head_x - 6, 17 + y), (head_x + 5, 16 + y),
                             (head_x + 8, 21 + y), (head_x + 7, 28 + y),
                             (head_x + 3, 32 + y), (head_x - 4, 30 + y),
                             (head_x - 7, 24 + y)], skin)
        d.polygon([(head_x - 6, 17 + y), (head_x + 5, 15 + y), (head_x + 8, 20 + y),
                   (head_x + 2, 20 + y), (head_x - 3, 23 + y), (head_x - 7, 22 + y)], fill=P["hair"])
        d.point((head_x + 5, 23 + y), fill=P["blue_light"] if kind == "cyborg" else P["ink"])
        d.line((head_x + 6, 27 + y, head_x + 8, 27 + y), fill=P["skin_shadow"])
        if kind == "cyborg":
            d.rectangle((head_x + 4, 21 + y, head_x + 8, 25 + y), fill=P["dark"])
            d.point((head_x + 6, 22 + y), fill=P["blue_glow"])
            d.line((head_x + 7, 24 + y, head_x + 9, 29 + y), fill=P["mid"])
        else:
            d.rectangle((head_x - 7, 18 + y, head_x - 5, 25 + y), fill=P["dark"])
            d.point((head_x - 6, 21 + y), fill=P["blue"])

    shoulder = (x + 6, 35 + y)
    rear_shoulder = (x - 5, 36 + y)
    if pose["arm"] == "attack":
        elbow, hand = (x + 16, 41 + y), (x + 26, 35 + y)
    elif pose["arm"] == "hurt":
        elbow, hand = (x - 1, 44 + y), (x - 8, 51 + y)
    else:
        elbow, hand = (x + 12, 43 + y), (x + 10, 55 + y + crouch)

    front_limb = P["light"] if kind == "robot" else (P["dark"] if kind == "cyborg" else skin)
    rear_limb = P["light"] if kind == "robot" else skin
    d.line([shoulder, elbow, hand], fill=P["ink"], width=8, joint="curve")
    d.line([shoulder, elbow], fill=P["off_white"], width=5, joint="curve")
    d.line([elbow, hand], fill=front_limb, width=4, joint="curve")
    d.line([rear_shoulder, (x - 12, 47 + y), (x - 8, 56 + y)], fill=P["ink"], width=7, joint="curve")
    d.line([rear_shoulder, (x - 12, 47 + y)], fill=P["off_white"], width=4, joint="curve")
    d.line([(x - 12, 47 + y), (x - 8, 56 + y)], fill=rear_limb, width=3, joint="curve")

    # Unique weapons/effects.
    if kind == "robot":
        d.rectangle((hand[0] - 4, hand[1] - 4, hand[0] + 7, hand[1] + 5), fill=P["ink"])
        d.rectangle((hand[0] - 2, hand[1] - 2, hand[0] + 6, hand[1] + 3), fill=P["red"])
        emitter_start = min(63, hand[0] + 5)
        emitter_end = min(63, hand[0] + 12)
        d.rectangle((emitter_start, hand[1] - 1, emitter_end, hand[1] + 1), fill=P["red_light"])
        if animation == "attack" and frame in {3, 4}:
            d.line((min(63, hand[0] + 10), hand[1], 63, hand[1]), fill=P["red_light"], width=2)
            d.point((61, hand[1] - 2), fill=P["white"])
    elif kind == "cyborg":
        outlined_ellipse(d, (hand[0] - 5, hand[1] - 5, hand[0] + 6, hand[1] + 6), P["blue"])
        d.rectangle((hand[0] - 1, hand[1] - 2, hand[0] + 4, hand[1] + 2), fill=P["blue_glow"])
        if animation == "attack":
            radius = 2 + min(6, frame)
            d.ellipse((hand[0] + 7 - radius, hand[1] - radius,
                       hand[0] + 7 + radius, hand[1] + radius), outline=P["blue_light"], width=2)
            d.point((min(63, hand[0] + 8), hand[1]), fill=P["white"])
    else:
        d.line((hand[0] - 1, hand[1] + 2, hand[0] + 1, hand[1] - 2), fill=P["dark"], width=4)
        if animation == "attack":
            tip = (min(63, hand[0] + 28), max(4, hand[1] - 12 - frame))
            d.line((hand[0], hand[1], tip[0], tip[1]), fill=P["red_dark"], width=5)
            d.line((hand[0], hand[1], tip[0], tip[1]), fill=P["red"], width=3)
            d.line((hand[0], hand[1], tip[0], tip[1]), fill=P["white"], width=1)
        else:
            d.line((x - 6, 47 + y, x - 13, 72 + y), fill=P["red"], width=3)

    if animation == "hurt" and frame == 1:
        im = tint_alpha(im, P["red"], 75)
    return upscale(im)


def make_battle_sheet(enemy: dict) -> Image.Image:
    sheet = Image.new("RGBA", (8 * 128, 4 * 192), P["transparent"])
    rows = [("idle", 4), ("attack", 6), ("hurt", 3), ("defeat", 6)]
    for row, (animation, count) in enumerate(rows):
        for frame in range(count):
            if enemy["kind"] == "drone":
                sprite = draw_battle_drone(animation, frame)
            else:
                sprite = draw_battle_humanoid(enemy["kind"], animation, frame)
            sheet.alpha_composite(sprite, (frame * 128, row * 192))
    return sheet


# ---------------------------------------------------------------------------
# Previews and manifest metadata


def checkerboard(size: tuple[int, int], cell: int = 20) -> Image.Image:
    canvas = Image.new("RGBA", size, (229, 231, 229, 255))
    d = ImageDraw.Draw(canvas)
    for y in range(0, size[1], cell):
        for x in range(0, size[0], cell):
            if (x // cell + y // cell) % 2:
                d.rectangle((x, y, x + cell - 1, y + cell - 1), fill=(207, 212, 214, 255))
    return canvas


def load_preview_font(size: int) -> ImageFont.ImageFont:
    for path in (
        "/usr/share/fonts/truetype/dejavu/DejaVuSans-Bold.ttf",
        "/usr/share/fonts/truetype/liberation2/LiberationSans-Bold.ttf",
    ):
        if Path(path).exists():
            return ImageFont.truetype(path, size)
    return ImageFont.load_default()


def make_previews() -> None:
    PREVIEW_DIR.mkdir(parents=True, exist_ok=True)
    roster = checkerboard((760, 280), 24)
    rd = ImageDraw.Draw(roster)
    title_font = load_preview_font(24)
    label_font = load_preview_font(15)
    rd.rectangle((0, 0, 759, 48), fill=P["ink"])
    rd.rectangle((0, 45, 759, 48), fill=P["red"])
    rd.text((22, 11), "UTOPIA SCI-FI ENEMY ROSTER", font=title_font, fill=P["white"])
    for i, enemy in enumerate(ENEMIES):
        sheet = Image.open(BATTLE_DIR / f"{enemy['id']}_battle_128x192.png").convert("RGBA")
        idle = sheet.crop((0, 0, 128, 192))
        x = 25 + i * 185
        roster.alpha_composite(idle, (x + 22, 56))
        rd.rectangle((x, 244, x + 174, 273), fill=(29, 37, 46, 225))
        label_box = rd.textbbox((0, 0), enemy["displayName"], font=label_font)
        label_width = label_box[2] - label_box[0]
        rd.text((x + (174 - label_width) // 2, 249), enemy["displayName"], font=label_font, fill=P["white"])
    save(roster, PREVIEW_DIR / "sci_fi_enemy_roster.png")

    attack_frames = []
    for frame_index in range(6):
        canvas = checkerboard((760, 230), 24)
        for i, enemy in enumerate(ENEMIES):
            sheet = Image.open(BATTLE_DIR / f"{enemy['id']}_battle_128x192.png").convert("RGBA")
            sprite = sheet.crop((frame_index * 128, 192, (frame_index + 1) * 128, 384))
            canvas.alpha_composite(sprite, (28 + i * 185, 18))
        attack_frames.append(canvas.convert("P", palette=Image.Palette.ADAPTIVE, colors=255))
    attack_frames[0].save(
        PREVIEW_DIR / "sci_fi_enemy_attacks.gif",
        save_all=True,
        append_images=attack_frames[1:],
        duration=120,
        loop=0,
        disposal=2,
    )

    field_frames = []
    for frame_index in range(4):
        canvas = checkerboard((520, 430), 20)
        for row in range(4):
            for i, enemy in enumerate(ENEMIES):
                sheet = Image.open(TOPDOWN_DIR / f"{enemy['id']}_topdown_64x96.png").convert("RGBA")
                sprite = sheet.crop((frame_index * 64, row * 96,
                                     (frame_index + 1) * 64, (row + 1) * 96))
                canvas.alpha_composite(sprite, (28 + i * 120, 18 + row * 102))
        field_frames.append(canvas.convert("P", palette=Image.Palette.ADAPTIVE, colors=255))
    field_frames[0].save(
        PREVIEW_DIR / "sci_fi_enemy_four_direction.gif",
        save_all=True,
        append_images=field_frames[1:],
        duration=150,
        loop=0,
        disposal=2,
    )

    battle_background = ASSET_ROOT / "backgrounds" / "battle_atrium_1280x720.png"
    if battle_background.exists():
        scene = Image.open(battle_background).convert("RGBA")
        for i, enemy in enumerate(ENEMIES):
            sheet = Image.open(BATTLE_DIR / f"{enemy['id']}_battle_128x192.png").convert("RGBA")
            idle = sheet.crop((0, 0, 128, 192))
            scene.alpha_composite(idle, (630 + i * 150, 365))
        save(scene, PREVIEW_DIR / "sci_fi_enemies_in_battle.png")


def update_manifest() -> None:
    path = ASSET_ROOT / "asset_manifest.json"
    data = json.loads(path.read_text(encoding="utf-8"))
    data["version"] = "1.2.0"
    data["enemyVisualTheme"] = "utopian science-fiction"
    for enemy in ENEMIES:
        entry = data["enemies"][enemy["id"]]
        entry["displayName"] = enemy["displayName"]
        entry["archetype"] = enemy["archetype"]
        entry["designPalette"] = ["off-white", "graphite", "red", "electric blue"]
        entry["compatibilityId"] = enemy["id"]
    path.write_text(json.dumps(data, indent=2) + "\n", encoding="utf-8")


def main() -> None:
    for enemy in ENEMIES:
        save(make_topdown_sheet(enemy), TOPDOWN_DIR / f"{enemy['id']}_topdown_64x96.png")
        save(make_battle_sheet(enemy), BATTLE_DIR / f"{enemy['id']}_battle_128x192.png")
    update_manifest()
    make_previews()
    print("Generated four utopian sci-fi enemy sets and previews.")


if __name__ == "__main__":
    main()
