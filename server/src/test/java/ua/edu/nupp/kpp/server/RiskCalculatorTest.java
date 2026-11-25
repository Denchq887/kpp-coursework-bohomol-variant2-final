package ua.edu.nupp.kpp.server;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/** Тести розрахунку факторів і інтегрального показника I = 0,40·F1 + 0,35·F2 + 0,25·F3. */
class RiskCalculatorTest {

    private static Observation observation(double measured, double limit, double scale, double duration) {
        Observation o = new Observation();
        o.setMeasuredValue(measured);
        o.setLimitValue(limit);
        o.setScaleScore(scale);
        o.setDurationScore(duration);
        return o;
    }

    @Test
    void f1WithoutExceedanceIsZero() {
        // Arrange: виміряне 0.04 не перевищує норматив 0.05
        // Act
        double f1 = RiskCalculator.factor1(0.04, 0.05);
        // Assert
        assertEquals(0.0, f1, 1e-9);
    }

    @Test
    void f1ForExceedanceIs52() {
        // (38 − 25) / 25 · 100 = 52
        assertEquals(52.0, RiskCalculator.factor1(38, 25), 1e-9);
    }

    @Test
    void f1IsCappedAt100() {
        // перевищення у 4 рази = 300 % → обмежується до 100
        assertEquals(100.0, RiskCalculator.factor1(100, 25), 1e-9);
    }

    @Test
    void f1RejectsNonPositiveLimit() {
        assertThrows(IllegalArgumentException.class, () -> RiskCalculator.factor1(10, 0));
    }

    @Test
    void integralIndexForControlFactorsIs59_3() {
        // 0,40·52 + 0,35·60 + 0,25·70 = 20,8 + 21 + 17,5 = 59,3
        assertEquals(59.3, RiskCalculator.calculate(52, 60, 70), 1e-9);
    }

    @Test
    void integralIndexRejectsFactorAbove100() {
        assertThrows(IllegalArgumentException.class, () -> RiskCalculator.calculate(101, 10, 10));
    }

    @Test
    void aggregateTakesWorstCaseForEachFactor() {
        // Спостереження 1: F1 = 52, F2 = 40, F3 = 70; спостереження 2: F1 = 0, F2 = 60, F3 = 30
        List<Observation> list = List.of(observation(38, 25, 40, 70), observation(20, 25, 60, 30));
        RiskCalculator.Factors f = RiskCalculator.aggregate(list);
        assertEquals(52.0, f.f1(), 1e-9);
        assertEquals(60.0, f.f2(), 1e-9);
        assertEquals(70.0, f.f3(), 1e-9);
    }

    @Test
    void aggregateWithoutObservationsIsZero() {
        RiskCalculator.Factors f = RiskCalculator.aggregate(List.of());
        assertEquals(0.0, RiskCalculator.calculate(f.f1(), f.f2(), f.f3()), 1e-9);
    }
}
