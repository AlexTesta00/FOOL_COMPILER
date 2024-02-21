package compiler;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

import compiler.AST.*;
import compiler.exc.*;
import compiler.lib.*;

/*
* SymbolTableASTVisitor Class
* It implements a visitor for building symbol tables by visiting the AST during the semantic analysis phase
* Symbol tables are used to find multiply-declared and undeclared variables
* Once the AST has been enriched with symbol tables, the Enriched AST is obtained
* */
public class SymbolTableASTVisitor extends BaseASTVisitor<Void,VoidException> {

	private final int GLOBAL_SCOPE = 0;

	// classTable for mapping each class name to its own virtual table
	private final Map<String, Map<String, STentry>> classTable = new HashMap<>();

	// symTable is a list of tables, one for each currently visible scope
	private final List<Map<String, STentry>> symTable = new ArrayList<>();

	private int nestingLevel = 0; // current nesting level
	private int decOffset = -2; // counter for offset of local declarations at current nesting level
	int stErrors = 0;

	SymbolTableASTVisitor() {}
	SymbolTableASTVisitor(boolean debug) { super(debug); } // enables print for debugging

	/*
	 * stLookup method to process a use of identifier
	 * Look up id in each symbol table in symTable starting from the current nesting level and back to level 0
	 * Link the use of the id with the found symbol table entry
	 * Return the found STentry; otherwise, return null
	 * */
	private STentry stLookup(String id) {
		int j = nestingLevel;
		STentry entry = null;
		while (j >= 0 && entry == null) 
			entry = symTable.get(j--).get(id);	
		return entry;
	}

	/*
	 * visitNode method to visit a ProgLetInNode
	 * Add a new empty hashtable to the front of symTable
	 * Process all declarations by visiting them
	 * Process the main expression of the program by visiting it
	 * Remove the first table from symTable
	 * */
	@Override
	public Void visitNode(ProgLetInNode n) {
		if (print) printNode(n);

		// Create a new empty hashtable
		Map<String, STentry> hm = new HashMap<>();
		// Add the new symbol table to symTable
		symTable.add(hm);

		// Visit all declarations
	    for (Node dec : n.declist) visit(dec);

		// Visit the main expression
		visit(n.exp);

		// Remove the current table from symTable
		symTable.remove(0);

		return null;
	}

	/*
	 * visitNode method to visit a ProgNode
	 * Process the main expression of the program by visiting it
	 * */
	@Override
	public Void visitNode(ProgNode n) {
		if (print) printNode(n);
		visit(n.exp);
		return null;
	}

	/*
	 * visitNode method to visit a FunNode
	 * Create a STentry for the function and look up function id in the current scope
	 * If it is there, then issue a "multiply declared function id" error
	 * Otherwise, add the STentry for the function to the current symbol table
	 * Create a new scope for the function parameters and add them to the new symbol table
	 * Process all declarations by visiting them
	 * Process the main expression of the function by visiting it
	 * Exit the scope and remove the first table from symTable
	 * */
	@Override
	public Void visitNode(FunNode n) {
		if (print) printNode(n);

		// Get the current symbol table
		Map<String, STentry> hm = symTable.get(nestingLevel);

		// Create a list for the function parameter types
		List<TypeNode> parTypes = new ArrayList<>();  
		for (ParNode par : n.parlist) parTypes.add(par.getType());

		// Create STentry for the function
		STentry entry = new STentry(nestingLevel, new ArrowTypeNode(parTypes, n.retType), decOffset--);

		// Check if there is already a declaration of the same function id in the current scope
		// Insert fun id into the current symbol table
		if (hm.put(n.id, entry) != null) {
			ErrorManager.printError(ErrorManager.ERROR_CODE,
								"Fun id " + n.id + " at line " + n.getLine() + " already declared");
			stErrors++;
		}

		// Inner scope
		nestingLevel++; // increment nesting level
		// Create a new hashmap for the symbol table
		Map<String, STentry> hmn = new HashMap<>();
		// Add the new symbol table to symTable
		symTable.add(hmn);
		int prevNLDecOffset = decOffset; // stores counter for offset of declarations at previous nesting level
		decOffset = -2; // reinitialize counter for offset of declarations at current nesting level

		// Check if there is already a declaration of the same parameter id in the current scope
		// Insert par id into the current symbol table
		int parOffset = 1;
		for (ParNode par : n.parlist)
			if (hmn.put(par.id, new STentry(nestingLevel, par.getType(), parOffset++)) != null) {
				ErrorManager.printError(ErrorManager.ERROR_CODE,
						"Par id " + par.id + " at line " + n.getLine() + " already declared");
				stErrors++;
			}

		// Visit all declarations
		for (Node dec : n.declist) visit(dec);

		// Visit the main expression
		visit(n.exp);

		// Remove the current table from symtable and decrement the current nesting level
		symTable.remove(nestingLevel--);

		decOffset = prevNLDecOffset; // restore counter for offset of declarations at previous nesting level

		return null;
	}

