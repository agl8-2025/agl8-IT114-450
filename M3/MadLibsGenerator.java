package M3;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

/*
Challenge 3: Mad Libs Generator (Randomized Stories)
-----------------------------------------------------
- Load a **random** story from the "stories" folder
- Extract **each line** into a collection (i.e., ArrayList)
- Prompts user for each placeholder (i.e., <adjective>) 
    - Any word the user types is acceptable, no need to verify if it matches the placeholder type
    - Any placeholder with underscores should display with spaces instead
- Replace placeholders with user input (assign back to original slot in collection)
*/

public class MadLibsGenerator extends BaseClass {
    private static final String STORIES_FOLDER = "M3/stories";
    private static String ucid = "agl8"; // <-- change to your ucid

    public static void main(String[] args) {
        printHeader(ucid, 3,
                "Objective: Implement a Mad Libs generator that replaces placeholders dynamically.");

        Scanner scanner = new Scanner(System.in);
        File folder = new File(STORIES_FOLDER);

        if (!folder.exists() || !folder.isDirectory() || folder.listFiles().length == 0) {
            System.out.println("Error: No stories found in the 'stories' folder.");
            printFooter(ucid, 3);
            scanner.close();
            return;
        }
        List<String> lines = new ArrayList<>();
        // Start edits

        try {
            File [] allStories = folder.listFiles();   // load a random story file
            int randomNumber = (int) (Math.random() * allStories.length);
            File chosenStory = allStories[randomNumber];

            Scanner fileScanner = new Scanner(chosenStory);  // parse the story lines
            while (fileScanner.hasNextLine()) {
                lines.add(fileScanner.nextLine());
            }
            fileScanner.close();

            List<String> finallines = new ArrayList<>();

            for (String oneLine : lines) {   // iterate through the lines
                String modifiedLine = oneLine;
                
                while (modifiedLine.contains("<")) {                             // prompt the user for each placeholder (note: there may be more than one                                          
                    int startOfPlaceholder = modifiedLine.indexOf("<");        // placeholder in a line)
                    int endOfPlaceholder = modifiedLine.indexOf(">");

                    String placeholder = modifiedLine.substring(startOfPlaceholder, endOfPlaceholder + 1);
                    String placeholderText = placeholder.substring(1, placeholder.length() - 1);
                    String prompt = placeholderText.replace("_", " ");

                    System.out.print("Provide a " + prompt + ": ");
                    String userInput = scanner.nextLine();
                    modifiedLine = modifiedLine.replaceFirst(placeholder, userInput);
                }
                finallines.add(modifiedLine);
            }
            lines = finallines;   // apply the update to the same collection slot
        }
        catch (FileNotFoundException e) {
            System.out.println("Could not read story.");
        }
        // End edits
        System.out.println("\nYour Completed Mad Libs Story:\n");
        StringBuilder finalStory = new StringBuilder();
        for (String line : lines) {
            finalStory.append(line).append("\n");
        }
        System.out.println(finalStory.toString());

        printFooter(ucid, 3);
        scanner.close();
    }
}
