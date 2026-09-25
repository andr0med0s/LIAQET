package com.liaqet.LIAQET;

import javafx.application.Platform;
import javafx.collections.ObservableList;
import javafx.scene.control.*;
import java.util.List;

/**
 * Контроллер управления бизнес-логикой и событиями интерфейса ЛИАКЭТ.
 */
public class KiraUiController {

    private final MainApp mainApp;
    private final FavoritesManager favoritesManager;
    private IndicatorPackage lastCalculatedPackage = null;
    private String currentActiveFigi = "BBG004730N88";
    private InstrumentItem currentlySelectedAsset = null;

    public KiraUiController(MainApp mainApp) {
        this.mainApp = mainApp;
        this.favoritesManager = new FavoritesManager();
        // Задаем стартовый актив по умолчанию
        this.currentlySelectedAsset = new InstrumentItem("Сбербанк России", "SBER", "BBG004730N88", "share");
    }

    public void handleRefreshAction(String figi, TextArea aiResponseArea) {
        if (figi != null && !figi.isEmpty()) {
            this.currentActiveFigi = figi;
            updateFavButtonState(mainApp.getFavBtn());
            aiResponseArea.setText("Пересчет квантового ядра под новый инструмент...\nЗапрос отправлен к Т-Банку.");
            executeAnalysis(figi, aiResponseArea);
        }
    }

    public void handleSearchAction(String searchQuery, ObservableList<InstrumentItem> searchObservableList, TextArea aiResponseArea) {
        if (searchQuery == null || searchQuery.trim().isEmpty()) {
            aiResponseArea.setText("Предупреждение: Введите тикер или название для поиска!");
            return;
        }

        aiResponseArea.setText("Сканирование реестра инструментов Московской Биржи...\nЗапрос отправлен к Т-Банку.");

        Thread searchThread = new Thread(() -> {
            try {
                String token = ConfigLoader.getToken();
                TInvestClient client = new TInvestClient(token);
                List<InstrumentItem> found = client.searchInstruments(searchQuery);

                Platform.runLater(() -> {
                    searchObservableList.setAll(found);
                    if (found.isEmpty()) {
                        aiResponseArea.setText("Поиск завершен: Боевые инструменты по запросу '" + searchQuery + "' не найдены.");
                    } else {
                        aiResponseArea.setText("Поиск успешно завершен. Найдено совпадений: " + found.size() + "\nВыберите инструмент в таблице поиска снизу.");
                    }
                });
            } catch (Exception e) {
                Platform.runLater(() -> aiResponseArea.setText("Ошибка при выполнении поиска: " + e.getMessage()));
            }
        });
        searchThread.setDaemon(true);
        searchThread.start();
    }

    public void handleFavToggleAction(ObservableList<InstrumentItem> favObservableList, Button favBtn) {
        if (currentlySelectedAsset == null) return;

        String uid = currentlySelectedAsset.uid();
        if (favoritesManager.contains(uid)) {
            favoritesManager.remove(uid);
        } else {
            favoritesManager.add(currentlySelectedAsset);
        }
        favObservableList.setAll(favoritesManager.getFavoritesList());
        updateFavButtonState(favBtn);
    }

    public void selectAssetForAnalysis(InstrumentItem item, TextField tickerInput, TextArea aiResponseArea) {
        if (item == null) return;
        this.currentlySelectedAsset = item;
        this.currentActiveFigi = item.uid();

        tickerInput.setText(item.ticker());
        mainApp.getAssetNameLabel().setText("АКТИВ: " + item.name() + " [" + item.ticker() + "]");

        updateFavButtonState(mainApp.getFavBtn());
        handleRefreshAction(item.uid(), aiResponseArea);
    }

