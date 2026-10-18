package com.aruw.oficina.domain.estoque;

import com.aruw.oficina.domain.shared.DomainException;

public class EstoqueInsuficienteException extends DomainException {

    public EstoqueInsuficienteException(String codigo, int disponivel, int solicitado) {
        super("Estoque insuficiente para o item " + codigo + ": disponível " + disponivel + ", solicitado " + solicitado);
    }
}
