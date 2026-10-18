package com.aruw.oficina.domain.estoque;

import com.aruw.oficina.domain.estoque.evento.EstoqueAbaixoDoMinimo;
import com.aruw.oficina.domain.shared.DomainException;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PecaTest {

    private Peca novaPeca(int quantidade, int minimo) {
        return new Peca("filtro-01", "Filtro", "Filtro de óleo", TipoItemEstoque.PECA, new BigDecimal("35"), quantidade, minimo);
    }

    @Test
    void criaPecaNormalizandoCodigoEPreco() {
        Peca peca = novaPeca(10, 2);
        assertThat(peca.getId()).isNotNull();
        assertThat(peca.getCodigo()).isEqualTo("FILTRO-01");
        assertThat(peca.getPrecoUnitario()).isEqualByComparingTo("35.00");
        assertThat(peca.getPrecoUnitario().scale()).isEqualTo(2);
        assertThat(peca.getTipo()).isEqualTo(TipoItemEstoque.PECA);
        assertThat(peca.getNome()).isEqualTo("Filtro");
        assertThat(peca.getDescricao()).isEqualTo("Filtro de óleo");
        assertThat(peca.getEstoqueMinimo()).isEqualTo(2);
    }

    @Test
    void entradaEBaixaAlteramSaldo() {
        Peca peca = novaPeca(10, 2);
        peca.darEntrada(5);
        peca.darBaixa(3);
        assertThat(peca.getQuantidadeEstoque()).isEqualTo(12);
        assertThat(peca.eventosPendentes()).isEmpty();
    }

    @Test
    void naoPermiteBaixaMaiorQueSaldo() {
        Peca peca = novaPeca(2, 0);
        assertThatThrownBy(() -> peca.darBaixa(3))
                .isInstanceOf(EstoqueInsuficienteException.class)
                .hasMessageContaining("disponível 2");
        assertThat(peca.getQuantidadeEstoque()).isEqualTo(2);
    }

    @Test
    void baixaQueAtingeMinimoRegistraEvento() {
        Peca peca = novaPeca(5, 3);
        peca.darBaixa(2);
        assertThat(peca.abaixoDoMinimo()).isTrue();
        assertThat(peca.puxarEventos()).singleElement().isInstanceOf(EstoqueAbaixoDoMinimo.class);
        assertThat(peca.puxarEventos()).isEmpty();
    }

    @Test
    void quantidadeDeMovimentacaoDeveSerPositiva() {
        Peca peca = novaPeca(5, 0);
        assertThatThrownBy(() -> peca.darEntrada(0)).isInstanceOf(DomainException.class);
        assertThatThrownBy(() -> peca.darBaixa(-1)).isInstanceOf(DomainException.class);
    }

    @Test
    void validaDadosObrigatorios() {
        BigDecimal preco = BigDecimal.ONE;
        assertThatThrownBy(() -> new Peca(" ", "n", null, TipoItemEstoque.PECA, preco, 0, 0)).hasMessageContaining("Código");
        assertThatThrownBy(() -> new Peca("c", "n", null, TipoItemEstoque.PECA, preco, -1, 0)).hasMessageContaining("inicial");
        assertThatThrownBy(() -> new Peca("c", "", null, TipoItemEstoque.PECA, preco, 0, 0)).hasMessageContaining("Nome");
        assertThatThrownBy(() -> new Peca("c", "n", null, null, preco, 0, 0)).hasMessageContaining("Tipo");
        assertThatThrownBy(() -> new Peca("c", "n", null, TipoItemEstoque.INSUMO, new BigDecimal("-1"), 0, 0)).hasMessageContaining("Preço");
        assertThatThrownBy(() -> new Peca("c", "n", null, TipoItemEstoque.INSUMO, preco, 0, -1)).hasMessageContaining("mínimo");
    }

    @Test
    void movimentacoesDeEstoque() {
        UUID pecaId = UUID.randomUUID();
        UUID osId = UUID.randomUUID();
        MovimentacaoEstoque entrada = MovimentacaoEstoque.entrada(pecaId, 3, "Compra");
        MovimentacaoEstoque saida = MovimentacaoEstoque.saidaPorOrdemServico(pecaId, 1, osId);

        assertThat(entrada.getTipo()).isEqualTo(MovimentacaoEstoque.Tipo.ENTRADA);
        assertThat(entrada.getQuantidade()).isEqualTo(3);
        assertThat(entrada.getMotivo()).isEqualTo("Compra");
        assertThat(entrada.getOrdemServicoId()).isNull();
        assertThat(entrada.getId()).isNotNull();
        assertThat(entrada.getDataHora()).isNotNull();
        assertThat(saida.getTipo()).isEqualTo(MovimentacaoEstoque.Tipo.SAIDA);
        assertThat(saida.getOrdemServicoId()).isEqualTo(osId);
        assertThat(saida.getPecaId()).isEqualTo(pecaId);
    }
}
