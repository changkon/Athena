package blaze.athena.document;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.List;

/**
 * @author Chang Kon Han
 * @author John Law
 * @author Wesley Yep
 * @since 01 Mar 2016
 */
public interface DocumentManager {

    String lineSeparator = "\r\n";

    /**
     * <p>Extracts the contents of a file</p>
     * @param stream
     * @return
     * @throws IOException
     */
    String toText(InputStream stream) throws IOException;

    /**
     * <p>Given an input string, formats it for print</p>
     * @param input
     * @return formatted text
     */

    default String formatText(String input) {
        String[] lines = input.split(lineSeparator);
        for (int i = lines.length-1; i > 0; i--) {
            String line = lines[i].trim();
            String prevLine = lines[i-1].trim();

            if (line.length() < 1) {
                lines[i - 1] = prevLine + ".";
            } else {
                if (prevLine.length() < 1) {
                    continue;
                }
                char prevLastLetter = prevLine.charAt(prevLine.length() - 1);
                String firstLetter = String.valueOf(line.charAt(0));
                if (prevLastLetter != '.' && firstLetter.matches("[-\\u2022\\u2023\\u25E6\\u2043\\u2219]")) { //check if its not a bullet point) [
                    lines[i - 1] = prevLine + ".";
                }
            }
        }
        List<String> list = Arrays.asList(lines);
        return String.join(lineSeparator, list);
    }

    default String formatTextForText(String input) {
        String[] lines = input.split(lineSeparator);
        for (int i = 0; i < lines.length-1; i++) {
            String line = lines[i].trim();
            //append full stop to end of line
            lines[i] = line + ".";
        }
        List<String> list = Arrays.asList(lines);
        return String.join(lineSeparator, list);
    }
}
