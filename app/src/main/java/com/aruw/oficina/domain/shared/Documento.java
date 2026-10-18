package com.aruw.oficina.domain.shared;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;

import java.util.Objects;

@Embeddable
public class Documento {

    public enum Tipo { CPF, CNPJ }

    @Column(name = "documento", nullable = false, length = 14)
    private String valor;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo_documento", nullable = false, length = 4)
    private Tipo tipo;

    protected Documento() {
    }

    private Documento(String valor, Tipo tipo) {
        this.valor = valor;
        this.tipo = tipo;
    }

    public static Documento of(String entrada) {
        if (entrada == null || entrada.isBlank()) {
            throw new DomainException("Documento (CPF/CNPJ) é obrigatório");
        }
        String normalizado = normalizar(entrada);
        if (normalizado.length() == 11 && cpfValido(normalizado)) {
            return new Documento(normalizado, Tipo.CPF);
        }
        if (normalizado.length() == 14 && cnpjValido(normalizado)) {
            return new Documento(normalizado, Tipo.CNPJ);
        }
        throw new DomainException("Documento inválido: informe um CPF ou CNPJ válido");
    }

    public static boolean isValido(String entrada) {
        try {
            of(entrada);
            return true;
        } catch (DomainException e) {
            return false;
        }
    }

    public static String normalizar(String entrada) {
        return entrada.replaceAll("[.\\-/\\s]", "").toUpperCase();
    }

    static boolean cpfValido(String cpf) {
        if (!cpf.matches("\\d{11}") || cpf.chars().distinct().count() == 1) {
            return false;
        }
        int d1 = digitoVerificador(cpf.substring(0, 9), 10);
        int d2 = digitoVerificador(cpf.substring(0, 9) + d1, 11);
        return cpf.endsWith("" + d1 + d2);
    }

    private static int digitoVerificador(String base, int pesoInicial) {
        int soma = 0;
        for (int i = 0; i < base.length(); i++) {
            soma += (base.charAt(i) - '0') * (pesoInicial - i);
        }
        int resto = soma % 11;
        return resto < 2 ? 0 : 11 - resto;
    }

    static boolean cnpjValido(String cnpj) {
        if (!cnpj.matches("[0-9A-Z]{12}\\d{2}") || cnpj.chars().distinct().count() == 1) {
            return false;
        }
        int d1 = digitoCnpj(cnpj.substring(0, 12));
        int d2 = digitoCnpj(cnpj.substring(0, 12) + d1);
        return cnpj.endsWith("" + d1 + d2);
    }

    private static int digitoCnpj(String base) {
        int soma = 0;
        int peso = 2;
        for (int i = base.length() - 1; i >= 0; i--) {
            // Valor do caractere = código ASCII - 48 (regra do CNPJ alfanumérico; dígitos permanecem iguais)
            soma += (base.charAt(i) - '0') * peso;
            peso = peso == 9 ? 2 : peso + 1;
        }
        int resto = soma % 11;
        return resto < 2 ? 0 : 11 - resto;
    }

    public String getValor() {
        return valor;
    }

    public Tipo getTipo() {
        return tipo;
    }

    public String formatado() {
        if (tipo == Tipo.CPF) {
            return valor.replaceFirst("(\\w{3})(\\w{3})(\\w{3})(\\w{2})", "$1.$2.$3-$4");
        }
        return valor.replaceFirst("(\\w{2})(\\w{3})(\\w{3})(\\w{4})(\\w{2})", "$1.$2.$3/$4-$5");
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Documento outro)) return false;
        return Objects.equals(valor, outro.valor);
    }

    @Override
    public int hashCode() {
        return Objects.hash(valor);
    }

    @Override
    public String toString() {
        return formatado();
    }
}
