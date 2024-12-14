package de.tostsoft.solarmonitoring.lib.utils;

public class MyStringUtils {
    static public String quoteRegExSpecialChars( String inputString)
    {
        StringBuilder escapedString = new StringBuilder();
        for (char c : inputString.toCharArray()) {
            if (!Character.isLetterOrDigit(c)) {
                escapedString.append("\\");
            }
            escapedString.append(c);
        }
        return escapedString.toString();
    }
}
