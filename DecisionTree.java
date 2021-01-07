import java.io.PrintStream;
import java.util.*;

public class DecisionTree {
    private ArrayList<DecisionNode> root;
    public static int counter = 0;

    public DecisionTree(){
        this.root = null;
    }

    public ArrayList<DecisionNode> generateTree(ArrayList<DecisionNode> current, ArrayList<List<String>> data, ArrayList<List<Pair>> attributeList){

        if(!attributeList.isEmpty()){       //When attribute list is not empty, theres still things to do.
            if(attributeList.size() == 1){  //if theres only one attribute class left in the attribute list
                ArrayList<Pair> outcomeList = getOutcomeList(data);                         //new data, so have to recalculate outcomeList (new count)
                int index = getLargestIndex(outcomeList);                                   //Leaf node is whatever is the highest in occurrence (majority scenario)

                current.add(new DecisionNode(outcomeList.get(index).getKey(), 0, null));

                return current;
            }

            if(current == null){            //Each pass will fill an arrayList, check if the current node is null
                ArrayList<Pair> outcomeList = getOutcomeList(data);

                if(outcomeList.size() == 1){        //if there is only one outcome left in the list, it becomes a leaf
                    current = new ArrayList<>();
                    current.add(new DecisionNode(outcomeList.get(0).getKey(), 0, null));

                    return current;
                }


                //Performing calculations on given data
                ArrayList<List<String>> attrOutCount = getAttrOutCount(data, outcomeList);
                ArrayList<Pair> gain = getGain(attrOutCount, outcomeList);         //Gets Gain of all Attribute Classes
                Pair newNode = findLargestPair(gain);                              //Finds highest gain
                int classID = Integer.parseInt(newNode.getKey());                  //Gets Class ID of highest gain
                int index = -1;

                //Searches in Attribute List for Class with Highest gain (may not be the same after first iteration)
                for(int i = 0; i < attributeList.size(); i++){
                    if(attributeList.get(i).get(0).getValue() == classID){
                        index = i;
                        break;
                    }
                }

                current = new ArrayList<>();
                //The Class with the highest gain is set as a node in the tree
                for(int i = 0; i < attributeList.get(index).size(); i++){
                    current.add(new DecisionNode(attributeList.get(index).get(i).getKey(), classID, null));
                }

                if(root == null){       //Hot fix, tree is generated but not attached to root so data is lost, first iteration will solve this after this statement
                    root = current;
                }

                //Remove the attribute with highest gain from attribute list
                attributeList.remove(index);

                //Clone ArrayList to be used further down the tree
                ArrayList<List<Pair>> newAttributeList = cloneArrayPair(attributeList);

                for(int i = 0; i < current.size(); i++){
                    //Remove the class that is begin used to split data
                    ArrayList<List<String>> extractedData = extractAttribute(data, current.get(i).getAttribute(), current.get(i).getID());

                    if(extractedData.size() == 0){      //if all the data was extracted, then find the outcome with the largest value and change the current node to an outcome
                        index = getLargestIndex(outcomeList);
                        current.set(i, new DecisionNode(outcomeList.get(index).getKey(), 0, null));
                        continue;   //Skip current node (since its a leaf)
                    }

                    //Recursively adding nodes
                    current.get(i).setChildren(generateTree(current.get(i).getChildren(), extractedData, newAttributeList));
                }
            }
        }

        return current;
    }

    public void testTree(ArrayList<DecisionNode> current, List<String> testData){
        if(current == null){
            return;
        }

        if(current.get(0).getAttribute().equals(testData.get(0))){
            counter++;
            return;
        }

        //Get the id of the current node, this will double as the index for the attribute in the test data
        int classID = current.get(0).getID();
        String attr = testData.get(classID);
        int index = -1;

        //Find the index of the attribute in the current node
        for(int i = 0; i < current.size(); i++){
            if(current.get(i).getAttribute().equals(attr)){         //If the attribute was found in the current node
                index = i;
                break;
            }
        }

        if(index == -1){
            System.out.println("MISMATCH FOUND");
            System.out.println("Attribute: " +attr);
            System.out.print("Current: ");
            for(int i = 0; i < current.size()-1; i++){
                System.out.print(current.get(i).getAttribute() +", ");
            }
            System.out.println(current.get(current.size()-1).getAttribute() +"\n");
            return;
        }

        testTree(current.get(index).getChildren(), testData);
    }

