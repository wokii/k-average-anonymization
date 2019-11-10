package io.github.wokii;



import java.lang.reflect.Array;
import java.util.*;

import java.util.Collection;


public class HanWangSolution implements StreamingKFilter {

    static double TOLERANCE_FACTOR = 10;
    static final int K = 5;
    private int count = 0;
    private int accumulatedPulishedCount = 0;
    private List<InputRecord> original = new ArrayList<>();

    private List<Indicator> indicators = new ArrayList<>();

    /*  this is the list that stores all the indicator;

     */

    private double getTolerance(){
        return 10;
        //return getRange()/TOLERANCE_FACTOR;
    }
    private double curMax = Double.MIN_VALUE;
    private double curMin = Double.MAX_VALUE;

    private double getRange() throws Exception{
        if (original.size() == 0) {
            throw new Exception("no data yet!");
        }
        return curMax - curMin;
    }

    static public Indicator constructIndicator(InputRecord input) {
        Indicator indicator = new Indicator();
        indicator.addRecord(input);
        return indicator;
    }

    private int findIndicator(double target) {
        indicators.sort(new IndicatorComparator());
        int l = 0, r = indicators.size()-1;

        while (l+1<r) {
            //System.out.println("l "+l);
            //System.out.println("r " + r);

            if (target < indicators.get(l).getVal()) {
                return l;
            } else if (target > indicators.get(r).getVal()) {
                return r;
            }

            int mid = (l+r) / 2;
            if (target<indicators.get(mid).getVal()) {
                r = mid;
            } else if (target == mid) {
                return mid;
            } else {
                l = mid;
            }

        }
        double leftDiff = Math.abs(indicators.get(l).getVal()-target), rightDiff =  Math.abs(indicators.get(r).getVal()-target);
        return leftDiff <= rightDiff? l:r;
    }
    private int findIndicator(InputRecord input) {
        double target = input.getRawValue();
        return findIndicator(target);

    }


    @Override
    public void processNewRecord(InputRecord input) {
        count += 1;

        original.add(input);

        if (input.getRawValue() > curMax) {
            curMax = input.getRawValue();
        }
        if (input.getRawValue() < curMin) {
            curMin = input.getRawValue();
        }

        if (indicators.isEmpty()) {
            Indicator indicator = constructIndicator(input);

            indicators.add(indicator);
        } else {
            // find the closest value in indicators and add this inputRecord to that indicator;

            int index = findIndicator(input);
            Indicator indicator = indicators.get(index);
            if (Math.abs(indicator.getVal() - input.getRawValue()) <= getTolerance()) {

                indicator.addRecord(input);

            } else {

                indicator = constructIndicator(input);
                indicators.add(indicator);
            }

        }

    }
    private LinkedListNode getGroupedIndicators() {
        LinkedListNode dummy = new LinkedListNode(null);
        LinkedListNode cur = dummy;

        ArrayList<LinkedListNode> shortHanded = new ArrayList<>();

        for (int i = 0; i < indicators.size(); i++) {
            LinkedListNode node = new LinkedListNode(new ArrayList<>(Arrays.asList(i)));
            node.setLength(indicators.get(i).getLength());
            if (node.getLength() < K) {
                shortHanded.add(node);
            }
            LinkedListNode.connect(cur, node);
            cur = node;
        }
        while (!shortHanded.isEmpty()) {
            LinkedListNode last = shortHanded.remove(shortHanded.size()-1);
            if (last.getLength() >= K) {
                continue;
            }
            if (last.prev != null && last.prev != dummy) {
                LinkedListNode secondLast = last.prev;
                LinkedListNode.remove(last);
                for (int index: last.val) {
                    secondLast.val.add(index);
                    secondLast.addLength(indicators.get(index).getLength());
                }

            } else if (last.next != null) {
                LinkedListNode lastNext = last.next;
                LinkedListNode.remove(last);
                for (int index: last.val) {
                    lastNext.val.add(index);
                    lastNext.addLength(indicators.get(index).getLength());
                }
            } else {
                // this is the only indicator group and its size is smaller than K, can not do much.
                //System.out.println("sad, have to break");
                System.out.println("smaller than K, these data will not be released.");
                break;
            }
        }

        return dummy.next;
    }

