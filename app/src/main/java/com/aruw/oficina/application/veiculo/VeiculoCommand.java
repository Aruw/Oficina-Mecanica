package com.aruw.oficina.application.veiculo;

import java.util.UUID;

public record VeiculoCommand(String placa, String marca, String modelo, int ano, UUID clienteId) {
}
