package org.example;

/**
 * Launcher — plain class (does NOT extend Application).
 *
 * When JavaFX is on the classpath (not the module-path) IntelliJ / java
 * refuses to start a class that extends Application directly.
 * Using a plain launcher class bypasses that restriction.
 *
 * Set THIS class as the Run-Configuration main class in IntelliJ.
 */
public class Launcher {
    public static void main(String[] args) {
        Main.main(args);
    }
}

