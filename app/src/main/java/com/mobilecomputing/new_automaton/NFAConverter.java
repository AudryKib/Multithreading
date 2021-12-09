package com.mobilecomputing.new_automaton;

/*
NFAConverter.java
Version: 11/15/21
Status: Unfinished, minimum functionality
Author: Jacob Gates, Derek Prijatelj
Organization: University of Iowa
 */

/*
Notes:
Inspiration for this code comes from Derek S. Prijatelj
Github link here: https://github.com/prijatelj/thompson-construction/blob/master/Thompson.java
Helpful wikipedia resources for
-- NFAs: https://en.wikipedia.org/wiki/Nondeterministic_finite_automaton
-- Thompson's Alogorithm: https://en.wikipedia.org/wiki/Thompson%27s_construction
 */

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Scanner;
import java.util.ArrayList;
import java.util.Stack;
import static java.lang.String.valueOf;

public class NFAConverter {
    /*
    This class is for converting regular expressions into an automaton and
    displaying that automaton as a graph using graphViz (gv) format.
    This class has a main method.
     */

    /*
    Class Trans represents transition diagrams for the automaton.
     */
    public static class Trans {
        public int from, to;
        public char label;

        public Trans(int st1, int st2, char l) {
            this.from = st1;
            this.to = st2;
            this.label = l;
        }

        public void printTrans() {
            String f, t;
            char l;
            f = valueOf(this.from);
            t = valueOf(this.to);
            l = this.label;
            System.out.println(f + " -" + l + "-> " + t);
        }
    }

    public static class NFA {
        public ArrayList<Integer> states;
        public ArrayList<Trans> trans;
        public int start;
        public int fin;

        public NFA() {
            this.states = new ArrayList<Integer>();
            this.trans = new ArrayList<Trans>();
            this.start = 0;
            this.fin = 0;
        }

        public NFA(int size) {
            this.states = new ArrayList<Integer>();
            this.trans = new ArrayList<Trans>();
            this.fin = 0;
            this.setStateSize(size);
        }

        public NFA(char c) {
            this.states = new ArrayList<Integer>();
            this.trans = new ArrayList<Trans>();
            this.setStateSize(2);
            this.fin = 1;
            this.trans.add(new Trans(0, 1, c));
        }

        public void setStateSize(int s) {
            for (int i = 0; i < s; i++) {
                this.states.add(i);
            }
        }

        public void printNFA() {
            for (Trans t : trans) {
                t.printTrans();
            }
        }
    }

    /*
    Kleene applies the kleene operator to an existing NFA
    Make start = start, final = final+1, add state with transition of
    the desired character, add epsilon transitions between state 1 and 2,
    add epsilon transition between 1 and zero (for repeated patterns)
     */

    public static NFA Kleene(NFA n) {
        int nSize = n.states.size();
        //System.out.println("nSize: "+nSize);
        NFA result = new NFA(nSize + 2); //We add two states
        result.trans.add(new Trans(0, 1, 'E'));
        for (Trans t : n.trans) { //add all preexisting transitions to new NFA
            result.trans.add(new Trans(t.from + 1, t.to + 1, t.label));
        }
        // add epsilon transition from old final state to new final state
        result.trans.add(new Trans(nSize, nSize + 1, 'E'));
        // loop back (* function)
        result.trans.add(new Trans(nSize, 1, 'E'));
        //BUG? Above line should go to state 0?

        //Add epsilon transition from start state to final state (0 repetitions)
        result.trans.add(new Trans(0, nSize + 1, 'E'));
        result.fin = nSize + 1;
        result.start = 0;
        return result;
    }

    /*
    Concat applies concatenation between two NFAs n1 and n2
    Make start = start1, final = final2, merge final1 and start2
     */

    public static NFA Concat(NFA n1, NFA n2) {
        int n1Size = n1.states.size();
        int n2Size = n2.states.size();
        NFA result = new NFA(n1Size + n2Size - 1);
        for (Trans t : n1.trans) { //add n1 transitions
            result.trans.add(t);
        }
        for (Trans t : n2.trans) { //add n2 transitions
            result.trans.add(new Trans(n1Size + t.from - 1, n1Size + t.to - 1, t.label));
        }
        result.start = 0;
        result.fin = n1Size + n2Size - 1;
        return result;
    }

    /*
    Union applies the union operation between NFAs n1 and n2
    Add a new start state and a new final state, add epsilon transitions
    from start to start1 and start2, and add epsilon transitions from
    final1 and final2 to final
     */

    public static NFA Union(NFA n1, NFA n2) {
        int n1Size = n1.states.size();
        int n2Size = n2.states.size();
        //System.out.println("n1Size: "+n1Size+" n2Size: "+n2Size);
        NFA result = new NFA(n1Size + n2Size + 2); // We are adding two states
        for (Trans t : n1.trans) { // Add transitions for first NFA
            result.trans.add(new Trans(t.from + 1, t.to + 1, t.label));
        }
        for (Trans t : n2.trans) { // Add transitions for second NFA
            result.trans.add(new Trans(t.from + 1 + n1Size, t.to + 1 + n1Size, t.label));
        }
        // Add starting epsilon transitions
        result.trans.add(new Trans(0, 1, 'E'));
        result.trans.add(new Trans(0, 1 + n1Size, 'E'));
        // Add final epsilon transitions
        int rSize = result.states.size();
        //System.out.println("rSize: "+rSize);
        result.trans.add(new Trans(n1Size, rSize - 1, 'E'));
        result.trans.add(new Trans(n2Size + n1Size, rSize - 1, 'E'));
        result.fin = rSize - 1;
        result.start = 0;
        return result;
    }

    // simplify the repeated boolean condition checks
    public static boolean alpha(char c) {
        return c >= 'a' && c <= 'z';
    }

