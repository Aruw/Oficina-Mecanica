package com.aruw.oficina.application.shared;

import com.aruw.oficina.domain.shared.AggregateRoot;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

@Component
public class PublicadorEventos {

    private final ApplicationEventPublisher publisher;

    public PublicadorEventos(ApplicationEventPublisher publisher) {
        this.publisher = publisher;
    }

    public void publicar(AggregateRoot agregado) {
        agregado.puxarEventos().forEach(publisher::publishEvent);
    }
}
