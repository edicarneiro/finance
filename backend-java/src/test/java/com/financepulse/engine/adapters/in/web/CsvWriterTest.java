package com.financepulse.engine.adapters.in.web;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.Test;

class CsvWriterTest {

    @Test
    void writesAHeaderFollowedByEachRowSeparatedByCommasAndCrlf() {
        String csv = CsvWriter.write(List.of("name", "amount"), List.of(List.of("Mercado", "50.00"), List.of("Farmácia", "20.00")));

        assertThat(csv).isEqualTo("name,amount\r\nMercado,50.00\r\nFarmácia,20.00\r\n");
    }

    @Test
    void quotesAValueContainingAComma() {
        String csv = CsvWriter.write(List.of("description"), List.of(List.of("Compra, com vírgula")));

        assertThat(csv).isEqualTo("description\r\n\"Compra, com vírgula\"\r\n");
    }

    @Test
    void escapesInternalQuotesByDoublingThem() {
        String csv = CsvWriter.write(List.of("description"), List.of(List.of("Ele disse \"oi\"")));

        assertThat(csv).isEqualTo("description\r\n\"Ele disse \"\"oi\"\"\"\r\n");
    }

    @Test
    void treatsANullValueAsAnEmptyField() {
        String csv = CsvWriter.write(List.of("description"), List.of(Arrays.asList((String) null)));

        assertThat(csv).isEqualTo("description\r\n\r\n");
    }

    @Test
    void neutralizesAFormulaInjectionAttemptStartingWithAnEqualsSign() {
        String csv = CsvWriter.write(List.of("description"), List.of(List.of("=cmd|'/c calc'!A1")));

        assertThat(csv).isEqualTo("description\r\n'=cmd|'/c calc'!A1\r\n");
    }

    @Test
    void neutralizesFormulaInjectionAttemptsStartingWithPlusMinusOrAt() {
        String csv = CsvWriter.write(List.of("v"), List.of(List.of("+1+1"), List.of("-1+1"), List.of("@SUM(A1)")));

        assertThat(csv).isEqualTo("v\r\n'+1+1\r\n'-1+1\r\n'@SUM(A1)\r\n");
    }

    @Test
    void doesNotNeutralizeAnOrdinaryValueThatMerelyContainsThoseCharacters() {
        String csv = CsvWriter.write(List.of("v"), List.of(List.of("Farmácia - Compra")));

        assertThat(csv).isEqualTo("v\r\nFarmácia - Compra\r\n");
    }
}
