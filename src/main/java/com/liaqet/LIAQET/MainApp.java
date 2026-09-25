package com.liaqet.LIAQET;

import javafx.application.Application;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Stage;

public class MainApp extends Application {

    // Глобальные компоненты интерфейса (Поля класса)
    private Label assetNameLabel;
    private Label tableHeader;
    private GridPane tableGrid;

    private TextArea aiQuestionArea;
    private TextArea aiResponseArea;
    private ComboBox<String> tfGroupCombo;
    private TextField tickerInput;
    private Button favBtn;

    // Списки Избранного
    private ListView<InstrumentItem> favListView;
    private ObservableList<InstrumentItem> favObservableList;

    // Списки Поиска (Теперь они видны во всех методах класса!)
    private ListView<InstrumentItem> searchListView;
    private ObservableList<InstrumentItem> searchObservableList;

    private KiraUiController controller;

    @Override
    public void start(Stage primaryStage) {
        controller = new KiraUiController(this);

        // Инициализируем стартовый инструмент по умолчанию (Сбербанк)
        controller.setCurrentlySelectedAsset(new InstrumentItem("Сбербанк России", "SBER", "BBG004730N88", "share"));

        BorderPane mainLayout = new BorderPane();
        mainLayout.setStyle("-fx-background-color: #11141a;");

        // Инициализация кнопок управления
        tickerInput = new TextField(controller.getCurrentlySelectedAsset().ticker());
        tfGroupCombo = new ComboBox<>();

        // ВОЗВРАЩАЕМ КНОПКУ ОБНОВИТЬ ДАННЫЕ
        Button refreshBtn = new Button("Обновить данные");
        refreshBtn.setStyle("-fx-background-color: #238636; -fx-text-fill: #ffffff; -fx-font-weight: bold;"); // Зеленый стиль

        Button searchBtn = new Button("Поиск тикера");
        searchBtn.setStyle("-fx-background-color: #00a0ff; -fx-text-fill: #ffffff; -fx-font-weight: bold;");

        Button aiBtn = new Button("Робот-Аналитик");
        aiBtn.setStyle("-fx-background-color: #1f6feb; -fx-text-fill: #ffffff; -fx-font-weight: bold;");

        favBtn = new Button();
        controller.updateFavButtonState(favBtn);


        // Инициализация списков (используем глобальные поля вместо локальных переменных)
        searchObservableList = FXCollections.observableArrayList();
        searchListView = new ListView<>(searchObservableList);
        searchListView.setPrefHeight(220);
        searchListView.setPrefWidth(285);
        searchListView.setStyle("-fx-control-inner-background: #0d1117; -fx-text-fill: #ffffff; -fx-font-family: 'Consolas'; -fx-border-color: #30363d;");

        favObservableList = FXCollections.observableArrayList(controller.getFavoritesManager().getFavoritesList());
        favListView = new ListView<>(favObservableList);
        favListView.setPrefHeight(220);
        favListView.setPrefWidth(285);
        favListView.setStyle("-fx-control-inner-background: #0d1117; -fx-text-fill: #ffffff; -fx-font-family: 'Consolas'; -fx-border-color: #30363d;");

        aiQuestionArea = new TextArea("Сделай комплексный анализ среднесрочных трендов и проверь силу дивергенции объема.");
        aiQuestionArea.setWrapText(true);
        aiQuestionArea.setPrefHeight(80);
        aiQuestionArea.setStyle("-fx-control-inner-background: #0d1117; -fx-text-fill: #ffffff; -fx-font-family: 'Consolas'; -fx-border-color: #30363d;");

        aiResponseArea = new TextArea("Ожидание запуска аналитического потока или отправки вопроса...");
        aiResponseArea.setWrapText(true);
        aiResponseArea.setEditable(false);
        aiResponseArea.setPrefHeight(420);
        aiResponseArea.setStyle("-fx-control-inner-background: #0d1117; -fx-text-fill: #d1d5db; -fx-font-family: 'Consolas'; -fx-border-color: #30363d;");

        assetNameLabel = new Label("АКТИВ: " + controller.getCurrentlySelectedAsset().name());
        assetNameLabel.setStyle("-fx-text-fill: #ffffff; -fx-font-size: 20px; -fx-font-weight: bold; -fx-font-family: 'Arial';");

        tableHeader = new Label("АНАЛИТИЧЕСКИЕ ИНДИКАТОРЫ (Сетка таймфреймов)");
        tableHeader.setStyle("-fx-text-fill: #58a6ff; -fx-font-size: 14px; -fx-font-weight: bold;");

        tableGrid = new GridPane();
        TerminalViewBuilder.setupTableGrid(tableGrid);
        buildTableHeaders();
        clearTableRows();

        // Сборка верхней панели управления
        HBox topBar = TerminalViewBuilder.buildTopBar(tickerInput, tfGroupCombo, refreshBtn, favBtn, aiBtn);
        topBar.getChildren().add(4, searchBtn);

        // Сборка центральной панели
        VBox centerLayout = new VBox(12);
        centerLayout.setPadding(new Insets(20));

        HBox listsContainer = new HBox(15);
        listsContainer.setPadding(new Insets(5, 0, 0, 0));
        HBox.setHgrow(listsContainer, Priority.ALWAYS);

        VBox searchBox = new VBox(5);
        Label searchListHeader = new Label("РЕЗУЛЬТАТЫ ПОИСКА (Клик для выбора)");
        searchListHeader.setStyle("-fx-text-fill: #00a0ff; -fx-font-size: 11px; -fx-font-weight: bold;");
        searchBox.getChildren().addAll(searchListHeader, searchListView);

        VBox favBox = new VBox(5);
        Label favListHeader = new Label("ИЗБРАННЫЕ ИНСТРУМЕНТЫ (Клик для выбора)");
        favListHeader.setStyle("-fx-text-fill: #8b949e; -fx-font-size: 11px; -fx-font-weight: bold;");
        favBox.getChildren().addAll(favListHeader, favListView);

        listsContainer.getChildren().addAll(searchBox, favBox);
        centerLayout.getChildren().addAll(assetNameLabel, tableHeader, tableGrid, listsContainer);

        // Сборка правой панели (ИИ)
        VBox rightLayout = new VBox(10);
        rightLayout.setPadding(new Insets(20));
        rightLayout.setPrefWidth(480);
        rightLayout.setStyle("-fx-background-color: #161b22; -fx-border-color: #30363d; -fx-border-width: 0 0 0 1;");

        Label aiQuestionHeader = new Label("1. ВВОД ВОПРОСА ИЛИ ЗАДАНИЯ ДЛЯ ИИ");
        aiQuestionHeader.setStyle("-fx-text-fill: #58a6ff; -fx-font-size: 12px; -fx-font-weight: bold;");

        Label aiResponseHeader = new Label("2. КВАНТОВЫЙ ВЕРДИКТ НЕЙРОСЕТИ (HERMES)");
        aiResponseHeader.setStyle("-fx-text-fill: #ff5858; -fx-font-size: 12px; -fx-font-weight: bold;");

        rightLayout.getChildren().addAll(aiQuestionHeader, aiQuestionArea, new Separator(), aiResponseHeader, aiResponseArea);

        mainLayout.setTop(topBar);
        mainLayout.setCenter(centerLayout);
        mainLayout.setRight(rightLayout);

        // === ПРИВЯЗКА СЛУШАТЕЛЕЙ К КОНТРОЛЛЕРУ ===
        tfGroupCombo.setOnAction(e -> refreshUiDisplay());

        // ИСПРАВЛЕНО: Кнопка обновляет данные строго по текущему зафиксированному FIGI из контроллера
        refreshBtn.setOnAction(e -> {
            String currentFigi = controller.getCurrentActiveFigi();
            controller.handleRefreshAction(currentFigi, aiResponseArea);
        });

        searchBtn.setOnAction(e -> controller.handleSearchAction(tickerInput.getText().trim(), searchObservableList, aiResponseArea));

        tickerInput.setOnKeyPressed(event -> {
            if (event.getCode() == javafx.scene.input.KeyCode.ENTER) {
                controller.handleSearchAction(tickerInput.getText().trim(), searchObservableList, aiResponseArea);
            }
        });

        favBtn.setOnAction(e -> controller.handleFavToggleAction(favObservableList, favBtn));

        // Фикс вызова: передаем ровно три параметра, как требует KiraUiController
        searchListView.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null) {
                controller.selectAssetForAnalysis(newVal, tickerInput, aiResponseArea);
            }
        });

        favListView.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null) {
                controller.selectAssetForAnalysis(newVal, tickerInput, aiResponseArea);
            }
        });

        aiBtn.setOnAction(e -> controller.handleAiAnalysis(assetNameLabel, tfGroupCombo, aiQuestionArea, aiResponseArea));

        Scene scene = new Scene(mainLayout, 1180, 750);
        primaryStage.setTitle("LIAQET Evolution Terminal v1.0 [JavaFX Edition]");
        primaryStage.setScene(scene);
        primaryStage.show();

        controller.handleRefreshAction(controller.getCurrentlySelectedAsset().uid(), aiResponseArea);
    }

    public void updateUiWithPackage(IndicatorPackage pack) {
        controller.setLastCalculatedPackage(pack);
        refreshUiDisplay();
    }

    private void refreshUiDisplay() {
        if (controller.getLastCalculatedPackage() == null) return;
        buildTableHeaders();
        String selectedGroup = tfGroupCombo.getValue();
        IndicatorPackage lp = controller.getLastCalculatedPackage();

        if (selectedGroup.contains("1 группа")) {
            tableHeader.setText("АНАЛИТИЧЕСКИЕ ИНДИКАТОРЫ (ИНВЕСТИЦИОННАЯ ГРУППА)");
            renderGridRow(1, "Неделя (1w)", lp.tfWeek, lp.tfWeekEma, lp.tfWeekMfi);
            renderGridRow(2, "День (1d)  ", lp.tfDay, lp.tfDayEma, lp.tfDayMfi);
            renderGridRow(3, "4 Часа (4h) ", lp.tf4h, lp.tf4hEma, lp.tf4hMfi);
        } else if (selectedGroup.contains("2 группа")) {
            tableHeader.setText("АНАЛИТИЧЕСКИЕ ИНДИКАТОРЫ (СРЕДНЕСРОЧНАЯ ГРУППА)");
            renderGridRow(1, "4 Часа (4h) ", lp.tf4h, lp.tf4hEma, lp.tf4hMfi);
            renderGridRow(2, "1 Час (1h)  ", lp.tf1h, lp.tf1hEma, lp.tf1hMfi);
            renderGridRow(3, "30 Мин (30m)", lp.tf30m, lp.tf30mEma, lp.tf30mMfi);
        } else {
            tableHeader.setText("АНАЛИТИЧЕСКИЕ ИНДИКАТОРЫ (СКАЛЬПЕРСКАЯ ГРУППА)");
            renderGridRow(1, "30 Мин (30m)", lp.tf30m, lp.tf30mEma, lp.tf30mMfi);
            renderGridRow(2, "15 Мин (15m)", lp.tf15m, lp.tf15mEma, lp.tf15mMfi);
            renderGridRow(3, "5 Мин (5m)  ", lp.tf5m, lp.tf5mEma, lp.tf5mMfi);
        }
    }

    private void renderGridRow(int rowIndex, String tfName, StochasticResult stoch, EmaResult ema, MfiResult mfi) {
        // УМНЫЙ ПЕРЕХВАТ: Если по дальнему фьючерсу брокер вернул 0 свечей (нет ликвидности)
        if (stoch.isError() || ema.isError() || mfi.isError()) {
            tableGrid.add(TerminalViewBuilder.createCellLabel(tfName, "#ffffff", "#141923"), 0, rowIndex);
            tableGrid.add(TerminalViewBuilder.createCellLabel("—", "#8b949e", "#161b22"), 1, rowIndex);
            tableGrid.add(TerminalViewBuilder.createCellLabel("—", "#8b949e", "#131a26"), 2, rowIndex);
            tableGrid.add(TerminalViewBuilder.createCellLabel("—", "#8b949e", "#1b1418"), 3, rowIndex);

            // Вместо страшной ошибки пишем красивый статус для дальних контрактов
            tableGrid.add(TerminalViewBuilder.createCellLabel("Нет ликвидности", "#ff9e22", "#261f11"), 4, rowIndex);
            return;
        }
        tableGrid.add(TerminalViewBuilder.createCellLabel(tfName, "#ffffff", "#141923"), 0, rowIndex);
        String stochArrow = "";
        String stochColor = "#ffffff";
        if (stoch.k() > stoch.d() + 0.5) {
            stochArrow = " ▲";
            stochColor = "#00ff96";
        } else if (stoch.k() < stoch.d() - 0.5) {
            stochArrow = " ▼";
            stochColor = "#ff5858";
        }
        tableGrid.add(TerminalViewBuilder.createCellLabel(String.format("%.1f / %.1f%s", stoch.k(), stoch.d(), stochArrow), stochColor, "#161b22"), 1, rowIndex);
        String divText = mfi.divergenceType().equals("BULLISH") ? " [Δ Бычья]" : (mfi.divergenceType().equals("BEARISH") ? " [Δ Медв]" : "");
        String mfiColor = mfi.divergenceType().equals("BULLISH") ? "#00ffff" : (mfi.divergenceType().equals("BEARISH") ? "#ff10ff" : "#b4d7ff");
        tableGrid.add(TerminalViewBuilder.createCellLabel(String.format("MFI:%.0f%s", mfi.value(), divText), mfiColor, "#131a26"), 2, rowIndex);
        String sign = ema.distancePercent() >= 0 ? "+" : "";
        String emaTextColor = ema.distancePercent() >= 0 ? "#ff6e6e" : "#00ff96";
        tableGrid.add(TerminalViewBuilder.createCellLabel(String.format("%s%.2f%%", sign, ema.distancePercent()), emaTextColor, "#1b1418"), 3, rowIndex);
        String strategyText = "Поиск паттерна";
        String strategyColor = "#8b949e";
        String strategyBg = "#161b22";

        if (mfi.divergenceType().equals("BULLISH") && ema.distancePercent() < -0.8) {
            strategyText = "★ СИЛЬНЫЙ BUY"; strategyColor = "#00ff96"; strategyBg = "#11261a";
        } else if (mfi.divergenceType().equals("BEARISH") && ema.distancePercent() > 0.8) {
            strategyText = "★ СИЛЬНЫЙ SELL"; strategyColor = "#ff3b3b"; strategyBg = "#2b1414";
        } else if (stoch.k() <= 20 && ema.distancePercent() < -1.5) {
            strategyText = "BUY (Скальп)"; strategyColor = "#00c878";
        } else if (stoch.k() >= 80 && ema.distancePercent() > 1.5) {
            strategyText = "SELL (Скальп)"; strategyColor = "#c83232";
        }
        tableGrid.add(TerminalViewBuilder.createCellLabel(strategyText, strategyColor, strategyBg), 4, rowIndex);
    }
    private void buildTableHeaders() {
        tableGrid.getChildren().clear();
        tableGrid.add(TerminalViewBuilder.createCellLabel("Таймфрейм\n(Интервал)", "#00a0ff", "#161b22"), 0, 0);
        tableGrid.add(TerminalViewBuilder.createCellLabel("Stochastic\nLines %K / %D", "#ffffff", "#161b22"), 1, 0);
        tableGrid.add(TerminalViewBuilder.createCellLabel("MFI v2 Дивер\nОсциллятор", "#b4d7ff", "#161b22"), 2, 0);
        tableGrid.add(TerminalViewBuilder.createCellLabel("до EMA 50\nОтклонение", "#ff6e6e", "#161b22"), 3, 0);
        tableGrid.add(TerminalViewBuilder.createCellLabel("Стратегия\nMean Reversion", "#00ff96", "#161b22"), 4, 0);
    }
    private void clearTableRows() {
        for (int row = 1; row <= 3; row++) {
            for (int col = 0; col < 5; col++) {
                tableGrid.add(TerminalViewBuilder.createCellLabel("Ожидание...", "#8b949e", "#11141a"), col, row);
            }
        }
    }
    public Label getAssetNameLabel() { return assetNameLabel; }
    public Button getFavBtn() { return favBtn; }
    public static void main(String[] args) {
        launch(args);
    }
}
