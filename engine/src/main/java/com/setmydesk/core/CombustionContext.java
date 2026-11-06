package com.setmydesk.core;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Base context carried through every cylinder: halt flag plus a per-stage trace. */
public abstract class CombustionContext {

    private boolean halted;
    private String haltReason;
    private final List<String> trace = new ArrayList<>();

    public void halt(String reason) {
        this.halted = true;
        this.haltReason = reason;
    }

    public boolean halted() {
        return halted;
    }

    public String haltReason() {
        return haltReason;
    }

    void record(String entry) {
        trace.add(entry);
    }

    public List<String> trace() {
        return Collections.unmodifiableList(trace);
    }
}
