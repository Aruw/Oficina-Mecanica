package com.aruw.oficina.application.estoque;

import com.aruw.oficina.application.shared.PublicadorEventos;
import com.aruw.oficina.application.shared.RecursoNaoEncontradoException;
import com.aruw.oficina.domain.estoque.MovimentacaoEstoque;
import com.aruw.oficina.domain.estoque.MovimentacaoEstoqueRepository;
import com.aruw.oficina.domain.estoque.Peca;
import com.aruw.oficina.domain.estoque.PecaRepository;
import com.aruw.oficina.domain.shared.DomainException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class PecaService {

    private final PecaRepository pecas;
    private final MovimentacaoEstoqueRepository movimentacoes;
    private final PublicadorEventos eventos;

    public PecaService(PecaRepository pecas, MovimentacaoEstoqueRepository movimentacoes, PublicadorEventos eventos) {
        this.pecas = pecas;
        this.movimentacoes = movimentacoes;
        this.eventos = eventos;
    }

    @Transactional
    public Peca criar(PecaCommand cmd) {
        if (cmd.codigo() != null && pecas.existsByCodigo(cmd.codigo().trim().toUpperCase())) {
            throw new DomainException("Já existe peça/insumo com o código " + cmd.codigo());
        }
        Peca peca = new Peca(cmd.codigo(), cmd.nome(), cmd.descricao(), cmd.tipo(), cmd.precoUnitario(),
                cmd.quantidadeEstoque(), cmd.estoqueMinimo());
        Peca salva = pecas.save(peca);
        if (cmd.quantidadeEstoque() > 0) {
            movimentacoes.save(MovimentacaoEstoque.entrada(salva.getId(), cmd.quantidadeEstoque(), "Saldo inicial"));
        }
        return salva;
    }

    @Transactional
    public Peca atualizar(UUID id, PecaCommand cmd) {
        Peca peca = obter(id);
        peca.atualizar(cmd.nome(), cmd.descricao(), cmd.tipo(), cmd.precoUnitario(), cmd.estoqueMinimo());
        return pecas.save(peca);
    }

    @Transactional
    public Peca registrarEntrada(UUID id, int quantidade, String motivo) {
        Peca peca = obter(id);
        peca.darEntrada(quantidade);
        movimentacoes.save(MovimentacaoEstoque.entrada(id, quantidade, motivo));
        return pecas.save(peca);
    }

    @Transactional
    public void baixarParaOrdemServico(UUID pecaId, int quantidade, UUID ordemServicoId) {
        Peca peca = obter(pecaId);
        peca.darBaixa(quantidade);
        movimentacoes.save(MovimentacaoEstoque.saidaPorOrdemServico(pecaId, quantidade, ordemServicoId));
        pecas.save(peca);
        eventos.publicar(peca);
    }

    @Transactional(readOnly = true)
    public Peca buscar(UUID id) {
        return obter(id);
    }

    private Peca obter(UUID id) {
        return pecas.findById(id).orElseThrow(() -> new RecursoNaoEncontradoException("Peça/insumo", id));
    }

    @Transactional(readOnly = true)
    public List<Peca> listar(boolean somenteAbaixoDoMinimo) {
        List<Peca> todas = pecas.findAll();
        return somenteAbaixoDoMinimo ? todas.stream().filter(Peca::abaixoDoMinimo).toList() : todas;
    }

    @Transactional(readOnly = true)
    public List<MovimentacaoEstoque> movimentacoes(UUID pecaId) {
        obter(pecaId);
        return movimentacoes.findByPecaIdOrderByDataHoraDesc(pecaId);
    }

    @Transactional
    public void remover(UUID id) {
        pecas.delete(obter(id));
    }
}