    /**
     * Gets the gain of the data. This is done by:
     *      Gain(Class) = Entropy(All) - Info(Attribute)
     *
     * @param attrOutCount 2D ArrayList that holds the information for each attribute on the total number of outcome occurrences and its entropy
     * @param outcomeList ArrayList of Pairs that holds all possible outcomes as well as its total count over the entire training dataset
     * @return Returns an ArrayList of doubles that contain the gain of every Class. The index corresponds to the Class ID - 1.
     */
    public ArrayList<Pair> getGain(ArrayList<List<String>> attrOutCount, ArrayList<Pair> outcomeList){
        getClassEntropy(attrOutCount);
        ArrayList<Pair> gain = getInfo(attrOutCount, outcomeList);
        double trainingEntropy = getTotalEntropy(outcomeList);

        for(int i = 0; i < gain.size(); i++){
            gain.set(i, new Pair(gain.get(i).getKey(), trainingEntropy - gain.get(i).getDoubleVal()));
        }

        return gain;
    }

    /**
     * Gets the total info that each Attribute Class contains. This is done by performing:
     *      Info(Class) = sum(a/b) * Entropy(Attribute))
     *
     *      a = total number of outcome occurrences in a specific attribute
     *      b = total number of outcome occurrence in the overall data
     *      Entropy(Attribute) = function to calculate the entropy of a specific attribute
     *
     * This will also modify the attrOutCount 2D ArrayList and reformat it into the following:
     *      [Attribute Name] | [Outcome 1] | [Outcome 2] | ... | Class ID
     *
     * @param attrOutCount 2D ArrayList that holds the information for each attribute on the total number of outcome occurrences and its entropy
     * @param outcomeList ArrayList of Pairs that holds all possible outcomes as well as its total count over the entire training dataset
     * @return Returns an ArrayList of Doubles that holds the info of each Class.
     */
    public ArrayList<Pair> getInfo(ArrayList<List<String>> attrOutCount, ArrayList<Pair> outcomeList){
        ArrayList<Pair> info = new ArrayList<>();
        ArrayList<Double> coeffs = new ArrayList<>();
        int totalOutcomes = 0;

        //Finds the total number of outcomes that occurs over the training data.
        for(int i = 0; i < outcomeList.size(); i++){
            totalOutcomes += outcomeList.get(i).getValue();
        }

        //Finds the total number of outcomes that occur with a single attribute and then divides that by the overall
        //number of outcomes. This creates the coefficient to be used when calculating info.
        for(int i = 0; i < attrOutCount.size(); i++){
            int totAttrOutcomes = 0;
            for(int j = 1; j < attrOutCount.get(i).size()-2; j++){
                totAttrOutcomes += Integer.parseInt(attrOutCount.get(i).get(j));
            }

            coeffs.add((double)totAttrOutcomes/(double)totalOutcomes);
        }

        for(int i = 0; i < attrOutCount.size(); i++){

            //The data is organized such that class ID is in the 2nd to last pos.
            //In addition to this, the class ID starts from 1
            int index = Integer.parseInt(attrOutCount.get(i).get(attrOutCount.get(i).size()-2));       //Attribute class ID repurposed as index value

            double temp = Double.parseDouble(attrOutCount.get(i).get(attrOutCount.get(i).size()-1));   //Save entropy value to temp variable
            attrOutCount.get(i).remove(attrOutCount.get(i).size()-1);                            //Remove entropy from last position, this will be recalculated further down the tree
            if(info.size() < index){        //The Attribute Class ID will always be > the size of the info ArrayList if that class ID hasn't been computed yet

                //If we're on a new Attribute class, add to info.
                info.add(new Pair(String.valueOf(index), coeffs.get(i)*temp));
            }

            //If info has already been on the same Attribute Class ID, then simply add on the weight adjusted entropy.
            else{
                double infoTemp = info.get(index-1).getDoubleVal() + (coeffs.get(i)*temp);
                info.set(index-1, new Pair(String.valueOf(index), infoTemp));
            }
        }

        return info;
    }

