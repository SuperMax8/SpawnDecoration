package fr.supermax_8.spawndecoration.utils;

public class StringUtils {


    public static int extractAndParseDigits(String input) {
        StringBuilder digits = new StringBuilder();

        for (char c : input.toCharArray()) {
            if (Character.isDigit(c)) {
                digits.append(c);
            }
        }

        // If no digits were found, return 0 or handle accordingly
        if (digits.isEmpty()) {
            return 0;
        }

        return Integer.parseInt(digits.toString());
    }



}