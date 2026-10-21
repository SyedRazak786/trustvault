package com.trustvault.privacy;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.springframework.stereotype.Component;

// Finds common sensitive patterns in text. This is a first layer only: it can miss sensitive content and
// it can flag harmless numbers, so the owner always makes the final decision. Nothing here blocks sharing.
@Component
public class PrivacyScanner {

    public record Finding(String kind, int count) {}

    private static final Pattern CARD = Pattern.compile("\\b(?:\\d[ -]?){13,19}\\b");
    private static final Pattern AADHAAR = Pattern.compile("\\b\\d{4}[ -]?\\d{4}[ -]?\\d{4}\\b");
    private static final Pattern PAN = Pattern.compile("\\b[A-Z]{5}[0-9]{4}[A-Z]\\b");
    private static final Pattern ACCOUNT = Pattern.compile("\\b\\d{9,18}\\b");

    // Returns one finding per kind that appears, with how many times it appears.
    public List<Finding> scan(String text) {
        List<Finding> findings = new ArrayList<>();
        int cards = countValidCards(text);
        if (cards > 0) findings.add(new Finding("CARD_NUMBER", cards));
        int aadhaar = count(AADHAAR, text);
        if (aadhaar > 0) findings.add(new Finding("AADHAAR_STYLE_NUMBER", aadhaar));
        int pan = count(PAN, text);
        if (pan > 0) findings.add(new Finding("PAN_STYLE_ID", pan));
        int accounts = countAccountLike(text);
        if (accounts > 0) findings.add(new Finding("LONG_NUMBER_POSSIBLE_ACCOUNT", accounts));
        return findings;
    }

    private static int count(Pattern p, String text) {
        Matcher m = p.matcher(text);
        int n = 0;
        while (m.find()) n++;
        return n;
    }

    // Only digit runs that pass the Luhn check are counted as card numbers, which removes most false alarms.
    private static int countValidCards(String text) {
        Matcher m = CARD.matcher(text);
        int n = 0;
        while (m.find()) {
            String digits = m.group().replaceAll("[ -]", "");
            if (digits.length() >= 13 && digits.length() <= 19 && luhn(digits)) n++;
        }
        return n;
    }

    // Long digit runs that are not already counted as card or Aadhaar-style numbers.
    private static int countAccountLike(String text) {
        Matcher m = ACCOUNT.matcher(text);
        int n = 0;
        while (m.find()) {
            String digits = m.group();
            if (digits.length() == 16 || digits.length() == 12) continue;
            n++;
        }
        return n;
    }

    static boolean luhn(String digits) {
        int sum = 0;
        boolean doubleIt = false;
        for (int i = digits.length() - 1; i >= 0; i--) {
            int d = digits.charAt(i) - '0';
            if (d < 0 || d > 9) return false;
            if (doubleIt) {
                d *= 2;
                if (d > 9) d -= 9;
            }
            sum += d;
            doubleIt = !doubleIt;
        }
        return sum % 10 == 0;
    }
}