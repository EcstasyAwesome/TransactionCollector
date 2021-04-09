package com.github.ecstasyawesome.transactioncollector;

public interface Event<A> {

  A getType();

  String getMessage();
}