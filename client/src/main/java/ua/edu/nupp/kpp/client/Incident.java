package ua.edu.nupp.kpp.client;

/**
 * Клієнтська модель екологічного інциденту. F1, F2, F3 та integralIndex розраховує
 * сервер (IncidentService + RiskCalculator): клієнт їх лише отримує та показує.
 * Дата registeredAt зберігається рядком у форматі ISO (рррр-мм-ддTгг:хх:сс).
 */
public class Incident {
    private Long id;
    private long locationId;
    private String title;
    private String category;
    private String description;
    private String registeredAt;
    private IncidentState state;
    private double f1;
    private double f2;
    private double f3;
    private double integralIndex;

    public Incident() {
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public long getLocationId() { return locationId; }
    public void setLocationId(long locationId) { this.locationId = locationId; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public String getRegisteredAt() { return registeredAt; }
    public void setRegisteredAt(String registeredAt) { this.registeredAt = registeredAt; }
    public IncidentState getState() { return state; }
    public void setState(IncidentState state) { this.state = state; }
    public double getF1() { return f1; }
    public void setF1(double f1) { this.f1 = f1; }
    public double getF2() { return f2; }
    public void setF2(double f2) { this.f2 = f2; }
    public double getF3() { return f3; }
    public void setF3(double f3) { this.f3 = f3; }
    public double getIntegralIndex() { return integralIndex; }
    public void setIntegralIndex(double integralIndex) { this.integralIndex = integralIndex; }

    @Override
    public String toString() {
        return "#" + id + " " + title;
    }
}
