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
import com.aruw.oficina.domain.ordemservico.ItemPeca;
import com.aruw.oficina.domain.ordemservico.OrdemServico;
import com.aruw.oficina.domain.ordemservico.OrdemServicoRepository;
import com.aruw.oficina.domain.ordemservico.StatusOrdemServico;
import com.aruw.oficina.domain.shared.Documento;
import com.aruw.oficina.domain.shared.DomainException;
import com.aruw.oficina.domain.shared.Placa;
import com.aruw.oficina.domain.veiculo.Veiculo;
import com.aruw.oficina.domain.veiculo.VeiculoRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import java.util.function.Consumer;

@Service
public class OrdemServicoService {

    private final OrdemServicoRepository ordens;
    private final ClienteRepository clientes;
    private final VeiculoRepository veiculos;
    private final ServicoRepository servicos;
    private final PecaRepository pecas;
    private final PecaService pecaService;
    private final PublicadorEventos eventos;

    public OrdemServicoService(OrdemServicoRepository ordens, ClienteRepository clientes, VeiculoRepository veiculos,
                               ServicoRepository servicos, PecaRepository pecas, PecaService pecaService,
                               PublicadorEventos eventos) {
        this.ordens = ordens;
        this.clientes = clientes;
        this.veiculos = veiculos;
        this.servicos = servicos;
        this.pecas = pecas;
        this.pecaService = pecaService;
        this.eventos = eventos;
    }

    @Transactional
    public OrdemServicoDetalhe abrir(AbrirOrdemServicoCommand cmd) {
        if (cmd.cliente() == null || cmd.veiculo() == null) {
            throw new DomainException("Informe os dados do cliente e do veículo");
        }
        Cliente cliente = identificarOuCadastrarCliente(cmd.cliente());
        Veiculo veiculo = identificarOuCadastrarVeiculo(cmd.veiculo(), cliente);

        OrdemServico os = OrdemServico.abrir(cliente.getId(), veiculo.getId(), cmd.descricaoProblema());
        cmd.servicosOuVazio().forEach(s -> incluirServico(os, s.servicoId(), s.quantidade()));
        cmd.pecasOuVazio().forEach(p -> incluirPeca(os, p.pecaId(), p.quantidade()));

        return salvarEDetalhar(os, cliente, veiculo);
    }

    private Cliente identificarOuCadastrarCliente(AbrirOrdemServicoCommand.Cliente dados) {
        Documento documento = Documento.of(dados.documento());
        return clientes.findByDocumentoValor(documento.getValor()).orElseGet(() -> {
            if (dados.nome() == null || dados.nome().isBlank()) {
                throw new RecursoNaoEncontradoException(
                        "Cliente " + documento.formatado() + " não cadastrado: informe o nome para cadastrá-lo");
            }
            return clientes.save(new Cliente(dados.nome(), documento, dados.email(), dados.telefone()));
        });
    }

    private Veiculo identificarOuCadastrarVeiculo(AbrirOrdemServicoCommand.Veiculo dados, Cliente cliente) {
        Placa placa = Placa.of(dados.placa());
        Veiculo veiculo = veiculos.findByPlacaValor(placa.getValor()).orElseGet(() -> {
            if (dados.ano() == null) {
                throw new DomainException("Veículo " + placa + " não cadastrado: informe marca, modelo e ano");
            }
            return veiculos.save(new Veiculo(placa, dados.marca(), dados.modelo(), dados.ano(), cliente.getId()));
        });
        if (!veiculo.pertenceA(cliente.getId())) {
            throw new DomainException("O veículo " + placa + " está vinculado a outro cliente");
        }
        return veiculo;
    }

    @Transactional
    public OrdemServicoDetalhe adicionarServico(UUID osId, UUID servicoId, int quantidade) {
        return executar(osId, os -> incluirServico(os, servicoId, quantidade));
    }

    @Transactional
    public OrdemServicoDetalhe adicionarPeca(UUID osId, UUID pecaId, int quantidade) {
        return executar(osId, os -> incluirPeca(os, pecaId, quantidade));
    }

    @Transactional
    public OrdemServicoDetalhe removerServico(UUID osId, UUID itemId) {
        return executar(osId, os -> os.removerItemServico(itemId));
    }

    @Transactional
    public OrdemServicoDetalhe removerPeca(UUID osId, UUID itemId) {
        return executar(osId, os -> os.removerItemPeca(itemId));
    }

