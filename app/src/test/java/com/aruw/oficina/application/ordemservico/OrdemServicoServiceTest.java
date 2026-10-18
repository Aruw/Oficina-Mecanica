package com.aruw.oficina.application.ordemservico;

import com.aruw.oficina.application.estoque.PecaService;
import com.aruw.oficina.application.shared.PublicadorEventos;
import com.aruw.oficina.application.shared.RecursoNaoEncontradoException;
import com.aruw.oficina.domain.catalogo.Servico;
import com.aruw.oficina.domain.catalogo.ServicoRepository;
import com.aruw.oficina.domain.cliente.Cliente;
import com.aruw.oficina.domain.cliente.ClienteRepository;
import com.aruw.oficina.domain.estoque.EstoqueInsuficienteException;
import com.aruw.oficina.domain.estoque.Peca;
import com.aruw.oficina.domain.estoque.PecaRepository;
import com.aruw.oficina.domain.estoque.TipoItemEstoque;
import com.aruw.oficina.domain.ordemservico.OrdemServico;
import com.aruw.oficina.domain.ordemservico.OrdemServicoRepository;
import com.aruw.oficina.domain.ordemservico.SituacaoOrcamento;
import com.aruw.oficina.domain.ordemservico.StatusOrdemServico;
import com.aruw.oficina.domain.shared.Documento;
import com.aruw.oficina.domain.shared.DomainException;
import com.aruw.oficina.domain.shared.Placa;
import com.aruw.oficina.domain.veiculo.Veiculo;
import com.aruw.oficina.domain.veiculo.VeiculoRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
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
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class OrdemServicoServiceTest {

    private static final String CPF = "52998224725";

    @Mock OrdemServicoRepository ordens;
    @Mock ClienteRepository clientes;
    @Mock VeiculoRepository veiculos;
    @Mock ServicoRepository servicos;
    @Mock PecaRepository pecas;
    @Mock PecaService pecaService;
    @Mock PublicadorEventos eventos;

    OrdemServicoService service;
    Cliente cliente;
    Veiculo veiculo;
    Servico servico;
    Peca peca;

    @BeforeEach
    void setUp() {
        service = new OrdemServicoService(ordens, clientes, veiculos, servicos, pecas, pecaService, eventos);
        cliente = new Cliente("Maria", Documento.of(CPF), null, null);
        veiculo = new Veiculo(Placa.of("ABC1D23"), "Fiat", "Uno", 2015, cliente.getId());
        servico = new Servico("Troca de óleo", null, new BigDecimal("80"), 40);
        peca = new Peca("OLEO", "Óleo", null, TipoItemEstoque.INSUMO, new BigDecimal("45"), 10, 2);

        when(ordens.save(any())).thenAnswer(i -> i.getArgument(0));
        when(clientes.save(any())).thenAnswer(i -> i.getArgument(0));
        when(veiculos.save(any())).thenAnswer(i -> i.getArgument(0));
        when(clientes.findById(cliente.getId())).thenReturn(Optional.of(cliente));
        when(veiculos.findById(veiculo.getId())).thenReturn(Optional.of(veiculo));
        when(servicos.findById(servico.getId())).thenReturn(Optional.of(servico));
        when(pecas.findById(peca.getId())).thenReturn(Optional.of(peca));
    }

    private AbrirOrdemServicoCommand comando(String nomeCliente, Integer ano, int qtdPeca) {
        return new AbrirOrdemServicoCommand(
                new AbrirOrdemServicoCommand.Cliente(CPF, nomeCliente, null, null),
                new AbrirOrdemServicoCommand.Veiculo("ABC1D23", "Fiat", "Uno", ano),
                "Revisão",
                List.of(new AbrirOrdemServicoCommand.ItemServico(servico.getId(), 1)),
                List.of(new AbrirOrdemServicoCommand.ItemPeca(peca.getId(), qtdPeca)));
    }

    private OrdemServico osRegistrada() {
        OrdemServico os = OrdemServico.abrir(cliente.getId(), veiculo.getId(), null);
        when(ordens.findById(os.getId())).thenReturn(Optional.of(os));
        return os;
    }

    @Test
    void abreOsComClienteEVeiculoExistentes() {
        when(clientes.findByDocumentoValor(CPF)).thenReturn(Optional.of(cliente));
        when(veiculos.findByPlacaValor("ABC1D23")).thenReturn(Optional.of(veiculo));

        OrdemServicoDetalhe detalhe = service.abrir(comando(null, null, 4));

        assertThat(detalhe.status()).isEqualTo(StatusOrdemServico.RECEBIDA);
        assertThat(detalhe.cliente().documento()).isEqualTo("529.982.247-25");
        assertThat(detalhe.veiculo().placa()).isEqualTo("ABC1D23");
        assertThat(detalhe.valorEstimado()).isEqualByComparingTo("260.00");
        assertThat(detalhe.servicos()).hasSize(1);
        assertThat(detalhe.pecas()).hasSize(1);
        verify(clientes, never()).save(any());
        verify(eventos).publicar(any(OrdemServico.class));
    }

    @Test
    void abreOsCadastrandoClienteEVeiculoNovos() {
        when(clientes.findByDocumentoValor(CPF)).thenReturn(Optional.empty());
        when(veiculos.findByPlacaValor("ABC1D23")).thenReturn(Optional.empty());

        OrdemServicoDetalhe detalhe = service.abrir(comando("João", 2020, 1));

        assertThat(detalhe.cliente().nome()).isEqualTo("João");
        assertThat(detalhe.veiculo().ano()).isEqualTo(2020);
        verify(clientes).save(any(Cliente.class));
        verify(veiculos).save(any(Veiculo.class));
    }

    @Test
    void exigeNomeParaClienteNaoCadastrado() {
        when(clientes.findByDocumentoValor(CPF)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.abrir(comando(null, 2020, 1)))
                .isInstanceOf(RecursoNaoEncontradoException.class)
                .hasMessageContaining("não cadastrado");
    }

    @Test
    void exigeDadosParaVeiculoNaoCadastrado() {
        when(clientes.findByDocumentoValor(CPF)).thenReturn(Optional.of(cliente));
        when(veiculos.findByPlacaValor("ABC1D23")).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.abrir(comando(null, null, 1))).isInstanceOf(DomainException.class);
    }

    @Test
    void exigeDadosDoClienteEVeiculo() {
        assertThatThrownBy(() -> service.abrir(new AbrirOrdemServicoCommand(null, null, null, null, null)))
                .isInstanceOf(DomainException.class);
    }

    @Test
    void recusaVeiculoDeOutroCliente() {
        Veiculo deOutro = new Veiculo(Placa.of("ABC1D23"), "Fiat", "Uno", 2015, UUID.randomUUID());
        when(clientes.findByDocumentoValor(CPF)).thenReturn(Optional.of(cliente));
        when(veiculos.findByPlacaValor("ABC1D23")).thenReturn(Optional.of(deOutro));
        assertThatThrownBy(() -> service.abrir(comando(null, null, 1))).hasMessageContaining("outro cliente");
    }

    @Test
    void recusaPecaSemEstoqueSuficiente() {
        when(clientes.findByDocumentoValor(CPF)).thenReturn(Optional.of(cliente));
        when(veiculos.findByPlacaValor("ABC1D23")).thenReturn(Optional.of(veiculo));
        assertThatThrownBy(() -> service.abrir(comando(null, null, 11))).isInstanceOf(EstoqueInsuficienteException.class);
    }

    @Test
    void consideraQuantidadeJaIncluidaNaVerificacaoDeEstoque() {
        OrdemServico os = osRegistrada();
        service.adicionarPeca(os.getId(), peca.getId(), 6);
        assertThatThrownBy(() -> service.adicionarPeca(os.getId(), peca.getId(), 5))
                .isInstanceOf(EstoqueInsuficienteException.class);
    }

    @Test
    void recusaServicoInativoOuInexistente() {
        OrdemServico os = osRegistrada();
        servico.desativar();
        assertThatThrownBy(() -> service.adicionarServico(os.getId(), servico.getId(), 1)).hasMessageContaining("inativo");
        UUID inexistente = UUID.randomUUID();
        when(servicos.findById(inexistente)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.adicionarServico(os.getId(), inexistente, 1))
                .isInstanceOf(RecursoNaoEncontradoException.class);
    }

    @Test
    void cicloDeVidaComAprovacaoBaixaEstoque() {
        OrdemServico os = osRegistrada();
        service.adicionarServico(os.getId(), servico.getId(), 1);
        OrdemServicoDetalhe comPeca = service.adicionarPeca(os.getId(), peca.getId(), 2);
        service.iniciarDiagnostico(os.getId());

        OrdemServicoDetalhe orcado = service.gerarOrcamento(os.getId());
        assertThat(orcado.status()).isEqualTo(StatusOrdemServico.AGUARDANDO_APROVACAO);
        assertThat(orcado.orcamento().valorTotal()).isEqualByComparingTo("170.00");

        OrdemServicoDetalhe aprovado = service.aprovarOrcamento(os.getId());
        assertThat(aprovado.status()).isEqualTo(StatusOrdemServico.EM_EXECUCAO);
        verify(pecaService).baixarParaOrdemServico(peca.getId(), 2, os.getId());

        assertThat(service.finalizar(os.getId()).status()).isEqualTo(StatusOrdemServico.FINALIZADA);
        OrdemServicoDetalhe entregue = service.entregar(os.getId());
        assertThat(entregue.status()).isEqualTo(StatusOrdemServico.ENTREGUE);
        assertThat(entregue.historico()).hasSize(6);
        assertThat(entregue.tempoExecucaoMinutos()).isNotNull();
        assertThat(comPeca.pecas()).hasSize(1);
    }

    @Test
    void falhaNaBaixaDeEstoqueImpedeAprovacao() {
        OrdemServico os = osRegistrada();
        service.adicionarPeca(os.getId(), peca.getId(), 2);
        service.iniciarDiagnostico(os.getId());
        service.gerarOrcamento(os.getId());
        doThrow(new EstoqueInsuficienteException("OLEO", 0, 2)).when(pecaService).baixarParaOrdemServico(any(), anyInt(), any());

        assertThatThrownBy(() -> service.aprovarOrcamento(os.getId())).isInstanceOf(EstoqueInsuficienteException.class);
    }

    @Test
    void removeItensERejeitaOrcamento() {
        OrdemServico os = osRegistrada();
        OrdemServicoDetalhe d = service.adicionarServico(os.getId(), servico.getId(), 1);
        d = service.adicionarPeca(os.getId(), peca.getId(), 1);
        d = service.removerPeca(os.getId(), d.pecas().getFirst().id());
        assertThat(d.pecas()).isEmpty();
        service.adicionarServico(os.getId(), servico.getId(), 1);
        d = service.removerServico(os.getId(), d.servicos().getFirst().id());
        assertThat(d.servicos()).hasSize(1);

        service.iniciarDiagnostico(os.getId());
        service.gerarOrcamento(os.getId());
        OrdemServicoDetalhe rejeitado = service.rejeitarOrcamento(os.getId(), "Caro");
        assertThat(rejeitado.status()).isEqualTo(StatusOrdemServico.FINALIZADA);
        assertThat(rejeitado.orcamento().situacao()).isEqualTo(SituacaoOrcamento.REJEITADO);
    }

    @Test
    void clienteConsultaEDecideComDocumentoCorreto() {
        OrdemServico os = osRegistrada();
        service.adicionarServico(os.getId(), servico.getId(), 1);
        service.iniciarDiagnostico(os.getId());
        service.gerarOrcamento(os.getId());

        assertThat(service.consultarPeloCliente(os.getId(), "529.982.247-25").status())
                .isEqualTo(StatusOrdemServico.AGUARDANDO_APROVACAO);
        assertThat(service.aprovarPeloCliente(os.getId(), CPF).status()).isEqualTo(StatusOrdemServico.EM_EXECUCAO);
    }

    @Test
    void clienteRejeitaOrcamento() {
        OrdemServico os = osRegistrada();
        service.adicionarServico(os.getId(), servico.getId(), 1);
        service.iniciarDiagnostico(os.getId());
        service.gerarOrcamento(os.getId());
        assertThat(service.rejeitarPeloCliente(os.getId(), CPF, "Sem verba").orcamento().observacao()).isEqualTo("Sem verba");
    }

    @Test
    void documentoDiferenteNaoEnxergaOs() {
        OrdemServico os = osRegistrada();
        assertThatThrownBy(() -> service.consultarPeloCliente(os.getId(), "11144477735"))
                .isInstanceOf(RecursoNaoEncontradoException.class);
    }

    @Test
    void osInexistente() {
        UUID id = UUID.randomUUID();
        when(ordens.findById(id)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.detalhar(id)).isInstanceOf(RecursoNaoEncontradoException.class);
    }

    @Test
    void listaOrdenandoPorPrioridade() {
        OrdemServico recebida = OrdemServico.abrir(cliente.getId(), veiculo.getId(), null);
        OrdemServico emDiagnostico = OrdemServico.abrir(cliente.getId(), veiculo.getId(), null);
        emDiagnostico.iniciarDiagnostico();
        when(ordens.findAll()).thenReturn(List.of(recebida, emDiagnostico));
        when(ordens.findByStatus(StatusOrdemServico.RECEBIDA)).thenReturn(List.of(recebida));

        assertThat(service.listar(null)).extracting(OrdemServicoDetalhe::status)
                .containsExactly(StatusOrdemServico.EM_DIAGNOSTICO, StatusOrdemServico.RECEBIDA);
        assertThat(service.listar(StatusOrdemServico.RECEBIDA)).hasSize(1);
    }
}
