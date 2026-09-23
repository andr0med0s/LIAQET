package com.liaqet.LIAQET;

import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Stage;

public class MainApp extends Application {

    // Глобальные ссылки на элементы UI
    private Label assetNameLabel; // НОВАЯ ПЛАШКА НАЗВАНИЯ АКТИВА
    private Label stochLabel;
    private Label emaLabel;
    private Label mfiLabel;
    private Label tableHeader;
    private TextArea aiResponseArea;
    private ComboBox<String> tfGroupCombo;
    private TextField tickerInput; // Вынесли в глобальные, чтобы читать из любого метода

    @Override
    public void start(Stage primaryStage) {
        BorderPane mainLayout = new BorderPane();
        mainLayout.setStyle("-fx-background-color: #11141a;");

        // 1. ВЕРХНЯЯ ПАНЕЛЬ
        HBox topBar = new HBox(15);
        topBar.setPadding(new Insets(15));
        topBar.setAlignment(Pos.CENTER_LEFT);
        topBar.setStyle("-fx-background-color: #161b22; -fx-border-color: #30363d; -fx-border-width: 0 0 1 0;");

        tickerInput = new TextField("BBG004730N88"); // Сбербанк по умолчанию
        tickerInput.setStyle("-fx-background-color: #0d1117; -fx-text-fill: #ffffff; -fx-border-color: #30363d; -fx-border-radius: 4;");

        tfGroupCombo = new ComboBox<>();
        tfGroupCombo.getItems().addAll("30м / 15м / 5м", "4ч / 1ч / 30м", "Неделя / День / 4ч");
        tfGroupCombo.setValue("30м / 15м / 5м");
        tfGroupCombo.setStyle("-fx-background-color: #ffffff; -fx-text-fill: #000000; -fx-font-weight: bold;");

        Button refreshBtn = new Button("Обновить данные");
        refreshBtn.setStyle("-fx-background-color: #238636; -fx-text-fill: #ffffff; -fx-font-weight: bold;");

        Button aiBtn = new Button("Робот-Аналитик");
        aiBtn.setStyle("-fx-background-color: #1f6feb; -fx-text-fill: #ffffff;");

        topBar.getChildren().addAll(
                new Label("Поиск (FIGI/UID):"), tickerInput,
                new Label("Группа TF:"), tfGroupCombo,
                refreshBtn, aiBtn
        );

        topBar.getChildren().stream()
                .filter(node -> node instanceof Label)
                .forEach(node -> ((Label) node).setStyle("-fx-text-fill: #8b949e;"));

        // 2. ЦЕНТРАЛЬНАЯ ПАНЕЛЬ (Индикаторы и Название)
        VBox centerLayout = new VBox(15);
        centerLayout.setPadding(new Insets(20));

        // Добавляем большую информационную панель названия инструмента
        assetNameLabel = new Label("АКТИВ: Загрузка названия...");
        assetNameLabel.setStyle("-fx-text-fill: #ffffff; -fx-font-size: 20px; -fx-font-weight: bold; -fx-font-family: 'Arial';");

        tableHeader = new Label("АНАЛИТИЧЕСКИЕ ИНДИКАТОРЫ (30м / 15м / 5м)");
        tableHeader.setStyle("-fx-text-fill: #58a6ff; -fx-font-size: 14px; -fx-font-weight: bold;");

        stochLabel = createIndicatorLabel("Stochastic %K: Ожидание...", "#ff9e22");
        emaLabel = createIndicatorLabel("EMA 50: Ожидание...", "#58a6ff");
        mfiLabel = createIndicatorLabel("MFI v2: Ожидание...", "#00ff96");

        centerLayout.getChildren().addAll(assetNameLabel, tableHeader, stochLabel, emaLabel, mfiLabel);

        // 3. ПРАВАЯ ПАНЕЛЬ
        VBox rightLayout = new VBox(10);
        rightLayout.setPadding(new Insets(20));
        rightLayout.setPrefWidth(350);
        rightLayout.setStyle("-fx-background-color: #161b22; -fx-border-color: #30363d; -fx-border-width: 0 0 0 1;");

        Label aiHeader = new Label("ВЕРДИКТ ЛОКАЛЬНОГО ИИ");
        aiHeader.setStyle("-fx-text-fill: #ff5858; -fx-font-size: 14px; -fx-font-weight: bold;");

        aiResponseArea = new TextArea("Ожидание запуска аналитического потока...");
        aiResponseArea.setWrapText(true);
        aiResponseArea.setEditable(false);
        aiResponseArea.setPrefHeight(500);
        aiResponseArea.setStyle("-fx-control-inner-background: #0d1117; -fx-text-fill: #d1d5db; -fx-font-family: 'Consolas';");

        rightLayout.getChildren().addAll(aiHeader, aiResponseArea);

        mainLayout.setTop(topBar);
        mainLayout.setCenter(centerLayout);
        mainLayout.setRight(rightLayout);

        // === ИНТЕРФЕЙСНАЯ ЛОГИКА (СЛУШАТЕЛИ КЛИКОВ) ===

        // Переключение выпадающего списка
        tfGroupCombo.setOnAction(e -> {
            String selectedGroup = tfGroupCombo.getValue();
            tableHeader.setText("АНАЛИТИЧЕСКИЕ ИНДИКАТОРЫ (" + selectedGroup + ")");
            aiResponseArea.setText("Группа таймфреймов изменена на: " + selectedGroup + "\nНажмите 'Обновить данные' для пересчета ядра.");
        });

        // ОЖИВЛЯЕМ КНОПКУ ОБНОВИТЬ ДАННЫЕ
        refreshBtn.setOnAction(event -> {
            String userFigi = tickerInput.getText().trim();
            if (!userFigi.isEmpty()) {
                stochLabel.setText("Stochastic %K: Считаем...");
                emaLabel.setText("EMA 50: Считаем...");
                mfiLabel.setText("MFI v2: Считаем...");
                aiResponseArea.setText("Пересчет квантового ядра под новый инструмент...\nЗапрос отправлен к Т-Банку.");

                // Запускаем анализ под тот FIGI, который ввел пользователь в окно поиска!
                executeAnalysis(userFigi);
            }
        });

        // Кнопка Робот-Аналитик
        aiBtn.setOnAction(event -> {
            aiResponseArea.setText("Робот-Аналитик ЛИАКЭТ запускает квантовые нейроны...\nСвязь с LM Studio установлена. Ожидайте генерации ответа...");

            Thread aiThread = new Thread(() -> {
                try {
                    String exactApiUrl = "http://127.0.0";
                    AiClient aiClient = new AiClient(exactApiUrl);

                    // Считываем текущее название инструмента прямо с экрана для контекста ИИ
                    String currentAsset = assetNameLabel.getText();

                    String aiVerdict = aiClient.sendAnalysisRequest(
                            currentAsset,
                            21.19, 21.19, 279.34, 25.38, "НЕТ"
                    );

                    javafx.application.Platform.runLater(() -> aiResponseArea.setText(aiVerdict));

                } catch (Exception e) {
                    javafx.application.Platform.runLater(() ->
                            aiResponseArea.setText("Критический сбой ИИ-модуля: " + e.getMessage())
                    );
                }
            });
            aiThread.setDaemon(true);
            aiThread.start();
        });

        Scene scene = new Scene(mainLayout, 1000, 650);
        primaryStage.setTitle("LIAQET Evolution Terminal v1.0 [JavaFX Edition]");
        primaryStage.setScene(scene);
        primaryStage.show();

        // Автоматический стартовый запуск при открытии программы
        executeAnalysis(tickerInput.getText());
    }

    private Label createIndicatorLabel(String text, String hexColor) {
        Label label = new Label(text);
        label.setStyle("-fx-text-fill: " + hexColor + "; -fx-font-size: 14px; -fx-font-family: 'Consolas'; -fx-background-color: #161b22; -fx-padding: 10; -fx-background-radius: 4;");
        label.setMaxWidth(Double.MAX_VALUE);
        return label;
    }

    // Изолированный метод для запуска потока расчета
    private void executeAnalysis(String figi) {
        String token = ConfigLoader.getToken();
        if (token != null && !token.isEmpty()) {
            // Динамически выводим, какой FIGI сейчас в обработке
            assetNameLabel.setText("АКТИВ: " + figi + " (Режим сканирования)");

            AssetAnalyticTask analyticTask = new AssetAnalyticTask(token, figi, stochLabel, emaLabel, mfiLabel, aiResponseArea);
            Thread backgroundThread = new Thread(analyticTask);
            backgroundThread.setDaemon(true);
            backgroundThread.start();
        }
    }

    public static void main(String[] args) {
        launch(args);
    }
}
