package ua.edu.nupp.kpp.server;

/**
 * Стани життєвого циклу екологічного інциденту.
 * ЗАРЕЄСТРОВАНО → ПЕРЕВІРЕНО → РЕАГУВАННЯ → ЗАКРИТО.
 * Правила переходів між станами реалізовані на Service-рівні (IncidentService).
 */
public enum IncidentState {
    REGISTERED,
    VERIFIED,
    RESPONSE,
    CLOSED
}