	/*
	 * visitNode method to visit a VarNode
	 * Process the expression of the variable declaration by visiting it
	 * Create a STentry for the variable and look up variable id in the current scope
	 * If it is there, then issue a "multiply declared variable id" error
	 * Otherwise, add the STentry for the variable to the current symbol table
	 * */
	@Override
	public Void visitNode(VarNode n) {
		if (print) printNode(n);

		// Visit the expression of the variable declaration
		visit(n.exp);

		// Get the current symbol table
		Map<String, STentry> hm = symTable.get(nestingLevel);

		// Create STentry for the variable
		STentry entry = new STentry(nestingLevel, n.getType(), decOffset--);

		// Check if there is already a declaration of the same variable id in the current scope
		// Insert var id into the current symbol table
		if (hm.put(n.id, entry) != null) {
			ErrorManager.printError(ErrorManager.ERROR_CODE,
					"Var id " + n.id + " at line " + n.getLine() + " already declared");
			stErrors++;
		}

		return null;
	}

	/*
	 * visitNode method to visit a PrintNode
	 * Process the expression to print by visiting it
	 * */
	@Override
	public Void visitNode(PrintNode n) {
		if (print) printNode(n);
		visit(n.exp);
		return null;
	}

	/*
	 * visitNode method to visit an IfNode
	 * Process the expression representing the conditional statement by visiting it
	 * Visit then branch and else branch
	 * */
	@Override
	public Void visitNode(IfNode n) {
		if (print) printNode(n);
		visit(n.cond);
		visit(n.th);
		visit(n.el);
		return null;
	}

	/*
	 * visitNode method to visit an EqualNode
	 * Visit left and right expressions
	 * */
	@Override
	public Void visitNode(EqualNode n) {
		if (print) printNode(n);
		visit(n.left);
		visit(n.right);
		return null;
	}

	/*
	 * visitNode method to visit a TimesNode
	 * Visit left and right expressions
	 * */
	@Override
	public Void visitNode(TimesNode n) {
		if (print) printNode(n);
		visit(n.left);
		visit(n.right);
		return null;
	}

	/*
	 * visitNode method to visit a PlusNode
	 * Visit left and right expressions
	 * */
	@Override
	public Void visitNode(PlusNode n) {
		if (print) printNode(n);
		visit(n.left);
		visit(n.right);
		return null;
	}

	/*
	 * visitNode method to visit a CallNode
	 * Look up fun id in each symbol table in symTable starting from the current nesting level
	 * If fun id is not in any table then issue an "undeclared function identifier" error
	 * Otherwise, link the use of fun id with the found symbol table entry
	 * Process all arguments by visiting them
	 * */
	@Override
	public Void visitNode(CallNode n) {
		if (print) printNode(n);

		// Look up fun id
		STentry entry = stLookup(n.id);

		// Check if there is fun id declaration in any symbol table
		if (entry == null) {
			ErrorManager.printError(ErrorManager.ERROR_CODE,
					"Fun id " + n.id + " at line " + n.getLine() + " not declared");
			stErrors++;
		} else { // set the entry and nesting level
			n.entry = entry;
			n.nl = nestingLevel;
		}

		// Process all arguments by visiting them
		for (Node arg : n.arglist) visit(arg);

		return null;
	}

