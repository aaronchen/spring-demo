package cc.desuka.demo.validation;

import jakarta.persistence.EntityManager;
import jakarta.persistence.TypedQuery;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import org.springframework.beans.BeanWrapper;
import org.springframework.beans.PropertyAccessorFactory;

public class UniqueValidator implements ConstraintValidator<Unique, Object> {

    private final EntityManager entityManager;
    private Class<?> entity;
    private String field;
    private String idField;
    private String message;

    public UniqueValidator(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    @Override
    public void initialize(Unique annotation) {
        this.entity = annotation.entity();
        this.field = annotation.field();
        this.idField = annotation.idField();
        this.message = annotation.message();
    }

    @Override
    public boolean isValid(Object obj, ConstraintValidatorContext context) {
        BeanWrapper wrapper = PropertyAccessorFactory.forBeanPropertyAccess(obj);
        Object value = wrapper.getPropertyValue(field);

        if (value == null || (value instanceof String s && s.isBlank())) {
            return true; // let @NotBlank handle null/blank
        }

        String trimmed = value instanceof String s ? s.trim() : value.toString();

        // Try to read the ID from the validated object.
        // If the object has the idField and it's non-null (update), exclude that record.
        // If the object doesn't have the idField or it's null (create), check all records.
        Object idValue = null;
        if (wrapper.isReadableProperty(idField)) {
            idValue = wrapper.getPropertyValue(idField);
        }

        String jpql = "SELECT COUNT(e) FROM " + entity.getSimpleName()
                + " e WHERE LOWER(e." + field + ") = LOWER(:value)";
        if (idValue != null) {
            jpql += " AND e." + idField + " != :excludeId";
        }

        TypedQuery<Long> query = entityManager.createQuery(jpql, Long.class)
                .setParameter("value", trimmed);
        if (idValue != null) {
            query.setParameter("excludeId", idValue);
        }

        boolean unique = query.getSingleResult() == 0;

        if (!unique) {
            context.disableDefaultConstraintViolation();
            context.buildConstraintViolationWithTemplate(message)
                    .addPropertyNode(field)
                    .addConstraintViolation();
        }

        return unique;
    }
}
