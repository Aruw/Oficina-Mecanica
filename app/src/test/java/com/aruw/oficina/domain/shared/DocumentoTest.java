package com.aruw.oficina.domain.shared;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class DocumentoTest {

    @ParameterizedTest
    @ValueSource(strings = {"529.982.247-25", "52998224725", " 111.444.777-35 "})
    void aceitaCpfValido(String cpf) {
        Documento documento = Documento.of(cpf);
        assertThat(documento.getTipo()).isEqualTo(Documento.Tipo.CPF);
        assertThat(documento.getValor()).matches("\\d{11}");
    }

    @ParameterizedTest
    @ValueSource(strings = {"11.222.333/0001-81", "11222333000181"})
    void aceitaCnpjNumericoValido(String cnpj) {
        Documento documento = Documento.of(cnpj);
        assertThat(documento.getTipo()).isEqualTo(Documento.Tipo.CNPJ);
        assertThat(documento.formatado()).isEqualTo("11.222.333/0001-81");
    }

    @Test
    void aceitaCnpjAlfanumerico() {
        Documento documento = Documento.of("12.abc.345/01de-35");
        assertThat(documento.getTipo()).isEqualTo(Documento.Tipo.CNPJ);
        assertThat(documento.getValor()).isEqualTo("12ABC34501DE35");
        assertThat(documento.formatado()).isEqualTo("12.ABC.345/01DE-35");
    }

    @ParameterizedTest
    @ValueSource(strings = {"529.982.247-24", "111.111.111-11", "123", "11.222.333/0001-80", "00000000000000",
            "12ABC34501DE36", "ABCDEFGHIJK"})
    void rejeitaDocumentoInvalido(String valor) {
        assertThatThrownBy(() -> Documento.of(valor)).isInstanceOf(DomainException.class);
        assertThat(Documento.isValido(valor)).isFalse();
    }

    @ParameterizedTest
    @NullAndEmptySource
    void documentoObrigatorio(String valor) {
        assertThatThrownBy(() -> Documento.of(valor))
                .isInstanceOf(DomainException.class)
                .hasMessageContaining("obrigatório");
    }

    @Test
    void cpfFormatadoEIgualdadePorValor() {
        Documento a = Documento.of("52998224725");
        Documento b = Documento.of("529.982.247-25");
        assertThat(a.formatado()).isEqualTo("529.982.247-25");
        assertThat(a).isEqualTo(b).hasSameHashCodeAs(b);
        assertThat(a.toString()).isEqualTo("529.982.247-25");
        assertThat(a).isNotEqualTo(Documento.of("11144477735"));
    }
}
