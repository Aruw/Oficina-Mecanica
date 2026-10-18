package com.aruw.oficina.application.catalogo;

import com.aruw.oficina.application.shared.RecursoNaoEncontradoException;
import com.aruw.oficina.domain.catalogo.Servico;
import com.aruw.oficina.domain.catalogo.ServicoRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class ServicoService {

    private final ServicoRepository servicos;

    public ServicoService(ServicoRepository servicos) {
        this.servicos = servicos;
    }

    @Transactional
    public Servico criar(ServicoCommand cmd) {
        Servico servico = new Servico(cmd.nome(), cmd.descricao(), cmd.preco(), cmd.tempoEstimadoMinutos());
        aplicarAtivo(servico, cmd.ativo());
        return servicos.save(servico);
    }

    @Transactional
    public Servico atualizar(UUID id, ServicoCommand cmd) {
        Servico servico = obter(id);
        servico.atualizar(cmd.nome(), cmd.descricao(), cmd.preco(), cmd.tempoEstimadoMinutos());
        aplicarAtivo(servico, cmd.ativo());
        return servicos.save(servico);
    }

    private static void aplicarAtivo(Servico servico, Boolean ativo) {
        if (Boolean.FALSE.equals(ativo)) {
            servico.desativar();
        } else if (Boolean.TRUE.equals(ativo)) {
            servico.ativar();
        }
    }

    @Transactional(readOnly = true)
    public Servico buscar(UUID id) {
        return obter(id);
    }

    private Servico obter(UUID id) {
        return servicos.findById(id).orElseThrow(() -> new RecursoNaoEncontradoException("Serviço", id));
    }

    @Transactional(readOnly = true)
    public List<Servico> listar() {
        return servicos.findAll();
    }

    @Transactional
    public void remover(UUID id) {
        servicos.delete(obter(id));
    }
}
