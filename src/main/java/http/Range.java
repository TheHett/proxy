package http;

import java.util.ArrayList;
import java.util.Collection;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by Hett on 02.09.2017.
 */
public class Range {

    private long start;
    private long end;

    public Range(long start, long end) {
        this.start = start;
        this.end = end;
    }


    public static Range[] parseRange(String range, long length) {
        // Range: bytes=200-1000, 2000-6576, 19000-
        return parse(range.substring(range.indexOf("=") + 1), length);
    }

    static final Pattern contentRangePattern = Pattern.compile("Content-Range: bytes\\s([-\\d\\s,]+)(?:/\\d+)?");
    public static Range[] parseContentRange(String range, long length) {
        // Content-Range: bytes 200-1000/67589
        Matcher matcher = contentRangePattern.matcher(range);
        if (matcher.find()) {
            return parse(matcher.group(1), length);
        }
        return  null;
    }

    /**
     * Returns the absolute (zero-based) content range value specified
     * by the given range string. If multiple ranges are requested, a single
     * range containing all of them is returned.
     *
     * @param range  the string containing the range description
     * @param length the full length of the requested resource
     * @return the requested range, or null if the range value is invalid
     */
    private static Range[] parse(String range, long length) {

        String[] tokens = splitElements(range);
        Range[] result = new Range[tokens.length];
        try {
            for (int i = 0; i < tokens.length; i++) {
                long min = Long.MAX_VALUE;
                long max = Long.MIN_VALUE;
                long start, end;
                int dash = tokens[i].indexOf('-');
                if (dash == 0) { // suffix range
                    start = length - parseLong(tokens[i].substring(1), 10);
                    end = length - 1;
                } else if (dash == tokens[i].length() - 1) { // open range
                    start = parseLong(tokens[i].substring(0, dash), 10);
                    end = length - 1;
                } else { // explicit range
                    start = parseLong(tokens[i].substring(0, dash), 10);
                    end = parseLong(tokens[i].substring(dash + 1), 10);
                }
                if (end < start)
                    throw new RuntimeException();
                if (start < min)
                    min = start;
                if (end > max)
                    max = end;
                if (max < 0) // no tokens
                    throw new RuntimeException();
                if (max >= length && min < length)
                    max = length - 1;

                result[i] = new Range(min, max);
            }

            return result;

        } catch (RuntimeException re) { // NFE, IOOBE or explicit RE
            return null; // RFC2616#14.35.1 - ignore header if invalid
        }
    }


    /**
     * Splits the given element list string (comma-separated header value)
     * into its constituent non-empty trimmed elements.
     * (RFC2616#2.1: element lists are delimited by a comma and optional LWS,
     * and empty elements are ignored).
     *
     * @param list the element list string
     * @return the non-empty elements in the list, or an empty array
     */
    private static String[] splitElements(String list) {
        return split(list, ',');
    }


    /**
     * Splits the given string into its constituent non-empty trimmed elements,
     * which are delimited by the given character. This is a more direct
     * and efficient implementation than using a regex (e.g. String.split()).
     *
     * @param str       the string to split
     * @param delimiter the character used as the delimiter between elements
     * @return the non-empty elements in the string, or an empty array
     */
    private static String[] split(String str, char delimiter) {
        if (str == null)
            return new String[0];
        Collection<String> elements = new ArrayList<>();
        int len = str.length();
        int start = 0;
        while (start < len) {
            int end = str.indexOf(delimiter, start);
            if (end == -1)
                end = len; // last token is until end of string
            String element = str.substring(start, end).trim();
            if (element.length() > 0)
                elements.add(element);
            start = end + 1;
        }
        return elements.toArray(new String[elements.size()]);
    }

    /**
     * Parses an unsigned long value. This method behaves the same as calling
     * {@link Long#parseLong(String, int)}, but considers the string invalid
     * if it starts with an ASCII minus sign ('-').
     *
     * @param s     the String containing the long representation to be parsed
     * @param radix the radix to be used while parsing s
     * @return the long represented by s in the specified radix
     * @throws NumberFormatException if the string does not contain a parsable
     *                               long, or it starts with an ASCII minus sign
     */
    private static long parseLong(String s, int radix) throws NumberFormatException {
        long size = Long.parseLong(s, radix); // throws NumberFormatException
        if (s.charAt(0) == '-')
            throw new NumberFormatException("invalid digit: '-'");
        return size;
    }

    public long getStart() {
        return start;
    }

    public long getEnd() {
        return end;
    }

    @Override
    public String toString() {
        return "[" + start + ", " + end + "]";
    }
}
