package agh.bozon;

import java.io.File;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import javafx.animation.AnimationTimer;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.CheckBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.Separator;
import javafx.scene.control.Spinner;
import javafx.scene.control.SpinnerValueFactory;
import javafx.scene.control.TextField;
import javafx.scene.control.ToolBar;
import javafx.scene.image.Image;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

public class AdAstraMapperApp extends Application {

    private TelescopeController telescopeController;
    private Label statusLabel;
    private Label timeLabel;
    private Canvas mapCanvas;
    private TelescopeStatus currentStatus;
    private final SkyMapTime skyMapTime = new SkyMapTime();

    private Image mapBackgroundImage;
    private final File APP_DIR = new File(System.getProperty("user.home"), "adastramapper");
    private ObservationStation station;
    private List<AstroObject> loadedObjects = new ArrayList<>();

    @Override
    public void start(Stage primaryStage) {
        if (!APP_DIR.exists()) APP_DIR.mkdirs();
        Moon.initOrekit(APP_DIR);

        station = new ObservationStation(49.568, 20.067, 1000.0);
        skyMapTime.setSelectedDateTime(LocalDateTime.now());

        telescopeController = new TelescopeController("192.168.2.16", 502, true);

        File imgFile = new File("adastramapper/src/main/resources/static/img/west.gif");
        if (imgFile.exists()) {
            // mapBackgroundImage = new Image(imgFile.toURI().toString());
        }
        else {
            System.out.print("Brak pliku: " + imgFile.getAbsolutePath() + "\n");
        }

        BorderPane root = new BorderPane();
        root.setTop(createToolBar(primaryStage));

        mapCanvas = new Canvas(800, 600);
        Pane canvasContainer = new Pane(mapCanvas);
        canvasContainer.setStyle("-fx-background-color: #050510;");
        root.setCenter(canvasContainer);

        root.setBottom(createBottomPanel());

        startTelescopePolling();
        startMapRenderLoop();

        Scene scene = new Scene(root, 1000, 800);
        primaryStage.setTitle("AdAstra Mapper");
        primaryStage.setScene(scene);
        primaryStage.show();
    }

    private ToolBar createToolBar(Stage stage) {
        Button btnLoadCat = new Button("Wczytaj obiekty (.cat)");
        Button btnSaveCat = new Button("Zapisz historię (.cat)");
        Button btnSetLocation = new Button("Ustaw Lokalizację (Stacja)");
        Button btnSetTime = new Button("Ustaw Datę i Godzinę");
        Button btnClearObjects = new Button("Wyczyść obiekty");

        btnLoadCat.setOnAction(e -> {
            FileChooser fc = new FileChooser();
            fc.setInitialDirectory(APP_DIR);
            fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("Pliki Katalogu", "*.cat"));
            List<File> files = fc.showOpenMultipleDialog(stage);
            loadedObjects = CatalogManager.loadCatalog(files, loadedObjects);
        });

