package com.aruw.oficina.application;

import com.aruw.oficina.application.catalogo.ServicoCommand;
import com.aruw.oficina.application.catalogo.ServicoService;
import com.aruw.oficina.application.cliente.ClienteCommand;
import com.aruw.oficina.application.cliente.ClienteService;
import com.aruw.oficina.application.estoque.PecaCommand;
import com.aruw.oficina.application.estoque.PecaService;
import com.aruw.oficina.application.shared.PublicadorEventos;
import com.aruw.oficina.application.shared.RecursoNaoEncontradoException;
import com.aruw.oficina.application.veiculo.VeiculoCommand;
import com.aruw.oficina.application.veiculo.VeiculoService;
import com.aruw.oficina.domain.catalogo.Servico;
import com.aruw.oficina.domain.catalogo.ServicoRepository;
import com.aruw.oficina.domain.cliente.Cliente;
import com.aruw.oficina.domain.cliente.ClienteRepository;
import com.aruw.oficina.domain.estoque.EstoqueInsuficienteException;
import com.aruw.oficina.domain.estoque.MovimentacaoEstoque;
import com.aruw.oficina.domain.estoque.MovimentacaoEstoqueRepository;
import com.aruw.oficina.domain.estoque.Peca;
import com.aruw.oficina.domain.estoque.PecaRepository;
import com.aruw.oficina.domain.estoque.TipoItemEstoque;
import com.aruw.oficina.domain.ordemservico.OrdemServicoRepository;
import com.aruw.oficina.domain.shared.Documento;
import com.aruw.oficina.domain.shared.DomainException;
import com.aruw.oficina.domain.shared.Placa;
import com.aruw.oficina.domain.veiculo.Veiculo;
import com.aruw.oficina.domain.veiculo.VeiculoRepository;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class CadastrosServiceTest {

    @Mock ClienteRepository clientes;
    @Mock VeiculoRepository veiculos;
    @Mock OrdemServicoRepository ordens;
    @Mock ServicoRepository servicos;
    @Mock PecaRepository pecas;
    @Mock MovimentacaoEstoqueRepository movimentacoes;
    @Mock PublicadorEventos eventos;

    @Nested
    class Clientes {

        ClienteService service() {
            when(clientes.save(any())).thenAnswer(i -> i.getArgument(0));
            return new ClienteService(clientes, veiculos, ordens);
        }

        @Test
        void criaAtualizaBuscaERemove() {
            ClienteService service = service();
            Cliente criado = service.criar(new ClienteCommand("Ana", "529.982.247-25", "a@a.com", null));
            assertThat(criado.getDocumento().getValor()).isEqualTo("52998224725");

            when(clientes.findById(criado.getId())).thenReturn(Optional.of(criado));
            when(clientes.findByDocumentoValor("52998224725")).thenReturn(Optional.of(criado));
            when(clientes.findAll()).thenReturn(List.of(criado));

            assertThat(service.atualizar(criado.getId(), new ClienteCommand("Ana Maria", null, null, "1199")).getNome())
                    .isEqualTo("Ana Maria");
            assertThat(service.buscarPorDocumento("529.982.247-25")).isSameAs(criado);
            assertThat(service.listar()).hasSize(1);

            service.remover(criado.getId());
            verify(clientes).delete(criado);
        }

        @Test
        void naoDuplicaDocumento() {
            when(clientes.existsByDocumentoValor("52998224725")).thenReturn(true);
            ClienteService service = service();
            assertThatThrownBy(() -> service.criar(new ClienteCommand("Ana", "52998224725", null, null)))
                    .isInstanceOf(DomainException.class);
        }

        @Test
        void naoRemoveClienteComVinculos() {
            Cliente cliente = new Cliente("Ana", Documento.of("52998224725"), null, null);
            when(clientes.findById(cliente.getId())).thenReturn(Optional.of(cliente));
            when(veiculos.existsByClienteId(cliente.getId())).thenReturn(true);
            ClienteService service = service();
            assertThatThrownBy(() -> service.remover(cliente.getId())).isInstanceOf(DomainException.class);
            verify(clientes, never()).delete(any());
        }

        @Test
        void clienteInexistente() {
            ClienteService service = service();
            UUID id = UUID.randomUUID();
            assertThatThrownBy(() -> service.buscar(id)).isInstanceOf(RecursoNaoEncontradoException.class);
            assertThatThrownBy(() -> service.buscarPorDocumento("11144477735")).isInstanceOf(RecursoNaoEncontradoException.class);
        }
    }

    @Nested
    class Veiculos {

        final Cliente dono = new Cliente("Ana", Documento.of("52998224725"), null, null);

        VeiculoService service() {
            when(veiculos.save(any())).thenAnswer(i -> i.getArgument(0));
            when(clientes.findById(dono.getId())).thenReturn(Optional.of(dono));
            return new VeiculoService(veiculos, clientes, ordens);
        }

        @Test
        void criaAtualizaListaERemove() {
            VeiculoService service = service();
            Veiculo v = service.criar(new VeiculoCommand("abc-1234", "VW", "Gol", 2010, dono.getId()));
            when(veiculos.findById(v.getId())).thenReturn(Optional.of(v));
            when(veiculos.findAll()).thenReturn(List.of(v));
            when(veiculos.findByClienteId(dono.getId())).thenReturn(List.of(v));

            assertThat(service.atualizar(v.getId(), new VeiculoCommand("ABC1234", "VW", "Gol G5", 2011, dono.getId())).getModelo())
                    .isEqualTo("Gol G5");
            assertThat(service.listar(null)).hasSize(1);
            assertThat(service.listar(dono.getId())).hasSize(1);

            service.remover(v.getId());
            verify(veiculos).delete(v);
        }

        @Test
        void validaPlacaDuplicadaClienteEVinculos() {
            VeiculoService service = service();
            when(veiculos.existsByPlacaValor("ABC1234")).thenReturn(true);
            assertThatThrownBy(() -> service.criar(new VeiculoCommand("ABC1234", "VW", "Gol", 2010, dono.getId())))
                    .isInstanceOf(DomainException.class);
            assertThatThrownBy(() -> service.criar(new VeiculoCommand("XYZ9876", "VW", "Gol", 2010, UUID.randomUUID())))
                    .isInstanceOf(RecursoNaoEncontradoException.class);

            Veiculo v = new Veiculo(Placa.of("XYZ9876"), "VW", "Gol", 2010, dono.getId());
            when(veiculos.findById(v.getId())).thenReturn(Optional.of(v));
            when(ordens.existsByVeiculoId(v.getId())).thenReturn(true);
            assertThatThrownBy(() -> service.remover(v.getId())).isInstanceOf(DomainException.class);
        }
    }

    @Nested
    class Servicos {

        @Test
        void crud() {
            when(servicos.save(any())).thenAnswer(i -> i.getArgument(0));
            ServicoService service = new ServicoService(servicos);
            Servico s = service.criar(new ServicoCommand("Alinhamento", null, BigDecimal.TEN, 30, null));
            assertThat(s.isAtivo()).isTrue();
            when(servicos.findById(s.getId())).thenReturn(Optional.of(s));
            when(servicos.findAll()).thenReturn(List.of(s));

            assertThat(service.atualizar(s.getId(), new ServicoCommand("Alinhamento 3D", null, BigDecimal.TEN, 40, false)).isAtivo())
                    .isFalse();
            assertThat(service.atualizar(s.getId(), new ServicoCommand("Alinhamento 3D", null, BigDecimal.TEN, 40, true)).isAtivo())
                    .isTrue();
            assertThat(service.listar()).hasSize(1);
            service.remover(s.getId());
            verify(servicos).delete(s);

            UUID inexistente = UUID.randomUUID();
            assertThatThrownBy(() -> service.buscar(inexistente)).isInstanceOf(RecursoNaoEncontradoException.class);
        }
    }

    @Nested
    class Pecas {

        PecaService service() {
            when(pecas.save(any())).thenAnswer(i -> i.getArgument(0));
            return new PecaService(pecas, movimentacoes, eventos);
        }

        @Test
        void criaComSaldoInicialEMovimenta() {
            PecaService service = service();
            Peca p = service.criar(new PecaCommand("filtro", "Filtro", null, TipoItemEstoque.PECA, BigDecimal.TEN, 5, 2));
            when(pecas.findById(p.getId())).thenReturn(Optional.of(p));

            service.registrarEntrada(p.getId(), 3, "Compra");
            service.baixarParaOrdemServico(p.getId(), 6, UUID.randomUUID());

            assertThat(p.getQuantidadeEstoque()).isEqualTo(2);
            ArgumentCaptor<MovimentacaoEstoque> captor = ArgumentCaptor.forClass(MovimentacaoEstoque.class);
            verify(movimentacoes, org.mockito.Mockito.times(3)).save(captor.capture());
            assertThat(captor.getAllValues()).extracting(MovimentacaoEstoque::getTipo).containsExactly(
                    MovimentacaoEstoque.Tipo.ENTRADA, MovimentacaoEstoque.Tipo.ENTRADA, MovimentacaoEstoque.Tipo.SAIDA);
            verify(eventos).publicar(p);

            when(pecas.findAll()).thenReturn(List.of(p));
            assertThat(service.listar(true)).containsExactly(p);
            assertThat(service.listar(false)).containsExactly(p);
            service.movimentacoes(p.getId());
            verify(movimentacoes).findByPecaIdOrderByDataHoraDesc(p.getId());

            service.atualizar(p.getId(), new PecaCommand("x", "Filtro novo", null, TipoItemEstoque.PECA, BigDecimal.ONE, 0, 1));
            assertThat(p.getNome()).isEqualTo("Filtro novo");
            service.remover(p.getId());
            verify(pecas).delete(p);
        }

        @Test
        void semSaldoInicialNaoRegistraMovimentacaoECodigoUnico() {
            PecaService service = service();
            service.criar(new PecaCommand("novo", "Item", null, TipoItemEstoque.INSUMO, BigDecimal.ONE, 0, 0));
            verify(movimentacoes, never()).save(any());

            when(pecas.existsByCodigo("DUP")).thenReturn(true);
            assertThatThrownBy(() -> service.criar(new PecaCommand("dup", "Item", null, TipoItemEstoque.INSUMO, BigDecimal.ONE, 0, 0)))
                    .isInstanceOf(DomainException.class);
        }

        @Test
        void baixaSemSaldo() {
            PecaService service = service();
            Peca p = new Peca("A", "A", null, TipoItemEstoque.PECA, BigDecimal.ONE, 1, 0);
            when(pecas.findById(p.getId())).thenReturn(Optional.of(p));
            UUID osId = UUID.randomUUID();
            assertThatThrownBy(() -> service.baixarParaOrdemServico(p.getId(), 2, osId))
                    .isInstanceOf(EstoqueInsuficienteException.class);
            UUID inexistente = UUID.randomUUID();
            assertThatThrownBy(() -> service.buscar(inexistente)).isInstanceOf(RecursoNaoEncontradoException.class);
        }
    }
}