    public void handleAiAnalysis(Label assetNameLabel, ComboBox<String> tfGroupCombo, TextArea aiQuestionArea, TextArea aiResponseArea) {
        if (lastCalculatedPackage == null) {
            aiResponseArea.setText("Ошибка: Показатели рынка ещё не рассчитаны.");
            return;
        }
        String customQuestion = aiQuestionArea.getText().trim();
        if (customQuestion.isEmpty()) {
            aiResponseArea.setText("Предупреждение: Напишите ваш вопрос в верхнее окно!");
            return;
        }

        aiResponseArea.setText("Робот-Аналитик ЛИАКЭТ обрабатывает ваш запрос...\nСвязь с LM Studio установлена. Идет генерация ответа...");

        Thread aiThread = new Thread(() -> {
            try {
                AiClient aiClient = new AiClient("http://localhost:1234/v1/chat/completions");
                String selectedGroup = tfGroupCombo.getValue();

                StochasticResult targetStoch; EmaResult targetEma; MfiResult targetMfi;
                if (selectedGroup.contains("1 группа")) {
                    targetStoch = lastCalculatedPackage.tfDay; targetEma = lastCalculatedPackage.tfDayEma; targetMfi = lastCalculatedPackage.tfDayMfi;
                } else if (selectedGroup.contains("2 группа")) {
                    targetStoch = lastCalculatedPackage.tf1h; targetEma = lastCalculatedPackage.tf1hEma; targetMfi = lastCalculatedPackage.tf1hMfi;
                } else {
                    targetStoch = lastCalculatedPackage.tf15m; targetEma = lastCalculatedPackage.tf15mEma; targetMfi = lastCalculatedPackage.tf15mMfi;
                }

                String aiVerdict = aiClient.sendDialogueRequest(
                        assetNameLabel.getText(), targetStoch.k(), targetStoch.d(),
                        targetEma.value(), targetMfi.value(), targetMfi.divergenceType(), customQuestion
                );
                Platform.runLater(() -> aiResponseArea.setText(aiVerdict));
            } catch (Exception e) {
                Platform.runLater(() -> aiResponseArea.setText("Критический сбой ИИ-модуля: " + e.getMessage()));
            }
        });
        aiThread.setDaemon(true);
        aiThread.start();
    }

    public void updateFavButtonState(Button favBtn) {
        if (favBtn == null) return;
        if (favoritesManager != null && favoritesManager.contains(currentActiveFigi)) {
            favBtn.setText("[+] В Избранном");
            favBtn.setStyle("-fx-background-color: #531f1f; -fx-text-fill: #ff9e9e; -fx-font-weight: bold;");
        } else {
            favBtn.setText("[-] Добавить");
            favBtn.setStyle("-fx-background-color: #1f5326; -fx-text-fill: #9effa6; -fx-font-weight: bold;");
        }
    }

    public void executeAnalysis(String figi, TextArea aiResponseArea) {
        String token = ConfigLoader.getToken();
        if (token != null && !token.isEmpty()) {
            mainApp.getAssetNameLabel().setText("АКТИВ: " + figi + " (Режим сканирования)");
            AssetAnalyticTask analyticTask = new AssetAnalyticTask(token, figi, mainApp, aiResponseArea);
            Thread backgroundThread = new Thread(analyticTask);
            backgroundThread.setDaemon(true);
            backgroundThread.start();
        }
    }

    public void setLastCalculatedPackage(IndicatorPackage pack) {
        this.lastCalculatedPackage = pack;
    }

    public IndicatorPackage getLastCalculatedPackage() {
        return lastCalculatedPackage;
    }

    public FavoritesManager getFavoritesManager() {
        return favoritesManager;
    }

    public String getCurrentActiveFigi() {
        return currentActiveFigi;
    }

    public InstrumentItem getCurrentlySelectedAsset() {
        return currentlySelectedAsset;
    }

    public void setCurrentlySelectedAsset(InstrumentItem item) {
        this.currentlySelectedAsset = item;
        if (item != null) {
            this.currentActiveFigi = item.uid();
        }
    }
}
