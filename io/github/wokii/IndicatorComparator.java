package io.github.wokii;

import java.util.Comparator;

public class IndicatorComparator implements Comparator<Indicator> {
    public int compare(Indicator a, Indicator b)
    {
        return Double.compare(a.getVal(), b.getVal());

    }
}
