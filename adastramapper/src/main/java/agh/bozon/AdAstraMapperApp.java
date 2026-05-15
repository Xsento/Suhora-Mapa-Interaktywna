package agh.bozon;

import javafx.animation.AnimationTimer;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class AdAstraMapperApp extends Application {

    private TelescopeController telescopeController;
    private Label statusLabel;
    private Canvas mapCanvas;
    private TelescopeStatus currentStatus;

    private Image mapBackgroundImage;
    private final File APP_DIR = new File(System.getProperty("user.home"), "adastramapper");
    private ObservationStation station;
    private List<AstroObject> loadedObjects = new ArrayList<>();

    @Override
    public void start(Stage primaryStage) {
        if (!APP_DIR.exists()) APP_DIR.mkdirs();
        Moon.initOrekit(APP_DIR);
        
        // Domyślna lokalizacja (np. Suhora: 49.568N, 20.067E, 1000m)
        station = new ObservationStation(49.568, 20.067, 1000.0);

        // Inicjalizacja sterownika MOCK (trzeci parametr to true = tryb testowy aktywny)
        telescopeController = new TelescopeController("192.168.2.16", 502, true);

        File imgFile = new File("adastramapper/src/main/resources/static/img/west.gif");
        if (imgFile.exists()) {
            mapBackgroundImage = new Image(imgFile.toURI().toString());
        }
        else
            System.out.print("Brak pliku: " + imgFile.getAbsolutePath() + "\n");

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

        btnLoadCat.setOnAction(e -> {
            FileChooser fc = new FileChooser();
            fc.setInitialDirectory(APP_DIR);
            fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("Pliki Katalogu", "*.cat"));
            File file = fc.showOpenDialog(stage);
            if (file != null) loadedObjects = CatalogManager.loadCatalog(file);
        });

        btnSaveCat.setOnAction(e -> {
            FileChooser fc = new FileChooser();
            fc.setInitialDirectory(APP_DIR);
            fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("Pliki Katalogu", "*.cat"));
            File file = fc.showSaveDialog(stage);
            if (file != null) CatalogManager.saveCatalog(file, loadedObjects);
        });

        btnSetLocation.setOnAction(e -> showLocationDialog());

        return new ToolBar(btnLoadCat, btnSaveCat, new Separator(), btnSetLocation);
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

    private VBox createBottomPanel() {
        VBox panel = new VBox(5);
        panel.setPadding(new Insets(10));
        panel.setStyle("-fx-background-color: #2b2b2b;");
        statusLabel = new Label("Oczekiwanie na dane z Modbus...");
        statusLabel.setStyle("-fx-text-fill: #00ff00; -fx-font-family: monospace;");
        panel.getChildren().add(statusLabel);
        return panel;
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
        
        if (mapBackgroundImage != null) {
            gc.drawImage(mapBackgroundImage, 0, 0, w, h);
        } else {
            gc.setStroke(Color.DARKSLATEGRAY);
            gc.strokeOval(50, 50, w - 100, h - 100);
        }

        // Rysowanie wczytanych obiektów z pliku .cat (Poglądowo, ułożone sferycznie)
        double[] objAzAlt = new double[2];
        AstroObject obj;
        for (int i = 0; i < loadedObjects.size(); i++) {
            obj = loadedObjects.get(i);
            objAzAlt = obj.getAzimuthElevation(station);
            drawPoint(gc, obj.getName(), objAzAlt[0], objAzAlt[1], Color.WHITE, w, h);
        }

        // Rysowanie pozycji Księżyca
        double[] moonAzAlt = Moon.getAzimuthElevation(station);
        if (moonAzAlt != null) {
            drawPoint(gc, "Księżyc", moonAzAlt[0], moonAzAlt[1], Color.LIGHTGRAY, w, h);
        }

        // Rysowanie teleskopu
        if (currentStatus != null) {
            drawPoint(gc, "Cel Teleskopu", currentStatus.starAz, currentStatus.starAlt, Color.RED, w, h);
        }
    }

    private void drawPoint(GraphicsContext gc, String name, double az, double alt, Color color, double w, double h) {
        if (alt < 0) return; // Ukryj obiekty pod horyzontem
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
