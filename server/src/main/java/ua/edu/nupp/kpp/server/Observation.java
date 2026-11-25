package ua.edu.nupp.kpp.server;

import java.time.LocalDateTime;

/**
 * Спостереження за інцидентом (таблиця observation); імпортується з CSV.
 * measuredValue і limitValue використовуються для розрахунку F1,
 * scaleScore — це F2, durationScore — це F3 (вже нормовані до 0..100).
 */
public class Observation {

    private Long id;
    private long incidentId;
    private LocalDateTime observedAt;
    private String parameter;
    private double measuredValue;
    private double limitValue;
    private double scaleScore;
    private double durationScore;

    public Observation() {
    }

    public Observation(Long id,
                       long incidentId,
                       LocalDateTime observedAt,
                       String parameter,
                       double measuredValue,
                       double limitValue,
                       double scaleScore,
                       double durationScore) {
        this.id = id;
        this.incidentId = incidentId;
        this.observedAt = observedAt;
        this.parameter = parameter;
        this.measuredValue = measuredValue;
        this.limitValue = limitValue;
        this.scaleScore = scaleScore;
        this.durationScore = durationScore;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public long getIncidentId() {
        return incidentId;
    }

    public void setIncidentId(long incidentId) {
        this.incidentId = incidentId;
    }

    public LocalDateTime getObservedAt() {
        return observedAt;
    }

    public void setObservedAt(LocalDateTime observedAt) {
        this.observedAt = observedAt;
    }

    public String getParameter() {
        return parameter;
    }

    public void setParameter(String parameter) {
        this.parameter = parameter;
    }

    public double getMeasuredValue() {
        return measuredValue;
    }

    public void setMeasuredValue(double measuredValue) {
        this.measuredValue = measuredValue;
    }

    public double getLimitValue() {
        return limitValue;
    }

    public void setLimitValue(double limitValue) {
        this.limitValue = limitValue;
    }

    public double getScaleScore() {
        return scaleScore;
    }

    public void setScaleScore(double scaleScore) {
        this.scaleScore = scaleScore;
    }

    public double getDurationScore() {
        return durationScore;
    }

    public void setDurationScore(double durationScore) {
        this.durationScore = durationScore;
    }
}
