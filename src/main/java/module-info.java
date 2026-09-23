module com.liaqet.terminal {
    requires javafx.controls;
    requires javafx.fxml;
    requires org.json;
    requires java.net.http; // Добавили доступ к сетевым запросам Java

    exports com.liaqet.LIAQET;
}
