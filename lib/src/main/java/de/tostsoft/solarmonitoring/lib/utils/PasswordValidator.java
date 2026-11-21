package de.tostsoft.solarmonitoring.lib.utils;

import java.util.regex.Pattern;

public final class PasswordValidator {

    private static final Pattern PASSWORD_REGEX = Pattern.compile(
            "^(?=.*[A-ZÄÜÖ])(?=.*[a-zäüöß])(?=.*\\d)(?=.*[@$%*#?!&])[A-ZÄÖÜa-zäöüß\\d@$!%*#?&]{10,64}$");

    private PasswordValidator() {
    }

    public static boolean isValidPassword(String password) {
        return password != null && PASSWORD_REGEX.matcher(password).matches();
    }
}
