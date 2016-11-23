package no.mnemonic.commons.component;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotate getters to mark target property as a dependency.
 * Dependencies will be initialized before its dependants, and destroyed afterwards.
 *
 * @author joakim
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD, ElementType.FIELD})
public @interface Dependency {
}
