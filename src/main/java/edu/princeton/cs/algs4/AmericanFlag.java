/******************************************************************************
 *  Compilation:  javac AmericanFlag.java
 *  Execution:    java AmericanFlag < input.txt
 *                java AmericanFlag int < input-non-negative-ints.txt
 *  Dependencies: StdIn.java StdOut.java Stack.java
 *  Data files:   https://algs4.cs.princeton.edu/51radix/words3.txt
 *                https://algs4.cs.princeton.edu/51radix/shells.txt
 *
 *  Sort an array of strings or integers in-place using American flag sort.
 *
 *  % java AmericanFlag < shells.txt
 *  are
 *  by
 *  sea
 *  seashells
 *  seashells
 *  sells
 *  sells
 *  she
 *  she
 *  shells
 *  shore
 *  surely
 *  the
 *  the
 *
 ******************************************************************************/

package edu.princeton.cs.algs4;

/**
 *  The {@code AmericanFlag} class provides static methods for sorting an
 *  array of extended ASCII strings or integers in-place using
 *  American flag sort. This is a non-recursive implementation.
 *  <p>
 *  For additional documentation,
 *  see <a href="https://algs4.cs.princeton.edu/51radix">Section 5.1</a> of
 *  <i>Algorithms, 4th Edition</i> by Robert Sedgewick and Kevin Wayne
 *  and <a href = "http://static.usenix.org/publications/compsystems/1993/win_mcilroy.pdf">
 *  Engineering Radix Sort</a> by McIlroy and Bostic.
 *  For a version that uses only one auxiliary array, see {@link AmericanFlagX}.
 *
 *  @author Robert Sedgewick
 *  @author Kevin Wayne
 *  @author Ivan Pesin
 */

public class AmericanFlag {
    private static final int BITS_PER_BYTE =   8;
    private static final int BITS_PER_INT  =  32;   // each Java int is 32 bits
    private static final int R             = 256;   // extend ASCII alphabet size
    private static final int CUTOFF        =  15;   // cutoff to insertion sort

    // do not instantiate
    private AmericanFlag() { }

    // return dth character of s, -1 if d = length of string
    private static int charAt(String s, int d) {
        assert d >= 0 && d <= s.length();
        if (d == s.length()) return -1;
        return s.charAt(d);
    }

    /**
     * Rearranges the array of extended ASCII strings in ascending order.
     * This is an unstable sorting algorithm.
     *
     * @param a the array to be sorted
     */
    public static void sort(String[] a) {
        sort(a, 0, a.length - 1);
    }

    // sort from a[lo] to a[hi], starting at the dth character
    public static void sort(String[] a, int lo, int hi) {
        // one-time allocation of data structures
        Stack<Integer> st = new Stack<Integer>();
        int[] first = new int[R+2];
        int[] next  = new int[R+2];
        int d = 0; // character index to sort by

        st.push(lo);
        st.push(hi);
        st.push(d);

        while (!st.isEmpty()) {
            d = st.pop();
            hi = st.pop();
            lo = st.pop();

            if (hi <= lo + CUTOFF) {
                insertion(a, lo, hi, d);
                continue;
            }

            // compute frequency counts
            for (int i = lo; i <= hi; i++) {
                int c = charAt(a[i], d) + 1; // account for -1 representing end-of-string
                first[c+1]++;
            }

            // first[c] = location of first string whose dth character = c
            first[0] = lo;
            for (int c = 0; c <= R; c++) {
                first[c+1] += first[c];

                if (c > 0 && first[c+1]-1 > first[c]) {
                    // add subproblem for character c (excludes sentinel c == 0)
                    st.push(first[c]);
                    st.push(first[c+1] - 1);
                    st.push(d+1);
                }
            }

            // next[c] = location to place next string whose dth character = c
            for (int c = 0; c < R+2; c++)
                next[c] = first[c];

            // permute data in place
            for (int k = lo; k <= hi; k++) {
                int c = charAt(a[k], d) + 1;
                while (first[c] > k) {
                    exch(a, k, next[c]++);
                    c = charAt(a[k], d) + 1;
                }
                next[c]++;
            }

            // clear first[] and next[] arrays
            for (int c = 0; c < R+2; c++) {
                first[c] = 0;
                next[c] = 0;
            }
        }
    }

    // insertion sort a[lo..hi], starting at dth character
    private static void insertion(String[] a, int lo, int hi, int d) {
        for (int i = lo; i <= hi; i++)
            for (int j = i; j > lo && less(a[j], a[j-1], d); j--)
                exch(a, j, j-1);
    }

    // exchange a[i] and a[j]
    private static void exch(String[] a, int i, int j) {
        String temp = a[i];
        a[i] = a[j];
        a[j] = temp;
    }

