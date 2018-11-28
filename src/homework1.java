import java.util.ArrayList;
import java.util.Hashtable;
import java.util.Scanner;

// <ORON> TODO: function to calculate the sizeof according to type => fix getsize()

class homework1
{
    // Properties
    private static int m_LableNumber = 0; // Labels counter for the Pcode
    public static boolean recordFlag = false;   // flag to indicate we are inside a record and update offset value
    public static int offsetValue = 0;
    public static boolean rightTreeSide = true;

    public static int CurrentAvailableAddress;
    public static final int TABLE_START_ADDRESS = 5;
    public static int CurrentArrayDimention=1;

    //public static int CountDim =0;
    public static ArrayList<Dimension> GlobalDimList;



    // Classes

    // Abstract Syntax Tree
    static final class AST {
        public final String value;
        public final AST left; // can be null
        public final AST right; // can be null

        private AST(String val,AST left, AST right) {
            value = val;
            this.left = left;
            this.right = right;
        }

        public static AST createAST(Scanner input) {
            if (!input.hasNext())
                return null;

            String value = input.nextLine();
            if (value.equals("~"))
                return null;

            return new AST(value, createAST(input), createAST(input));
        }
    }

    static final class Dimension{
        //Properties
        public int startIndex;
        public int endIndex;
        public int size;
        public int ixa;

        //Methods
        public void SetStartIndex(int p_newStartIndex){startIndex = p_newStartIndex;}
        public void SetEndIndex(int p_newEndIndex){endIndex = p_newEndIndex;}
        public void SetSize(int p_size){size = p_size;}
        public void SetIxa(int p_ixa){ixa = p_ixa;}


    }

    static final class Variable
    {
        // Properties
        public int Size;
        public String Name;
        public String Type;
        public int Address;
        public int Offset; //<ORON> TODO: check if really needed for every variable
        public int Subpart; //<LEEOR_ADDING> TODO: check if really needed for every variable

        public ArrayList<Dimension> dimensionsList;
        public Variable(){dimensionsList=new ArrayList<Dimension>();} // c'tor for Variable


        // Methods
        public int GetSize() { return this.Size;}
        public void SetSize(int p_newSize){Size = p_newSize;}
        public String GetName() { return Name;}
        public void SetName(String p_newName){Name = p_newName;}
        public int GetAddress() { return Address;}
        public void SetAddress(int p_newAddress){Address = p_newAddress;}
        public String GetType()
        {
            return Type;
        }
        public void SetType(String p_newType){Type = p_newType;}
        public void SetOffset(int p_offset) {Offset = p_offset;}    //<ORON>
        public void SetSubpart(int p_subpart){Subpart = p_subpart;}
        public int GetSubpart(){return Subpart;}
    }

    static final class SymbolTable
    {
        // Properties
        public static Hashtable<String, Variable> m_SymbolTable;         /**<LEEOR> TODO: change to Map or Dictionary  <ORON> Why should we change?</>**/
        //public static int CurrentAvailableAddress;            //TODO: this has moved to be a global variable
        //public static final int TABLE_START_ADDRESS = 5;

        // C'tor
        public SymbolTable()
        {
            m_SymbolTable = new Hashtable<>();
            CurrentAvailableAddress = TABLE_START_ADDRESS;
        }

        // Methods
        public static SymbolTable generateSymbolTable(AST tree)
        {
            AST pointer = tree;
            SymbolTable table = new SymbolTable();
            if(pointer.value.equals("program") && pointer.right.left != null) FillSymbolTable(table,pointer.right.left);
            return table;
        }

        private static int GetTypeSize(String type)
        {
            int result = 1;
            switch(type)
            {
                case "int":
                case "real":
                case "bool":
                case "constInt":
                case "constReal":
                case "constBool":
                case "pointer":
                {
                    result = 1;
                    break;
                }

                default:
                {


                    result = (m_SymbolTable.get(type)).GetSize();
                    break;
                }
            }
            return result;
        }

        public static int GetRecordSize(AST p_tree)
        {
            if (p_tree == null)
                return 0;
            int sum = GetRecordSize(p_tree.left);
            if ((p_tree.right != null) && (p_tree.right.value.equals("var")))
                {

                    sum += GetTypeSize(p_tree.right.left.left.value);
                }
            return sum;
        }

