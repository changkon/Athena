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
        int mergeCount = 0;
        for (int i =0; i < lines.length-1 && i+1+mergeCount < lines.length; i++) {
            String firstLine = lines[i].trim();
            String secondLine = lines[i+1+mergeCount].trim();

            if (firstLine.length() == 0 || secondLine.length() == 0) {
                mergeCount = 0;
                continue;
            }
            char secondLineFirstLetter = secondLine.charAt(0);
            char firstLineLastLetter = firstLine.charAt(firstLine.length()-1);
            if (firstLineLastLetter != '.' && !secondLine.contains("==========") &&
                    (Character.isLetter(secondLineFirstLetter) || Character.isDigit(secondLineFirstLetter) || secondLineFirstLetter == '(')) {
                lines[i] = firstLine + " " + secondLine;
                lines[i+1+mergeCount] = "";
                i--; //reset i to test this line again
                mergeCount++;
            }
        }
        for (int i = 0; i < lines.length; i++) {
            if (!lines[i].trim().equals("")) {
                lines[i] += ".";
            }
        }
        List<String> list = Arrays.asList(lines);
        return String.join(lineSeparator, list);
    }
}
