package ua.edu.nupp.kpp.client;

/** Сервер не відповідає (Tomcat не запущено, мережева помилка). Відрізняється від відмови сервера з кодом 400/500. */
public class ServerUnavailableException extends RuntimeException {

    public ServerUnavailableException(String message, Throwable cause) {
        super(message, cause);
    }
}
