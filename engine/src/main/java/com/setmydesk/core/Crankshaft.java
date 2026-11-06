package com.setmydesk.core;

import java.util.List;

/**
 * Runs cylinders in a fixed firing order. Stops as soon as one halts. Every stage is
 * recorded in the context trace with its timing, so each decision can be explained.
 */
public final class Crankshaft<C extends CombustionContext> {

    private final List<? extends Cylinder<C>> cylinders;

    public Crankshaft(List<? extends Cylinder<C>> cylinders) {
        this.cylinders = List.copyOf(cylinders);
    }

    public C turn(C ctx) {
        for (Cylinder<C> cylinder : cylinders) {
            if (ctx.halted()) break;
            if (!cylinder.shouldFire(ctx)) {
                ctx.record(cylinder.name() + ": skipped");
                continue;
            }
            long start = System.nanoTime();
            cylinder.fire(ctx);
            long micros = (System.nanoTime() - start) / 1_000;
            ctx.record(cylinder.name() + ": " + (ctx.halted() ? "halted (" + ctx.haltReason() + ")" : "ok") + " " + micros + "µs");
        }
        return ctx;
    }

    public List<String> firingOrder() {
        return cylinders.stream().map(Cylinder::name).toList();
    }
}