    /**
     * Gets the entropy of an individual attribute. The information is placed back into the 2D ArrayList that is passed to it.
     * The information will be formatted as the following:
     *          [Attribute Name] | [Outcome1] | [Outcome2] | ... | [Class ID] | [Attribute Entropy]
     *
     * @param attrOutCount A 2D ArrayList of Strings that contains the attribute name along with of count of the number of each occurrence and its Class ID.
     */
    public void getClassEntropy(ArrayList<List<String>> attrOutCount){

        for(int i = 0; i < attrOutCount.size(); i++){
            int totalOutcomes = 0;
            ArrayList<Integer> individualOutcomeTotals = new ArrayList<>();
            double entropy = 0.0;
            //Data is formatted in the following fashion:
            //      [Attribute Name] | ...Outcomes... | Class ID
            //The result of attrOutCount will become as follows once this method is complete;
            //      [Attribute Name] | ...Outcomes... | Class ID | entropy
            //We're only interested in the outcomes.
            for(int j = 1; j < attrOutCount.get(i).size()-1; j++){
                totalOutcomes += Integer.parseInt(attrOutCount.get(i).get(j));
                individualOutcomeTotals.add(Integer.parseInt(attrOutCount.get(i).get(j)));
            }

            for(int j = 0; j < individualOutcomeTotals.size(); j++){
                String temp = String.valueOf(individualOutcomeTotals.get(j));
                double tempDouble = Double.parseDouble(temp);

                if(tempDouble == 0.0){      //if tempDouble (the numerator) is 0, this will cause issues as log2 will return -infinity
                    entropy -= 0;
                }
                else{
                    entropy -= (tempDouble/ (double) totalOutcomes) * log2((tempDouble/((double)totalOutcomes)));
                }
            }

            attrOutCount.get(i).add(String.valueOf(entropy));
        }
    }

    /**
     * Gets the total entropy of the entire dataset.
     *
     * @param outcomeList An arraylist that contains all the possible outcomes along with the total count.
     * @return Returns the entropy of the dataset.
     */
    public double getTotalEntropy(ArrayList<Pair> outcomeList){
        int count = 0;
        double entropy = 0.0;
        for(int i = 0; i < outcomeList.size(); i++){
            count += outcomeList.get(i).getValue();
        }

        for(int i = 0; i < outcomeList.size(); i++){
            double ratio = (double)outcomeList.get(i).getValue()/(double) count;
            entropy -= ratio * log2(ratio);
        }

        return entropy;
    }

    /**
     * Splits an attribute depending on how many end up with a particular outcome.
     *
     * Format:
     *      Attribute | Outcome 1 | Outcome 2 | ...[As many outcomes]... | Attribute Class ID
     *
     * @param data Training data for the decision tree.
     * @param outcomeList ArrayList with all the possible outcomes.
     * @return Returns a 2D ArrayList that contains the attribute along with the count of each outcome occurred with that attribute.
     */
    public ArrayList<List<String>> getAttrOutCount(ArrayList<List<String>> data, ArrayList<Pair> outcomeList){
        ArrayList<List<String>> outcomeCount = new ArrayList<>();

        ArrayList<String> outcomes = new ArrayList<>();
        for(int i = 0; i < outcomeList.size(); i++){
            outcomes.add(outcomeList.get(i).getKey());
        }

        //Loop through data arraylist
        for(int i = 1; i < data.get(0).size(); i++){            //iterate through columns
            for(int j = 0; j < data.size(); j++){               //iterate through rows
                boolean foundMatching = false;

                for(int k = 0; k < outcomeCount.size(); k++){                                       //iterate through outcomeCount arrayList
                    if(outcomeCount.get(k).get(0).equals(data.get(j).get(i))){                      //Check if attribute already exists in the list (index 0 is the name of the index)
                        foundMatching = true;                                                       //if attribute was found, mark as found
                        int outcomeIndex = outcomes.indexOf(data.get(j).get(0));                    //find the index of the outcome for the existing attribute
                        int countTemp = Integer.parseInt(outcomeCount.get(k).get(outcomeIndex+1)) + 1;

                        outcomeCount.get(k).set(outcomeIndex+1, String.valueOf(countTemp));
                    }
                }

                if(!foundMatching){
                    int outcomeIndex = outcomes.indexOf(data.get(j).get(0));  //Figure out which outcome corresponds to the attribute
                    List<String> temp = new ArrayList<>();
                    temp.add(data.get(j).get(i));                            //Add attribute that did not exist to a temp list
                    for(int k = 0; k < outcomes.size(); k++){                //Create the formatted attribute list data
                        if(k == outcomeIndex){
                            temp.add("1");                                  //Set count of the outcome index to 1
                        }
                        else{
                            temp.add("0");                                  //outcome of the outcomes will be set to 0.
                        }
                    }
                    temp.add(String.valueOf(i));
                    outcomeCount.add(temp);                                  //Add formatted data to outcomeCount list
                }


            }
        }
        return outcomeCount;
    }

