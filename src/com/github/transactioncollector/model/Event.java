package com.github.transactioncollector.model;

public interface Event<A> {
    A getType();

    String getMessage();
}