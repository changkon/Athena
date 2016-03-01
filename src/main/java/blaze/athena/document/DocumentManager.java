package blaze.athena.document;

import java.io.File;
import java.io.IOException;
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
     * @param file
     * @return text contents of a file upload
     */
    String toText(File file) throws IOException;

    /**
     * <p>Given an input string, formats it for print</p>
     * @param input
     * @return formatted text
     */

    default String formatText(String input) {
        String[] lines = input.split(lineSeparator);
        for (int i =0; i < lines.length-1; i++) {
            String firstLine = lines[i].trim();
            String secondLine = lines[i+1].trim();
            int next = 1;
            if (secondLine.equals("") && i < lines.length - 2) {
                secondLine = lines[i+2].trim();
                next = 2;
            }
            if (firstLine.length() == 0 || secondLine.length() == 0) {
                continue;
            }
            char secondLineFirstLetter = secondLine.charAt(0);
            char firstLineLastLetter = firstLine.charAt(firstLine.length()-1);
            if (firstLineLastLetter != '.' && Character.isLowerCase(secondLineFirstLetter)) {
                lines[i] += secondLine;
                lines[i+next] = "";
            }
        }
        List<String> list = Arrays.asList(lines);
        return String.join(lineSeparator, list);
    }
}
