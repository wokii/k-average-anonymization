package io.github.wokii;


public class LinkedBlockNode {


    public LinkedBlockNode(InputRecord val) {
        this.val = val;

        this.top = null;
        this.bot = null;
        this.next = null;
        this.prev = null;

        this.isHead = false;
    }
    public LinkedBlockNode(InputRecord val, boolean isHead) {
        this.val = val;

        this.top = null;
        this.bot = null;
        this.next = null;
        this.prev = null;

        this.isHead = isHead;
        this.length = 0;
    }

    public boolean isHead;
    public InputRecord val;
    public LinkedBlockNode top;
    public LinkedBlockNode bot;
    public LinkedBlockNode next;
    public LinkedBlockNode prev;
    public int length;
    public int getLength() throws Exception{
        if (!isHead) {
            throw new Exception("only head has attribute length");
        }
        return length;
    }



    public static void main(String[] args) {
        LinkedBlockNode first = new LinkedBlockNode(new InputRecord(0, 1), true);
        LinkedBlockNode second = new LinkedBlockNode(new InputRecord(1, 2), true);
        first.bot = second;
        second.top = first;
        LinkedBlockNode f1 = new LinkedBlockNode(new InputRecord(1, 1.1));
        first.next = f1;
        f1.prev = first;

        LinkedBlockNode s1 = new LinkedBlockNode(new InputRecord(1, 2.1));
        second.next = s1;
        s1.prev = second;

        System.out.println(first.val.toString());
        System.out.println(first.next.val.toString());
        System.out.println(first.bot.val.toString());
        System.out.println(first.bot.next.val.toString());




    }
}
