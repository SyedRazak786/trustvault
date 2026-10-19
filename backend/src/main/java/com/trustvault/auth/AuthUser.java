package com.trustvault.auth;

// The signed-in user for one request. Built from a verified token, never from client input.
public record AuthUser(long id, String role) {}