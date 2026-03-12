package cc.desuka.demo.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.ElementType;
import java.lang.annotation.Repeatable;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Constraint(validatedBy = UniqueValidator.class)
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Repeatable(Unique.List.class)
public @interface Unique {
    String message() default "{validation.unique}";
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};

    Class<?> entity();
    String field();
    String idField() default "id";

    @Target(ElementType.TYPE)
    @Retention(RetentionPolicy.RUNTIME)
    @interface List {
        Unique[] value();
    }
}