    private void incluirServico(OrdemServico os, UUID servicoId, int quantidade) {
        Servico servico = servicos.findById(servicoId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Serviço", servicoId));
        if (!servico.isAtivo()) {
            throw new DomainException("Serviço " + servico.getNome() + " está inativo");
        }
        os.adicionarServico(servico.getId(), servico.getNome(), servico.getPreco(), quantidade);
    }

    private void incluirPeca(OrdemServico os, UUID pecaId, int quantidade) {
        Peca peca = pecas.findById(pecaId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Peça/insumo", pecaId));
        int totalNaOs = quantidade + os.getItensPeca().stream()
                .filter(i -> i.getPecaId().equals(pecaId))
                .mapToInt(ItemPeca::getQuantidade)
                .sum();
        if (!peca.possuiDisponivel(totalNaOs)) {
            throw new EstoqueInsuficienteException(peca.getCodigo(), peca.getQuantidadeEstoque(), totalNaOs);
        }
        os.adicionarPeca(peca.getId(), peca.getNome(), peca.getTipo(), peca.getPrecoUnitario(), quantidade);
    }

    @Transactional
    public OrdemServicoDetalhe iniciarDiagnostico(UUID osId) {
        return executar(osId, OrdemServico::iniciarDiagnostico);
    }

    @Transactional
    public OrdemServicoDetalhe gerarOrcamento(UUID osId) {
        return executar(osId, OrdemServico::gerarOrcamento);
    }

    @Transactional
    public OrdemServicoDetalhe aprovarOrcamento(UUID osId) {
        return executar(osId, this::aprovar);
    }

    @Transactional
    public OrdemServicoDetalhe rejeitarOrcamento(UUID osId, String motivo) {
        return executar(osId, os -> os.rejeitarOrcamento(motivo));
    }

    @Transactional
    public OrdemServicoDetalhe finalizar(UUID osId) {
        return executar(osId, OrdemServico::finalizar);
    }

    @Transactional
    public OrdemServicoDetalhe entregar(UUID osId) {
        return executar(osId, OrdemServico::entregar);
    }

    private void aprovar(OrdemServico os) {
        os.aprovarOrcamento();
        os.getItensPeca().forEach(item -> pecaService.baixarParaOrdemServico(item.getPecaId(), item.getQuantidade(), os.getId()));
    }

    @Transactional(readOnly = true)
    public OrdemServicoDetalhe detalhar(UUID osId) {
        return detalhar(buscar(osId));
    }

    @Transactional(readOnly = true)
    public List<OrdemServicoDetalhe> listar(StatusOrdemServico status) {
        List<OrdemServico> lista = status == null ? ordens.findAll() : ordens.findByStatus(status);
        return lista.stream()
                .sorted(Comparator.comparingInt((OrdemServico os) -> os.getStatus().prioridade())
                        .thenComparing(OrdemServico::getCriadaEm))
                .map(this::detalhar)
                .toList();
    }

    @Transactional(readOnly = true)
    public OrdemServicoDetalhe consultarPeloCliente(UUID osId, String documento) {
        return detalhar(buscarDoCliente(osId, documento));
    }

    @Transactional
    public OrdemServicoDetalhe aprovarPeloCliente(UUID osId, String documento) {
        OrdemServico os = buscarDoCliente(osId, documento);
        aprovar(os);
        return salvarEDetalhar(os);
    }

    @Transactional
    public OrdemServicoDetalhe rejeitarPeloCliente(UUID osId, String documento, String motivo) {
        OrdemServico os = buscarDoCliente(osId, documento);
        os.rejeitarOrcamento(motivo);
        return salvarEDetalhar(os);
    }

    private OrdemServico buscarDoCliente(UUID osId, String documento) {
        String valor = Documento.of(documento).getValor();
        OrdemServico os = buscar(osId);
        boolean doCliente = clientes.findById(os.getClienteId())
                .map(c -> c.getDocumento().getValor().equals(valor))
                .orElse(false);
        if (!doCliente) {
            throw new RecursoNaoEncontradoException("Ordem de serviço", osId);
        }
        return os;
    }

    private OrdemServico buscar(UUID osId) {
        return ordens.findById(osId).orElseThrow(() -> new RecursoNaoEncontradoException("Ordem de serviço", osId));
    }

    private OrdemServicoDetalhe executar(UUID osId, Consumer<OrdemServico> acao) {
        OrdemServico os = buscar(osId);
        acao.accept(os);
        return salvarEDetalhar(os);
    }

    private OrdemServicoDetalhe salvarEDetalhar(OrdemServico os) {
        OrdemServico salva = ordens.save(os);
        eventos.publicar(os);
        return detalhar(salva);
    }

    private OrdemServicoDetalhe salvarEDetalhar(OrdemServico os, Cliente cliente, Veiculo veiculo) {
        OrdemServico salva = ordens.save(os);
        eventos.publicar(os);
        return OrdemServicoDetalhe.de(salva, cliente, veiculo);
    }

    private OrdemServicoDetalhe detalhar(OrdemServico os) {
        Cliente cliente = clientes.findById(os.getClienteId()).orElse(null);
        Veiculo veiculo = veiculos.findById(os.getVeiculoId()).orElse(null);
        return OrdemServicoDetalhe.de(os, cliente, veiculo);
    }
}
