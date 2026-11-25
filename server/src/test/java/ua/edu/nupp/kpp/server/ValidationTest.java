package ua.edu.nupp.kpp.server;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Тести валідації основних сутностей. Некоректні дані відхиляються в Service
 * до звернення до DAO, тому MySQL для цих тестів не потрібна.
 */
class ValidationTest {

    @Test
    void locationRejectsBlankName() {
        Location l = new Location(null, " ", "Полтавська область", 49.6, 34.5);
        assertThrows(IllegalArgumentException.class, () -> new LocationService().add(l));
    }

    @Test
    void locationRejectsLatitudeOutOfRange() {
        Location l = new Location(null, "Тест", "Полтавська область", 200, 34.5);
        assertThrows(IllegalArgumentException.class, () -> new LocationService().add(l));
    }

    @Test
    void incidentRejectsBlankTitle() {
        Incident i = new Incident();
        i.setLocationId(1);
        i.setTitle("");
        i.setCategory("WATER_POLLUTION");
        i.setDescription("Опис");
        assertThrows(IllegalArgumentException.class, () -> new IncidentService().add(i));
    }

    @Test
    void observationRejectsDurationScoreAbove100() {
        Observation o = new Observation();
        o.setIncidentId(1);
        o.setObservedAt(LocalDateTime.of(2026, 10, 11, 9, 30));
        o.setParameter("Нафтопродукти");
        o.setMeasuredValue(0.18);
        o.setLimitValue(0.05);
        o.setScaleScore(40);
        o.setDurationScore(120);
        assertThrows(IllegalArgumentException.class, () -> new ObservationService().add(o));
    }

    @Test
    void observationRejectsNonPositiveLimit() {
        Observation o = new Observation();
        o.setIncidentId(1);
        o.setObservedAt(LocalDateTime.of(2026, 10, 11, 9, 30));
        o.setParameter("Нафтопродукти");
        o.setMeasuredValue(0.18);
        o.setLimitValue(0);
        o.setScaleScore(40);
        o.setDurationScore(30);
        assertThrows(IllegalArgumentException.class, () -> new ObservationService().add(o));
    }

    @Test
    void responseMeasureRequiresPlannedDate() {
        ResponseMeasure m = new ResponseMeasure();
        m.setIncidentId(1);
        m.setDescription("Відбір проб");
        m.setResponsible("Лабораторія");
        m.setPlannedDate((LocalDate) null);
        assertThrows(IllegalArgumentException.class, () -> new ResponseMeasureService().add(m));
    }
}