	/*
	 * visitNode method to visit an IdNode
	 * Look up variable or parameter id in each symbol table in symTable starting from the current nesting level
	 * If the id is not in any table then issue an "undeclared variable or parameter identifier" error
	 * Otherwise, link the use of the id with the found symbol table entry
	 * */
	@Override
	public Void visitNode(IdNode n) {
		if (print) printNode(n);

		// Look up fun id
		STentry entry = stLookup(n.id);

		// Check if there is the id declaration in any symbol table
		if (entry == null) {
			ErrorManager.printError(ErrorManager.ERROR_CODE,
					"Var or Par id " + n.id + " at line "+ n.getLine() + " not declared");
			stErrors++;
		} else { // set the entry and nesting level
			n.entry = entry;
			n.nestingLevel = nestingLevel;
		}

		return null;
	}

	/*
	 * visitNode method to visit a BoolNode
	 * */
	@Override
	public Void visitNode(BoolNode n) {
		if (print) printNode(n, n.val.toString());
		return null;
	}

	/*
	 * visitNode method to visit an IntNode
	 * */
	@Override
	public Void visitNode(IntNode n) {
		if (print) printNode(n, n.val.toString());
		return null;
	}

	/*----------------------------------------------OPERATOR EXTENSION------------------------------------------------*/

	/*
	 * visitNode method to visit a LessEqualNode
	 * Visit left and right expressions
	 * */
	@Override
	public Void visitNode(LessEqualNode n) {
		if (print) printNode(n);
		visit(n.left);
		visit(n.right);
		return null;
	}

	/*
	 * visitNode method to visit a GreaterEqualNode
	 * Visit left and right expressions
	 * */
	@Override
	public Void visitNode(GreaterEqualNode n) {
		if (print) printNode(n);
		visit(n.left);
		visit(n.right);
		return null;
	}

	/*
	 * visitNode method to visit an OrNode
	 * Visit left and right expressions
	 * */
	@Override
	public Void visitNode(OrNode n) {
		if (print) printNode(n);
		visit(n.left);
		visit(n.right);
		return null;
	}

	/*
	 * visitNode method to visit an AndNode
	 * Visit left and right expressions
	 * */
	@Override
	public Void visitNode(AndNode n) {
		if (print) printNode(n);
		visit(n.left);
		visit(n.right);
		return null;
	}

	/*
	 * visitNode method to visit a DivNode
	 * Visit left and right expressions
	 * */
	@Override
	public Void visitNode(DivNode n) {
		if (print) printNode(n);
		visit(n.left);
		visit(n.right);
		return null;
	}

	/*
	 * visitNode method to visit a MinusNode
	 * Visit left and right expressions
	 * */
	@Override
	public Void visitNode(MinusNode n) {
		if (print) printNode(n);
		visit(n.left);
		visit(n.right);
		return null;
	}

	/*
	 * visitNode method to visit a NotNode
	 * Visit the expression
	 * */
	@Override
	public Void visitNode(NotNode n) {
		if (print) printNode(n);
		visit(n.exp);
		return null;
	}

	/*----------------------------------------------CLASS EXTENSION---------------------------------------------------*/