    private void sortInput(List<InputRecord> input) {
        input.sort(new InputComparator());
    }



    private double calculateAverage(List<InputRecord> inputs) {
        double sum = 0;
        for (InputRecord rec: inputs) {
            sum += rec.getRawValue();
        }
        return sum/inputs.size();

    }

    @Override
    public Collection<OutputRecord> returnPublishableRecords() {
        LinkedListNode groupHead = getGroupedIndicators();
        LinkedListNode cur = groupHead;

        List<OutputRecord> rtn = new ArrayList<>();
        HashSet<Integer> reserved = new HashSet<>();
        for (int i = 0; i < indicators.size(); i++) {
            reserved.add(i);
        }
        int publishedCount = 0;
        while (cur != null) {
            double totalSum = 0;
            int totalLength = 0;
            for (int indicatorIndex: cur.val) {
                Indicator curIndicator = indicators.get(indicatorIndex);
                totalSum += curIndicator.getSum();
                totalLength += curIndicator.getLength();
            }
            if (totalLength >= K) {
                //System.out.println("totalLength : "+totalLength );
                double anonymisedValue = totalSum / totalLength;

                for (int indicatorIndex : cur.val) {
                    reserved.remove(indicatorIndex);
                    for (InputRecord inputRecord : indicators.get(indicatorIndex).getList()) {
                        publishedCount += 1;
                        rtn.add(new OutputRecord(inputRecord, inputRecord.getTime(), anonymisedValue));
                    }
                }
            } //else {
//                for (int indicatorIndex : cur.val) {
//                    System.out.println("skipped indicator[" + indicatorIndex + "]");
//                }
//            }
            cur = cur.next;
        }
        ArrayList<Indicator> newIndicators = new ArrayList<>();
        int newCount = 0;
        for (int reservedIndex : reserved) {
            newIndicators.add(indicators.get(reservedIndex));
            newCount += indicators.get(reservedIndex).getLength();
        }
        indicators = newIndicators;
        int idealPublishedCount = count-newCount;
        /*
        if (idealPublishedCount != publishedCount) {
            System.out.println("expected published = " + idealPublishedCount);
            System.out.println("actual published Count = " + publishedCount);
            System.out.println("accumulated Published count = " + accumulatedPulishedCount);

            System.out.println("SOMETHING IS WRONG!!!!!");
            System.out.println(rtn.size());
            System.out.println(rtn.toString());
        } */
        // this block was used to check why the record was published multiple times.
        // turns out that I forgot to remove the group node away from the group linkedlist.

        count = newCount;
        accumulatedPulishedCount += publishedCount;
        // System.out.println("accumulated Published count = " + accumulatedPulishedCount);

        return rtn;
    }
    public static void main(String[] args) {
        //testSortIndicators();
        testInputOutWithoutRandom();
    }
    @SuppressWarnings("Duplicates")
    static public void testInputOutWithoutRandom() {

        HanWangSolution tree1 = new HanWangSolution();
        double[] inputsDouble = {7.027975769181993, 83.70539227008959, 4.21142939069502, 46.015658180857855, 92.86906934683175, 37.41892023919755, 4.232061130441411, 47.287212519116586, 0.5854543561210623, 61.35118972919166, 25.668640166496882, 11.585527771544813, 74.0792771960401, 42.122222966641345, 21.87149251839029, 79.83819302335505, 11.099623590114904, 41.67376809640798, 69.00315042172087, 82.59718207402956, 45.68895332909215, 10.80058484381844, 70.68808555171574, 25.370999086456624, 2.1029668966235593, 79.31338048899151, 85.98677019347079, 33.21329077702343, 67.19177684851813, 45.626973324249995, 25.582321601324132, 81.90249365326946, 27.937356880682017, 7.805521853872555, 31.08760220024649, 45.09792675610712, 86.17423061833317, 87.95998879325533, 38.02511672752169, 29.533463523826875, 93.47742506385912, 2.38124402897556, 24.301635114235097, 53.61072640114344, 45.783719222562794, 20.905518495397157, 62.89393248414509, 48.6795329097002, 39.5192397002038, 40.747195609090156, 29.981655244211638, 26.84407909649934, 18.838554618566338, 72.39720275211945, 67.90775958027125, 64.30916052028185, 37.03735064622923, 98.17354528942838, 66.11658934141167, 95.08146960261254, 14.282889508098485, 17.62382911560716, 2.6141096498756067, 96.2668441137985, 98.51341411447389, 38.217865776142176, 83.51441694524189, 73.25506616752861, 26.689768228545784, 4.631760046612243, 28.88112095001485, 20.75964042910575, 39.53944212958709, 85.4131481128043, 79.34176572624945, 43.98071760502088, 24.515381892800846, 51.93054776023674, 39.84793931493471, 73.08515796067692, 48.89884406793675, 91.24804213857786, 34.47333494873841, 93.74846334379, 97.80767299329509, 91.92983661201092, 19.28645112941486, 46.67912378731485, 50.328378860434306, 29.767460889581265, 89.10424486423499, 19.846687646858875, 11.072417176846095, 59.03570741331474, 18.327531883925772, 68.56064838938651, 64.16414426138152, 84.12167669667048, 96.0784564131317, 50.59803318153092};

        List<InputRecord> inputs = new ArrayList<>();
        for (int i = 0; i < 100; i++) {
            //System.out.println(i);
            tree1.processNewRecord(new InputRecord(i, inputsDouble[i]));

        }

        InputRecord input2 = new InputRecord(4, 4.5);
        tree1.processNewRecord(input2);
        tree1.sortInput(tree1.original);

//        for (InputRecord rec : tree1.original) {
//            System.out.println(rec.toString());
//        }

        Collection<OutputRecord> result = tree1.returnPublishableRecords();
//        for (OutputRecord output : result) {
//            System.out.println(output.toString());
//        }
        HashMap<Double, Integer> map = new HashMap<>();

        for (OutputRecord output : result) {
            map.put(output.getAnonymisedValue(), map.getOrDefault(output.getAnonymisedValue(), 0) + 1);

        }
        for (Double key: map.keySet()) {
            System.out.println("key: " + key + " val: " + map.get(key));
        }
        System.out.println(result.toString());
    }

