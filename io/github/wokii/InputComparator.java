package io.github.wokii;


import java.util.Comparator;

public class InputComparator implements Comparator<InputRecord> {
    public int compare(InputRecord a, InputRecord b)
    {
        return Double.compare(a.getRawValue(), b.getRawValue());

    }
}
