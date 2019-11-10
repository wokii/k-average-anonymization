package io.github.wokii;


import java.util.ArrayList;

public class Indicator {
    public Indicator() {

        this.sum = 0;
        list = new ArrayList<>();
    }
    public double sum;
    private ArrayList<InputRecord> list;

    public double getVal() {
        return sum/list.size();
    }
    public ArrayList<InputRecord> getList() {
        return list;
    }
    public void addRecord(InputRecord input) {
        list.add(input);
        sum += input.getRawValue();
    }

    public int getLength() {
        return list.size();
    }

    public double getSum() {
        return sum;
    }


}