	/*
	 * visitNode method to visit a ClassNode
	 * Create a ClassTypeNode to represent the type of the current class
	 * Check if the superclass is declared and set the super entry
	 * Set superclass fields and methods in the ClassTypeNode
	 * Create the STentry for the current class and add it to the global symbol table
	 * Create a virtual table for the current class inheriting the methods of the superclass if present
	 * Add the virtual table to classTable and symTable
	 * For each field of the current class, visit it, create the STentry and enrich the ClassTypeNode
	 * Add each field to the virtual table of the current class
	 * For each method of the current class, visit it and enrich the ClassTypeNode
	 * Add each method to the virtual table of the current class
	 * Remove the current symbol table and restore the nesting level
	 * */
	@Override
	public Void visitNode(ClassNode n) throws VoidException {
		if (print) printNode(n, n.id);

		// Create a classTypeNode variable to represent the type of the class
		ClassTypeNode classTypeNode = new ClassTypeNode();

		// Temporary classTypeNode for the superclass
		ClassTypeNode temporaryClassTypeNode = new ClassTypeNode();

		// Check if the superclass exists
		if (n.superId != null) {
			// Check if the superclass is declared
			if (classTable.containsKey(n.superId)) {
				STentry superParentSTEntry = symTable.get(GLOBAL_SCOPE).get(n.superId);
				ClassTypeNode superParentTypeNode = (ClassTypeNode) superParentSTEntry.type;
				temporaryClassTypeNode = new ClassTypeNode(superParentTypeNode);
				n.superSTentry = superParentSTEntry;
			} else {
				ErrorManager.printError(ErrorManager.WARNING_CODE,
						"Class: " + n.superId + " at line: " + n.getLine() + " isn't declared");
				stErrors++;
			}
		}

		// Add the superclass in the current class
		classTypeNode = temporaryClassTypeNode;

		n.setType(classTypeNode); // for class and subclass checking

		// Create the entry for the class
		STentry sTentry = new STentry(GLOBAL_SCOPE, classTypeNode, decOffset--);

		// Add the class id to the global symbol table to check for duplicates
		Map<String, STentry> globalSymbolTable = symTable.get(GLOBAL_SCOPE);
		if (globalSymbolTable.put(n.id, sTentry) != null) {
			ErrorManager.printError(ErrorManager.WARNING_CODE,
					"Class: " + n.id + " at line: " + n.getLine() + " is already declared");
			stErrors++;
		}

		// Create a virtual class table for the class
		Map<String, STentry> virtualClassTable = new HashMap<>();
		if (n.superId != null) {
			// Copy the virtual class table of the superclass into the virtual class table of the current class
			virtualClassTable.putAll(classTable.get(n.superId));
		}
		// Add the virtualClassTable to classTable
		this.classTable.put(n.id, virtualClassTable);
		// Add the virtualClassTable to symTable
		this.symTable.add(virtualClassTable);

		// Offset to visit fields in class
		AtomicInteger classFieldsOffset = new AtomicInteger(-1);

		// Container of field names
		Set<String> fieldsContainer = new HashSet<>();

		// Container of methods for the class
		Set<String> methodContainer = new HashSet<>();

		// Store old decOffset
		int oldStepDecOffset = 0;

		nestingLevel++; // fields and methods for the scope in class are at nesting level one

		// Compute the field offset
		if (n.superId != null) {
			ClassTypeNode superType = (ClassTypeNode) symTable.get(GLOBAL_SCOPE).get(n.superId).type;
			classFieldsOffset = new AtomicInteger(-superType.allFields.size() - 1);
		}

		/* Fields in class */
		// Add all fields into the fieldsContainer and visit them all
		for (FieldNode field: n.fieldNodeList) {
			String fieldId = field.id;
			// Check if the field is already declared
			if (fieldsContainer.contains(fieldId)) {
				ErrorManager.printError(ErrorManager.WARNING_CODE,
						"Field: " + fieldId + " at line: " + n.getLine() + " is already declared");
				stErrors++;
			} else {
				fieldsContainer.add(fieldId);
			}
			visit(field);

			// Create the entry for the field
			STentry fieldEntry = new STentry(nestingLevel, field.getType(), classFieldsOffset.getAndDecrement());
			// Check if the superclass exists
			if (n.superId != null && virtualClassTable.containsKey(fieldId)) {
				STentry overrideField = virtualClassTable.get(fieldId);
				if (overrideField.type instanceof MethodTypeNode) {
					ErrorManager.printError(ErrorManager.ERROR_CODE,
							"Cannot override method: " + fieldId + " with a field: " + fieldId);
					stErrors++;
				} else {
					fieldEntry = new STentry(nestingLevel, field.getType(), overrideField.offset);
					classTypeNode.allFields.set(-fieldEntry.offset - 1, fieldEntry.type);
				}
			} else {
				// Add the type of the field to allFields of classTypeNode
				classTypeNode.allFields.add(-fieldEntry.offset - 1, fieldEntry.type);
			}

			// Add the field to the virtual table
			virtualClassTable.put(fieldId, fieldEntry);
			field.offset = fieldEntry.offset;
		}

		/* Methods in class */
		// Set the method offset
		oldStepDecOffset = decOffset;
		decOffset = 0;

		// Check if the superclass exists
		if (n.superId != null) {
			// Get the type of the superclass
			ClassTypeNode superClassType = (ClassTypeNode) symTable.get(GLOBAL_SCOPE).get(n.superId).type;
			// Set the decOffset
			decOffset = superClassType.allMethods.size();
		}

		for (MethodNode method: n.methodNodeList) {
			// Check if the method is already declared
			String methodId = method.id;
			if (methodContainer.contains(methodId)) {
				ErrorManager.printError(ErrorManager.WARNING_CODE,
						"Method: " + methodId + " at line: " + n.getLine() + " is already declared");
				stErrors++;
			} else {
				// Add the method into the methodContainer
				methodContainer.add(methodId);
			}
			// Visit the method and add it to the virtual table
			visit(method);
			classTypeNode.allMethods.add(method.offset,
					((MethodTypeNode) symTable.get(nestingLevel).get(methodId).type).fun);
		}

		// Return to the old offset
		decOffset = oldStepDecOffset;

		// Remove the current symbol table from symTable and decrement the current nesting level
		symTable.remove(nestingLevel--);

		return null;
	}