    @SuppressWarnings("Duplicates")

    static public void testInputOut() {
        Random rand = new Random();

        HanWangSolution tree1 = new HanWangSolution();
        List<Double> inputsDouble = new ArrayList<>();
        for (int i = 0; i < 100; i++) {
            //System.out.println(i);
            inputsDouble.add(rand.nextDouble()*100);

        }
        System.out.println(inputsDouble);
        List<InputRecord> inputs = new ArrayList<>();
        for (int i = 0; i < 100; i++) {
            //System.out.println(i);
            tree1.processNewRecord(new InputRecord(i, (rand.nextInt()%100)));

        }

        InputRecord input2 = new InputRecord(4, 4.5);
        tree1.processNewRecord(input2);
        tree1.sortInput(tree1.original);

//        for (InputRecord rec : tree1.original) {
//            System.out.println(rec.toString());
//        }

        Collection<OutputRecord> result = tree1.returnPublishableRecords();
//        for (OutputRecord output : result) {
//            System.out.println(output.toString());
//        }
        HashMap<Double, Integer> map = new HashMap<>();

        for (OutputRecord output : result) {
            map.put(output.getAnonymisedValue(), map.getOrDefault(output.getAnonymisedValue(), 0) + 1);

        }
        for (Double key: map.keySet()) {
            System.out.println("key: " + key + " val: " + map.get(key));
        }
        System.out.println(result.toString());
    }

    static public void testSortIndicators() {
        HanWangSolution filter = new HanWangSolution();
        filter.indicators.add(constructIndicator(new InputRecord(4, 5)));
        filter.indicators.add(constructIndicator(new InputRecord(4, 4)));
        filter.indicators.add(constructIndicator(new InputRecord(4, 3)));
        filter.indicators.sort(new IndicatorComparator());
        filter.indicators.get(2).addRecord(new InputRecord(3,10));

        for (Indicator indicator: filter.indicators) {
            System.out.println(indicator.sum);
            System.out.println(indicator.getLength());
            System.out.println(indicator.getVal());
        }
        System.out.println(filter.findIndicator(100));
        System.out.println(filter.getGroupedIndicators().toStringAll());




    }
}
