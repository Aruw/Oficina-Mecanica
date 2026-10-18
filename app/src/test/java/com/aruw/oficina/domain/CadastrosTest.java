package com.aruw.oficina.domain;

import com.aruw.oficina.domain.catalogo.Servico;
import com.aruw.oficina.domain.cliente.Cliente;
import com.aruw.oficina.domain.shared.Documento;
import com.aruw.oficina.domain.shared.DomainException;
import com.aruw.oficina.domain.shared.Placa;
import com.aruw.oficina.domain.veiculo.Veiculo;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Year;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CadastrosTest {

    @Test
    void clienteValidaNomeEDocumento() {
        Cliente cliente = new Cliente("  Maria  ", Documento.of("52998224725"), "m@x.com", "11999999999");
        assertThat(cliente.getNome()).isEqualTo("Maria");
        assertThat(cliente.getId()).isNotNull();
        assertThat(cliente.getCriadoEm()).isNotNull();
        assertThat(cliente.getEmail()).isEqualTo("m@x.com");
        assertThat(cliente.getTelefone()).isEqualTo("11999999999");

        cliente.atualizar("Maria Silva", null, null);
        assertThat(cliente.getNome()).isEqualTo("Maria Silva");

        assertThatThrownBy(() -> new Cliente("x", null, null, null)).isInstanceOf(DomainException.class);
        assertThatThrownBy(() -> cliente.atualizar(" ", null, null)).hasMessageContaining("Nome");
    }

    @Test
    void veiculoValidaDados() {
        UUID clienteId = UUID.randomUUID();
        Veiculo veiculo = new Veiculo(Placa.of("ABC1D23"), "Fiat", "Uno", 2015, clienteId);
        assertThat(veiculo.pertenceA(clienteId)).isTrue();
        assertThat(veiculo.pertenceA(UUID.randomUUID())).isFalse();
        assertThat(veiculo.getMarca()).isEqualTo("Fiat");
        assertThat(veiculo.getModelo()).isEqualTo("Uno");
        assertThat(veiculo.getAno()).isEqualTo(2015);
        assertThat(veiculo.getCriadoEm()).isNotNull();

        int proximoAno = Year.now(Veiculo.FUSO_HORARIO).getValue() + 1;
        Placa placa = Placa.of("ABC1234");
        assertThatThrownBy(() -> new Veiculo(null, "a", "b", 2000, clienteId)).hasMessageContaining("Placa");
        assertThatThrownBy(() -> new Veiculo(placa, "", "b", 2000, clienteId)).hasMessageContaining("Marca");
        assertThatThrownBy(() -> new Veiculo(placa, "a", null, 2000, clienteId)).hasMessageContaining("Modelo");
        assertThatThrownBy(() -> new Veiculo(placa, "a", "b", 1899, clienteId)).hasMessageContaining("Ano");
        assertThatThrownBy(() -> new Veiculo(placa, "a", "b", proximoAno + 1, clienteId)).hasMessageContaining("Ano");
        assertThatThrownBy(() -> new Veiculo(placa, "a", "b", 2000, null)).hasMessageContaining("cliente");
        assertThat(new Veiculo(placa, "a", "b", proximoAno, clienteId).getAno()).isEqualTo(proximoAno);
    }

    @Test
    void servicoValidaPrecoETempo() {
        Servico servico = new Servico("Alinhamento", "desc", new BigDecimal("120"), 60);
        assertThat(servico.isAtivo()).isTrue();
        assertThat(servico.getPreco()).isEqualByComparingTo("120.00");
        assertThat(servico.getDescricao()).isEqualTo("desc");
        servico.desativar();
        assertThat(servico.isAtivo()).isFalse();
        servico.ativar();
        assertThat(servico.isAtivo()).isTrue();

        assertThatThrownBy(() -> new Servico("", null, BigDecimal.ONE, 1)).hasMessageContaining("Nome");
        assertThatThrownBy(() -> new Servico("a", null, null, 1)).hasMessageContaining("Preço");
        assertThatThrownBy(() -> new Servico("a", null, new BigDecimal("-1"), 1)).hasMessageContaining("Preço");
        assertThatThrownBy(() -> new Servico("a", null, BigDecimal.ONE, 0)).hasMessageContaining("Tempo");
    }
}
