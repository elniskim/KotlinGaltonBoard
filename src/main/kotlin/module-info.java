module com.example.galtonboard {
    requires javafx.controls;
    requires javafx.fxml;
    requires kotlin.stdlib;


    opens com.example.galtonboard to javafx.fxml;
    exports com.example.galtonboard;
}