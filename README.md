# Інформаційна система реєстрації та опрацювання екологічних інцидентів

Курсова робота з дисципліни «Кросплатформене програмування», варіант №2
(НУ «Полтавська політехніка», група 204-ТН, автор — Богомол Д. В.).

Система реєструє екологічні інциденти (забруднення води, повітря, ґрунту), зберігає результати
спостережень, автоматично розраховує інтегральний показник небезпеки
`I = 0,40·F1 + 0,35·F2 + 0,25·F3` і веде інцидент чотиристанним життєвим циклом
`REGISTERED → VERIFIED → RESPONSE → CLOSED`.

## Технології

- Java 21, Maven (два окремі модулі)
- Сервер: Servlet API 4.0.1 (`javax.servlet.*`), Apache Tomcat 9.x, Jackson, JDBC, MySQL Connector/J
- Клієнт: JavaFX 21 (`TableView`, `TabPane`, `Task`, `ProgressBar`), `java.net.http.HttpClient`, Jackson
- База даних: MySQL (XAMPP), `database.sql`
- Тести: JUnit 5; журналювання: `java.util.logging`

## Структура

```
database.sql           створення бази ecological_incidents і демонстраційні дані
server/                web-застосунок (WAR): моделі, DAO/JDBC, Service, RiskCalculator, Servlet-и
  src/main/webapp/WEB-INF/web.xml
  src/test/java        модульні тести JUnit 5
client/                JavaFX-клієнт: моделі, HttpIncidentClient, MainApp, імпорт CSV
  sample-data/         observations.csv (та файли для перевірки помилок)
```

Маршрут обробки: `JavaFX → HttpIncidentClient → HTTP/JSON → Servlet → Service → DAO → JDBC → MySQL`.
Клієнт не містить JDBC і не підключається до MySQL напряму.

## Підготовка MySQL

1. Запустіть MySQL в XAMPP Control Panel (для phpMyAdmin додатково Apache).
2. Виконайте `database.sql` (phpMyAdmin → Import). Буде створено базу `ecological_incidents`.
3. Сервер підключається до `localhost:3306` з користувачем `root` без пароля
   (параметри — у `server/.../DBUtil.java`; змініть їх для власного середовища).

## Складання й запуск сервера

```
cd server
mvn clean package
```

Файл `server/target/ecological-incidents-server.war` скопіюйте до `<TOMCAT>/webapps`
(Apache Tomcat 9.x, не 10/11) і запустіть `<TOMCAT>/bin/catalina.bat run`.
Перевірка: <http://localhost:8080/ecological-incidents-server/incidents> повертає JSON.

Маршрути: `/locations`, `/incidents`, `/observations`, `/measures`.
GET — читання; POST (`action=add|update|delete`, для інцидентів також `transition`, `recalculate`) — зміни.

## Запуск JavaFX-клієнта

Коли MySQL і Tomcat запущені:

```
cd client
mvn javafx:run
```

Запускати потрібно з каталогу `client`, щоб працював відносний шлях `sample-data/observations.csv`.

## Тести

```
cd server
mvn clean test
```

Тести перевіряють розрахунок показника, нормування факторів, життєвий цикл і валідацію
і не потребують Tomcat, JavaFX та робочої бази MySQL.
Тести розбору CSV клієнта: `cd client` → `mvn clean test`.

## CSV-імпорт

Файл `client/sample-data/observations.csv`: UTF-8, роздільник `;`, перший рядок — заголовок

```
incident_id;observed_at;parameter;measured_value;limit_value;scale_score;duration_score
1;2026-10-11 09:30:00;Нафтопродукти (мг/дм³);0.18;0.05;40;35
```

Усі сім полів обов'язкові; `observed_at` — `рррр-мм-дд гг:хх:сс`; дробові числа з крапкою.
Імпорт запускається кнопкою «Імпорт CSV» на вкладці «Спостереження» у фоновому потоці
(`Task`) з індикатором `ProgressBar`. Рядки з помилкою формату пропускаються, відмови сервера
рахуються окремо. Повторний імпорт створює дублікати, бо унікальності спостережень немає.
Файли `observations-invalid.csv` і `observations-bad-header.csv` призначені для перевірки помилок.
