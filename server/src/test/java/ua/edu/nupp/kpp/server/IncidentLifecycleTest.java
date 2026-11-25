package ua.edu.nupp.kpp.server;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Тести чотиристанного життєвого циклу REGISTERED → VERIFIED → RESPONSE → CLOSED. */
class IncidentLifecycleTest {

    @Test
    void registeredToVerifiedAllowed() {
        assertTrue(IncidentService.isAllowedTransition(IncidentState.REGISTERED, IncidentState.VERIFIED));
    }

    @Test
    void verifiedToResponseAllowed() {
        assertTrue(IncidentService.isAllowedTransition(IncidentState.VERIFIED, IncidentState.RESPONSE));
    }

    @Test
    void responseToClosedAllowed() {
        assertTrue(IncidentService.isAllowedTransition(IncidentState.RESPONSE, IncidentState.CLOSED));
    }

    @Test
    void registeredToClosedRejected() {
        assertFalse(IncidentService.isAllowedTransition(IncidentState.REGISTERED, IncidentState.CLOSED));
    }

    @Test
    void backwardAndRepeatedTransitionsRejected() {
        assertFalse(IncidentService.isAllowedTransition(IncidentState.VERIFIED, IncidentState.REGISTERED));
        assertFalse(IncidentService.isAllowedTransition(IncidentState.CLOSED, IncidentState.RESPONSE));
        assertFalse(IncidentService.isAllowedTransition(IncidentState.RESPONSE, IncidentState.RESPONSE));
    }

    @Test
    void changeStateRejectsMissingState() {
        // перевірка виконується до звернення до бази даних
        assertThrows(IllegalArgumentException.class, () -> new IncidentService().changeState(1, null));
    }
}
