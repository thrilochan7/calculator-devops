package com.spe.calculator;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class CalculatorTest {

    private final Calculator calculator = new Calculator();

    @Test
    void add() {
        assertEquals(5, calculator.add(2, 3));
        assertEquals(-1, calculator.add(2, -3));
    }

    @Test
    void subtract() {
        assertEquals(-1, calculator.subtract(2, 3));
        assertEquals(7.5, calculator.subtract(10, 2.5));
    }

    @Test
    void multiply() {
        assertEquals(6, calculator.multiply(2, 3));
        assertEquals(0, calculator.multiply(5, 0));
    }

    @Test
    void divide() {
        assertEquals(2.5, calculator.divide(5, 2));
        assertEquals(-3, calculator.divide(9, -3));
    }

    @Test
    void divideByZeroThrows() {
        assertThrows(ArithmeticException.class, () -> calculator.divide(5, 0));
    }
}
