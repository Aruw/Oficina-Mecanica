package com.aruw.oficina.application.shared;

public class RecursoNaoEncontradoException extends RuntimeException {

    public RecursoNaoEncontradoException(String recurso, Object identificador) {
        super(recurso + " não encontrado(a): " + identificador);
    }

    public RecursoNaoEncontradoException(String mensagem) {
        super(mensagem);
    }
}
