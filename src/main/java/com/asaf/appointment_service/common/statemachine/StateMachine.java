package com.asaf.appointment_service.common.statemachine;

import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public abstract class StateMachine<S extends Enum<S>> {

    private final Map<S, Set<S>> transitions = new HashMap<>();
    private final Class<S> stateType;

    protected StateMachine(Class<S> stateType) {
        this.stateType = stateType;
        defineTransitions();
    }

    protected abstract void defineTransitions();

    protected void addTransition(S from, S to) {
        transitions.computeIfAbsent(from, k -> EnumSet.noneOf(stateType)).add(to);
    }

    public boolean canTransition(S from, S to) {
        Set<S> allowedStates = transitions.get(from);
        return allowedStates != null && allowedStates.contains(to);
    }

    public void validateTransition(S from, S to) {
        if (!canTransition(from, to)) {
            throw new InvalidStateTransitionException(from, to);
        }
    }

    public Set<S> getAllowedTransitions(S from) {
        return transitions.getOrDefault(from, EnumSet.noneOf(stateType));
    }

    public static class InvalidStateTransitionException extends RuntimeException {
        public <S extends Enum<S>> InvalidStateTransitionException(S from, S to) {
            super(String.format("Geçersiz durum geçişi: %s -> %s", from, to));
        }
    }
}
