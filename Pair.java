public class Pair {
    private String key = "";
    private int value;
    private double doubleVal;

    public Pair(){
        key = "";
        value = 0;
        doubleVal = 0;
    }

    public Pair(String key, int value){
        this.key = key;
        this.value = value;
    }

    public Pair(String key, double value){
        this.key = key;
        this.doubleVal = value;
    }

    //Setters and Getters
    public void setKey(String key){this.key = key;}
    public void setValue(int value){this.value = value;}
    public void setDoubleVal(double value){this.doubleVal = value;}


    public String getKey(){return key;}
    public int getValue(){return value;}
    public double getDoubleVal(){return doubleVal;}

}