        /**
         * FillSymbolTable is a recursive method;
         * The method delivers variables from the AST into the Symbol Table;
         * p_tree: an AST;
         *            Expected to start from Scope branch of the tree;
         *
         * p_symbolTable: an SymbolTable;
         */

        private static void FillSymbolTable(SymbolTable p_symbolTable, AST p_tree)
        {
            if(p_tree == null) {return;}

            if(p_tree.left != null) {FillSymbolTable(p_symbolTable,p_tree.left);} // Check left sub-tree

            if(p_tree.value.equals("var"))
            {

                /**<ORON> TODO: fill code for array:*/

                if ((p_tree.right.value!= null) && (p_tree.right.value.equals("array")))
                {
                    GlobalDimList = new ArrayList<Dimension>();
                    Variable new_variable = new Variable();
                    new_variable.SetName(p_tree.left.left.value);
                    new_variable.SetAddress(CurrentAvailableAddress);
                    if((p_tree.right.right!= null) && (p_tree.right.right.value.equals("identifier")))
                    {
                        new_variable.SetType(p_tree.right.right.left.value);
                    }
                    else {
                        new_variable.SetType(p_tree.right.right.value);
                    }

                    FillSymbolTable(p_symbolTable, p_tree.right.left);

                    new_variable.dimensionsList.addAll(GlobalDimList);
                    int TotalArraySize = 0;
                    for (int i=0; i< new_variable.dimensionsList.size(); i++ )
                    {
                        TotalArraySize += (new_variable.dimensionsList.get(i).size)*GetTypeSize(new_variable.GetType());
                    }
                    CurrentAvailableAddress += TotalArraySize;
                    new_variable.SetSize(TotalArraySize);

                    int TotalIxa = GetTypeSize(new_variable.GetType());
                    for (int i = new_variable.dimensionsList.size()-1; i>=0; i-- )
                    {
                        new_variable.dimensionsList.get(i).SetIxa(TotalIxa);
                        TotalIxa *= new_variable.dimensionsList.get(i).size;
                    }

                    int TotalSubpart = 0;
                    for (int i=0; i< new_variable.dimensionsList.size(); i++ )
                    {
                        TotalSubpart += (new_variable.dimensionsList.get(i).ixa * new_variable.dimensionsList.get(i).startIndex);
                    }
                    new_variable.SetSubpart(TotalSubpart);

                    p_symbolTable.m_SymbolTable.put(new_variable.GetName(),new_variable);

                }

               /**<ORON> TODO: new code here - if record, do... else act normal:*/

               else if ((p_tree.right.value!= null) && (p_tree.right.value.equals("record")))
                {
                    //create the variable for record FIRST, so it will get an address
                    Variable new_variable = new Variable();
                    new_variable.SetType(p_tree.right.value);
                    new_variable.SetAddress(CurrentAvailableAddress);
                    //new_variable.SetSize(1); /** <LEEOR> TODO: CHANGE LATER (AVOID MAGIC NUMBER) **/
                    new_variable.SetName(p_tree.left.left.value);
                    //CurrentAvailableAddress += new_variable.Size;//TODO: DO NOT increase the address size. record does not hold it's own address
                    p_symbolTable.m_SymbolTable.put(new_variable.GetName(),new_variable);

                    if(recordFlag==true)            //TODO:  check this condition in case of offset problem
                    {
                        new_variable.SetOffset(offsetValue);
                        //offsetValue = offsetValue + new_variable.GetSize();
                        offsetValue = 0;
                    }
                    else recordFlag = true;  //recordFlag is the condition to set variable offset. it means we are inside a record
                    FillSymbolTable(p_symbolTable,p_tree.right.left); // Re-enter the fill function. Check left sub-tree...
                    recordFlag = false; //exit "record mode" and reset the offset
                    offsetValue = 0;    //TODO: think about adding self offset condition

                    //System.out.println("TEST:  BEFORE GET SIZE " + new_variable.GetName());
                    //System.out.println("TEST:  BEFORE GET SIZE VALUE " + p_tree.right.left.value);
                    new_variable.SetSize(GetRecordSize(p_tree.right.left));

                    p_symbolTable.m_SymbolTable.put(new_variable.GetName(),new_variable);
                }

                else
                {
                    Variable new_variable = new Variable();
                    new_variable.SetType(p_tree.right.value);
                    new_variable.SetAddress(CurrentAvailableAddress);
                    new_variable.SetSize(GetTypeSize(p_tree.right.value)); /** <LEEOR> TODO: CHANGE LATER (AVOID MAGIC NUMBER) **/
                    new_variable.SetName(p_tree.left.left.value);

                    if(recordFlag==true) {          //<ORON> deal with offset
                        new_variable.SetOffset(offsetValue);
                        offsetValue = offsetValue + new_variable.GetSize();
                    }

                    CurrentAvailableAddress += new_variable.Size;
                    p_symbolTable.m_SymbolTable.put(new_variable.GetName(),new_variable);
                    return;
                }
            }

            else if ((p_tree.value!= null) && (p_tree.value.equals("range")))
            {
                Dimension dim = new Dimension();
                dim.SetStartIndex(Integer.parseInt(p_tree.left.left.value));
                dim.SetEndIndex(Integer.parseInt(p_tree.right.left.value));
                dim.SetSize(dim.endIndex-dim.startIndex+1);
                //TODO fill ixa value
                GlobalDimList.add(dim);
            }

            else if(p_tree.right != null) {FillSymbolTable(p_symbolTable, p_tree.right);} // Check right sub-tree
        }
    }

