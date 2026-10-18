package com.aruw.oficina.domain.shared;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PlacaTest {

    @Test
    void aceitaPlacaPadraoAntigoNormalizando() {
        Placa placa = Placa.of("abc-1234");
        assertThat(placa.getValor()).isEqualTo("ABC1234");
        assertThat(placa.isMercosul()).isFalse();
    }

    @Test
    void aceitaPlacaMercosul() {
        Placa placa = Placa.of("BRA2E19");
        assertThat(placa.isMercosul()).isTrue();
        assertThat(placa).hasToString("BRA2E19");
    }

    @ParameterizedTest
    @ValueSource(strings = {"AB1234", "ABCD123", "1234ABC", "ABC12E4", "ABC-12345"})
    void rejeitaPlacaInvalida(String valor) {
        assertThatThrownBy(() -> Placa.of(valor)).isInstanceOf(DomainException.class);
        assertThat(Placa.isValida(valor)).isFalse();
    }

    @ParameterizedTest
    @NullAndEmptySource
    void placaObrigatoria(String valor) {
        assertThatThrownBy(() -> Placa.of(valor)).hasMessageContaining("obrigatória");
    }

    @Test
    void igualdadePorValor() {
        assertThat(Placa.of("abc1d23")).isEqualTo(Placa.of("ABC1D23")).hasSameHashCodeAs(Placa.of("ABC-1D23"));
        assertThat(Placa.of("ABC1D23")).isNotEqualTo(Placa.of("ABC1234"));
    }
}
