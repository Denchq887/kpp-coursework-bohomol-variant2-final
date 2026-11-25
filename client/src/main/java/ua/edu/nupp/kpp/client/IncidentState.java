package ua.edu.nupp.kpp.client;

/**
 * Стани життєвого циклу інциденту. Значення збігаються з серверним enum,
 * бо стан передається через HTTP/JSON. Правила переходів тут не описуються —
 * їх перевіряє IncidentService на сервері.
 */
public enum IncidentState {
    REGISTERED("Зареєстровано"),
    VERIFIED("Підтверджено"),
    RESPONSE("Реагування"),
    CLOSED("Закрито");

    private final String title;

    IncidentState(String title) {
        this.title = title;
    }

    public String getTitle() {
        return title;
    }

    @Override
    public String toString() {
        return title + " (" + name() + ")";
    }
}
