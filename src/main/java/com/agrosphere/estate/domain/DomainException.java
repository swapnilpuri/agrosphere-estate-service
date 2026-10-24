package com.agrosphere.estate.domain;
public class DomainException extends RuntimeException {
    public enum Kind { INVALID, NOT_FOUND, CONFLICT }
    private final Kind kind;
    public DomainException(Kind kind, String message) { super(message); this.kind = kind; }
    public Kind kind() { return kind; }
    public static DomainException invalid(String message) { return new DomainException(Kind.INVALID, message); }
    public static DomainException missing(String type) { return new DomainException(Kind.NOT_FOUND, type + " not found"); }
    public static DomainException conflict(String message) { return new DomainException(Kind.CONFLICT, message); }
}