    /**
     * Finds all the possible outcomes in the data. This assumes that the outcomes the user is looking for is placed
     * in index 0 of each row.
     *
     * @param data Training data for the system.
     * @return Returns an ArrayList of Pairs that hold all possible outcomes along with its total count.
     */
    public ArrayList<Pair> getOutcomeList(ArrayList<List<String>> data){
        ArrayList<Pair> outcomeList = new ArrayList<>();

        for(int i = 0; i < data.size(); i++) {
            boolean foundMatching = false;
            for(int j = 0; j < outcomeList.size(); j++){
                if(outcomeList.get(j).getKey().equals(data.get(i).get(0))){
                    foundMatching = true;
                    int temp = outcomeList.get(j).getValue() + 1;
                    outcomeList.get(j).setValue(temp);
                    break;
                }
            }

            if(!foundMatching){
                outcomeList.add(new Pair(data.get(i).get(0), 1));
            }
        }
        return outcomeList;
    }

    /**
     * Creates a 2D ArrayList that holds all the unique attributes in a particular dimension (Class)
     *
     * @param data 2D ArrayList that holds String elements
     * @return Returns a jagged 2D ArrayList that holds all unique elements that appear in each column.
     */
    public static ArrayList<List<Pair>> getAttributeList(ArrayList<List<String>> data){
        ArrayList<List<String>> elements = new ArrayList<>();

        for(int attribute = 0; attribute < data.get(attribute).size(); attribute++){ //Column of 2D ArrayList
            ArrayList<String> newAttributes = new ArrayList<>();

            //Row of 2D ArrayList
            for(int dataRow = 0; dataRow < data.size(); dataRow++) {

                //If a unique element is found, put the unique element into the row and increment respective count
                if(newAttributes.isEmpty() || !newAttributes.contains(data.get(dataRow).get(attribute))){
                    newAttributes.add(data.get(dataRow).get(attribute));
                }
            }

            elements.add(newAttributes);
        }

        ArrayList<List<Pair>> temp = new ArrayList<>();
        for(int i = 0; i < elements.size(); i++){
            List<Pair> tempPair = new ArrayList<>();
            for(int j = 0; j < elements.get(i).size(); j++){
                tempPair.add(new Pair(elements.get(i).get(j), i));
            }
            temp.add(tempPair);
        }

        System.out.println("GENERATING ATTRIBUTE LIST:");

        for(int i = 0; i < temp.size(); i++){
            for(int j = 0; j < temp.get(i).size()-1; j++){
                System.out.print(temp.get(i).get(j).getKey() + ", ");
            }
            System.out.println(temp.get(i).get(temp.get(i).size()-1).getKey());
        }

        return temp;
    }

    /**
     * Extracts an entire attribute out of the given data based on the provided attribute name and class ID
     *
     *
     * @param data 2D ArrayList that contains the data will be used to extract a certain attribute
     * @param attributeName The attribute to be extracted
     * @param classID The class the attribute falls in
     * @return Returns the extracted data as a 2D ArrayList
     */
    public ArrayList<List<String>> extractAttribute(ArrayList<List<String>> data, String attributeName, int classID){
        ArrayList<List<String>> extractedClass = new ArrayList<>();
        for(int i = 0; i < data.size(); i++){
            //If we find the matching class and matching attribute to extract, then add it to the extracted class.
            if(data.get(i).get(classID).equals(attributeName)){
                List<String> temp = new ArrayList<>(/*data.get(i)*/);

                for(int j = 0; j < data.get(i).size(); j++){
                    temp.add(data.get(i).get(j));
                }

                extractedClass.add(temp);
            }
        }

        return extractedClass;
    }

    //This method might be unnecessary
    /**
     * Removes an attribute from the attribute output count 2D ArrayList given a Class ID and readjusts the
     * total outcome count.
     *
     * @param attrOutCount ArrayList containing information on each attribute and their count
     * @param outcomeList ArrayList of pairs containing the total count of each possible outcome
     * @param classID The Class ID to be removed from the attrOutCount 2D ArrayList
     */
    public void removeClass(ArrayList<List<String>> attrOutCount, ArrayList<Pair> outcomeList, int classID){
        ArrayList<Integer> outcomeCount = new ArrayList<>();
        for(int i = 0; i < outcomeList.size(); i++){            //Initializes ArrayList
            outcomeCount.add(0);
        }
        //Loop through the attribute outcome count to find the matching ID
        for(int i = 0; i < attrOutCount.size(); i++){
            if(Integer.parseInt(attrOutCount.get(i).get(attrOutCount.size()-1)) == classID){    //If they match, remove
                for(int j = 1; j < attrOutCount.get(i).size()-1; j++){                          //Sum up all the removed outcome counts
                    outcomeCount.set(j-1, outcomeCount.get(j-1) + Integer.parseInt(attrOutCount.get(i).get(j)));
                }
                attrOutCount.remove(i);
            }
        }

        //Re-adjust the outcomeList's total outcome counts for calculations later down the tree.
        for(int i = 0; i < outcomeList.size(); i++){
            outcomeList.get(i).setValue(outcomeList.get(i).getValue() - outcomeCount.get(i));
        }
    }

