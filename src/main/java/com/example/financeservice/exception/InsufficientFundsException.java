package com.example.financeservice.exception;

class InsufficientFundsException extends RuntimeException {

  public InsufficientFundsException(String message) {
    super(message);
  }
}