        btnSaveCat.setOnAction(e -> {
            FileChooser fc = new FileChooser();
            fc.setInitialDirectory(APP_DIR);
            fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("Pliki Katalogu", "*.cat"));
            File file = fc.showSaveDialog(stage);
            if (file != null) CatalogManager.saveCatalog(file, loadedObjects);
        });

        btnSetLocation.setOnAction(e -> showLocationDialog());
        btnSetTime.setOnAction(e -> showTimeSelectionDialog());

        btnClearObjects.setOnAction(e -> {
            loadedObjects.clear();
        });

        return new ToolBar(btnLoadCat, btnSaveCat, btnClearObjects, new Separator(), btnSetLocation, btnSetTime);
    }

    private void showLocationDialog() {
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("Lokalizacja teleskopu");

        GridPane grid = new GridPane();
        grid.setHgap(10); grid.setVgap(10); grid.setPadding(new Insets(10));
        TextField latField = new TextField(String.valueOf(station.getLatitude()));
        TextField lonField = new TextField(String.valueOf(station.getLongitude()));
        TextField altField = new TextField(String.valueOf(station.getAltitude()));

        grid.add(new Label("Szerokość (Lat):"), 0, 0); grid.add(latField, 1, 0);
        grid.add(new Label("Długość (Lon):"), 0, 1);   grid.add(lonField, 1, 1);
        grid.add(new Label("Wysokość (m):"), 0, 2);    grid.add(altField, 1, 2);

        dialog.getDialogPane().setContent(grid);
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
        dialog.showAndWait().ifPresent(res -> {
            if (res == ButtonType.OK) {
                try {
                    station.setLatitude(Double.parseDouble(latField.getText()));
                    station.setLongitude(Double.parseDouble(lonField.getText()));
                    station.setAltitude(Double.parseDouble(altField.getText()));
                } catch (NumberFormatException ex) { /* Zignoruj błędny format */ }
            }
        });
    }

    private void showTimeSelectionDialog() {
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("Data i godzina dla mapy nieba");

        GridPane grid = new GridPane();
        grid.setHgap(12);
        grid.setVgap(10);
        grid.setPadding(new Insets(12));

        LocalDateTime current = skyMapTime.getSelectedDateTime();
        DatePicker datePicker = new DatePicker(current.toLocalDate());
        Spinner<Integer> hourSpinner = new Spinner<>(
                new SpinnerValueFactory.IntegerSpinnerValueFactory(0, 23, current.getHour()));
        Spinner<Integer> minuteSpinner = new Spinner<>(
                new SpinnerValueFactory.IntegerSpinnerValueFactory(0, 59, current.getMinute()));
        CheckBox useSystemTimeCheck = new CheckBox("Użyj czasu systemowego");
        useSystemTimeCheck.setSelected(skyMapTime.isUseSystemTime());

        hourSpinner.setEditable(true);
        minuteSpinner.setEditable(true);
        datePicker.setDisable(skyMapTime.isUseSystemTime());
        hourSpinner.setDisable(skyMapTime.isUseSystemTime());
        minuteSpinner.setDisable(skyMapTime.isUseSystemTime());

        useSystemTimeCheck.selectedProperty().addListener((obs, oldValue, newValue) -> {
            datePicker.setDisable(newValue);
            hourSpinner.setDisable(newValue);
            minuteSpinner.setDisable(newValue);
        });

        grid.add(new Label("Data:"), 0, 0);
        grid.add(datePicker, 1, 0);
        grid.add(new Label("Godzina:"), 0, 1);
        grid.add(hourSpinner, 1, 1);
        grid.add(new Label("Minuty:"), 0, 2);
        grid.add(minuteSpinner, 1, 2);
        grid.add(useSystemTimeCheck, 0, 3, 2, 1);

        dialog.getDialogPane().setContent(grid);
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        dialog.showAndWait().ifPresent(result -> {
            if (result == ButtonType.OK) {
                if (useSystemTimeCheck.isSelected()) {
                    skyMapTime.setUseSystemTime(true);
                } else {
                    LocalDate chosenDate = datePicker.getValue();
                    if (chosenDate == null) {
                        chosenDate = LocalDate.now();
                    }
                    LocalTime chosenTime = LocalTime.of(hourSpinner.getValue(), minuteSpinner.getValue());
                    skyMapTime.setSelectedDateTime(LocalDateTime.of(chosenDate, chosenTime));
                    skyMapTime.setUseSystemTime(false);
                }
                refreshTimeLabel();
            }
        });
    }

    private VBox createBottomPanel() {
        VBox panel = new VBox(5);
        panel.setPadding(new Insets(10));
        panel.setStyle("-fx-background-color: #2b2b2b;");
        statusLabel = new Label("Oczekiwanie na dane z Modbus...");
        statusLabel.setStyle("-fx-text-fill: #00ff00; -fx-font-family: monospace;");
        timeLabel = new Label();
        refreshTimeLabel();
        panel.getChildren().addAll(statusLabel, timeLabel);
        return panel;
    }

    private void refreshTimeLabel() {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
        String text;
        if (skyMapTime.isUseSystemTime()) {
            text = "Czas mapy: aktualny systemowy (" + LocalDateTime.now().format(formatter) + ")";
        } else {
            text = "Czas mapy: " + skyMapTime.getSelectedDateTime().format(formatter);
        }
        if (timeLabel != null) {
            timeLabel.setText(text);
        }
    }

    private void startTelescopePolling() {
        Thread modbusThread = new Thread(() -> {
            while (!Thread.currentThread().isInterrupted()) {
                try {
                    currentStatus = telescopeController.getStatus();
                    StringBuilder sb = new StringBuilder();
                    sb.append(String.format("StarAz: %.2f | StarAlt: %.2f | DomeAz: %.2f | TrkState: %d | Temp: %.1f",
                            currentStatus.starAz, currentStatus.starAlt, currentStatus.domeAz, currentStatus.trkState, currentStatus.telTemperature));
                    Platform.runLater(() -> statusLabel.setText(sb.toString()));
                    Thread.sleep(1000);
                } catch (Exception e) {
                    Platform.runLater(() -> statusLabel.setText("Błąd Modbus: " + e.getMessage()));
                    try { Thread.sleep(3000); } catch (InterruptedException ex) { break; }
                }
            }
        });
        modbusThread.setDaemon(true);
        modbusThread.start();
    }

    private void startMapRenderLoop() {
        new AnimationTimer() {
            @Override public void handle(long now) { renderMap(); }
        }.start();
    }

    private void renderMap() {
        GraphicsContext gc = mapCanvas.getGraphicsContext2D();
        double w = mapCanvas.getWidth(), h = mapCanvas.getHeight();
        gc.clearRect(0, 0, w, h);

        Date mapDate = skyMapTime.resolveDate();

        if (mapBackgroundImage != null) {
            gc.drawImage(mapBackgroundImage, 0, 0, w, h);
        } else {
            gc.setStroke(Color.DARKSLATEGRAY);
            gc.strokeOval(50, 50, w - 100, h - 100);
        }

        for (AstroObject obj : loadedObjects) {
            double[] position = obj.getDeclinationHourAngle(station, mapDate);
            if (position == null) continue;
            drawPoint(gc, obj.getName(), position[0], position[1], Color.WHITE, w, h);
        }

        double[] moonDecHour = Moon.getDeclinationHourAngle(station, mapDate);
        if (moonDecHour != null) {
            drawPoint(gc, "Księżyc", moonDecHour[0], moonDecHour[1], Color.LIGHTGRAY, w, h);
        }

        if (currentStatus != null) {
            drawPoint(gc, "Cel Teleskopu", currentStatus.starAz, currentStatus.starAlt, Color.RED, w, h);
        }
    }

    private void drawPoint(GraphicsContext gc, String name, double az, double alt, Color color, double w, double h) {
        if (alt < 0) return;
        double radius = Math.min(w, h) / 2 - 50;
        double rAlt = radius * (1.0 - (alt / 90.0));
        double radAz = Math.toRadians(az - 90);
        double x = (w / 2) + rAlt * Math.cos(radAz);
        double y = (h / 2) + rAlt * Math.sin(radAz);

        gc.setFill(color);
        gc.fillOval(x - 4, y - 4, 8, 8);
        gc.fillText(name, x + 10, y + 4);
    }

    public static void main(String[] args) { launch(args); }
}
