import java.util.ArrayList;
import java.util.Hashtable;
import java.util.Scanner;

class homework1
{
    // Properties
    private static int m_LableNumber = 0; // Labels counter for the PCode
    public static int offsetValue = 0;
    public static boolean rightTreeSide = true;
    public static int m_SwitchNumber = -1; // starts from -1 since every switch we start with ++ and ending it with -- operators
    public static int CurrentAvailableAddress;
    public static final int TABLE_START_ADDRESS = 5;
    public static ArrayList<Dimension> GlobalDimList;

    //public static int CurrentArrayDimention=1;
    //public static boolean recordFlag = false;   // flag to indicate we are inside a record and update offset value
    //public static int CountDim =0;

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
        public int Offset;
        public int Subpart;
        public String PointerOf;
        public ArrayList<Dimension> dimensionsList;
        public String FunctionName;
        public int SSP; // for functions

        // C'tor
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
        public String GetPointerOf(){return PointerOf;}
        public void SetPointerOf(String pointerTo){PointerOf = pointerTo;}
        public String GetFunctionName(){return FunctionName;}
        public void SetFunctionName(String newFuncName){FunctionName = newFuncName;}
        public void SetSSP(int newSSP){SSP = newSSP;}
        public int GetSSP(){return SSP;}
    }

    static final class SymbolTable
    {
        // Properties
        public static Hashtable<String, Variable> m_SymbolTable;
        public static Hashtable<String, String> m_FunctionTable; // saves every function and its father

        // C'tor
        public SymbolTable()
        {
            m_SymbolTable = new Hashtable<>();
            m_FunctionTable = new Hashtable<>();
            CurrentAvailableAddress = TABLE_START_ADDRESS;
        }

        // Methods
        public static SymbolTable generateSymbolTable(AST tree)
        {
            AST pointer = tree;
            SymbolTable table = new SymbolTable();
            FillFunctionDetails(table,tree,"");

//            if(pointer.value.equals("program") && pointer.right.left != null)
//            {
//                FillSymbolTable(table,pointer.right.left);
//                FillFunctionDetails(table,tree,null);
//            }

            return table;
        }

        /**
         * GetTypeSize method is used to define the total memory-size of a variable;
         * The method gets an name of a variable/a name of a value-type (String),
         * and return it's size.
         * if the given parameter is a known variable, the method ask the Symbol Table the size of the variable.
         *
         * p_symbolTable: an SymbolTable;
         */
        private static int GetTypeSize(String type)
        {
            int result;
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
                    if(m_SymbolTable.containsKey(type))
                    {result = (m_SymbolTable.get(type)).GetSize();}
                    else
                    {
                        result = 1;
                    }
                    break;
                }
            }
            return result;
        }

        /**
         * GetRecordSize method is used to define the total  memory-size of a record;
         * The method gets an AST holds an node of a record,
         * then runs over all it's direct variables and return their total size.
         *
         * p_tree: an AST;
         * p_symbolTable: an SymbolTable;
         */
        public static int GetRecordSize(AST p_tree)
        {
            if (p_tree == null)
                return 0;
            int sum = GetRecordSize(p_tree.left);
            if ((p_tree.right != null) && (p_tree.right.value.equals("var") || p_tree.right.value.equals("byValue"))) //<LEEOR>TODO: Check if that is enough to habdle "byValue" cases
            {

                sum += GetTypeSize(p_tree.right.left.left.value);
            }
            return sum;
        }

        /**
         * SetAllOffestsInRecord is a recursive method;
         * The method gets an sub-tree of an record, and define the offset value for any direct member of this record;
         * p_tree: an AST;
         *            Expected to start from 'DeclerationList' node under a 'record' node;
         *
         * p_symbolTable: an SymbolTable;
         */
        public static void SetAllOffestsInRecord(SymbolTable p_symbolTable, AST p_tree)
        {
            if(p_tree==null) return;
            if(p_tree.left!=null) // not the first variable in the record
            {
                SetAllOffestsInRecord(p_symbolTable,p_tree.left); // go to the next variable
            }
            else if(p_tree.left==null) // the last variable in the record
            {
                if(p_tree.right!=null && (p_tree.right.value.equals("var") || p_tree.right.value.equals("byValue"))) // <LEEOR> TODO: is "byValue" correct
                {

                    if(p_tree.right.left!=null && p_tree.right.left.right!=null)
                    {
                        p_symbolTable.m_SymbolTable.get(p_tree.right.left.left.value).SetOffset(offsetValue); // offsetValue should be zero
                    }
                }
                return;
            }
            if(p_tree.right!=null && (p_tree.right.value.equals("var") || p_tree.right.value.equals("byValue"))) // not the first <LEEOR> TODO: is "byvalue" correct?
            {

                if(p_tree.left.right!=null && p_tree.left.right.value.equals("var"))
                {
                    int prevSize = p_symbolTable.m_SymbolTable.get(p_tree.left.right.left.left.value).GetSize();
                    offsetValue += prevSize;
                    p_symbolTable.m_SymbolTable.get(p_tree.right.left.left.value).SetOffset(offsetValue);

                }
            }
            return;
        }

        /**
         * FillSymbolTable is a recursive method;
         * The method delivers variables from the AST into the Symbol Table;
         * p_tree: an AST;
         *            Expected to start from Scope branch of the tree;
         *
         * p_symbolTable: an SymbolTable;
         */
        private static void FillSymbolTable(SymbolTable p_symbolTable, AST p_tree, String p_FunctionName)
        {
            if(p_tree == null) {return;}

            if(p_tree.left != null){
                FillSymbolTable(p_symbolTable,p_tree.left, p_FunctionName); // Check left sub-tree
            }

            if((p_tree.value.equals("var") || p_tree.value.equals("byValue"))) // <LEEOR> TODO: is "byValue" correct?
            {

                if ((p_tree.right.value!= null) && (p_tree.right.value.equals("array")))
                {
                    GlobalDimList = new ArrayList<Dimension>();
                    Variable new_variable = new Variable();
                    new_variable.SetFunctionName(p_FunctionName);
                    new_variable.SetName(p_tree.left.left.value);
                    new_variable.SetAddress(CurrentAvailableAddress);
                    if((p_tree.right.right!= null) && (p_tree.right.right.value.equals("identifier")))
                    {
                        new_variable.SetType(p_tree.right.right.left.value);
                    }
                    else {
                        new_variable.SetType(p_tree.right.right.value);
                    }

                    FillSymbolTable(p_symbolTable, p_tree.right.left, p_FunctionName);

                    new_variable.dimensionsList.addAll(GlobalDimList);
                    int TotalArraySize = 1;
                    for (int i=0; i< new_variable.dimensionsList.size(); i++ )
                    {
                        TotalArraySize *= (new_variable.dimensionsList.get(i).size);
                    }
                    TotalArraySize *= GetTypeSize(new_variable.GetType());

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
                    System.out.println(">>>>>>new var:\tname=" +new_variable.GetName() + "\tsize=" +Integer.toString(new_variable.GetSize()));  //<LEEOR> TEST


                }
                else if ((p_tree.right.value!= null) && (p_tree.right.value.equals("record")))
                {
                    //create the variable for record FIRST, so it will get an address.
                    Variable new_variable = new Variable();
                    new_variable.SetFunctionName(p_FunctionName);
                    new_variable.SetType(p_tree.right.value);
                    new_variable.SetAddress(CurrentAvailableAddress);
                    new_variable.SetName(p_tree.left.left.value);
                    p_symbolTable.m_SymbolTable.put(new_variable.GetName(),new_variable);
                    System.out.println(">>>>>>new var:\tname=" +new_variable.GetName() + "\tsize=" +Integer.toString(new_variable.GetSize())); //<LEEOR> TEST

                    FillSymbolTable(p_symbolTable,p_tree.right.left, p_FunctionName); // Re-enter the fill function. Check left sub-tree.

                    //<LEEOR_ADDING_TODAY_FINAL> this function suppose to set the offset value of any variable inside the current record.

                    offsetValue = 0; // Reset global offsetValue for the function
                    SetAllOffestsInRecord(p_symbolTable,p_tree.right.left);
                    offsetValue = 0; //Reset global offsetValue after the function

                    ///<LEEOR_ADDING_TODAY_FINAL> end of my addition

                    new_variable.SetSize(GetRecordSize(p_tree.right.left)); // Sets the record size.

                    p_symbolTable.m_SymbolTable.put(new_variable.GetName(),new_variable);
                    System.out.println(">>>>>>new var:\tname=" +new_variable.GetName() + "\tsize=" +Integer.toString(new_variable.GetSize())); //<LEEOR> TEST
                }

                else // any other type of variable
                {
                    Variable new_variable = new Variable();
                    new_variable.SetFunctionName(p_FunctionName);
                    new_variable.SetType(p_tree.right.value);
                    new_variable.SetAddress(CurrentAvailableAddress);
                    new_variable.SetSize(GetTypeSize(p_tree.right.value));
                    new_variable.SetName(p_tree.left.left.value);

                    if((p_tree.right.value.equals("pointer")) && (p_tree.right.left.value.equals("identifier")))
                    {
                        new_variable.SetPointerOf(p_tree.right.left.left.value);
                    }
                    CurrentAvailableAddress += new_variable.Size;
                    p_symbolTable.m_SymbolTable.put(new_variable.GetName(),new_variable);
                    System.out.println(">>>>>>new var:\tname=" +new_variable.GetName() + "\tsize=" +Integer.toString(new_variable.GetSize()));  //<LEEOR> TEST
                    return;
                }
            }
            else if ((p_tree.value!= null) && (p_tree.value.equals("range")))
            {
                Dimension dim = new Dimension();
                dim.SetStartIndex(Integer.parseInt(p_tree.left.left.value));
                dim.SetEndIndex(Integer.parseInt(p_tree.right.left.value));
                dim.SetSize(dim.endIndex-dim.startIndex+1);
                GlobalDimList.add(dim);
            }

            else if(p_tree.right != null) {FillSymbolTable(p_symbolTable, p_tree.right, p_FunctionName);} // Check right sub-tree
        }

        private static void FillFunctionDetails(SymbolTable p_symbolTable, AST p_tree, String p_father)
        {
            if(p_tree==null)
            {
                return;
            }
            String funcName= p_tree.left.left.left.value;

            CurrentAvailableAddress = TABLE_START_ADDRESS;
            if(p_tree.right!=null && p_tree.right.left != null && p_tree.right.left.left!=null)
            {
                System.out.println(">>>>>>TEST_1: SSP=" + Integer.toString(CurrentAvailableAddress));  //<LEEOR> TEST
                FillSymbolTable(p_symbolTable,p_tree.left.right.left, funcName); // function parameters
                System.out.println(">>>>>>TEST_2: SSP=" + Integer.toString(CurrentAvailableAddress));  //<LEEOR> TEST
                FillSymbolTable(p_symbolTable,p_tree.right.left.left, funcName); // function member variables
                System.out.println(">>>>>>TEST_3: SSP=" + Integer.toString(CurrentAvailableAddress));  //<LEEOR> TEST
            }
            Variable new_var = new Variable();
            new_var.SetName(funcName);
            new_var.SetFunctionName(funcName);
            new_var.SetSize(0);
            new_var.SetAddress(0);
            new_var.SetSSP(CurrentAvailableAddress);
            switch (p_tree.value)
            {
                case "program":
                    new_var.SetType("program");
                    break;
                case "procedure":
                    new_var.SetType("procedure");
                    break;
                case "function":
                    new_var.SetType("function");
                    break;
            }
            p_symbolTable.m_SymbolTable.put(funcName,new_var);
            System.out.println(">>>>>>new var:\tname=" +new_var.GetName() + "\tsize=" +Integer.toString(new_var.GetSize()));
            p_symbolTable.m_FunctionTable.put(funcName,p_father);
            if(p_tree.right!=null
                    && p_tree.right.left!=null
                    && p_tree.right.left.right!=null)
            {
                AST CurrFunctList = p_tree.right.left.right;
                while (CurrFunctList!=null)
                {
                    FillFunctionDetails(p_symbolTable,CurrFunctList.right,funcName);
                    CurrFunctList = CurrFunctList.left;
                }
            }
        }
    }

    // Methods

    private static void generatePCode(AST ast, SymbolTable symbolTable)
    {
        if(ast == null) return;

        Variable CurrentFunc = symbolTable.m_SymbolTable.get(ast.left.left.left.value);

        //region Print function header

        // prints function name
        System.out.println(CurrentFunc.GetName() + ":");

        // print ssp:
        System.out.println("ssp " + Integer.toString(CurrentFunc.SSP));

        // print ujp
        System.out.println("ujp "+CurrentFunc.GetName()+"_begin");
        //endregion

        //region Print sub-functions
        if(ast.right!=null && ast.right.left!=null && ast.right.left.right!=null)
        {
            AST CurrentFunctionList = ast.right.left.right;
            HandleFunctionList(CurrentFunctionList,symbolTable);
        }
        //endregion

        //region Print function code
        System.out.println(CurrentFunc.GetName()+"_begin:");
        if(ast.right.value.equals("content") && ast.right.right != null)
        {
            MakePcode(ast.right.right,symbolTable);
        }
        //endregion

        switch (CurrentFunc.GetType())
        {
            case "program":
                System.out.println("stp");
                break;
            case "function":
                System.out.println("retf");
                break;
            case "procedure":
                System.out.println("retp");
                break;
        }
    }

    private static void HandleFunctionList(AST p_Ast, SymbolTable p_SymbolTable)
    {
        if(p_Ast==null) return;
        if(!p_Ast.value.equals("functionsList")) return;

        HandleFunctionList(p_Ast.left,p_SymbolTable);
        generatePCode(p_Ast.right,p_SymbolTable);
    }

    /**
     *  CheckIfInd Gets an AST node of the tree and a side factor (left-subTree or right-subTree),
     *  and decide rather to print "IND" command or not.
     *
     *  p_tree: an AST;
     *  side: an Integer: 0 for left, 1 for right.
     *
     * **/
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
                    System.out.println("ind");
                }
                else if(p_tree != null && p_tree.right != null && p_tree.right.value.contains("record"))   //<ORON>
                    CheckIfInd(p_tree.right, 1);
            }

        }

    }

    /**
     * GetArrayName returns an Variable of the current Array.
     * This method is used where PCode should access an array, but there are no direct or explicit values of it at the tree
     * (such as variable's name). The method finds the correct array and returns it's variable from the SymbolTable.
     *
     * p_tree: an AST;
     * p_symbolTable: a symbolTable.
     *
     * **/
    private static Variable GetArrayName(AST p_tree, SymbolTable p_symbolTable)
    {
        Variable CurrentArray = null;

        if(p_tree.left.value.equals("pointer"))
        {
            CurrentArray = GetArrayName(p_tree.left, p_symbolTable);
        }

        else if(p_tree.left.value.equals("record"))
        {
            CurrentArray = p_symbolTable.m_SymbolTable.get(p_tree.left.right.left.value);
        }
        else if(p_tree.left.value.equals("identifier"))
        {
            CurrentArray = p_symbolTable.m_SymbolTable.get(p_tree.left.left.value);
        }
        else if((p_tree.left.value.equals("array")))
        {
            CurrentArray = GetArrayName(p_tree.left, p_symbolTable);
            CurrentArray = p_symbolTable.m_SymbolTable.get(CurrentArray.GetType());
        }
        return CurrentArray;
    }

    /**
     * HandleArrayPcode is an recursive method.
     * The method is responsible for generate the PCode inside an array correctly-
     * that includes printing all "LDC", "IXA", "DEC" and other commands.
     *
     * this method also uses the "MakePcode" method to generate any part of current array that is out of it's own boundaries
     * (i.e. an indexList of the array that contains a value of another record/array).
     *
     * p_arrayName: the name of the variable holds the current array.
     * p_dimNum: the Number of dimensions of the current array.
     * p_tree: an AST.
     * p_symbolTable: a SymbolTable.
     *
     * **/
    private static void HandleArrayPcode(String p_arrayName, int p_dimNum, AST p_tree, SymbolTable p_symbolTable)
    {
        if(p_tree == null) {return;}

        HandleArrayPcode(p_arrayName,p_dimNum-1 ,p_tree.left, p_symbolTable);

        if(p_tree.value.equals("indexList"))
        {
            MakePcode(p_tree.right,p_symbolTable);

            if((p_tree.right.value.equals("record"))||(p_tree.right.value.equals("identifier"))
                    || p_tree.right.value.equals("array")) // adding this "array" condition to the total if condition
            {
                System.out.println("ind");
            }
            int CurrentIxa = p_symbolTable.m_SymbolTable.get(p_arrayName).dimensionsList.get(p_dimNum).ixa;
            System.out.println("ixa " + Integer.toString(CurrentIxa));

        }
    }

    /**
     * PrintReverseCases is a recursive method.
     * this method prints all cases of an Switch condition in reverse order, for the last part of an switch-condition PCode.
     *
     * PrintReverseCades gets as parameter an AST node (p_tree), acpected to contain "caseList" value;
     * The method prints in correct ordedr (right to left) all the cases under the given AST.
     * **/
    private static void PrintReverseCases(AST p_tree) //<LEEOR_ADDING>
    {
        if(p_tree == null || !p_tree.value.equals("caseList"))
        {return;}

        // Print right Sub-Tree
        AST CasePointer = p_tree.right;
        if(CasePointer != null && CasePointer.value.equals("case"))
        {
            if(CasePointer.left != null && CasePointer.left.left != null)
                System.out.println("ujp case_"+m_LableNumber+"_"+CasePointer.left.left.value);
        }

        // Print left Sub-Tree
        CasePointer = p_tree.left;
        if(CasePointer!=null && CasePointer.value.equals("caseList"))
        {
            PrintReverseCases(CasePointer);
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

            System.out.println("while_" + label_1 + ":");
            MakePcode(p_tree.left,p_symbolTable);
            System.out.println("fjp while_out_" + label_2);

            MakePcode(p_tree.right,p_symbolTable);
            System.out.println("ujp while_" + label_1);
            System.out.println("while_out_" + label_2 + ":");
            return;
        }
        //endregion

        //region Generate Left SubTree
        if(!p_tree.value.equals("case"))
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
            case "case": // inside an Switxh statement
                if(p_tree.left != null && p_tree.left.left != null)
                {
                    System.out.println("case_" + m_LableNumber + "_" +p_tree.left.left.value+":");
                }

            default: break;
        }

        //endregion

        //region Handle If statement
        if(currentValue.contains("if") && p_tree.right != null &&
                (p_tree.right.right == null || p_tree.right.left == null)) // If statement without else condition
        {
            int label = m_LableNumber++;
            System.out.println("fjp skip_if_" + label);
            MakePcode(p_tree.right, p_symbolTable);
            System.out.println("skip_if_" + label + ":");
            return;
        }
        else if(currentValue.contains("if") && p_tree.right != null && p_tree.right.right != null) // If statement with 'else' condition
        {
            int label_1 = m_LableNumber++;
            System.out.println("fjp skip_if_" + label_1);
            MakePcode(p_tree.right.left, p_symbolTable);

            if(!p_tree.right.value.equals("else")) // there is a 'break' in the right sub tree instead of 'else'
            {
                m_LableNumber--; // before a 'break' node at 'else' right-sub-tree
                MakePcode(p_tree.right.right, p_symbolTable);
                m_LableNumber++; // after a 'break' node at 'else' right-sub-tree
                System.out.println("skip_if_" + label_1 + ":");
                return;
            }

            // ordinary 'else; condition
            int label_2 = m_LableNumber++;
            System.out.println("ujp skip_else_" + label_2);
            System.out.println("skip_if_" + label_1 + ":");
            MakePcode(p_tree.right.right, p_symbolTable);
            System.out.println("skip_else_" + label_2 + ":");

            return;
        }

        //endregion

        //region Handle Switch statement - first part (inorder)

        if(currentValue.equals("switch"))
        {
            int currentSwitchNumber = m_LableNumber;
            CheckIfInd(p_tree, 0);
            m_SwitchNumber++;

            System.out.println("neg");
            System.out.println("ixj switch_end_"+ Integer.toString(currentSwitchNumber));
            m_SwitchNumber++;
        }

        //endregion

        //region Handle Array

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

            if(CurrentArray.GetType().equals("pointer") )
            {
                while(CurrentArray.GetType().equals("pointer"))
                {
                    CurrentArray = p_symbolTable.m_SymbolTable.get(CurrentArray.GetPointerOf());
                }

                // CurrentArray = p_symbolTable.m_SymbolTable.get(CurrentArray.GetPointerOf());    //<ORON> this line moved into the while loop above to avoid null
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

        switch (currentValue) // relevant cases for IND command after generated right subTree ("assignment" has a different case later)
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
            case "else":
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

            case "pointer":
            {
                System.out.println("ind");
                break;
            }

            case "record":
            {
                System.out.println("inc " + (p_symbolTable.m_SymbolTable.get(p_tree.right.left.value)).Offset);
                break;
            }

            case "identifier":
            {

                if((rightTreeSide) && (p_symbolTable.m_SymbolTable.get(p_tree.left.value) != null))    //This if statement fix the unwanted "ldc" print for right hand son
                System.out.println("lda 0 " + p_symbolTable.m_SymbolTable.get(p_tree.left.value).Address); //<ORON> TODO: change this to general function. changed this line from ldc to lda

                break;
            }


            //region All cases of Switch - second part (post-order) <LEEOR_ADDING>
            case "switch":
                PrintReverseCases(p_tree.right); // prints ucj for all cases in the condition, reverse order
                System.out.println("switch_end_" + m_LableNumber++ + ":");
                m_SwitchNumber--;
                break;

            case "caseList":
                break;

            case "case":
                if(p_tree.left != null)
                {
                    System.out.println("ujp switch_end_"+m_LableNumber);
                }
                break;

            //endregion

            case "break":
            {
                System.out.println("ujp while_out_" + (m_LableNumber-1));
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
            {
                if (p_tree.right.value.equals("record") || p_tree.right.value.equals("array")|| p_tree.right.value.equals("identifier")) //<ORON ADDED> added identifier option
                {CheckIfInd(p_tree, 1);}   //<LEEOR_ADDING_TODAY>
                System.out.println("sto");
                break;
            }
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