    // Methods


    private static void generatePCode(AST ast, SymbolTable symbolTable)
    {
        // TODO: go over AST and print code

        //region tests assigned addresses
        /*
        //TODO: this code will be deleted. use it to check variable address. see it in console.
        System.out.println("\n*******************************************");    //TODO: REMOVE THIS. used to check if assigned address is correct
        System.out.println("********      ADDRESS TESTING     *********\n");    //TODO: REMOVE THIS. used to check if assigned address is correct
        System.out.println("Address of f:" + symbolTable.m_SymbolTable.get("f").GetAddress() );    //TODO: REMOVE THIS. used to check if assigned address is correct
        System.out.println("Address of e:" + symbolTable.m_SymbolTable.get("e").GetAddress() );    //TODO: REMOVE THIS. used to check if assigned address is correct
        System.out.println("Address of d:" + symbolTable.m_SymbolTable.get("d").GetAddress() );    //TODO: REMOVE THIS. used to check if assigned address is correct
        System.out.println("Address of c:" + symbolTable.m_SymbolTable.get("c").GetAddress() );    //TODO: REMOVE THIS. used to check if assigned address is correct
        System.out.println("Address of b:" + symbolTable.m_SymbolTable.get("b").GetAddress() );    //TODO: REMOVE THIS. used to check if assigned address is correct
        System.out.println("\n*********  END OF ADDRESS TESTING  ********");    //TODO: REMOVE THIS. used to check if assigned address is correct
        System.out.println("*******************************************\n");    //TODO: REMOVE THIS. used to check if assigned address is correct
        */
        //endregion

        if(ast.right.value.equals("content") && ast.right.right != null)
        {
            MakePcode(ast.right.right,symbolTable);
        }
    }

    private static void CheckIfInd(AST p_tree, int side)
    {
        switch (side)
        {
            case 0: // left
            {
                if(p_tree!=null && p_tree.left != null &&
                           (p_tree.left.value.contains("identifier")
                        || p_tree.left.value.contains("record")
                        || p_tree.left.value.contains("array"))) //<LEEOR_ADDING> also checks if need to use ind for the left
                {
                    System.out.println("ind");
                }
                break;
            }
            case 1: // right
            {
                if(p_tree!=null && p_tree.right != null &&
                        (p_tree.right.value.contains("identifier")
                                || p_tree.right.value.contains("record")
                                || p_tree.right.value.contains("array")))
                {
                    System.out.println("ind");  //TODO: some ind are missing, check again after we finish HW2
                }
                else if(p_tree != null && p_tree.right != null && p_tree.right.value.contains("record"))   //<ORON>
                    CheckIfInd(p_tree.right, 1);
            }

        }

    }

