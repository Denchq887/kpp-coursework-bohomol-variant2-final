DROP DATABASE IF EXISTS ecological_incidents;
CREATE DATABASE ecological_incidents CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE ecological_incidents;

-- Таблиця локацій
CREATE TABLE location (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(160) NOT NULL,
    region VARCHAR(120) NOT NULL,
    latitude DOUBLE NOT NULL,
    longitude DOUBLE NOT NULL
);

-- Таблиця інцидентів (об'єкт життєвого циклу)
-- state: REGISTERED -> VERIFIED -> RESPONSE -> CLOSED
-- f1, f2, f3 нормуються до 0–100; integral_index = 0.40*f1 + 0.35*f2 + 0.25*f3
CREATE TABLE incident (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    location_id BIGINT NOT NULL,
    title VARCHAR(200) NOT NULL,
    category VARCHAR(40) NOT NULL,
    description VARCHAR(1000) NOT NULL,
    registered_at DATETIME NOT NULL,
    state VARCHAR(30) NOT NULL,
    f1 DOUBLE NOT NULL DEFAULT 0,
    f2 DOUBLE NOT NULL DEFAULT 0,
    f3 DOUBLE NOT NULL DEFAULT 0,
    integral_index DOUBLE NOT NULL DEFAULT 0,
    FOREIGN KEY (location_id) REFERENCES location(id)
);

-- Таблиця спостережень (імпортується з CSV)
CREATE TABLE observation (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    incident_id BIGINT NOT NULL,
    observed_at DATETIME NOT NULL,
    parameter VARCHAR(80) NOT NULL,
    measured_value DOUBLE NOT NULL,
    limit_value DOUBLE NOT NULL,
    scale_score DOUBLE NOT NULL,
    duration_score DOUBLE NOT NULL,
    FOREIGN KEY (incident_id) REFERENCES incident(id)
);

-- Таблиця заходів реагування
CREATE TABLE response_measure (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    incident_id BIGINT NOT NULL,
    description VARCHAR(500) NOT NULL,
    responsible VARCHAR(120) NOT NULL,
    planned_date DATE NOT NULL,
    completed BOOLEAN NOT NULL DEFAULT FALSE,
    FOREIGN KEY (incident_id) REFERENCES incident(id)
);

-- Початкові демонстраційні дані
-- Значення f1, f2, f3 та integral_index інцидентів узгоджені з правилом розрахунку на сервері:
-- F1 = нормоване перевищення нормативу, F2 = scale_score, F3 = duration_score
-- (береться максимум за спостереженнями інциденту), I = 0.40*F1 + 0.35*F2 + 0.25*F3.
INSERT INTO location (name, region, latitude, longitude) VALUES
('Річка Ворскла, ділянка в межах Полтави', 'Полтавська область', 49.5883, 34.5514),
('Кременчуцьке водосховище, Кременчуцький район', 'Полтавська область', 49.0659, 33.4100),
('Околиця Миргорода, район стихійного сміттєзвалища', 'Полтавська область', 49.9686, 33.6103);

INSERT INTO incident
(location_id, title, category, description, registered_at, state, f1, f2, f3, integral_index)
VALUES
(1, 'Нафтова плівка на поверхні річки', 'WATER_POLLUTION',
 'Мешканці повідомили про райдужну плівку та характерний запах нафтопродуктів біля берега.',
 '2026-10-10 08:15:00', 'REGISTERED', 60, 35, 30, 43.75),
(2, 'Підвищений вміст сполук азоту у воді', 'WATER_POLLUTION',
 'Під час планового відбору проб виявлено перевищення нормативу за амонійним азотом.',
 '2026-10-12 10:40:00', 'VERIFIED', 80, 50, 60, 64.5),
(3, 'Тління відходів на стихійному сміттєзвалищі', 'AIR_POLLUTION',
 'Задимлення території, перевищення допустимої концентрації дрібнодисперсного пилу.',
 '2026-10-14 14:05:00', 'RESPONSE', 80, 70, 80, 76.5);

INSERT INTO observation
(incident_id, observed_at, parameter, measured_value, limit_value, scale_score, duration_score)
VALUES
(1, '2026-10-10 09:00:00', 'Нафтопродукти (мг/дм³)', 0.08, 0.05, 35, 30),
(2, '2026-10-12 11:30:00', 'Амонійний азот (мг/дм³)', 0.8, 0.5, 45, 45),
(2, '2026-10-13 10:00:00', 'Амонійний азот (мг/дм³)', 0.9, 0.5, 50, 60),
(3, '2026-10-14 15:20:00', 'Пил PM10 (мкг/м³)', 90.0, 50.0, 70, 80);

INSERT INTO response_measure (incident_id, description, responsible, planned_date, completed) VALUES
(3, 'Локалізація осередку тління та його гасіння', 'Аварійно-рятувальний підрозділ', '2026-10-15', FALSE),
(3, 'Відбір проб повітря на межі житлової забудови', 'Лабораторія екологічного моніторингу', '2026-10-16', FALSE);
