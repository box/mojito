package com.box.l10n.mojito.rest.security;

public record PasswordChangeRequest(String currentPassword, String newPassword) {}
