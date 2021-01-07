import java.util.ArrayList;

public class DecisionNode {
    private String attribute;
    private int classID;
    private ArrayList<DecisionNode> children;

    public DecisionNode(){
        attribute = "";
        classID = -1;
        children = null;
    }

    public DecisionNode(String attribute){
        this.attribute = attribute;
        classID = -1;
        children = null;
    }

    public DecisionNode(String attribute, int classID, ArrayList<DecisionNode> children){
        this.attribute = attribute;
        this.children = children;
        this.classID = classID;
    }

    //Getters and Setters
    public void setAttribute(String attribute){this.attribute = attribute;}
    public void setChildren(ArrayList<DecisionNode> children){this.children = children;}
    public void setID(int classID){this.classID = classID;}

    public String getAttribute(){return attribute;}
    public ArrayList<DecisionNode> getChildren(){return children;}
    public int getID(){return classID;}
}
