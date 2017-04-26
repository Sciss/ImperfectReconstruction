package foo;

public class Foo {
    public static int compareNames(final String name1, final String name2) {
        final int n1 = name1.length();
        final int n2 = name2.length();
        final int min = Math.min(n1, n2);

        for (int i = 0; i < min; i++) {
            char c1 = name1.charAt(i);
            char c2 = name2.charAt(i);
            boolean d1 = Character.isDigit(c1);
            boolean d2 = Character.isDigit(c2);

            if (d1 && d2) {
                // Enter numerical comparison
                char c3, c4;
                do {
                    i++;
                    c3 = i < n1 ? name1.charAt(i) : 'x';
                    c4 = i < n2 ? name2.charAt(i) : 'x';
                    if (c1 == c2 && c3 != c4) {
                        c1 = c3;
                        c2 = c4;
                    }
                    d1 = Character.isDigit(c3);
                    d2 = Character.isDigit(c4);
                }
                while (d1 && d2);

                if (d1 != d2) {
                    return d1 ? 1 : -1;
                }
                if (c1 != c2) {
                    return c1 - c2;
                }
                i--;

            } else if (c1 != c2) {
                c1 = Character.toUpperCase(c1);
                c2 = Character.toUpperCase(c2);

                if (c1 != c2) {
                    c1 = Character.toLowerCase(c1);
                    c2 = Character.toLowerCase(c2);

                    if (c1 != c2) {
                        // No overflow because of numeric promotion
                        return c1 - c2;
                    }
                }
            }
        }
        return n1 - n2;
    }
}