	/*
	* visitNode method to visit a FieldNode
	* Visit the field in a class
	* */
	@Override
	public Void visitNode(FieldNode n){
		if (print) printNode(n);
		return null;
	}

	/*
	 * visitNode method to visit a MethodNode
	 * Create a MethodTypeNode to represent the functional type of the method
	 * Create the STentry for the method
	 * If the method is already present in the current symbol table, check if the overriding is correct
	 * Insert the method STentry in the current symbol table
	 * Create a symbol table for the method scope and add it to the symTable
	 * For each parameter, create the STentry and insert it in the method symbol table
	 * If the param is already present in the method symbol table then issue a "multiply declared par id" error	 *
	 * Visit all declarations
	 * Visit the expression of the method
	 * Remove the method scope from the symTable
	 * */
	@Override
	public Void visitNode(MethodNode n) throws VoidException {
		if (print) printNode(n);

		// Get the current symbol table
		Map<String, STentry> localTable = symTable.get(nestingLevel);

		// Get the parameters of the method and create a list of TypeNode containing their types
		List<TypeNode> params = n.parList.stream().map(ParNode::getType).toList();

		// Create a MethodTypeNode to represent the functional type of the method
		MethodTypeNode methodTypeNode = new MethodTypeNode(new ArrowTypeNode(params, n.retType));

		// Create the STentry for the method
		STentry sTentry = new STentry(nestingLevel, methodTypeNode, decOffset++);

		// Check if the method already exists in the current symbol table
		if (localTable.containsKey(n.id)) {
			var overrideMethod = localTable.get(n.id);
			if (overrideMethod != null && overrideMethod.type instanceof MethodTypeNode) { // correct override
				sTentry = new STentry(nestingLevel, methodTypeNode, overrideMethod.offset);
				decOffset--;
			} else {
				ErrorManager.printError(ErrorManager.ERROR_CODE,
						"Cannot override a class method: " + n.id);
				stErrors++;
			}
		}

		// Update the offset of the method
		n.offset = sTentry.offset;

		// Insert the method entry in the local symbol table
		localTable.put(n.id, sTentry);

		// Create a hashmap for the method symbol table
		Map<String, STentry> currentMethodTable = new HashMap<>();

		// Update the nesting level
		nestingLevel++;

		// Add the currentMethodTable to the symTable
		symTable.add(currentMethodTable);

		// Set the declaration offset
		int oldStep = decOffset;
		decOffset = -2;

		AtomicInteger parOffset = new AtomicInteger(1);

		// For each parameter of the method
		n.parList.forEach((param) -> {
			// Create an entry
			STentry parEntry = new STentry(nestingLevel, param.getType(), parOffset.getAndIncrement());
			// Check if the parameter is already present in the currentMethodTable
			if (currentMethodTable.put(param.id, parEntry) != null) {
				ErrorManager.printError(ErrorManager.WARNING_CODE,
						"Par: " + param.id + " at line: " + n.getLine() + " already declared");
				stErrors++;
			}
		});

		// Visit all declarations
		n.decList.forEach(this::visit);

		// Visit the method expression
		visit(n.exp);

		// Remove the current table from symTable and decrement the current nesting level
		symTable.remove(nestingLevel--);

		// Restore the offset declaration
		decOffset = oldStep;

		return null;
	}

