package com.powsali.loan_module;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import java.io.Console;
import java.util.Arrays;

/**
 * Local developer utility. Reads a password interactively so no plaintext
 * password is embedded in source code or passed as a command-line argument.
 */
public class PasswordGenerator {

    public static void main(String[] args) {
        Console console = System.console();
        if (console == null) {
            throw new IllegalStateException(
                    "Run this utility from a terminal that supports secure password input.");
        }

        char[] password = console.readPassword("Password to hash: ");
        if (password == null || password.length == 0) {
            throw new IllegalArgumentException("A non-empty password is required.");
        }

        try {
            String encoded = new BCryptPasswordEncoder().encode(new String(password));
            System.out.println(encoded);
        } finally {
            Arrays.fill(password, (char) 0);
        }
    }
}
