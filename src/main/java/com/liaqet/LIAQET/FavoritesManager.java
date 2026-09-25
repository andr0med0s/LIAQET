package com.liaqet.LIAQET;

import org.json.JSONArray;
import org.json.JSONObject;
import java.io.File;
import java.io.FileWriter;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

/**
 * Управляет локальным кэшем избранных инструментов через JSON локальное хранилище.
 */
public class FavoritesManager {
    private static final String FILE_NAME = "favorites.json";
    private final List<InstrumentItem> favoritesList = new ArrayList<>();

    public FavoritesManager() {
        loadFavorites();
    }

    /**
     * Загружает список инструментов из файла JSON
     */
    public void loadFavorites() {
        favoritesList.clear();
        File file = new File(FILE_NAME);
        if (!file.exists()) {
            System.out.println("[Избранное] Локальный файл конфигурации favorites.json не найден. Будет создан новый при добавлении.");
            return;
        }

        try {
            String content = new String(Files.readAllBytes(Paths.get(FILE_NAME)));
            JSONArray jsonArray = new JSONArray(content);
            for (int i = 0; i < jsonArray.length(); i++) {
                JSONObject obj = jsonArray.getJSONObject(i);
                favoritesList.add(new InstrumentItem(
                        obj.getString("name"),
                        obj.getString("ticker"),
                        obj.getString("uid"),
                        obj.getString("type")
                ));
            }
            System.out.println("[Избранное] Успешно загружено инструментов из кэша: " + favoritesList.size());
        } catch (Exception e) {
            System.err.println("[Избранное] Ошибка при чтении файла JSON: " + e.getMessage());
        }
    }

    /**
     * Сохраняет текущую коллекцию в локальный файл JSON
     */
    public void saveFavorites() {
        try {
            JSONArray jsonArray = new JSONArray();
            for (InstrumentItem item : favoritesList) {
                JSONObject obj = new JSONObject();
                obj.put("name", item.name());
                obj.put("ticker", item.ticker());
                obj.put("uid", item.uid());
                obj.put("type", item.type());
                jsonArray.put(obj);
            }

            try (FileWriter writer = new FileWriter(FILE_NAME)) {
                writer.write(jsonArray.toString(2)); // Сохраняем с красивыми отступами в 2 пробела
            }
            System.out.println("[Избранное] Изменения успешно зафиксированы в локальном JSON хранилище.");
        } catch (Exception e) {
            System.err.println("[Избранное] Ошибка записи в файл конфигурации: " + e.getMessage());
        }
    }

    public void add(InstrumentItem item) {
        if (!contains(item.uid())) {
            favoritesList.add(item);
            saveFavorites();
        }
    }

    public void remove(String uid) {
        favoritesList.removeIf(item -> item.uid().equals(uid));
        saveFavorites();
    }

    public boolean contains(String uid) {
        return favoritesList.stream().anyMatch(item -> item.uid().equals(uid));
    }

    public List<InstrumentItem> getFavoritesList() {
        return favoritesList;
    }
}