    /**
     * Finds the largest double value in an ArrayList of Pairs
     *      Pair is formatted as:
     *          Key       = Attribute Class
     *          DoubleVal = Gain
     *
     * @param gain ArrayList to be processed
     * @return Returns a Pair with the largest double value.
     */
    public Pair findLargestPair(ArrayList<Pair> gain){
        Pair largest = new Pair(gain.get(0).getKey(), gain.get(0).getDoubleVal());

        for(int i = 1; i < gain.size(); i++){
            if(gain.get(i).getDoubleVal() > largest.getDoubleVal()){
                largest = new Pair(gain.get(i).getKey(), gain.get(i).getDoubleVal());
            }
        }

        return largest;
    }

    public int getLargestIndex(ArrayList<Pair> list){
        int largest = -1;
        int index = -1;
        for(int i = 0; i < list.size(); i++){
            if(list.get(i).getValue() > largest){
                largest = list.get(i).getValue();
                index = i;
            }
        }

        return index;
    }

    /**
     * Clones a 2D ArrayList of pairs
     *
     * @param list 2D ArrayList to be cloned
     * @return Returns a clone of the passed 2D ArrayList
     */
    public ArrayList<List<Pair>> cloneArrayPair(ArrayList<List<Pair>> list){
        ArrayList<List<Pair>> newList = new ArrayList<>();
        for(int i = 0; i < list.size(); i++){
            List<Pair> tempList = new ArrayList<>();
            for(int j = 0; j < list.get(i).size(); j++){
                tempList.add(new Pair(list.get(i).get(j).getKey(), list.get(i).get(j).getValue()));
            }
            newList.add(tempList);
        }

        return newList;
    }

    /**
     * Computes log base 2 of a double
     *
     * @param a Double value to perform log2 on.
     * @return Returns the log base 2 of a.
     */
    public double log2(double a){
        return Math.log(a)/Math.log(2);
    }

    /**
     * Getter. Gets Root node.
     *
     * @return Returns root node.
     */
    public ArrayList<DecisionNode> getRoot(){
        return root;
    }

    public int getCounter(){
        return counter;
    }



//Code from FP Tree


    /**
     * Traverses the tree and writes out its path
     *
     * @param current The current node that the traversal starts on. To print the whole tree start at root.
     * @param sb StringBuilder to help write out path
     * @param padding Padding for formating the tree
     * @param pointer Pointer for formating the tree
     */
    public void traverse(ArrayList<DecisionNode> current, StringBuilder sb, String padding, String pointer){
        //If current node is not null
        if(current != null){

            //Loop through current's ArrayList and traverse through each of its children
            for(int i = 0; i < current.size(); i++){
                sb.append(padding);

                //If the current node that we're on in the ArrayList is the last in the List, then change the
                //pointer string to represent a leaf node
                if(i == current.size()-1){
                    pointer = "└──";
                }
                sb.append(pointer);
                sb.append(current.get(i).getAttribute());
                sb.append("\n");
                //If it is not the last iteration, print a line
                String pad = padding + ((i+1 != current.size()) ? "│  " : "   ");
                String point;

                //If the current node's children is not null
                if(current.get(i).getChildren() != null){
                    //Check if that child's size is greater than 1. If it is, it isn't then apply the pointer that
                    //indicates additional leaves
                    if(current.get(i).getChildren().size() > 1){
                        point = "├──";
                    }
                    else{
                        point = "└──";
                    }
                }

                //If node doesn't have children, it is a leaf node.
                else{
                    point = "└──";
                }

                traverse(current.get(i).getChildren(), sb, pad, point);
            }

        }
    }

    /**
     * Prints out the tree to a file.
     *
     * @param os OutputStream to a file
     */
    public void print(PrintStream os){
        StringBuilder sb = new StringBuilder();
        traverse(root, sb, "", "");
        os.print(sb.toString());
    }

}
