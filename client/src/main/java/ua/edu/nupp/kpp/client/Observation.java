package ua.edu.nupp.kpp.client;

/** Клієнтська модель спостереження (результат виміру контрольованого показника). */
public class Observation {
    private Long id;
    private long incidentId;
    private String observedAt;
    private String parameter;
    private double measuredValue;
    private double limitValue;
    private double scaleScore;
    private double durationScore;

    public Observation() {
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public long getIncidentId() { return incidentId; }
    public void setIncidentId(long incidentId) { this.incidentId = incidentId; }
    public String getObservedAt() { return observedAt; }
    public void setObservedAt(String observedAt) { this.observedAt = observedAt; }
    public String getParameter() { return parameter; }
    public void setParameter(String parameter) { this.parameter = parameter; }
    public double getMeasuredValue() { return measuredValue; }
    public void setMeasuredValue(double measuredValue) { this.measuredValue = measuredValue; }
    public double getLimitValue() { return limitValue; }
    public void setLimitValue(double limitValue) { this.limitValue = limitValue; }
    public double getScaleScore() { return scaleScore; }
    public void setScaleScore(double scaleScore) { this.scaleScore = scaleScore; }
    public double getDurationScore() { return durationScore; }
    public void setDurationScore(double durationScore) { this.durationScore = durationScore; }
}
