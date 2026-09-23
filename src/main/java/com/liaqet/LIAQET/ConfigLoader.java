package com.liaqet.LIAQET;

import java.io.InputStream;
import java.util.Properties;

/**
 * Загрузчик конфигурации для безопасного чтения API-токенов.
 */
public class ConfigLoader {
    private static final Properties properties = new Properties();

    static {
        // Загружаем файл из папки resources встроенным загрузчиком классов JVM
        try (InputStream input = ConfigLoader.class.getClassLoader().getResourceAsStream("config.properties")) {
            if (input == null) {
                System.err.println("[Критическая ошибка] Файл config.properties не найден в папки resources!");
            } else {
                properties.load(input);
                System.out.println("[Конфигурация] Файл config.properties успешно загружен в память.");
            }
        } catch (Exception e) {
            System.err.println("[Ошибка] Не удалось прочитать конфигурацию: " + e.getMessage());
        }
    }

    /**
     * Возвращает значение токена по ключу.
     */
    public static String getToken() {
        return properties.getProperty("tbank.token", "");
    }
}
