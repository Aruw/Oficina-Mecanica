package com.aruw.oficina.api.validation;

import com.aruw.oficina.domain.shared.Placa;
import jakarta.validation.Constraint;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import jakarta.validation.Payload;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Documented
@Constraint(validatedBy = PlacaValida.Validator.class)
@Target({ElementType.FIELD, ElementType.PARAMETER, ElementType.RECORD_COMPONENT})
@Retention(RetentionPolicy.RUNTIME)
public @interface PlacaValida {

    String message() default "Placa inválida: use o padrão ABC1234 ou Mercosul ABC1D23";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};

    class Validator implements ConstraintValidator<PlacaValida, String> {
        @Override
        public boolean isValid(String value, ConstraintValidatorContext context) {
            return value == null || Placa.isValida(value);
        }
    }
}
