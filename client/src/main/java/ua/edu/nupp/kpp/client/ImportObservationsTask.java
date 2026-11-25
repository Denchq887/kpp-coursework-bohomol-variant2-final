package ua.edu.nupp.kpp.client;

import javafx.concurrent.Task;

import java.nio.file.Path;

/**
 * Фонове виконання CSV-імпорту. Тривала робота (читання файла, розбір, HTTP-запити) виконується
 * в call() у окремому потоці, тому вікно JavaFX не блокується. Хід виконання передається лише
 * через updateProgress і updateMessage; елементи інтерфейсу тут не змінюються.
 * Результат — підсумок імпорту; кількість успішно імпортованих записів — result.imported().
 */
public class ImportObservationsTask extends Task<ObservationCsvImporter.Result> {

    private final Path file;
    private final HttpIncidentClient api;

    public ImportObservationsTask(Path file, HttpIncidentClient api) {
        this.file = file;
        this.api = api;
    }

    @Override
    protected ObservationCsvImporter.Result call() throws Exception {
        updateMessage("Читання файла " + file);
        updateProgress(0, 1);
        return ObservationCsvImporter.importAll(
                file,
                api::addObservation,
                (processed, total, message) -> {
                    updateProgress(processed, total);
                    updateMessage(message);
                },
                this::isCancelled);
    }
}
