package com.aruw.oficina.domain.ordemservico;

import com.aruw.oficina.domain.shared.DomainException;

public class TransicaoStatusInvalidaException extends DomainException {

    public TransicaoStatusInvalidaException(StatusOrdemServico atual, StatusOrdemServico destino) {
        super("Transição de status inválida: " + atual + " → " + destino);
    }

    public TransicaoStatusInvalidaException(String mensagem) {
        super(mensagem);
    }
}
