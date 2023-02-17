module io.github.adermont.livinggame {
    requires javafx.controls;
    requires javafx.fxml;

    opens io.github.adermont.gameoflife to javafx.fxml;
    exports io.github.adermont.gameoflife;
}