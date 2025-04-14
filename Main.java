public class Main {
   
    public static String reverse(String str) {
        if (str.isEmpty()) return str;  // base case
        return reverse(str.substring(1)) + str.charAt(0);  // recursive case
    }

    public static void main(String[] args) {
        String input = "hello";
        System.out.println("Reversed string: " + reverse(input));
    }


}