	/*
	 * visitNode method to visit a ClassCallNode
	 * Look up class id in each symbol table in symTable starting from the current nesting level
	 * If the id was not declared then issue "undeclared identifier" error
	 * If the id was declared check if the type is RefType
	 * Check if the method is present in the virtual table of the class
	 * Get the method entry from the virtual table
	 * If the type is not a RefTypeNode print an error
	 * If the method is not present in the virtual table print an error
	 * Visit all arguments of the method
	 * */
	@Override
	public Void visitNode(ClassCallNode n) throws VoidException {
		if (print) printNode(n);

		// Look up class id
		STentry sTentry = stLookup(n.id);
		if (sTentry == null) {
			ErrorManager.printError(ErrorManager.ERROR_CODE, "Id: " + n.id + "wasn't declared");
			stErrors++;

		// If the id is present, check if the type is RefType
		} else if (sTentry.type instanceof RefTypeNode) {
			n.entry = sTentry;
			n.nestingLevel = nestingLevel;
			// Get the virtual table of the class
			Map<String, STentry> virtualTable = this.classTable.get(((RefTypeNode) sTentry.type).id);

			// Check if the method id is present in the class virtual table
			if (virtualTable.containsKey(n.methodId)) {
				// Set the method entry with the entry and nesting level obtained from the virtual table
				n.methodEntry = virtualTable.get(n.methodId);
			} else { // the method is not present in the class virtual table
				ErrorManager.printError(ErrorManager.WARNING_CODE,
						"Id: " + n.id + " at line: " + n.getLine() + " has no method: " + n.methodId);
				stErrors++;
			}

		} else { // the type is not RefType
			ErrorManager.printError(ErrorManager.WARNING_CODE,
					"Id: " + n.id + " at line: " + n.getLine() + " isn't RefType");
			stErrors++;
		}

		// Visit all arguments
		n.arg.forEach(this::visit);

		return null;
	}

	/*
	 * visitNode method to visit a NewNode
	 * Check if the class id is present in the classTable
	 * If the class id was not declared then issue an "undeclared identifier" error
	 * If the class id was declared get the class entry from the global symbol table
	 * Visit all arguments
	 * */
	@Override
	public Void visitNode(NewNode n) throws VoidException {
		if (print) printNode(n);

		// Check if the class id is present in the classTable
		if (!this.classTable.containsKey(n.id)) {
			ErrorManager.printError(ErrorManager.ERROR_CODE,
					"Class: " + n.id + " was not declared");
			stErrors++;
		}

		// If the class exists, set the entry and nesting level obtained from the global symbol table
		n.entry = symTable.get(GLOBAL_SCOPE).get(n.id);
		n.nestingLevel = nestingLevel;

		// Visit all arguments
		n.arg.forEach(this::visit);
		return null;
	}

	/*
	 * visitNode method to visit an EmptyNode
	 * */
	@Override
	public Void visitNode(EmptyNode n) throws VoidException {
		if (print) printNode(n);
		return null;
	}

	/*
	 * visitNode method to visit a ClassTypeNode
	 * */
	@Override
	public Void visitNode(ClassTypeNode n) throws VoidException {
		if (print) printNode(n);
		return null;
	}

	/*
	 * visitNode method to visit a MethodTypeNode
	 * */
	@Override
	public Void visitNode(MethodTypeNode n) throws VoidException {
		if (print) printNode(n);
		return null;
	}

	/*
	 * visitNode method to visit a RefTypeNode
	 * Check if the class id was declared. If not, print an error message
	 * */
	@Override
	public Void visitNode(RefTypeNode n) throws VoidException {
		if (print) printNode(n);
		if (classTable.get(n.id) == null) {
			ErrorManager.printError(ErrorManager.ERROR_CODE,
					"Class : " + n.id + " at line: " + n.getLine() + " wasn't declared");
			stErrors++;
		}
		return null;
	}

	/*----------------------------------------------ERROR MANAGER------------------------------------------------*/
	/*
	 * ErrorManager class
	 * Handle the compilation errors by printing them with different colors based on their importance
	 * */
	private static class ErrorManager {
		public static int ERROR_CODE = 1;
		public static int WARNING_CODE = 0;
		public static final String ANSI_RESET = "\u001B[0m";
		public static final String ANSI_RED = "\u001B[31m";
		public static final String ANSI_YELLOW = "\u001B[33m";
		private static void printError(int code, String msg){
			if (code == ERROR_CODE) {
				System.out.println(ANSI_RED + msg + ANSI_RESET);
			} else {
				System.out.println(ANSI_YELLOW + msg + ANSI_RESET);
			}
		}
	}
}