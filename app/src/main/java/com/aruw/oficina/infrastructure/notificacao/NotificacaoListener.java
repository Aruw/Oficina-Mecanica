package com.aruw.oficina.infrastructure.notificacao;

import com.aruw.oficina.domain.estoque.evento.EstoqueAbaixoDoMinimo;
import com.aruw.oficina.domain.ordemservico.evento.OrcamentoAprovado;
import com.aruw.oficina.domain.ordemservico.evento.OrcamentoGerado;
import com.aruw.oficina.domain.ordemservico.evento.OrcamentoRejeitado;
import com.aruw.oficina.domain.ordemservico.evento.OrdemServicoAberta;
import com.aruw.oficina.domain.ordemservico.evento.StatusOrdemServicoAlterado;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
public class NotificacaoListener {

    private static final Logger log = LoggerFactory.getLogger(NotificacaoListener.class);

    @TransactionalEventListener
    public void on(OrdemServicoAberta e) {
        log.info("[OS {}] Ordem de serviço aberta para o cliente {}", e.ordemServicoId(), e.clienteId());
    }

    @TransactionalEventListener
    public void on(StatusOrdemServicoAlterado e) {
        log.info("[OS {}] Status alterado: {} -> {}", e.ordemServicoId(), e.anterior(), e.novo());
    }

    @TransactionalEventListener
    public void on(OrcamentoGerado e) {
        log.info("[OS {}] Orçamento de R$ {} enviado ao cliente {} para aprovação", e.ordemServicoId(), e.valorTotal(), e.clienteId());
    }

    @TransactionalEventListener
    public void on(OrcamentoAprovado e) {
        log.info("[OS {}] Orçamento aprovado pelo cliente {}", e.ordemServicoId(), e.clienteId());
    }

    @TransactionalEventListener
    public void on(OrcamentoRejeitado e) {
        log.info("[OS {}] Orçamento rejeitado pelo cliente {}: {}", e.ordemServicoId(), e.clienteId(), e.motivo());
    }

    @TransactionalEventListener
    public void on(EstoqueAbaixoDoMinimo e) {
        log.warn("[Estoque] Item {} abaixo do mínimo: saldo {} (mínimo {}) — providenciar reposição",
                e.codigo(), e.quantidadeAtual(), e.estoqueMinimo());
    }
}
