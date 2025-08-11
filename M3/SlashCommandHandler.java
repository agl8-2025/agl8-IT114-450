package M3;

/*
Challenge 2: Simple Slash Command Handler
-----------------------------------------
- Accept user input as slash commands
  - "/greet <name>" → Prints "Hello, <name>!"
  - "/roll <num>d<sides>" → Roll <num> dice with <sides> and returns a single outcome as "Rolled <num>d<sides> and got <result>!"
  - "/echo <message>" → Prints the message back
  - "/quit" → Exits the program
- Commands are case-insensitive
- Print an error for unrecognized commands
- Print errors for invalid command formats (when applicable)
- Capture 3 variations of each command except "/quit"
*/

import java.util.Scanner;


public class SlashCommandHandler extends BaseClass {
    private static String ucid = "agl8"; // <-- change to your UCID

    public static void main(String[] args) {
        printHeader(ucid, 2, "Objective: Implement a simple slash command parser.");

        Scanner scanner = new Scanner(System.in);

        // Can define any variables needed here

        while (true) {
            System.out.print("Enter command: ");
            // get entered text
            String userInput = scanner.nextLine();

            String[] parts = userInput.split(" ");
            String command = parts[0];

            // check if greet
            if (command.equalsIgnoreCase("/greet")) {
                if (parts.length == 2) {
                    String name = parts[1];
                    System.out.println("Hello, " + name + "!");
                }
            }
            //// process greet

            // check if roll
            else if (command.equalsIgnoreCase("/roll")) {
                if (parts.length == 2 && parts[1].contains("d")) {
                    String[] diceParts = parts[1].split("d");
                    if (diceParts.length == 2) {
                        try {
                            int numDice = Integer.parseInt(diceParts[0]);
                            int numSides = Integer.parseInt(diceParts[1]);
                            int total = 0;
                            for (int i = 0; i < numDice; i++) {
                                total += (int)(Math.random() * numSides) + 1;
                            }
                            System.out.println("Rolled" + parts[1] + " and got " + total + "!");
                        }  
                        catch (NumberFormatException e) {   
                            System.out.println("Invalid format. Use numbers like 2d6."); 
                        }
                    }
                    else {
                        System.out.println("Invalid format. Use: /roll <num>d<sides>");
                    }
                }
                else {
                    System.out.println("Invalid format. Use: /roll <num>d<sides>");
                }
            }
            //// process roll
            //// handle invalid formats

            // check if echo
            else if (command.equalsIgnoreCase("/echo")) {
                if (parts.length > 1) {
                    String message = userInput.substring(command.length()).trim();
                    System.out.println(message);
                }
                else {
                    System.out.println("Invalid format. Use: /echo <message>");
                }
            }
            //// process echo

            // check if quit
            else if (command.equalsIgnoreCase("/quit")) {
                break;
            }
            else {
                System.out.println("Unrecognized command: " + command);
            }
        }
            //// process quit
            // handle invalid commnads
        
        System.out.println("Exiting.");
        printFooter(ucid, 2);
        scanner.close();
    }
}
