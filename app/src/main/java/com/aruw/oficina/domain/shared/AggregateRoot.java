package com.aruw.oficina.domain.shared;

import jakarta.persistence.MappedSuperclass;
import jakarta.persistence.Transient;
import jakarta.persistence.Version;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@MappedSuperclass
public abstract class AggregateRoot {

    @Version
    private Long versao;

    @Transient
    private final List<DomainEvent> eventos = new ArrayList<>();

    protected void registrarEvento(DomainEvent evento) {
        eventos.add(evento);
    }

    public List<DomainEvent> puxarEventos() {
        List<DomainEvent> pendentes = List.copyOf(eventos);
        eventos.clear();
        return pendentes;
    }

    public List<DomainEvent> eventosPendentes() {
        return Collections.unmodifiableList(eventos);
    }

    public Long getVersao() {
        return versao;
    }
}
