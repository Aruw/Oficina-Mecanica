package com.aruw.oficina.application.cliente;

public record ClienteCommand(String nome, String documento, String email, String telefone) {
}