    public static boolean alphabet(char c) {
        return alpha(c) || c == 'E';
    }

    public static boolean regexOperator(char c) {
        return c == '[' || c == ']' || c == '*' || c == '|';
    }

    public static boolean validRegExChar(char c) {
        return alphabet(c) || regexOperator(c);
    }

    // validRegEx() - checks if given string is a valid regular expression.
    public static boolean validRegEx(String regex) {
        if (regex.isEmpty())
            return true; // Be careful
        for (char c : regex.toCharArray())
            if (!validRegExChar(c))
                return false;
        return true;
    }

    /*
     Compile2 takes a string and performs the operations in a recursive manner
     Does not work
     TODO: Check Precedence is correct, add [] functionality
     */
    public static NFA compile2(String regex) {
        if (!validRegEx(regex)) { // Test that the regex is parsable
            System.out.println("Invalid Regular Expression");
            return new NFA();
        }
        // Base Case
        if (regex.length() == 0) {
            return new NFA();
        }
        for (int i = 0; i < regex.length(); i++) {
            char c = regex.charAt(i);
            if (c == '*') {
                return Kleene(new NFA(regex.charAt(i - 1)));
            }
        }
        System.out.println("Something weird happened...");
        return new NFA();
    }

    public static NFA compile(String regex) {
        if (!validRegEx(regex)) {
            System.out.println("Invalid Regular Expression Input.");
            return new NFA(); // empty NFA if invalid regex
        }

        Stack<Character> operators = new Stack<Character>();
        Stack<NFA> operands = new Stack<NFA>();
        Stack<NFA> concat_stack = new Stack<NFA>();
        boolean ccflag = false; // concat flag
        char op, c; // current character of string
        int para_count = 0;
        NFA nfa1, nfa2;

        for (int i = 0; i < regex.length(); i++) {
            c = regex.charAt(i);
            if (alphabet(c)) {
                operands.push(new NFA(c));
                if (ccflag) { // concat this w/ previous
                    operators.push('.'); // '.' used to represent concat.
                } else
                    ccflag = true;
            } else {
                if (c == ']') {
                    ccflag = false;
                    if (para_count == 0) {
                        System.out.println("Error: More end paranthesis " +
                                "than beginning paranthesis");
                        System.exit(1);
                    } else {
                        para_count--;
                    }
                    // process stuff on stack till '('
                    while (!operators.empty() && operators.peek() != '[') {
                        op = operators.pop();
                        if (op == '.') {
                            nfa2 = operands.pop();
                            nfa1 = operands.pop();
                            operands.push(Concat(nfa1, nfa2));
                        } else if (op == '|') {
                            nfa2 = operands.pop();

                            if (!operators.empty() &&
                                    operators.peek() == '.') {

                                concat_stack.push(operands.pop());
                                while (!operators.empty() &&
                                        operators.peek() == '.') {

                                    concat_stack.push(operands.pop());
                                    operators.pop();
                                }
                                nfa1 = Concat(concat_stack.pop(),
                                        concat_stack.pop());
                                while (concat_stack.size() > 0) {
                                    nfa1 = Concat(nfa1, concat_stack.pop());
                                }
                            } else {
                                nfa1 = operands.pop();
                            }
                            operands.push(Union(nfa1, nfa2));
                        }
                    }
                } else if (c == '*') {
                    operands.push(Kleene(operands.pop()));
                    ccflag = true;
                } else if (c == '[') { // if any other operator: push
                    operators.push(c);
                    para_count++;
                } else if (c == '|') {
                    operators.push(c);
                    ccflag = false;
                }
            }
        }
        while (operators.size() > 0) {
            if (operands.empty()) {
                System.out.println("Error: imbalanace in operands and "
                        + "operators");
                System.exit(1);
            }
            op = operators.pop();
            if (op == '.') {
                nfa2 = operands.pop();
                nfa1 = operands.pop();
                operands.push(Concat(nfa1, nfa2));
            } else if (op == '|') {
                nfa2 = operands.pop();
                if (!operators.empty() && operators.peek() == '.') {
                    concat_stack.push(operands.pop());
                    while (!operators.empty() && operators.peek() == '.') {
                        concat_stack.push(operands.pop());
                        operators.pop();
                    }
                    nfa1 = Concat(concat_stack.pop(),
                            concat_stack.pop());
                    while (concat_stack.size() > 0) {
                        nfa1 = Concat(nfa1, concat_stack.pop());
                    }
                } else {
                    nfa1 = operands.pop();
                }
                operands.push(Union(nfa1, nfa2));
            }
        }
        return operands.pop();
    }

    public static String toGraphViz(NFA n) {
        String result = "digraph D { \n\n";
        for (int s : n.states) {
            result = result + valueOf(s) + " [shape=point]" + "\n";
        }
        result = result + "\n";
        result = result + "start -> 0 \n";
        for (Trans t : n.trans) {
            result = result + valueOf(t.from) + " -> " + valueOf(t.to) + " [label= " + t.label + "]" + "\n";
        }
        result = result + "\n}";
        return result;
    }

}
        /*
    public static void writeGraphViz(String filename, NFA n) {
        try {
            File myObj = new File(filename);
            if (myObj.createNewFile()) {
                System.out.println("File created");
            } else {
                System.out.println("File already exists");
            }
        } catch (IOException e) {
            System.out.println("Error");
        }
        try {
            FileWriter myWriter = new FileWriter(filename);
            myWriter.write(toGraphViz(n));
            myWriter.close();
        } catch (IOException e) {
            System.out.println("Error");
        }
    } */

    //Main method asks for a string then returns the NFA
    //TODO Make the main method take an argument from command line for integration within the app

