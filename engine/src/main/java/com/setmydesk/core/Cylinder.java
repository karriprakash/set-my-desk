package com.setmydesk.core;

/**
 * One stage of the engine. Each cylinder does exactly one job on the shared context and
 * may {@link CombustionContext#halt halt} the stroke (for example, a duplicate idea).
 */
public interface Cylinder<C extends CombustionContext> {

    /** Short stable name, shown in the pipeline trace. */
    String name();

    /** Whether this cylinder has anything to do for this context. */
    default boolean shouldFire(C ctx) {
        return true;
    }

    void fire(C ctx);
}
