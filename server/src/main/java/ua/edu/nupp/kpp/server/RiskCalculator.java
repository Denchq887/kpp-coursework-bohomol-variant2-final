package ua.edu.nupp.kpp.server;

import java.util.List;

/**
 * Розрахунок інтегрального показника небезпеки екологічного інциденту:
 * I = 0,40·F1 + 0,35·F2 + 0,25·F3, де всі фактори нормуються до 0..100.
 * <ul>
 *   <li>F1 — тяжкість прояву: нормоване перевищення нормативу;</li>
 *   <li>F2 — масштаб ураження (scaleScore спостереження);</li>
 *   <li>F3 — тривалість/стійкість впливу (durationScore спостереження).</li>
 * </ul>
 * Клас не є ні моделлю, ні DAO: він лише виконує обчислення для Service-рівня.
 */
public final class RiskCalculator {

    public static final double WEIGHT_F1 = 0.40;
    public static final double WEIGHT_F2 = 0.35;
    public static final double WEIGHT_F3 = 0.25;

    /** Значення трьох факторів інциденту (кожне в діапазоні 0..100). */
    public record Factors(double f1, double f2, double f3) {
    }

    private RiskCalculator() {
    }

    /**
     * F1 = max(0; min(100; (measuredValue − limitValue) / limitValue · 100)).
     * Якщо значення не перевищує норматив, F1 = 0.
     */
    public static double factor1(double measuredValue, double limitValue) {
        if (!Double.isFinite(measuredValue) || measuredValue < 0
                || !Double.isFinite(limitValue) || limitValue <= 0) {
            throw new IllegalArgumentException("Некоректні значення для розрахунку F1");
        }
        if (measuredValue <= limitValue) {
            return 0;
        }
        double excess = (measuredValue - limitValue) / limitValue * 100.0;
        return Math.max(0, Math.min(100, excess));
    }

    /**
     * I = 0,40·F1 + 0,35·F2 + 0,25·F3, округлене до двох знаків після коми.
     */
    public static double calculate(double f1, double f2, double f3) {
        requireFactor(f1);
        requireFactor(f2);
        requireFactor(f3);
        double result = WEIGHT_F1 * f1 + WEIGHT_F2 * f2 + WEIGHT_F3 * f3;
        return Math.round(result * 100.0) / 100.0;
    }

    /**
     * Фактори інциденту за всіма його спостереженнями: береться найгірший випадок
     * (максимум) окремо для F1, F2 і F3. Якщо спостережень ще немає — усі фактори 0.
     */
    public static Factors aggregate(List<Observation> observations) {
        double f1 = 0;
        double f2 = 0;
        double f3 = 0;
        for (Observation o : observations) {
            f1 = Math.max(f1, factor1(o.getMeasuredValue(), o.getLimitValue()));
            f2 = Math.max(f2, o.getScaleScore());
            f3 = Math.max(f3, o.getDurationScore());
        }
        return new Factors(f1, f2, f3);
    }

    private static void requireFactor(double f) {
        if (!Double.isFinite(f) || f < 0 || f > 100) {
            throw new IllegalArgumentException("Фактори F1, F2, F3 мають бути в діапазоні 0..100");
        }
    }
}
