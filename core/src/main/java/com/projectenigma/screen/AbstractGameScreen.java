package com.projectenigma.screen;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.InputProcessor;
import com.badlogic.gdx.Screen;
import com.projectenigma.ProjectEnigmaGame;

public abstract class AbstractGameScreen implements Screen {
    protected final ProjectEnigmaGame game;
    private InputProcessor inputProcessor;

    protected AbstractGameScreen(ProjectEnigmaGame game) {
        this.game = game;
    }

    protected final void useInput(InputProcessor processor) {
        inputProcessor = processor;
    }

    @Override
    public void show() {
        if (inputProcessor != null) {
            Gdx.input.setInputProcessor(inputProcessor);
        }
    }

    @Override
    public void hide() {
        if (Gdx.input != null && Gdx.input.getInputProcessor() == inputProcessor) {
            Gdx.input.setInputProcessor(null);
        }
    }

    @Override
    public void pause() {
    }

    @Override
    public void resume() {
    }

    @Override
    public void dispose() {
    }
}
