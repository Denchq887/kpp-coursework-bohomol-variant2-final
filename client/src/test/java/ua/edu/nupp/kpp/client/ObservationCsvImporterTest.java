package ua.edu.nupp.kpp.client;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Тести розбору CSV та послідовного імпорту (без JavaFX, HTTP і MySQL). */
class ObservationCsvImporterTest {

    private static final String HEADER = ObservationCsvImporter.HEADER;
    private static final String GOOD = "1;2026-10-11 09:30:00;Нафтопродукти (мг/дм³);0.18;0.05;40;35";

    private static Path csv(String... lines) throws IOException {
        Path p = Files.createTempFile("obs", ".csv");
        Files.write(p, List.of(lines), StandardCharsets.UTF_8);
        p.toFile().deleteOnExit();
        return p;
    }

    @Test
    void parseLineCreatesObservation() {
        Observation o = ObservationCsvImporter.parseLine(GOOD, 2);
        assertEquals(1L, o.getIncidentId());
        assertEquals("2026-10-11 09:30:00", o.getObservedAt());
        assertEquals("Нафтопродукти (мг/дм³)", o.getParameter());
        assertEquals(0.18, o.getMeasuredValue(), 1e-9);
        assertEquals(35.0, o.getDurationScore(), 1e-9);
    }

    @Test
    void parseLineRejectsWrongColumnCount() {
        assertThrows(IllegalArgumentException.class,
                () -> ObservationCsvImporter.parseLine("1;2026-10-11 09:30:00;Нафта", 3));
    }

    @Test
    void parseLineRejectsInvalidNumber() {
        assertThrows(IllegalArgumentException.class, () -> ObservationCsvImporter
                .parseLine("1;2026-10-11 09:30:00;Нафта;abc;0.05;40;35", 4));
    }

    @Test
    void importCountsImportedSkippedAndRejected() throws IOException {
        Path file = csv(HEADER, GOOD, "1;2026-10-12;Нафта", "", GOOD.replace("1;", "999;"));
        List<Observation> sent = new ArrayList<>();
        ObservationCsvImporter.Result r = ObservationCsvImporter.importAll(file, o -> {
            if (o.getIncidentId() == 999) {
                throw new RuntimeException("Інцидент не знайдено");
            }
            sent.add(o);
        }, (done, total, msg) -> { }, () -> false);
        assertEquals(3, r.total());
        assertEquals(1, r.imported());
        assertEquals(1, r.skipped());
        assertEquals(1, r.rejected());
        assertEquals(1, sent.size());
    }

    @Test
    void importRejectsWrongHeader() throws IOException {
        Path file = csv("id;time", GOOD);
        assertThrows(IllegalArgumentException.class, () -> ObservationCsvImporter
                .importAll(file, o -> { }, (a, b, c) -> { }, () -> false));
    }

    @Test
    void importFailsForMissingFile() {
        assertThrows(NoSuchFileException.class, () -> ObservationCsvImporter
                .importAll(Path.of("sample-data", "no-such-file.csv"), o -> { }, (a, b, c) -> { }, () -> false));
    }

    @Test
    void importStopsWhenServerUnavailable() throws IOException {
        Path file = csv(HEADER, GOOD, GOOD);
        assertThrows(ServerUnavailableException.class, () -> ObservationCsvImporter.importAll(file, o -> {
            throw new ServerUnavailableException("Сервер недоступний", null);
        }, (a, b, c) -> { }, () -> false));
    }

    @Test
    void importCanBeCancelled() throws IOException {
        Path file = csv(HEADER, GOOD, GOOD, GOOD);
        ObservationCsvImporter.Result r = ObservationCsvImporter
                .importAll(file, o -> { }, (a, b, c) -> { }, () -> true);
        assertTrue(r.cancelled());
        assertEquals(0, r.imported());
    }
}
