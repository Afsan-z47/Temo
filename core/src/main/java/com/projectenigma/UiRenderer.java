package com.projectenigma;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.GlyphLayout;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.utils.Align;

public final class UiRenderer {
    public static final float WIDTH = 1280f;
    public static final float HEIGHT = 720f;

    private UiRenderer() {
    }

    public static void panel(ShapeRenderer shapes, float x, float y, float width, float height, Color color) {
        shapes.setColor(color);
        shapes.rect(x, y, width, height);
    }

    public static void outline(ShapeRenderer shapes, float x, float y, float width, float height, Color color) {
        shapes.setColor(color);
        shapes.rect(x, y, width, height);
    }

    public static void bar(ShapeRenderer shapes, float x, float y, float width, float height,
                           int current, int maximum, Color fill) {
        shapes.setColor(Palette.WALL);
        shapes.rect(x, y, width, height);
        float ratio = maximum <= 0 ? 0f : MathUtils.clamp(current / (float) maximum, 0f, 1f);
        shapes.setColor(fill);
        shapes.rect(x + 2f, y + 2f, (width - 4f) * ratio, height - 4f);
    }

    public static void text(SpriteBatch batch, BitmapFont font, String text, float x, float y, Color color) {
        font.setColor(color);
        font.draw(batch, text, x, y);
    }

    public static void centeredText(SpriteBatch batch, BitmapFont font, String text,
                                    float centerX, float y, Color color) {
        GlyphLayout layout = new GlyphLayout(font, text);
        text(batch, font, text, centerX - layout.width / 2f, y, color);
    }

    public static void wrappedText(SpriteBatch batch, BitmapFont font, String text, float x, float y,
                                   float targetWidth, Color color) {
        font.setColor(color);
        font.draw(batch, text, x, y, targetWidth, Align.left, true);
    }
}
