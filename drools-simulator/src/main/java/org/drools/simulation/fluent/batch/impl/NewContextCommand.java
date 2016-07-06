package org.drools.simulation.fluent.batch.impl;

import org.drools.core.command.RequestContextImpl;
import org.drools.core.command.impl.GenericCommand;
import org.kie.internal.command.Context;

public class NewContextCommand<Void> implements GenericCommand<Void> {
    private String name;

    public NewContextCommand(String name) {
        this.name = name;
    }

    @Override
    public Void execute(Context context) {
        Context returned = context.getContextManager().createContext(name);
        ((RequestContextImpl)context).setApplicationContext(returned);
//        return returned;
//
//        T returned = (T) context.get("returned");
//        return returned;

        return null;
    }
}
