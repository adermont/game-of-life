module io.github.adermont.livinggame {
    requires javafx.controls;
    requires javafx.fxml;

    opens io.github.adermont.livinggame to javafx.fxml;
    exports io.github.adermont.livinggame;
}