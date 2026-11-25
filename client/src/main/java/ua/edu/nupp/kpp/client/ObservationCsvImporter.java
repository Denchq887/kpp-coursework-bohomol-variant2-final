package ua.edu.nupp.kpp.client;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import java.util.logging.Logger;

/**
 * Імпорт спостережень із CSV. Клас не залежить від JavaFX і HTTP: записи передаються через
 * Consumer (у програмі це HttpIncidentClient.addObservation), а хід виконання — через Listener.
 * Тому логіку можна перевірити модульними тестами, а фонове виконання забезпечує
 * ImportObservationsTask (JavaFX Task).
 *
 * Формат файла sample-data/observations.csv (UTF-8, роздільник «;», перший рядок — заголовок):
 * incident_id;observed_at;parameter;measured_value;limit_value;scale_score;duration_score
 * Усі сім полів обов'язкові. Клієнт перевіряє лише формат (кількість стовпців, порожні поля, типи);
 * діапазони, зв'язки й предметні правила перевіряє Service на сервері.
 *
 * Правило для некоректних рядків: рядок із помилкою формату пропускається (на сервер не
 * передається), помилка записується в журнал і в результат; інші рядки обробляються далі.
 * Рядок, який відхилив сервер (код 400), рахується окремо як rejected. Якщо сервер недоступний,
 * імпорт зупиняється.
 */
public final class ObservationCsvImporter {

    private static final Logger LOG = Logger.getLogger(ObservationCsvImporter.class.getName());

    /** Відносний шлях: файл береться з каталогу sample-data поточного каталогу client. */
    public static final Path DEFAULT_FILE = Path.of("sample-data", "observations.csv");

    public static final String HEADER =
            "incident_id;observed_at;parameter;measured_value;limit_value;scale_score;duration_score";

    private static final int COLUMNS = 7;

    private ObservationCsvImporter() {
    }

    /** Підсумок імпорту. */
    public record Result(int total, int processed, int imported, int skipped, int rejected,
                         boolean cancelled, List<String> messages) {
    }

    /** Отримувач відомостей про хід імпорту (у Task передає їх у ProgressBar і Label). */
    public interface Listener {
        void update(int processed, int total, String message);
    }

    /**
     * Послідовно опрацьовує всі рядки файла.
     *
     * @throws IOException              файл відсутній або не читається
     * @throws IllegalArgumentException заголовок не відповідає формату
     * @throws ServerUnavailableException сервер недоступний (імпорт зупиняється)
     */
    public static Result importAll(Path file, Consumer<Observation> sender, Listener listener,
                                   BooleanSupplier cancelled) throws IOException {
        LOG.info("Початок імпорту CSV: " + file);
        List<String> lines;
        try {
            lines = Files.readAllLines(file, StandardCharsets.UTF_8);
        } catch (IOException e) {
            LOG.severe("Не вдалося прочитати CSV-файл " + file + ": " + e.getClass().getSimpleName());
            throw e;
        }
        if (lines.isEmpty()) {
            LOG.severe("CSV-файл порожній");
            throw new IllegalArgumentException("CSV-файл порожній");
        }
        String header = lines.get(0).replace("﻿", "").trim();
        if (!HEADER.equals(header)) {
            LOG.severe("Неправильний заголовок CSV");
            throw new IllegalArgumentException("Неправильний заголовок CSV. Очікується: " + HEADER);
        }

        int total = 0;
        for (int i = 1; i < lines.size(); i++) {
            if (!lines.get(i).isBlank()) {
                total++;
            }
        }

        List<String> messages = new ArrayList<>();
        int processed = 0;
        int imported = 0;
        int skipped = 0;
        int rejected = 0;
        boolean wasCancelled = false;

        for (int i = 1; i < lines.size(); i++) {
            String line = lines.get(i);
            if (line.isBlank()) {
                continue;
            }
            if (cancelled.getAsBoolean()) {
                wasCancelled = true;
                LOG.warning("Імпорт скасовано користувачем після " + processed + " рядків");
                break;
            }
            int lineNumber = i + 1;
            processed++;
            try {
                Observation o = parseLine(line, lineNumber);
                sender.accept(o);
                imported++;
            } catch (IllegalArgumentException e) {
                skipped++;
                messages.add(e.getMessage());
                LOG.warning("Рядок пропущено: " + e.getMessage());
            } catch (ServerUnavailableException e) {
                LOG.severe("Сервер недоступний, імпорт зупинено. Імпортовано: " + imported);
                throw e;
            } catch (RuntimeException e) {
                rejected++;
                String msg = "Рядок " + lineNumber + ": сервер відхилив запис — " + e.getMessage();
                messages.add(msg);
                LOG.warning(msg);
            }
            listener.update(processed, total,
                    "Опрацьовано " + processed + " з " + total + ", імпортовано " + imported);
        }

        Result result = new Result(total, processed, imported, skipped, rejected, wasCancelled, messages);
        LOG.info("Імпорт CSV завершено: усього " + total + ", опрацьовано " + processed
                + ", імпортовано " + imported + ", пропущено " + skipped
                + ", відхилено сервером " + rejected);
        return result;
    }

    /** Розбирає один рядок і створює клієнтську модель. */
    public static Observation parseLine(String line, int lineNumber) {
        String[] p = line.split(";", -1);
        if (p.length != COLUMNS) {
            throw new IllegalArgumentException("Рядок " + lineNumber + ": очікується "
                    + COLUMNS + " стовпців, знайдено " + p.length);
        }
        for (int i = 0; i < p.length; i++) {
            if (p[i].isBlank()) {
                throw new IllegalArgumentException("Рядок " + lineNumber + ": порожнє обов'язкове поле №" + (i + 1));
            }
        }
        try {
            Observation o = new Observation();
            o.setIncidentId(Long.parseLong(p[0].trim()));
            o.setObservedAt(parseDateTime(p[1].trim()));
            o.setParameter(p[2].trim());
            o.setMeasuredValue(number(p[3]));
            o.setLimitValue(number(p[4]));
            o.setScaleScore(number(p[5]));
            o.setDurationScore(number(p[6]));
            return o;
        } catch (NumberFormatException | DateTimeParseException e) {
            throw new IllegalArgumentException("Рядок " + lineNumber + ": некоректний формат числа або дати");
        }
    }

    private static double number(String value) {
        return Double.parseDouble(value.trim().replace(',', '.'));
    }

    /** Перевіряє дату й приводить її до рядка «рррр-мм-дд гг:хх:сс». */
    private static String parseDateTime(String value) {
        String v = value.replace(' ', 'T');
        if (v.length() == 16) {
            v = v + ":00";
        }
        return LocalDateTime.parse(v).format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
    }
}