    private static Variable GetArrayName(AST p_tree, SymbolTable p_symbolTable)
    {
        Variable CurrentArray = null;
        if(p_tree.left.value.equals("record"))
        {
            CurrentArray = p_symbolTable.m_SymbolTable.get(p_tree.left.right.left.value);
        }
        else if(p_tree.left.value.equals("identifier"))
        {
            CurrentArray = p_symbolTable.m_SymbolTable.get(p_tree.left.left.value);
        }
        else if(p_tree.left.value.equals("array"))
        {
            CurrentArray = GetArrayName(p_tree.left, p_symbolTable);
            CurrentArray = p_symbolTable.m_SymbolTable.get(CurrentArray.GetType());
        }
        return CurrentArray;
    }

    //<LEEOR_ADDING HandleArrayPcode>
    private static void HandleArrayPcode(String p_arrayName, int p_dimNum, AST p_tree, SymbolTable p_symbolTable)
    {
        if(p_tree == null) {return;}
        HandleArrayPcode(p_arrayName,p_dimNum-1 ,p_tree.left, p_symbolTable);
        if(p_tree.value.equals("indexList"))
        {
          MakePcode(p_tree.right,p_symbolTable);
          int CurrentIxa = p_symbolTable.m_SymbolTable.get(p_arrayName).dimensionsList.get(p_dimNum).ixa;
          System.out.println("ixa " + Integer.toString(CurrentIxa));
        }
    }