    // is v less than w, starting at character d
    private static boolean less(String v, String w, int d) {
        // assert v.substring(0, d).equals(w.substring(0, d));
        for (int i = d; i <  Math.min(v.length(), w.length()); i++) {
            if (v.charAt(i) < w.charAt(i)) return true;
            if (v.charAt(i) > w.charAt(i)) return false;
        }
        return v.length() < w.length();
    }

   /**
     * Rearranges the array of 32-bit integers in ascending order.
     * Currently, assumes that the integers are nonnegative.
     *
     * @param a the array to be sorted
     */
    public static void sort(int[] a) {
        sort(a, 0, a.length-1);
    }

    // MSD sort from a[lo] to a[hi]
    private static void sort(int[] a, int lo, int hi) {
        // one-time allocation of data structures
        Stack<Integer> st = new Stack<Integer>();
        int[] first = new int[R+1];
        int[] next  = new int[R+1];
        int mask = R - 1;   // 0xFF;
        int d = 0;          // byte to sort by

        st.push(lo);
        st.push(hi);
        st.push(d);

        while (!st.isEmpty()) {
            d = st.pop();
            hi = st.pop();
            lo = st.pop();

            if (hi <= lo + CUTOFF) {
                insertion(a, lo, hi, d);
                continue;
            }

            // compute frequency counts (need R = 256)
            int shift = BITS_PER_INT - BITS_PER_BYTE*d - BITS_PER_BYTE;
            for (int i = lo; i <= hi; i++) {
                int c = (a[i] >> shift) & mask;
                first[c+1]++;
            }

            // first[c] = location of first int whose dth byte = c
            first[0] = lo;
            for (int c = 0; c < R; c++) {
                first[c+1] += first[c];

                if (d < 3 && first[c+1]-1 > first[c]) {
                    // add subproblem for byte c
                    st.push(first[c]);
                    st.push(first[c+1] - 1);
                    st.push(d+1);
                }
            }

            // next[c] = location to place next string whose dth byte = c
            for (int c = 0; c < R+1; c++)
                next[c] = first[c];

            // permute data in place
            for (int k = lo; k <= hi; k++) {
                int c = (a[k] >> shift) & mask;
                while (first[c] > k) {
                    exch(a, k, next[c]++);
                    c = (a[k] >> shift) & mask;
                }
                next[c]++;
            }

            // clear first[] and next[] arrays
            for (int c = 0; c < R+1; c++) {
                first[c] = 0;
                next[c] = 0;
            }
        }
    }

    // insertion sort a[lo..hi], starting at dth byte
    private static void insertion(int[] a, int lo, int hi, int d) {
        for (int i = lo; i <= hi; i++)
            for (int j = i; j > lo && less(a[j], a[j-1], d); j--)
                exch(a, j, j-1);
    }

    // exchange a[i] and a[j]
    private static void exch(int[] a, int i, int j) {
        int temp = a[i];
        a[i] = a[j];
        a[j] = temp;
    }

    // is v less than w, starting at byte d
    private static boolean less(int v, int w, int d) {
        int mask = R - 1;   // 0xFF;
        for (int i = d; i < 4; i++) {
            int shift = BITS_PER_INT - BITS_PER_BYTE*i - BITS_PER_BYTE;
            int a = (v >> shift) & mask;
            int b = (w >> shift) & mask;
            if (a < b) return true;
            if (a > b) return false;
        }
        return false;
    }

    /**
     * Reads in a sequence of extended ASCII strings or non-negative ints from standard input;
     * American flag sorts them;
     * and prints them to standard output in ascending order.
     *
     * @param args the command-line arguments: "int" to read input as non-negative integers
     */
    public static void main(String[] args) {
        if (args.length > 0 && args[0].equals("int")) {
            int[] a = StdIn.readAllInts();
            sort(a);

            // print results
            for (int i = 0; i < a.length; i++)
                StdOut.println(a[i]);
        }

        else {
            String[] a = StdIn.readAllStrings();
            sort(a);
            // print results
            for (int i = 0; i < a.length; i++)
                StdOut.println(a[i]);
        }
    }
}


/******************************************************************************
 *  Copyright 2002-2025, Robert Sedgewick and Kevin Wayne.
 *
 *  This file is part of algs4.jar, which accompanies the textbook
 *
 *      Algorithms, 4th edition by Robert Sedgewick and Kevin Wayne,
 *      Addison-Wesley Professional, 2011, ISBN 0-321-57351-X.
 *      http://algs4.cs.princeton.edu
 *
 *
 *  algs4.jar is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  algs4.jar is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with algs4.jar.  If not, see http://www.gnu.org/licenses.
 ******************************************************************************/
