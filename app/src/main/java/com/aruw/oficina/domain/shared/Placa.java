package com.aruw.oficina.domain.shared;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

import java.util.Objects;
import java.util.regex.Pattern;

@Embeddable
public class Placa {

    private static final Pattern PADRAO_ANTIGO = Pattern.compile("^[A-Z]{3}\\d{4}$");
    private static final Pattern PADRAO_MERCOSUL = Pattern.compile("^[A-Z]{3}\\d[A-Z]\\d{2}$");

    @Column(name = "placa", nullable = false, length = 7)
    private String valor;

    protected Placa() {
    }

    private Placa(String valor) {
        this.valor = valor;
    }

    public static Placa of(String entrada) {
        if (entrada == null || entrada.isBlank()) {
            throw new DomainException("Placa é obrigatória");
        }
        String normalizada = normalizar(entrada);
        if (!PADRAO_ANTIGO.matcher(normalizada).matches() && !PADRAO_MERCOSUL.matcher(normalizada).matches()) {
            throw new DomainException("Placa inválida: use o padrão ABC1234 ou Mercosul ABC1D23");
        }
        return new Placa(normalizada);
    }

    public static boolean isValida(String entrada) {
        try {
            of(entrada);
            return true;
        } catch (DomainException e) {
            return false;
        }
    }

    public static String normalizar(String entrada) {
        return entrada.replaceAll("[-\\s]", "").toUpperCase();
    }

    public boolean isMercosul() {
        return PADRAO_MERCOSUL.matcher(valor).matches();
    }

    public String getValor() {
        return valor;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Placa outra)) return false;
        return Objects.equals(valor, outra.valor);
    }

    @Override
    public int hashCode() {
        return Objects.hash(valor);
    }

    @Override
    public String toString() {
        return valor;
    }
}