    private static void MakePcode(AST p_tree, SymbolTable p_symbolTable)
    {
        if(p_tree == null) {return;}

        String currentValue = (String)p_tree.value;

        //region Handle While statement
        if(currentValue.contains("while"))
        {
            int label_1 = m_LableNumber++;
            int label_2 = m_LableNumber++;

            System.out.println("L" + label_1 + ":");
            MakePcode(p_tree.left,p_symbolTable);
            System.out.println("fjp L" + label_2);

            MakePcode(p_tree.right,p_symbolTable);
            System.out.println("ujp L" + label_1);
            System.out.println("L" + label_2 + ":");
            return;
        }
        //endregion

        //region Generate Left SubTree
        MakePcode(p_tree.left, p_symbolTable);
        switch (currentValue)
        {
            case "plus":
            case "minus":
            case "multiply":
            case "divide":
            case "negative":
            case "not":
            case "or":
            case "and":
            case "if":
                /**<LEEOR> TODO: CHECK IF NEED TO ADD CASE FOR ELSE</LEEOR>**/ //case "else":
            case "equals":
            case "notEquals":
            case "lessThan":
            case "greaterThan":
            case "lessOrEquals":
            case "greaterOrEquals":
            case "print":
            {
                CheckIfInd(p_tree,0); // checks if left sub-tree has identifier
                break;
            }

            default: break;
        }
        //endregion

        //region Handle If statement
        if(currentValue.contains("if") && p_tree.right != null && (p_tree.right.right == null || p_tree.right.left == null)) // if without else condition
        {
            int label = m_LableNumber++;
            System.out.println("fjp L" + label);
            MakePcode(p_tree.right, p_symbolTable);
            System.out.println("L" + label + ":");
            return;
        }
        else if(currentValue.contains("if") && p_tree.right != null && p_tree.right.right != null) // if with else condition
        {
            int label_1 = m_LableNumber++;
            int label_2 = m_LableNumber++;

            System.out.println("fjp L" + label_1);
            MakePcode(p_tree.right.left, p_symbolTable);
            System.out.println("ujp L" + label_2);
            System.out.println("L" + label_1 + ":");
            MakePcode(p_tree.right.right,p_symbolTable);
            System.out.println("L" + label_2 + ":");
            return;
        }

//        if (currentValue.equals("array")) {
//            int address = SymbolTable.m_SymbolTable.get(p_tree.left.left.value).Address;
//            System.out.println("ldc " + address);
//        }
        //endregion

        //region Handle Array <LEEOR_ADDING>

        if(p_tree.value.equals("array"))
        {
            Variable CurrentArray = GetArrayName(p_tree, p_symbolTable);
            if(p_tree.left.value.equals("record"))
            {
                CurrentArray = p_symbolTable.m_SymbolTable.get(p_tree.left.right.left.value);
            }
            else if(p_tree.left.value.equals("identifier"))
            {
                CurrentArray = p_symbolTable.m_SymbolTable.get(p_tree.left.left.value);
            }
            HandleArrayPcode(CurrentArray.GetName(), CurrentArray.dimensionsList.size()-1, p_tree.right, p_symbolTable);
            String ArraySubpart = Integer.toString(CurrentArray.GetSubpart());
            System.out.println("dec " + ArraySubpart);
            return;
        }

        //endregion Handle Array

        //region Generate Right SubTree
        if (p_tree.value.equals("record"))
            rightTreeSide = false;  //This variable fix the unwanted "ldc" print of right hand son
        MakePcode(p_tree.right, p_symbolTable);
        if (p_tree.value.equals("record"))
            rightTreeSide = true;

        switch (currentValue)
        {
            case "plus":
            case "minus":
            case "multiply":
            case "divide":
            case "negative":
            case "not":
            case "or":
            case "and":
            case "if":
            case "equals":
            case "notEquals":
            case "lessThan":
            case "greaterThan":
            case "lessOrEquals":
            case "greaterOrEquals":
            case "print":
            {
                CheckIfInd(p_tree,1); // checks if right sub-tree has identifier
                break;
            }

            default: break;
        }
        //endregion

        //region Handle All post-order statements
        switch(currentValue)
        {
            //unused:
            // Conditions:
            case "else": /**<LEEOR> TODO: think if real condition is needed </LEEOR>**/
                // General:
            case "program":
            case "content":
            case "identifierAndParameters":
            case "inOutParameters":
            case "scope":
            case "declarationsList":
            case "statementsList":
            case "var":
            {
                break;
            }

            case "range":
            {
                Dimension new_dimension = new Dimension();
                new_dimension.SetStartIndex(Integer.parseInt(p_tree.left.left.value));   // Integer.parseInt converts string to int
                new_dimension.SetEndIndex(Integer.parseInt(p_tree.right.left.value));

            }

            //TODO: new code added here for pointer and record
            case "pointer":
            {
                System.out.println("ind"); //TODO: should there be a null check here?  "pointer" is always ind...
                break;
            }

            case "record":
            {
                System.out.println("inc " + (p_symbolTable.m_SymbolTable.get(p_tree.right.left.value)).Offset); //TODO: Add condition for record of records
                break;
            }

            case "identifier":
            {

                if((rightTreeSide) && (p_symbolTable.m_SymbolTable.get(p_tree.left.value) != null))    //This if statement fix the unwanted "ldc" print for right hand son
                    System.out.println("ldc " + p_symbolTable.m_SymbolTable.get(p_tree.left.value).Address);

                break;
            }

            // ops:
            case "plus":
                System.out.println("add");
                break;
            case "minus":
                System.out.println("sub");
                break;
            case "multiply":
                System.out.println("mul");
                break;
            case "divide":
                System.out.println("div");
                break;
            case "negative":
                System.out.println("neg");
                break;
            case "not":
                System.out.println("not");
                break;
            case "or":
                System.out.println("or");
                break;
            case "and":
                System.out.println("and");
                break;

            case "equals":
                System.out.println("equ");
                break;
            case "notEquals":
                System.out.println("neq");
                break;
            case "lessThan":
                System.out.println("les");
                break;
            case "greaterThan":
                System.out.println("grt");
                break;
            case "lessOrEquals":
                System.out.println("leq");
                break;
            case "greaterOrEquals":
                System.out.println("geq");
                break;
            case "assignment":
                if (p_tree.right.value.equals("record")) //<ORON>
                    CheckIfInd(p_tree.right, 1);   //<ORON>
                System.out.println("sto");
                break;
            case "print":
            {
                System.out.println("print");
                break;
            }

            // Boolean Values
            case "true":
            {
                System.out.println("ldc 1");
                break;
            }
            case "false":
            {
                System.out.println("ldc 0");
                break;
            }

            //Types:
            case "int":
            case "real":
            case "bool":
            case "constInt":
            case "constReal":
            case "constBool":
            {
                if (p_tree.left != null) {
                    System.out.println("ldc " + p_tree.left.value);
                }
                break;
            }

            // Other cases:
            default:
            {
                break;
            }
        }
        //endregion
    }

    public static void main(String[] args)
    {
        Scanner scanner = new Scanner(System.in);
        AST ast = AST.createAST(scanner);
        SymbolTable symbolTable = SymbolTable.generateSymbolTable(ast);
        generatePCode(ast, symbolTable);
    }
}
