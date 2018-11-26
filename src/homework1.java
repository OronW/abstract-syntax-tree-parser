import java.util.Hashtable;
import java.util.Scanner;

// <ORON> TODO: function to calculate the sizeof according to type => fix getsize()

class homework1
{
    // Properties
    private static int m_LableNumber = 0; // Labels counter for the Pcode
    public static boolean recordFlag = false;   // flag to indicate we are inside a record and update offset value
    public static int offsetValue = 0;

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

    static final class Variable
    {
        // Properties
        public int Size;
        public String Name;
        public String Type;
        public int Address;
        public int Offset; //<ORON> TODO: check if really needed for every variable



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
    }

    static final class SymbolTable
    {
        // Properties
        public Hashtable<String, Variable> m_SymbolTable;         /**<LEEOR> TODO: change to Map or Dictionary  <ORON> Why should we change?</>**/
    public static int CurrentAvailableAddress;
        public static final int TABLE_START_ADDRESS = 5;

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

        /**
         * FillSymbolTable is a recursive method;
         * The method delivers variables from the AST into the Symbol Table;
         * @param p_tree: an AST;
         *            Expected to start from Scope branch of the tree;
         *
         * @param p_symbolTable: an SymbolTable;
         */
        private static void FillSymbolTable(SymbolTable p_symbolTable, AST p_tree)
        {
            if(p_tree == null) {return;}

            if(p_tree.left != null) {FillSymbolTable(p_symbolTable,p_tree.left);} // Check left sub-tree

            if(p_tree.value.equals("var"))
            {
                /**<ORON> TODO: new code here - if record do... else act normal:*/
                if ((p_tree.right.value!= null) && (p_tree.right.value.equals("array"))) // TODO: currently empty - fill
                {
                    return;
                }

                if ((p_tree.right.value!= null) && (p_tree.right.value.equals("record")))
                {
                    {
                        recordFlag = true;
                        FillSymbolTable(p_symbolTable,p_tree.right.left); // Re-enter the fill function. Check left sub-tree...

                        recordFlag = false; //reset the offset
                        offsetValue = 0;
                    }
                }
                else {/**<ORON> TODO: end of added changes 25/11*/

                    Variable new_variable = new Variable();
                    new_variable.SetType(p_tree.right.value);

                    new_variable.SetSize(1); /** <LEEOR> TODO: CHANGE LATER (AVOID MAGIC NUMBER) **/
                    new_variable.SetName(p_tree.left.left.value);
                    new_variable.SetAddress(p_symbolTable.CurrentAvailableAddress);

                    if(recordFlag==true) {          //<ORON> deal with offset
                        new_variable.SetOffset(offsetValue);
                        offsetValue = offsetValue + new_variable.GetSize();
                    }

                    p_symbolTable.CurrentAvailableAddress += new_variable.Size;
                    p_symbolTable.m_SymbolTable.put(new_variable.GetName(),new_variable);
                    return;
                }
            }
            else if(p_tree.right != null) {FillSymbolTable(p_symbolTable, p_tree.right);} // Check right sub-tree
        }
    }

    // Methods
    private static void generatePCode(AST ast, SymbolTable symbolTable)
    {
        // TODO: go over AST and print code

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
                if(p_tree!=null && p_tree.left != null && p_tree.left.value.contains("identifier"))
                {
                    System.out.println("ind");
                }
                break;
            }
            case 1: // right
            {
                if(p_tree != null && p_tree.right != null && p_tree.right.value.contains("identifier"))
                {
                    System.out.println("ind");  //TODO: some ind are missing
                }
                else if(p_tree != null && p_tree.right != null && p_tree.right.value.contains("record"))   //<ORON>
                    CheckIfInd(p_tree.right, 1);
            }

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
        //endregion

        //region Generate Right SubTree
        MakePcode(p_tree.right, p_symbolTable);
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
                //case "else":
            case "equals":
            case "notEquals":
            case "lessThan":
            case "greaterThan":
            case "lessOrEquals":
            case "greaterOrEquals":
            case "print":
            {
                CheckIfInd(p_tree,1); // checks if left sub-tree has identifier
                break;
            }

            /*case "identifier":
            {
                System.out.println("inc " + (p_symbolTable.m_SymbolTable.get(p_tree.left.value)).Offset);
                break;
            }*/
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

            //TODO: new code added here for record
            case "record":
            {
                System.out.println("inc " + (p_symbolTable.m_SymbolTable.get(p_tree.right.left.value)).Offset);
                break;
            }

            case "identifier":
            {
                if((p_symbolTable.m_SymbolTable.get(p_tree.left.value) != null))    //todo: check if needed - this if statment is used for testings
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
