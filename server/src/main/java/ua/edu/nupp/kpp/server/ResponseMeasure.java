package ua.edu.nupp.kpp.server;

import java.time.LocalDate;

/**
 * Захід реагування на інцидент (таблиця response_measure).
 */
public class ResponseMeasure {

    private Long id;
    private long incidentId;
    private String description;
    private String responsible;
    private LocalDate plannedDate;
    private boolean completed;

    public ResponseMeasure() {
    }

    public ResponseMeasure(Long id,
                           long incidentId,
                           String description,
                           String responsible,
                           LocalDate plannedDate,
                           boolean completed) {
        this.id = id;
        this.incidentId = incidentId;
        this.description = description;
        this.responsible = responsible;
        this.plannedDate = plannedDate;
        this.completed = completed;
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

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getResponsible() {
        return responsible;
    }

    public void setResponsible(String responsible) {
        this.responsible = responsible;
    }

    public LocalDate getPlannedDate() {
        return plannedDate;
    }

    public void setPlannedDate(LocalDate plannedDate) {
        this.plannedDate = plannedDate;
    }

    public boolean isCompleted() {
        return completed;
    }

    public void setCompleted(boolean completed) {
        this.completed = completed;
    }
}
