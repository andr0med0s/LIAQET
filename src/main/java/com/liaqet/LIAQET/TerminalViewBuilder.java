package com.liaqet.LIAQET;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;

/**
 * Модуль компоновки интерфейса терминала LIAQET.
 * Отвечает исключительно за создание, стилизацию и размещение UI-компонентов.
 */
public class TerminalViewBuilder {

    public static HBox buildTopBar(TextField tickerInput, ComboBox<String> tfGroupCombo,
                                   Button refreshBtn, Button favBtn, Button aiBtn) {
        HBox topBar = new HBox(15);
        topBar.setPadding(new Insets(15));
        topBar.setAlignment(Pos.CENTER_LEFT);
        topBar.setStyle("-fx-background-color: #161b22; -fx-border-color: #30363d; -fx-border-width: 0 0 1 0;");

        tickerInput.setStyle("-fx-background-color: #0d1117; -fx-text-fill: #ffffff; -fx-border-color: #30363d; -fx-border-radius: 4;");

        tfGroupCombo.getItems().addAll("1 группа: неделя / день / 4ч", "2 группа: 4ч / 1ч / 30м", "3 группа: 30м / 15м / 5м");
        tfGroupCombo.setValue("3 группа: 30м / 15м / 5м");
        tfGroupCombo.setStyle("-fx-background-color: #ffffff; -fx-text-fill: #000000; -fx-font-weight: bold;");

        refreshBtn.setStyle("-fx-background-color: #238636; -fx-text-fill: #ffffff; -fx-font-weight: bold;");
        aiBtn.setStyle("-fx-background-color: #1f6feb; -fx-text-fill: #ffffff; -fx-font-weight: bold;");

        topBar.getChildren().addAll(
                new Label("Поиск (FIGI/UID):"), tickerInput,
                new Label("Группа TF:"), tfGroupCombo,
                refreshBtn, favBtn, aiBtn
        );

        topBar.getChildren().stream()
                .filter(node -> node instanceof Label)
                .forEach(node -> ((Label) node).setStyle("-fx-text-fill: #8b949e;"));

        return topBar;
    }

    public static void setupTableGrid(GridPane tableGrid) {
        tableGrid.setHgap(2);
        tableGrid.setVgap(4);
        tableGrid.setStyle("-fx-background-color: #0d1117; -fx-padding: 10; -fx-background-radius: 6; -fx-border-color: #30363d; -fx-border-radius: 6;");

        ColumnConstraints col1 = new ColumnConstraints(120);
        ColumnConstraints col2 = new ColumnConstraints(110);
        ColumnConstraints col3 = new ColumnConstraints(110);
        ColumnConstraints col4 = new ColumnConstraints(100);
        ColumnConstraints col5 = new ColumnConstraints(120);
        tableGrid.getColumnConstraints().addAll(col1, col2, col3, col4, col5);
    }

    public static Label createCellLabel(String text, String textHexColor, String bgHexColor) {
        Label label = new Label(text);
        label.setAlignment(Pos.CENTER_LEFT);
        label.setPadding(new Insets(8, 10, 8, 10));
        label.setMaxWidth(Double.MAX_VALUE);
        label.setMaxHeight(Double.MAX_VALUE);
        label.setStyle("-fx-text-fill: " + textHexColor + "; -fx-background-color: " + bgHexColor + "; -fx-font-family: 'Consolas'; -fx-font-size: 12px; -fx-background-radius: 2;");
        return label;
    }
}
