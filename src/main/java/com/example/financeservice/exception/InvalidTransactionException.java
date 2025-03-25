package com.example.financeservice.exception;

public class InvalidTransactionException extends RuntimeException {

  public InvalidTransactionException(String message) {
    super(message);
  }
}
