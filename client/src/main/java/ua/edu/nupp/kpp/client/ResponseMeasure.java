package ua.edu.nupp.kpp.client;

/** Клієнтська модель заходу реагування. plannedDate — рядок рррр-мм-дд. */
public class ResponseMeasure {
    private Long id;
    private long incidentId;
    private String description;
    private String responsible;
    private String plannedDate;
    private boolean completed;

    public ResponseMeasure() {
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public long getIncidentId() { return incidentId; }
    public void setIncidentId(long incidentId) { this.incidentId = incidentId; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public String getResponsible() { return responsible; }
    public void setResponsible(String responsible) { this.responsible = responsible; }
    public String getPlannedDate() { return plannedDate; }
    public void setPlannedDate(String plannedDate) { this.plannedDate = plannedDate; }
    public boolean isCompleted() { return completed; }
    public void setCompleted(boolean completed) { this.completed = completed; }
}
