import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class calculator123 {
    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);

        System.out.println("=== Simple Calculator with Lists and Queues ===");
        System.out.println("Instructions:");
        System.out.println(" - Enter expressions with +, -, *, /, and parentheses.");
        System.out.println(" - Example: (2 + 3) * 4 - 5 / (1 + 1)");
        System.out.println(" - Type 'exit' to quit.\n");

        while (true) {
            System.out.print("Enter expression: ");
            String input = scanner.nextLine().trim();

            if (input.equalsIgnoreCase("exit")) {
                System.out.println("Exiting calculator. Goodbye!");
                break;
            }

            List<Integer> unmatchedPositions = findUnmatchedParenthesesPositions(input);
            if (!unmatchedPositions.isEmpty()) {
                System.out.println("Unbalanced parentheses at positions: " + unmatchedPositions);
                System.out.print("Please enter the position (index) to add a parenthesis: ");
                int position;
                try {
                    position = Integer.parseInt(scanner.nextLine());
                    System.out.print("Which parenthesis to add? (left/right): ");
                    String type = scanner.nextLine().trim().toLowerCase();
                    if (type.equals("left")) {
                        input = insertParenthesis(input, position, '(');
                    } else if (type.equals("right")) {
                        input = insertParenthesis(input, position, ')');
                    } else {
                        System.out.println("Invalid type. Please enter 'left' or 'right'.");
                        continue;
                    }
                } catch (NumberFormatException e) {
                    System.out.println("Invalid position. Please try again.");
                    continue;
                }
            }

            try {
                double result = evaluateExpression(input);
                ArrayList<Double> numbers = extractNumbers(input);

                if (numbers.isEmpty()) {
                    System.out.println("No numbers found in expression.");
                } else {
                    System.out.print("Display numbers as (arraylist/linkedlist/queue): ");
                    String choice = scanner.nextLine().trim().toLowerCase();

                    Collection<Double> collection;

                    switch (choice) {
                        case "linkedlist":
                            collection = new LinkedList<>(numbers);
                            break;
                        case "queue":
                            collection = new LinkedList<>(numbers);
                            break;
                        case "arraylist":
                        default:
                            collection = new ArrayList<>(numbers);
                    }

                    performOperations(collection, scanner);

                    while (true) {
                        System.out.print("Do you want to change the type of list? (yes/no): ");
                        String convert = scanner.nextLine().trim().toLowerCase();
                        if (!convert.equals("yes")) break;

                        System.out.print("Convert to (arraylist/linkedlist/queue): ");
                        String newType = scanner.nextLine().trim().toLowerCase();

                        switch (newType) {
                            case "linkedlist":
                                collection = new LinkedList<>(collection);
                                break;
                            case "queue":
                                collection = new LinkedList<>(collection);
                                break;
                            case "arraylist":
                                collection = new ArrayList<>(collection);
                                break;
                            default:
                                System.out.println("Invalid choice.");
                                continue;
                        }

                        performOperations(collection, scanner);
                    }
                }

                TreeSet<Double> sortedSet = new TreeSet<>(numbers);
                ArrayList<Double> sortedNumbers = new ArrayList<>(sortedSet);
                System.out.println("Sorted numbers (no duplicates): " + sortedNumbers);

                ArrayList<Double> evenNumbers = new ArrayList<>();
                ArrayList<Double> oddNumbers = new ArrayList<>();

                for (Double n : sortedNumbers) {
                    if (n % 1 == 0) {
                        if (((int) Math.round(n)) % 2 == 0) evenNumbers.add(n);
                        else oddNumbers.add(n);
                    }
                }

                System.out.println("Even numbers: " + evenNumbers);
                System.out.println("Odd numbers: " + oddNumbers);
                System.out.println("Result of expression: " + result + "\n");

            } catch (Exception e) {
                System.out.println("Invalid expression. Please try again.");
            }
        }

        scanner.close();
    }

    private static List<Integer> findUnmatchedParenthesesPositions(String expr) {
        LinkedList<Integer> stack = new LinkedList<>();
        List<Integer> errors = new ArrayList<>();

        for (int i = 0; i < expr.length(); i++) {
            char ch = expr.charAt(i);
            if (ch == '(') stack.push(i);
            else if (ch == ')') {
                if (stack.isEmpty()) errors.add(i);
                else stack.pop();
            }
        }

        errors.addAll(stack);
        return errors;
    }

    private static String insertParenthesis(String expr, int position, char parenthesis) {
        if (position < 0 || position > expr.length()) {
            System.out.println("Position out of bounds. No changes made.");
            return expr;
        }
        return expr.substring(0, position) + parenthesis + expr.substring(position);
    }

    private static ArrayList<Double> extractNumbers(String expr) {
        ArrayList<Double> numbers = new ArrayList<>();
        Pattern pattern = Pattern.compile("(-?\\d+(\\.\\d+)?)");
        Matcher matcher = pattern.matcher(expr);
        while (matcher.find()) {
            try {
                numbers.add(Double.parseDouble(matcher.group(1)));
            } catch (NumberFormatException ignored) {}
        }
        return numbers;
    }

    private static double evaluateExpression(String expression) {
        return new Object() {
            int pos = -1, ch;

            void nextChar() {
                ch = (++pos < expression.length()) ? expression.charAt(pos) : -1;
            }

            boolean eat(int charToEat) {
                while (ch == ' ') nextChar();
                if (ch == charToEat) {
                    nextChar();
                    return true;
                }
                return false;
            }

            double parse() {
                nextChar();
                double x = parseExpression();
                if (pos < expression.length()) throw new RuntimeException("Unexpected: " + (char) ch);
                return x;
            }

            double parseExpression() {
                double x = parseTerm();
                while (true) {
                    if (eat('+')) x += parseTerm();
                    else if (eat('-')) x -= parseTerm();
                    else return x;
                }
            }

            double parseTerm() {
                double x = parseFactor();
                while (true) {
                    if (eat('*')) x *= parseFactor();
                    else if (eat('/')) x /= parseFactor();
                    else return x;
                }
            }

            double parseFactor() {
                if (eat('+')) return parseFactor();
                if (eat('-')) return -parseFactor();

                double x;
                int startPos = this.pos;

                if (eat('(')) {
                    x = parseExpression();
                    eat(')');
                } else if ((ch >= '0' && ch <= '9') || ch == '.') {
                    while ((ch >= '0' && ch <= '9') || ch == '.') nextChar();
                    x = Double.parseDouble(expression.substring(startPos, this.pos));
                } else {
                    throw new RuntimeException("Unexpected character: " + (char) ch);
                }

                return x;
            }
        }.parse();
    }

    private static void performOperations(Collection<Double> collection, Scanner scanner) {
        System.out.println("\nCollection type: " + collection.getClass().getSimpleName());
        System.out.println("Current values: " + collection);

        if (collection instanceof Queue) {
            Queue<Double> queue = (Queue<Double>) collection;
            int rotationCount = 0;
            int addedSinceLastRotation = 0;

            // Prompt for queue size
            System.out.print("Enter the size of the queue: ");
            int queueSize;
            try {
                queueSize = Integer.parseInt(scanner.nextLine());
            } catch (NumberFormatException e) {
                System.out.println("Invalid size. Defaulting to 5.");
                queueSize = 5; // Default size
            }

            while (true) {
                System.out.println("\nQueue options:");
                System.out.println("1. Show next number");
                System.out.println("2. Show total numbers");
                System.out.println("3. Add a number to queue");
                System.out.println("4. Remove next number");
                System.out.println("5. Rotate queue");
                System.out.println("0. Back");

                System.out.print("Enter option: ");
                int option;
                try {
                    option = Integer.parseInt(scanner.nextLine());
                } catch (Exception e) {
                    System.out.println("Invalid input.");
                    continue;
                }

                if (option == 0) break;

                switch (option) {
                    case 1:
                        System.out.println("Next number: " + queue.peek());
                        break;
                    case 2:
                        System.out.println("Total numbers: " + queue.size());
                        break;
                    case 3:
                        System.out.print("Enter number to add: ");
                        double val = Double.parseDouble(scanner.nextLine());
                        if (queue.size() == queueSize) {
                            queue.poll(); // Remove oldest element if full
                        }
                        queue.offer(val);
                        addedSinceLastRotation++;

                        if (addedSinceLastRotation == queueSize) {
                            Double front = queue.poll();
                            queue.offer(front);
                            rotationCount++;
                            addedSinceLastRotation = 0;
                            System.out.println("Queue rotated. Rotation count: " + rotationCount);
                        }
                        break;
                    case 4:
                        System.out.println("Removed number: " + queue.poll());
                        break;
                    case 5:
                        if (!queue.isEmpty()) {
                            Double front = queue.poll();
                            queue.offer(front);
                            rotationCount++;
                            System.out.println("Queue rotated. Rotation count: " + rotationCount);
                        } else {
                            System.out.println("Queue is empty. Cannot rotate.");
                        }
                        break;
                    default:
                        System.out.println("Invalid option.");
                }

                System.out.println("Current queue: " + queue);
            }
        }

        else if (collection instanceof ArrayList) {
            ArrayList<Double> list = (ArrayList<Double>) collection;
            while (true) {
                System.out.println("\nArrayList options:");
                System.out.println("1. Show number at position");
                System.out.println("2. Show total numbers");
                System.out.println("3. Check if a number exists");
                System.out.println("4. Add a number");
                System.out.println("5. Remove a number");
                System.out.println("0. Back");

                System.out.print("Enter option: ");
                int option;
                try {
                    option = Integer.parseInt(scanner.nextLine());
                } catch (Exception e) {
                    System.out.println("Invalid input.");
                    continue;
                }

                if (option == 0) break;

                try {
                    switch (option) {
                        case 1:
                            System.out.print("Enter position: ");
                            int index = Integer.parseInt(scanner.nextLine());
                            System.out.println("Number at position: " + list.get(index));
                            break;
                        case 2:
                            System.out.println("Total numbers: " + list.size());
                            break;
                        case 3:
                            System.out.print("Enter number to find: ");
                            double val = Double.parseDouble(scanner.nextLine());
                            System.out.println("Exists: " + list.contains(val));
                            break;
                        case 4:
                            System.out.print("Enter number to add: ");
                            val = Double.parseDouble(scanner.nextLine());
                            list.add(val);
                            break;
                        case 5:
                            System.out.print("Enter number to remove: ");
                            val = Double.parseDouble(scanner.nextLine());
                            list.remove(val);
                            break;
                        default:
                            System.out.println("Invalid option.");
                    }
                } catch (Exception e) {
                    System.out.println("Error: " + e.getMessage());
                }

                System.out.println("Updated values: " + list);
            }
        }

        else if (collection instanceof LinkedList) {
            LinkedList<Double> list = (LinkedList<Double>) collection;
            while (true) {
                System.out.println("\nLinkedList options:");
                System.out.println("1. Show first number");
                System.out.println("2. Show last number");
                System.out.println("3. Add a number at beginning");
                System.out.println("4. Add a number at end");
                System.out.println("5. Remove first number");
                System.out.println("6. Remove last number");
                System.out.println("0. Back");

                System.out.print("Enter option: ");
                int option;
                try {
                    option = Integer.parseInt(scanner.nextLine());
                } catch (Exception e) {
                    System.out.println("Invalid input.");
                    continue;
                }

                if (option == 0) break;

                try {
                    switch (option) {
                        case 1:
                            System.out.println("First number: " + list.getFirst());
                            break;
                        case 2:
                            System.out.println("Last number: " + list.getLast());
                            break;
                        case 3:
                            System.out.print("Enter number to add at start: ");
                            double val = Double.parseDouble(scanner.nextLine());
                            list.addFirst(val);
                            break;
                        case 4:
                            System.out.print("Enter number to add at end: ");
                            val = Double.parseDouble(scanner.nextLine());
                            list.addLast(val);
                            break;
                        case 5:
                            list.removeFirst();
                            break;
                        case 6:
                            list.removeLast();
                            break;
                        default:
                            System.out.println("Invalid option.");
                    }
                } catch (Exception e) {
                    System.out.println("Error: " + e.getMessage());
                }

                System.out.println("Updated values: " + list);
            }
        }
    }
}