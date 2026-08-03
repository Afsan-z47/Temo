package com.projectenigma;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.utils.Json;
import com.badlogic.gdx.utils.JsonWriter;
import com.badlogic.gdx.utils.SerializationException;
import com.projectenigma.model.GameSession;

public final class SaveService {
    private static final String SAVE_PATH = "save/project-enigma-save.json";
    private static final String LEGACY_SAVE_PATH = "save/dungeon-rpg-save.json";
    private final Json json;

    public SaveService() {
        json = new Json();
        json.setOutputType(JsonWriter.OutputType.json);
        json.setUsePrototypes(false);
    }

    public boolean hasSave() {
        return saveFile().exists() || legacySaveFile().exists();
    }

    public void save(GameSession session) {
        if (session == null || session.hero == null || !session.hero.isAlive()) {
            return;
        }
        FileHandle file = saveFile();
        file.parent().mkdirs();
        file.writeString(json.prettyPrint(session), false, "UTF-8");
    }

    public GameSession load() {
        FileHandle currentFile = saveFile();
        boolean loadingLegacySave = !currentFile.exists();
        FileHandle file = loadingLegacySave ? legacySaveFile() : currentFile;
        if (!file.exists()) {
            return null;
        }
        try {
            GameSession session = json.fromJson(GameSession.class, file.readString("UTF-8"));
            if (session == null || session.hero == null || !session.hero.isAlive()) {
                return null;
            }
            session.rebuildTransientState();
            if (loadingLegacySave) {
                save(session);
            }
            return session;
        } catch (SerializationException | IllegalArgumentException exception) {
            Gdx.app.error("SaveService", "The save file could not be loaded. A new game can still be started.", exception);
            return null;
        }
    }

    public void deleteSave() {
        FileHandle file = saveFile();
        if (file.exists()) {
            file.delete();
        }
        FileHandle legacyFile = legacySaveFile();
        if (legacyFile.exists()) {
            legacyFile.delete();
        }
    }

    public String displayPath() {
        return SAVE_PATH;
    }

    private FileHandle saveFile() {
        return Gdx.files.local(SAVE_PATH);
    }

    private FileHandle legacySaveFile() {
        return Gdx.files.local(LEGACY_SAVE_PATH);
    }
}
