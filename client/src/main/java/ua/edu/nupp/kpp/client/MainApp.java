package ua.edu.nupp.kpp.client;

import javafx.application.Application;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.util.Locale;
import java.util.function.Function;

/**
 * Головне вікно JavaFX-клієнта «Екологічні інциденти».
 * Інтерфейс лише збирає введені значення й викликає HttpIncidentClient; після кожної зміни
 * дані повторно завантажуються із сервера. Правила життєвого циклу й формула інтегрального
 * показника виконуються на сервері (IncidentService, RiskCalculator) і тут не дублюються.
 */
public class MainApp extends Application {

    private final HttpIncidentClient api = new HttpIncidentClient();

    private final ObservableList<Location> locations = FXCollections.observableArrayList();
    private final ObservableList<Incident> incidents = FXCollections.observableArrayList();
    private final ObservableList<Observation> observations = FXCollections.observableArrayList();
    private final ObservableList<ResponseMeasure> measures = FXCollections.observableArrayList();

    @Override
    public void start(Stage stage) {
        TabPane tabs = new TabPane(
                locationsTab(), incidentsTab(), observationsTab(), measuresTab());
        tabs.setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE);

        stage.setTitle("Інформаційна система реєстрації та опрацювання екологічних інцидентів");
        stage.setScene(new Scene(tabs, 1150, 700));
        stage.show();
        refreshAll();
    }

    public static void main(String[] args) {
        launch(args);
    }

    // ------------------------------------------------------------------ оновлення даних

    private void refreshAll() {
        safe(() -> {
            locations.setAll(api.getLocations());
            incidents.setAll(api.getIncidents());
            observations.setAll(api.getObservations());
            measures.setAll(api.getMeasures());
        });
    }

    // ------------------------------------------------------------------ вкладка «Локації»

    private Tab locationsTab() {
        TableView<Location> table = new TableView<>(locations);
        table.getColumns().add(column("ID", "id", 60));
        table.getColumns().add(column("Назва", "name", 320));
        table.getColumns().add(column("Область", "region", 200));
        table.getColumns().add(column("Широта", "latitude", 100));
        table.getColumns().add(column("Довгота", "longitude", 100));

        TextField name = new TextField();
        TextField region = new TextField();
        TextField lat = new TextField();
        TextField lon = new TextField();

        table.getSelectionModel().selectedItemProperty().addListener((o, a, l) -> {
            if (l != null) {
                name.setText(l.getName());
                region.setText(l.getRegion());
                lat.setText(String.valueOf(l.getLatitude()));
                lon.setText(String.valueOf(l.getLongitude()));
            }
        });

        Button add = new Button("Додати");
        add.setOnAction(e -> safe(() -> {
            api.addLocation(readLocation(null, name, region, lat, lon));
            refreshAll();
        }));
        Button edit = new Button("Змінити");
        edit.setOnAction(e -> safe(() -> {
            Location sel = selected(table);
            api.updateLocation(readLocation(sel.getId(), name, region, lat, lon));
            refreshAll();
        }));
        Button del = new Button("Видалити");
        del.setOnAction(e -> safe(() -> {
            api.deleteLocation(selected(table).getId());
            refreshAll();
        }));
        Button refresh = new Button("Оновити");
        refresh.setOnAction(e -> refreshAll());

        GridPane form = grid();
        form.addRow(0, new Label("Назва місця:"), name, new Label("Область:"), region);
        form.addRow(1, new Label("Широта:"), lat, new Label("Довгота:"), lon);

        return tab("Локації", table, form, new HBox(8, add, edit, del, refresh));
    }

    private Location readLocation(Long id, TextField name, TextField region, TextField lat, TextField lon) {
        return new Location(id, name.getText(), region.getText(),
                Double.parseDouble(lat.getText().trim().replace(',', '.')),
                Double.parseDouble(lon.getText().trim().replace(',', '.')));
    }

    // ------------------------------------------------------------------ вкладка «Інциденти»

    private Tab incidentsTab() {
        TableView<Incident> table = new TableView<>(incidents);
        table.getColumns().add(column("ID", "id", 50));
        TableColumn<Incident, String> locCol = new TableColumn<>("Локація");
        locCol.setPrefWidth(200);
        locCol.setCellValueFactory(c -> new SimpleStringProperty(
                locationName(c.getValue().getLocationId())));
        table.getColumns().add(locCol);
        table.getColumns().add(column("Назва інциденту", "title", 260));
        table.getColumns().add(column("Категорія", "category", 140));
        table.getColumns().add(column("Стан", "state", 150));
        table.getColumns().add(numberColumn("F1", Incident::getF1));
        table.getColumns().add(numberColumn("F2", Incident::getF2));
        table.getColumns().add(numberColumn("F3", Incident::getF3));
        table.getColumns().add(numberColumn("Показник I", Incident::getIntegralIndex));

        ComboBox<Location> location = new ComboBox<>(locations);
        TextField title = new TextField();
        ComboBox<String> category = new ComboBox<>(FXCollections.observableArrayList(
                "WATER_POLLUTION", "AIR_POLLUTION", "SOIL_POLLUTION", "WASTE"));
        category.setEditable(true);
        TextArea description = new TextArea();
        description.setPrefRowCount(2);
        description.setWrapText(true);
        TextField registeredAt = new TextField();
        registeredAt.setPromptText("рррр-мм-дд гг:хх:сс (порожньо — зараз)");

        ComboBox<IncidentState> state = new ComboBox<>(
                FXCollections.observableArrayList(IncidentState.values()));
        Label indexLabel = new Label("Оберіть інцидент у таблиці, щоб побачити інтегральний показник");

        table.getSelectionModel().selectedItemProperty().addListener((o, a, i) -> {
            if (i != null) {
                selectById(location, locations, i.getLocationId(), Location::getId);
                title.setText(i.getTitle());
                category.getEditor().setText(i.getCategory());
                description.setText(i.getDescription());
                registeredAt.setText(i.getRegisteredAt() == null ? "" : i.getRegisteredAt().replace('T', ' '));
                state.setValue(i.getState());
                indexLabel.setText(String.format(Locale.ROOT,
                        "Інтегральний показник I = %.2f  (F1 = %.2f, F2 = %.2f, F3 = %.2f)",
                        i.getIntegralIndex(), i.getF1(), i.getF2(), i.getF3()));
            }
        });

        Button add = new Button("Зареєструвати");
        add.setOnAction(e -> safe(() -> {
            api.addIncident(readIncident(null, location, title, category, description, registeredAt));
            refreshAll();
        }));
        Button edit = new Button("Змінити");
        edit.setOnAction(e -> safe(() -> {
            Incident sel = selected(table);
            api.updateIncident(readIncident(sel.getId(), location, title, category, description, registeredAt));
            refreshAll();
        }));
        Button del = new Button("Видалити");
        del.setOnAction(e -> safe(() -> {
            api.deleteIncident(selected(table).getId());
            refreshAll();
        }));
        Button refresh = new Button("Оновити");
        refresh.setOnAction(e -> refreshAll());

        Button changeState = new Button("Змінити стан");
        changeState.setOnAction(e -> safe(() -> {
            Incident sel = selected(table);
            if (state.getValue() == null) {
                throw new IllegalArgumentException("Оберіть новий стан");
            }
            // Клієнт лише передає бажаний стан, чи дозволений перехід — вирішує сервер
            api.changeIncidentState(sel.getId(), state.getValue());
            refreshAll();
            selectById(table, incidents, sel.getId(), Incident::getId);
        }));
        Button recalc = new Button("Перерахувати показник");
        recalc.setOnAction(e -> safe(() -> {
            Incident sel = selected(table);
            api.recalculateIncident(sel.getId());
            refreshAll();
            selectById(table, incidents, sel.getId(), Incident::getId);
        }));

        GridPane form = grid();
        form.addRow(0, new Label("Локація:"), location, new Label("Назва інциденту:"), title);
        form.addRow(1, new Label("Категорія:"), category, new Label("Час реєстрації:"), registeredAt);
        form.addRow(2, new Label("Опис:"), description);
        GridPane.setColumnSpan(description, 3);
        form.addRow(3, new Label("Стан життєвого циклу:"), state, changeState, recalc);

        VBox extra = new VBox(8, form, indexLabel);
        return tab("Інциденти", table, extra, new HBox(8, add, edit, del, refresh));
    }

    private Incident readIncident(Long id, ComboBox<Location> location, TextField title,
                                  ComboBox<String> category, TextArea description, TextField registeredAt) {
        if (location.getValue() == null) {
            throw new IllegalArgumentException("Оберіть локацію");
        }
        Incident i = new Incident();
        i.setId(id);
        i.setLocationId(location.getValue().getId());
        i.setTitle(title.getText());
        i.setCategory(category.getEditor().getText());
        i.setDescription(description.getText());
        i.setRegisteredAt(registeredAt.getText());
        return i;
    }

    // ------------------------------------------------------------------ вкладка «Спостереження»

    private Tab observationsTab() {
        TableView<Observation> table = new TableView<>(observations);
        table.getColumns().add(column("ID", "id", 50));
        TableColumn<Observation, String> incCol = new TableColumn<>("Інцидент");
        incCol.setPrefWidth(230);
        incCol.setCellValueFactory(c -> new SimpleStringProperty(
                incidentTitle(c.getValue().getIncidentId())));
        table.getColumns().add(incCol);
        TableColumn<Observation, String> timeCol = new TableColumn<>("Час спостереження");
        timeCol.setPrefWidth(160);
        timeCol.setCellValueFactory(c -> new SimpleStringProperty(
                c.getValue().getObservedAt() == null ? "" : c.getValue().getObservedAt().replace('T', ' ')));
        table.getColumns().add(timeCol);
        table.getColumns().add(column("Контрольований показник", "parameter", 220));
        table.getColumns().add(column("Виміряно M", "measuredValue", 100));
        table.getColumns().add(column("Норма L", "limitValue", 90));
        table.getColumns().add(column("Масштаб F2", "scaleScore", 100));
        table.getColumns().add(column("Тривалість F3", "durationScore", 110));

        ComboBox<Incident> incident = new ComboBox<>(incidents);
        TextField observedAt = new TextField();
        observedAt.setPromptText("рррр-мм-дд гг:хх:сс");
        TextField parameter = new TextField();
        TextField measured = new TextField();
        TextField limit = new TextField();
        TextField scale = new TextField();
        TextField duration = new TextField();

        table.getSelectionModel().selectedItemProperty().addListener((o, a, s) -> {
            if (s != null) {
                selectById(incident, incidents, s.getIncidentId(), Incident::getId);
                observedAt.setText(s.getObservedAt() == null ? "" : s.getObservedAt().replace('T', ' '));
                parameter.setText(s.getParameter());
                measured.setText(String.valueOf(s.getMeasuredValue()));
                limit.setText(String.valueOf(s.getLimitValue()));
                scale.setText(String.valueOf(s.getScaleScore()));
                duration.setText(String.valueOf(s.getDurationScore()));
            }
        });

        Button add = new Button("Додати");
        add.setOnAction(e -> safe(() -> {
            api.addObservation(readObservation(null, incident, observedAt, parameter, measured, limit, scale, duration));
            refreshAll();
        }));
        Button edit = new Button("Змінити");
        edit.setOnAction(e -> safe(() -> {
            Observation sel = selected(table);
            api.updateObservation(readObservation(sel.getId(), incident, observedAt, parameter,
                    measured, limit, scale, duration));
            refreshAll();
        }));
        Button del = new Button("Видалити");
        del.setOnAction(e -> safe(() -> {
            api.deleteObservation(selected(table).getId());
            refreshAll();
        }));
        Button refresh = new Button("Оновити");
        refresh.setOnAction(e -> refreshAll());

        // Фоновий CSV-імпорт (етап IV): Task, ProgressBar, статус, блокування повторного запуску
        Button csv = new Button("Імпорт CSV");
        Button cancelCsv = new Button("Скасувати імпорт");
        cancelCsv.setDisable(true);
        ProgressBar progress = new ProgressBar(0);
        progress.setPrefWidth(220);
        Label csvStatus = new Label("Імпорт не запускався. Файл: " + ObservationCsvImporter.DEFAULT_FILE);

        csv.setOnAction(e -> {
            Alert confirm = new Alert(Alert.AlertType.CONFIRMATION,
                    "Записи з файла " + ObservationCsvImporter.DEFAULT_FILE
                            + " буде додано до бази. Повторний імпорт створює дублікати. Продовжити?");
            confirm.setHeaderText("Імпорт CSV");
            if (confirm.showAndWait().filter(b -> b == ButtonType.OK).isEmpty()) {
                return;
            }
            // Кожен Task запускається лише один раз; поки імпорт триває, кнопку заблоковано
            ImportObservationsTask task = new ImportObservationsTask(ObservationCsvImporter.DEFAULT_FILE, api);
            csv.setDisable(true);
            cancelCsv.setDisable(false);
            progress.progressProperty().bind(task.progressProperty());
            csvStatus.textProperty().bind(task.messageProperty());
            cancelCsv.setOnAction(ev -> task.cancel());

            task.setOnSucceeded(ev -> {
                finishImport(csv, cancelCsv, progress, csvStatus);
                ObservationCsvImporter.Result r = task.getValue();
                progress.setProgress(1);
                csvStatus.setText("Імпорт завершено. Імпортовано: " + r.imported() + " з " + r.total());
                refreshAll();
                showInfo("Усього рядків: " + r.total() + "\nІмпортовано: " + r.imported()
                        + "\nПропущено (формат): " + r.skipped()
                        + "\nВідхилено сервером: " + r.rejected()
                        + (r.cancelled() ? "\nІмпорт було скасовано." : "")
                        + (r.messages().isEmpty() ? "" : "\n\n" + String.join("\n", r.messages())));
            });
            task.setOnFailed(ev -> {
                finishImport(csv, cancelCsv, progress, csvStatus);
                progress.setProgress(0);
                csvStatus.setText("Імпорт зупинено через помилку");
                Throwable ex = task.getException();
                refreshAll();
                showError(ex == null || ex.getMessage() == null ? "Помилка імпорту"
                        : (ex instanceof java.nio.file.NoSuchFileException
                        ? "CSV-файл не знайдено: " + ex.getMessage()
                        + ". Запускайте клієнт із каталогу client." : ex.getMessage()));
            });
            task.setOnCancelled(ev -> {
                finishImport(csv, cancelCsv, progress, csvStatus);
                csvStatus.setText("Імпорт скасовано");
                refreshAll();
            });

            Thread worker = new Thread(task, "csv-import");
            worker.setDaemon(true);
            worker.start();
        });

        GridPane form = grid();
        form.addRow(0, new Label("Інцидент:"), incident, new Label("Час спостереження:"), observedAt);
        form.addRow(1, new Label("Контрольований показник:"), parameter, new Label("Виміряне значення M:"), measured);
        form.addRow(2, new Label("Допустима норма L:"), limit, new Label("Оцінка масштабу F2 (0–100):"), scale);
        form.addRow(3, new Label("Оцінка тривалості F3 (0–100):"), duration);

        HBox csvBox = new HBox(8, csv, cancelCsv, progress, csvStatus);
        VBox formAndCsv = new VBox(10, form, csvBox);
        return tab("Спостереження", table, formAndCsv, new HBox(8, add, edit, del, refresh));
    }

    /** Знімає прив'язки (setText на прив'язаному Label недопустимий) і розблоковує кнопки. */
    private static void finishImport(Button csv, Button cancelCsv, ProgressBar progress, Label status) {
        progress.progressProperty().unbind();
        status.textProperty().unbind();
        csv.setDisable(false);
        cancelCsv.setDisable(true);
    }

    private Observation readObservation(Long id, ComboBox<Incident> incident, TextField observedAt,
                                        TextField parameter, TextField measured, TextField limit,
                                        TextField scale, TextField duration) {
        if (incident.getValue() == null) {
            throw new IllegalArgumentException("Оберіть інцидент");
        }
        Observation o = new Observation();
        o.setId(id);
        o.setIncidentId(incident.getValue().getId());
        o.setObservedAt(observedAt.getText().trim());
        o.setParameter(parameter.getText());
        o.setMeasuredValue(number(measured));
        o.setLimitValue(number(limit));
        o.setScaleScore(number(scale));
        o.setDurationScore(number(duration));
        return o;
    }

    // ------------------------------------------------------------------ вкладка «Заходи реагування»

    private Tab measuresTab() {
        TableView<ResponseMeasure> table = new TableView<>(measures);
        table.getColumns().add(column("ID", "id", 50));
        TableColumn<ResponseMeasure, String> incCol = new TableColumn<>("Інцидент");
        incCol.setPrefWidth(230);
        incCol.setCellValueFactory(c -> new SimpleStringProperty(
                incidentTitle(c.getValue().getIncidentId())));
        table.getColumns().add(incCol);
        table.getColumns().add(column("Захід реагування", "description", 300));
        table.getColumns().add(column("Відповідальний", "responsible", 220));
        table.getColumns().add(column("Планова дата", "plannedDate", 110));
        TableColumn<ResponseMeasure, String> doneCol = new TableColumn<>("Виконано");
        doneCol.setPrefWidth(90);
        doneCol.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().isCompleted() ? "Так" : "Ні"));
        table.getColumns().add(doneCol);

        ComboBox<Incident> incident = new ComboBox<>(incidents);
        TextField description = new TextField();
        TextField responsible = new TextField();
        TextField plannedDate = new TextField();
        plannedDate.setPromptText("рррр-мм-дд");
        CheckBox completed = new CheckBox("Виконано");

        table.getSelectionModel().selectedItemProperty().addListener((o, a, m) -> {
            if (m != null) {
                selectById(incident, incidents, m.getIncidentId(), Incident::getId);
                description.setText(m.getDescription());
                responsible.setText(m.getResponsible());
                plannedDate.setText(m.getPlannedDate());
                completed.setSelected(m.isCompleted());
            }
        });

        Button add = new Button("Додати");
        add.setOnAction(e -> safe(() -> {
            api.addMeasure(readMeasure(null, incident, description, responsible, plannedDate, completed));
            refreshAll();
        }));
        Button edit = new Button("Змінити");
        edit.setOnAction(e -> safe(() -> {
            ResponseMeasure sel = selected(table);
            api.updateMeasure(readMeasure(sel.getId(), incident, description, responsible, plannedDate, completed));
            refreshAll();
        }));
        Button del = new Button("Видалити");
        del.setOnAction(e -> safe(() -> {
            api.deleteMeasure(selected(table).getId());
            refreshAll();
        }));
        Button refresh = new Button("Оновити");
        refresh.setOnAction(e -> refreshAll());

        GridPane form = grid();
        form.addRow(0, new Label("Інцидент:"), incident, new Label("Захід реагування:"), description);
        form.addRow(1, new Label("Відповідальний:"), responsible, new Label("Планова дата:"), plannedDate);
        form.addRow(2, completed);

        return tab("Заходи реагування", table, form, new HBox(8, add, edit, del, refresh));
    }

    private ResponseMeasure readMeasure(Long id, ComboBox<Incident> incident, TextField description,
                                        TextField responsible, TextField plannedDate, CheckBox completed) {
        if (incident.getValue() == null) {
            throw new IllegalArgumentException("Оберіть інцидент");
        }
        ResponseMeasure m = new ResponseMeasure();
        m.setId(id);
        m.setIncidentId(incident.getValue().getId());
        m.setDescription(description.getText());
        m.setResponsible(responsible.getText());
        m.setPlannedDate(plannedDate.getText().trim());
        m.setCompleted(completed.isSelected());
        return m;
    }

    // ------------------------------------------------------------------ допоміжні методи інтерфейсу

    private static Tab tab(String title, TableView<?> table, javafx.scene.Node form, HBox buttons) {
        VBox bottom = new VBox(10, form, buttons);
        bottom.setPadding(new Insets(10));
        BorderPane pane = new BorderPane();
        pane.setCenter(table);
        pane.setBottom(bottom);
        return new Tab(title, pane);
    }

    private static GridPane grid() {
        GridPane g = new GridPane();
        g.setHgap(8);
        g.setVgap(8);
        return g;
    }

    private static <T> TableColumn<T, Object> column(String title, String property, double width) {
        TableColumn<T, Object> c = new TableColumn<>(title);
        c.setPrefWidth(width);
        c.setCellValueFactory(new PropertyValueFactory<>(property));
        return c;
    }

    private static <T> TableColumn<T, String> numberColumn(String title, Function<T, Double> getter) {
        TableColumn<T, String> c = new TableColumn<>(title);
        c.setPrefWidth(90);
        c.setCellValueFactory(cd -> new SimpleStringProperty(
                String.format(Locale.ROOT, "%.2f", getter.apply(cd.getValue()))));
        return c;
    }

    private static <T> T selected(TableView<T> table) {
        T item = table.getSelectionModel().getSelectedItem();
        if (item == null) {
            throw new IllegalArgumentException("Виберіть запис");
        }
        return item;
    }

    /** Вибирає у списку (ComboBox або TableView) елемент із заданим id. */
    private static <T> void selectById(ComboBox<T> box, ObservableList<T> items, long id,
                                       Function<T, Long> idOf) {
        for (T item : items) {
            if (idOf.apply(item) == id) {
                box.setValue(item);
                return;
            }
        }
        box.setValue(null);
    }

    private static <T> void selectById(TableView<T> table, ObservableList<T> items, long id,
                                       Function<T, Long> idOf) {
        for (T item : items) {
            if (idOf.apply(item) == id) {
                table.getSelectionModel().select(item);
                return;
            }
        }
    }

    private String locationName(long id) {
        for (Location l : locations) {
            if (l.getId() != null && l.getId() == id) {
                return l.getName();
            }
        }
        return "#" + id;
    }

    private String incidentTitle(long id) {
        for (Incident i : incidents) {
            if (i.getId() != null && i.getId() == id) {
                return "#" + id + " " + i.getTitle();
            }
        }
        return "#" + id;
    }

    private static double number(TextField field) {
        return Double.parseDouble(field.getText().trim().replace(',', '.'));
    }

    /** Усі дії користувача виконуються через safe(): помилка показується вікном, а не stack trace. */
    private void safe(Runnable action) {
        try {
            action.run();
        } catch (NumberFormatException e) {
            showError("Некоректне числове значення. Перевірте поля форми.");
        } catch (RuntimeException e) {
            showError(e.getMessage() == null ? "Невідома помилка" : e.getMessage());
        }
    }

    private static void showError(String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR, message);
        alert.setHeaderText("Помилка");
        alert.showAndWait();
    }

    private static void showInfo(String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION, message);
        alert.setHeaderText("Імпорт CSV");
        alert.showAndWait();
    }
}
