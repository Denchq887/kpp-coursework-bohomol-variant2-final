package ua.edu.nupp.kpp.server;

import java.time.LocalDateTime;

/**
 * Екологічний інцидент — об'єкт життєвого циклу (таблиця incident).
 * Поля f1, f2, f3 та integralIndex лише зберігають значення;
 * їх розраховує серверна частина (RiskCalculator на Service-рівні).
 */
public class Incident {

    private Long id;
    private long locationId;
    private String title;
    private String category;
    private String description;
    private LocalDateTime registeredAt;
    private IncidentState state;
    private double f1;
    private double f2;
    private double f3;
    private double integralIndex;

    public Incident() {
    }

    public Incident(Long id,
                    long locationId,
                    String title,
                    String category,
                    String description,
                    LocalDateTime registeredAt,
                    IncidentState state,
                    double f1,
                    double f2,
                    double f3,
                    double integralIndex) {
        this.id = id;
        this.locationId = locationId;
        this.title = title;
        this.category = category;
        this.description = description;
        this.registeredAt = registeredAt;
        this.state = state;
        this.f1 = f1;
        this.f2 = f2;
        this.f3 = f3;
        this.integralIndex = integralIndex;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public long getLocationId() {
        return locationId;
    }

    public void setLocationId(long locationId) {
        this.locationId = locationId;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public LocalDateTime getRegisteredAt() {
        return registeredAt;
    }

    public void setRegisteredAt(LocalDateTime registeredAt) {
        this.registeredAt = registeredAt;
    }

    public IncidentState getState() {
        return state;
    }

    public void setState(IncidentState state) {
        this.state = state;
    }

    public double getF1() {
        return f1;
    }

    public void setF1(double f1) {
        this.f1 = f1;
    }

    public double getF2() {
        return f2;
    }

    public void setF2(double f2) {
        this.f2 = f2;
    }

    public double getF3() {
        return f3;
    }

    public void setF3(double f3) {
        this.f3 = f3;
    }

    public double getIntegralIndex() {
        return integralIndex;
    }

    public void setIntegralIndex(double integralIndex) {
        this.integralIndex = integralIndex;
    }
}
