import java.io.*;
import java.util.*;
public class DecisionMain {
    public static void main(String[] args){
        long startTime = System.nanoTime();

        //Raw data
        ArrayList<List<String>> data = new ArrayList<>();

        /*
        * CHANGE FILE IN THE LINE BELOW. MAKE SURE THE FORMAT FOR THE DATA IS:
        *           [outcome] | [class1] | [class2] | ... | [class m]
        * */
        File file = new File("mushrooms.data");
        try {
            Scanner scanner = new Scanner(file);
            while(scanner.hasNext()){
                List<String> elementData = new ArrayList<>(Arrays.asList(scanner.next().split(",")));
                data.add(elementData);
            }
        }catch(FileNotFoundException ex){System.out.println("Error: File not Found");}

        //Since it is possible for data to have attributes with the same names, make them unique
        uniqueAttr(data);

        ArrayList<List<String>> testData = splitData(data);                           //This will split the input data. 70% training, 30% testing, data now holds the training data
        ArrayList<List<Pair>> attributeList = getAttributeList(data);               //This will get the attribute list of the input data

        DecisionTree a = new DecisionTree();
        a.generateTree(a.getRoot(), data, attributeList);
        try{
            PrintStream os = new PrintStream("output.txt");
            a.print(os);
        }
        catch(Exception e){}

        //Insert data that was not used to create the tree to navigate the tree.
        for(int i = 0; i < testData.size(); i++){
            a.testTree(a.getRoot(), testData.get(i));
        }

        if(testData.size() == a.getCounter()){
            System.out.println("@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@");
            System.out.println("@@@@@@@@@ EVERYTHING WORKS @@@@@@@@@");
            System.out.println("@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@");
        }

        System.out.println("Test Data Size: " +testData.size());
        System.out.println("Correct matches: " +a.getCounter());
        System.out.println("Number of Mismatches: " +(testData.size()-a.getCounter()));


        //PRINT OUT PROGRAM INFORMATION
        long endTime = System.nanoTime();
        long duration = (endTime - startTime)/1000000;
        System.out.println("\n====================== Program Information ===========================");
        System.out.println("Memory usage: " + (((double)Runtime.getRuntime().totalMemory() - (double)Runtime.getRuntime().freeMemory())/(double)(1024*1024)) + "MB");
        System.out.println("Execution Time: " +duration +"ms");
    }

    /****************************************************************************************************************
     *                                                                                                              *
     *                                                                                                              *
     *                                         !!  HELPER FUNCTIONS  !!                                             *
     *                                                                                                              *
     *                                                                                                              *
     ****************************************************************************************************************/

    /**
     * Splits the raw data into training data and testing data. 30% of the raw data will be used for training
     * and 70% will be used for test.
     *
     * @param data The raw data.
     * @return Returns the test data set.
     */
    public static ArrayList<List<String>> splitData(ArrayList<List<String>> data){
        ArrayList<List<String>> testData = new ArrayList<>();

        for(int i = 0; i < data.size(); i++){
            if(Math.random() < 0.3){
                testData.add(data.get(i));
                data.remove(i);
            }
        }
        return testData;
    }

    /**
     * Splits an attribute depending on how many end up with a particular outcome.
     *
     * Format:
     *      Attribute | Outcome 1 | Outcome 2 | ...[As many outcomes]... | Attribute Class ID
     *
     * @param data Training data for the decision tree.
     * @param outcomeTotal ArrayList with all the possible outcomes.
     * @return Returns a 2D ArrayList that contains the attribute along with the count of each outcome occurred with that attribute.
     */
    public static ArrayList<List<String>> getAttrOutCount(ArrayList<List<String>> data, ArrayList<Pair> outcomeTotal){
        ArrayList<List<String>> outcomeCount = new ArrayList<>();

        ArrayList<String> outcomes = new ArrayList<>();
        for(int i = 0; i < outcomeTotal.size(); i++){
            outcomes.add(outcomeTotal.get(i).getKey());
        }

        //Loop through data arraylist
        for(int i = 1; i < data.get(0).size(); i++){            //iterate through columns
            for(int j = 0; j < data.size(); j++){               //iterate through rows
                boolean foundMatching = false;

                for(int k = 0; k < outcomeCount.size(); k++){    //iterate through outcomeCount arrayList
                    if(outcomeCount.get(k).get(0).equals(data.get(j).get(i))){       //Check if attribute already exists in the list (index 0 is the name of the index)
                        foundMatching = true;                                       //if attribute was found, mark as found
                        int outcomeIndex = outcomes.indexOf(data.get(j).get(0));      //find the index of the outcome for the existing attribute
                        int countTemp = Integer.parseInt(outcomeCount.get(k).get(outcomeIndex+1)) + 1;

                        outcomeCount.get(k).set(outcomeIndex+1, String.valueOf(countTemp));
                    }
                }

                if(!foundMatching){
                    int outcomeIndex = outcomes.indexOf(data.get(j).get(0));  //Figure out which outcome corresponds to the attribute
                    List<String> temp = new ArrayList<>();
                    temp.add(data.get(j).get(i));                           //Add attribute that did not exist to a temp list
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
    public static ArrayList<Pair> getOutcomeList(ArrayList<List<String>> data){
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

        System.out.println("OUTCOME LIST: ");
        for(int i = 0; i < outcomeList.size(); i++){
            System.out.println(outcomeList.get(i).getKey() + "," +outcomeList.get(i).getValue());
        }
        System.out.println();

        return outcomeList;
    }

    /**
     * Renames each element by appending the index number to the element name. This is simply to distinguish every
     * attribute to ensure that they are all unique.
     *
     * @param data Returns modified dataset with unique attribute names.
     */
    public static void uniqueAttr(ArrayList<List<String>> data){
            for(int i = 0; i < data.size(); i++){
                for(int j = 0; j < data.get(i).size(); j++){
                    data.get(i).set(j, data.get(i).get(j) + j);
                }
            }
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

        System.out.println();
        return temp;
    }

    /**
     * Prints out a 2D ArrayList of Strings.
     *
     * @param data 2D ArrayList to be printed
     */
    public static void print2DStringList(ArrayList<List<String>> data){
        for (List<String> datum : data) {
            System.out.println(datum);
        }
    }
}
