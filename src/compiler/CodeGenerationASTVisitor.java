package compiler;

import compiler.AST.*;
import compiler.lib.*;
import compiler.exc.*;
import svm.ExecuteVM;

import java.util.ArrayList;
import java.util.List;

import static compiler.lib.FOOLlib.*;

/*
* CodeGenerationASTVisitor Class
* It implements the visitor pattern to generate code by translating the AST into another language
* */
public class CodeGenerationASTVisitor extends BaseASTVisitor<String, VoidException> {

  // dispatchTablesClasses is a list of Dispatch Tables, one for each class
  // Dispatch Table is a list of labels, one for each method of the class
  private final List<List<String>> dispatchTablesClasses = new ArrayList<>();

  CodeGenerationASTVisitor() {}
  CodeGenerationASTVisitor(boolean debug) { super(false,debug); } // enables print for debugging

  /*
   * visitNode method to generate code for a ProgLetInNode
   * */
  @Override
	public String visitNode(ProgLetInNode n) {
		if (print) printNode(n);
		String declCode = null;
		for (Node dec : n.declist) declCode = nlJoin(declCode,visit(dec));
		return nlJoin(
			"push 0",	
			declCode, // generate code for declarations (allocation)			
			visit(n.exp), // generate code for the main expression
			"halt", // terminate the execution
			getCode()
		);
	}

	/*
	 *  visitNode method to generate code for a ProgNode
	 * */
	@Override
	public String visitNode(ProgNode n) {
		if (print) printNode(n);
		return nlJoin(
			visit(n.exp), // generate code for the main expression
			"halt" // terminate the execution
		);
	}

	/*
	 * visitNode method to generate code for a FunNode
	 * */
	@Override
	public String visitNode(FunNode n) {
		if (print) printNode(n,n.id);
		String declCode = null;
		String popDecl = null;
		String popParl = null;
		for (Node dec : n.declist) {
			declCode = nlJoin(declCode,visit(dec));
			popDecl = nlJoin(popDecl,"pop");
		}
		for (int i = 0; i < n.parlist.size(); i++) popParl = nlJoin(popParl,"pop");
		String funl = freshFunLabel();
		putCode(
			nlJoin(
				funl+":",
				"cfp", // set $fp to $sp value
				"lra", // load $ra value
				declCode, // generate code for local declarations (they use the new $fp)
				visit(n.exp), // generate code for function body expression
				"stm", // set $tm to popped value (function result)
				popDecl, // remove local declarations from stack
				"sra", // set $ra to popped value
				"pop", // remove Access Link from stack
				popParl, // remove parameters from stack
				"sfp", // set $fp to popped value (Control Link)
				"ltm", // load $tm value (function result)
				"lra", // load $ra value
				"js"  // jump to the popped address
			)
		);
		return "push " + funl;
	}

	/*
	 * visitNode method to generate code for a VarNode
	 * */
	@Override
	public String visitNode(VarNode n) {
		if (print) printNode(n,n.id);
		return visit(n.exp); // generate code for the expression of the variable declaration
	}

	/*
	 * visitNode method to generate code for a PrintNode
	 * */
	@Override
	public String visitNode(PrintNode n) {
		if (print) printNode(n);
		return nlJoin(
			visit(n.exp), // generate code for the expression to print
			"print"
		);
	}

	/*
	 * visitNode method to generate code for a IfNode
	 * */
	@Override
	public String visitNode(IfNode n) {
		if (print) printNode(n);
		String l1 = freshLabel();
	 	String l2 = freshLabel();		
		return nlJoin(
			visit(n.cond), // generate code for the condition
			"push 1",
			"beq " + l1, // jump to l1 if the condition is true
			visit(n.el), // generate code for the else branch
			"b " + l2, // jump to l2
			l1 + ":",
			visit(n.th), // generate code for then branch
			l2 + ":"
		);
	}

	/*
	 * visitNode method to generate code for an EqualNode
	 * */
	@Override
	public String visitNode(EqualNode n) {
		if (print) printNode(n);
		String l1 = freshLabel();
	 	String l2 = freshLabel();
		return nlJoin(
			visit(n.left), // generate code for the left expression
			visit(n.right), // generate code for the right expression
			"beq " + l1, // jump to l1 if the values of the two expressions are equal
			"push 0",
			"b " + l2, // jump to l2
			l1 + ":",
			"push 1",
			l2 + ":"
		);
	}

	/*
	 * visitNode method to generate code for a TimesNode
	 * */
	@Override
	public String visitNode(TimesNode n) {
		if (print) printNode(n);
		return nlJoin(
			visit(n.left), // generate code for the left expression
			visit(n.right), // generate code for the right expression
			"mult" // replace the two values on top of the stack with their product
		);	
	}

	/*
	 * visitNode method to generate code for a PlusNode
	 * */
	@Override
	public String visitNode(PlusNode n) {
		if (print) printNode(n);
		return nlJoin(
			visit(n.left), // generate code for the left expression
			visit(n.right), // generate code for the right expression
			"add" // replace the two values on top of the stack with their sum
		);
	}

	/*
	 * visitNode method to generate code for a CallNode
	 * */
	@Override
	public String visitNode(CallNode n) {
		if (print) printNode(n,n.id);
		String argCode = null, getAR = null;
		for (int i = n.arglist.size() - 1; i >= 0; i--) argCode = nlJoin(argCode,visit(n.arglist.get(i)));
		for (int i = 0; i < n.nl - n.entry.nl; i++) getAR = nlJoin(getAR,"lw");
		return nlJoin(
			"lfp", // load Control Link (pointer to frame of function "id" caller)
			argCode, // generate code for arguments in reversed order
			"lfp", getAR, // retrieve address of frame containing "id" declaration
                          // by following the static chain (of Access Links)
            "stm", // set $tm to popped value (with the aim of duplicating top of stack)
            "ltm", // load Access Link (pointer to frame of function "id" declaration)
            "ltm", // duplicate top of stack

			(n.entry.type instanceof MethodTypeNode) ? "lw" : "", // recover address of method in dispatch table to jump
            "push " + n.entry.offset, "add", // compute address of "id" declaration
			"lw", // load address of "id" function
            "js"  // jump to the popped address (saving address of subsequent instruction in $ra)
		);
	}

	/*
	 * visitNode method to generate code for IdNode
	 * */
	@Override
	public String visitNode(IdNode n) {
		if (print) printNode(n,n.id);
		String getAR = null;
		for (int i = 0; i < n.nestingLevel - n.entry.nl; i++) getAR = nlJoin(getAR,"lw");
		return nlJoin(
			"lfp", getAR, // retrieve address of frame containing "id" declaration
			             		 // by following the static chain (of Access Links)
			"push " + n.entry.offset, "add", // compute address of "id" declaration
			"lw" // load value of "id" variable
		);
	}

	/*
	 * visitNode method to generate code for BoolNode
	 * */
	@Override
	public String visitNode(BoolNode n) {
		if (print) printNode(n,n.val.toString());
		return "push " + (n.val ? 1 : 0);
	}

	/*
	 * visitNode method to generate code for IntNode
	 * */
	@Override
	public String visitNode(IntNode n) {
		if (print) printNode(n,n.val.toString());
		return "push " + n.val;
	}

	/*----------------------------------------------OPERATOR EXTENSION------------------------------------------------*/

	/*
	 * visitNode method to generate code for LessEqualNode
	 * */
	@Override
	public String visitNode(LessEqualNode n) throws VoidException {
		if (print) printNode(n);
		String l1 = freshLabel();
		String l2 = freshLabel();
		return nlJoin(
				visit(n.left), // generate code for the left expression
				visit(n.right), // generate code for the right expression
				"bleq " + l1, // jump to l1 if the left value is less than or equal to the right value
				"push 0",
				"b " + l2, // jump to l2
				l1 + ":",
				"push 1",
				l2 + ":"
		);
	}

	/*
	 * visitNode method to generate code for GreaterEqualNode
	 * */
	@Override
	public String visitNode(GreaterEqualNode n) throws VoidException {
		if (print) printNode(n);
		String l1 = freshLabel();
		String l2 = freshLabel();
		return nlJoin(
				visit(n.left), // generate code for the left expression
				visit(n.right), // generate code for the right expression
				"push 1",
				"sub", // pop the two values 1 and right (respectively) and push right-1
				"bleq " + l1, // jump to l1 if left is less than or equal to the result of the subtraction
				"push 1",
				"b " + l2, // jump to l2
				l1 + ":",
				"push 0",
				l2 + ":"
		);
	}

	/*
	 * visitNode method to generate code for OrNode
	 * */
	@Override
	public String visitNode(OrNode n) throws VoidException {
		if (print) printNode(n);
		String l1 = freshLabel();
		String l2 = freshLabel();
		return nlJoin(
				visit(n.left), // generate code for the left expression
				"push 1",
				"beq " + l1, // jump to l1 if left is equal to 1
				visit(n.right), // generate code for the right expression
				"push 1",
				"beq " + l1, // jump to l1 if right is equal to 1
				"push 0",
				"b " + l2, // jump to l2
				l1 + ":",
				"push 1",
				l2 + ":"
		);
	}

	/*
	 * visitNode method to generate code for AndNode
	 * */
	@Override
	public String visitNode(AndNode n) throws VoidException {
		if (print) printNode(n);
		String l1 = freshLabel();
		String l2 = freshLabel();
		return nlJoin(
				visit(n.left), // generate code for the left expression
				"push 0",
				"beq " + l1, // jump to l1 if left is equal to 0
				visit(n.right), // generate code for the right expression
				"push 0",
				"beq " + l1, // jump to l1 if right is equal to 0
				"push 1",
				"b " + l2, // jump to l2
				l1 + ":",
				"push 0",
				l2 + ":"
		);
	}

	/*
	 * visitNode method to generate code for DivNode
	 * */
	@Override
	public String visitNode(DivNode n) throws VoidException {
		if (print) printNode(n);
		return nlJoin(
				visit(n.left), // generate code for the left expression
				visit(n.right), // generate code for the right expression
				"div" // pop the two values right and left (respectively) and push left/right
		);
	}

	/*
	 * visitNode method to generate code for MinusNode
	 * */
	@Override
	public String visitNode(MinusNode n) throws VoidException {
		if (print) printNode(n);
		return nlJoin(
				visit(n.left), // generate code for the left expression
				visit(n.right), // generate code for the right expression
				"sub"  // pop the two values right and left (respectively) and push left-right
		);
	}

	/*
	 * visitNode method to generate code for NotNode
	 * */
	@Override
	public String visitNode(NotNode n) throws VoidException {
		if (print) printNode(n);
		final String l1 = freshLabel();
		final String l2 = freshLabel();
		return nlJoin(
				visit(n.exp), // generate code for the expression
				"push 0",
				"beq " + l1, // jump to l1 if the value of the expression is equal to 0
				"push 0",
				"b " + l2, // jump to l2
				l1 + ":",
				"push 1",
				l2 + ":"
		);
	}

	/*----------------------------------------------CLASS EXTENSION---------------------------------------------------*/

	/*
	 * visitNode method to generate code for ClassNode
	 * */
	@Override
	public String visitNode(ClassNode n) throws VoidException {
		if (print) printNode(n);

		// Create the dispatch table of the current class
		List<String> dispatchTable = new ArrayList<>();
		// Add the dispatch table of the current class to dispatchTablesClasses
		dispatchTablesClasses.add(dispatchTable);

		// Check if the superclass exists
		if (n.superSTentry != null) {
			// Get the dispatch table of the superclass from the dispatchTablesClasses
			List<String> superClassDispatchTable = dispatchTablesClasses.get(-n.superSTentry.offset - 2);
			// Add all methods of the superclass to the dispatch table of the current class
			dispatchTable.addAll(superClassDispatchTable);
		}

		// Consider the methods of the current class in order of appearance
		n.methodNodeList.forEach((method) -> {
			visit(method);
			if (method.offset < dispatchTable.size()) { // the method belongs to the superclass
				// Read label and update dispatchTable
				dispatchTable.add(method.offset, method.label);
			} else { // the method belongs to the current class
				// Read label and update dispatchTable
				dispatchTable.add(method.label);
			}
		});

		String dispatchCode = "";
		for (final String label : dispatchTable) {
			dispatchCode = nlJoin(
					dispatchCode,
					"push " + label,
					"lhp", // load $hp value
					"sw", // store the label of the method in $hp
					"lhp", // load $hp value
					"push 1",
					"add", // increment $hp
					"shp" // store $hp
			);
		}

		return nlJoin(
				"lhp",
				dispatchCode
		);
	}

	/*
	 * visitNode method to generate code for MethodNode
	 * */
	@Override
	public String visitNode(MethodNode n) throws VoidException {
		if (print) printNode(n);

		// Generate a new label for the method address
        n.label = freshLabel();

		String declCode = null;
		String popDecl = null;
		String popParl = null;
		for (Node dec : n.decList) {
			declCode = nlJoin(declCode,visit(dec));
			popDecl = nlJoin(popDecl,"pop");
		}
		for (int i = 0; i < n.parList.size(); i++) popParl = nlJoin(popParl,"pop");
		putCode(
				nlJoin(
						n.label + ":",
						"cfp", // set $fp to $sp value
						"lra", // load $ra value
						declCode, // generate code for local declarations (they use the new $fp!!!)
						visit(n.exp), // generate code for function body expression
						"stm", // set $tm to popped value (function result)
						popDecl, // remove local declarations from stack
						"sra", // set $ra to popped value
						"pop", // remove Access Link from stack
						popParl, // remove parameters from stack
						"sfp", // set $fp to popped value (Control Link)
						"ltm", // load $tm value (function result)
						"lra", // load $ra value
						"js"  // jump to the popped address
				)
		);
		return null;
	}

	/*
	 * visitNode method to generate code for ClassCallNode
	 * */
	@Override
	public String visitNode(ClassCallNode n) throws VoidException {
		if (print) printNode(n,n.id);
		String argCode = null;
		String getAR = null;
		for (int i = n.arg.size() -1 ; i >= 0; i--) argCode = nlJoin(argCode, visit(n.arg.get(i)));
		for (int i = 0; i < n.nestingLevel - n.entry.nl; i++) getAR = nlJoin(getAR,"lw");
		return nlJoin(
				"lfp", // load Control Link (pointer to frame of function "id" caller)
				argCode, // generate code for arguments in reversed order
				"lfp", getAR, // retrieve address of frame containing "id" declaration
							  // by following the static chain (of Access Links)
				"push " + n.entry.offset, "add", // compute address of "id" class declaration
				"lw", // load address of "id" class
				"stm", // set $tm to popped value (with the aim of duplicating top of stack)
				"ltm", // load Access Link (pointer to frame of function "id" declaration)
				"ltm", // duplicate top of stack
				"lw", // load dispatch table address
				"push " + n.methodEntry.offset, "add", // compute address of "id" method declaration
				"lw", // load address of "id" method
				"js"  // jump to the popped address (saving address of subsequent instruction in $ra)
		);
	}

	/*
	 * visitNode method to generate code for NewNode
	 * */
	@Override
	public String visitNode(NewNode n) throws VoidException {
		if (print) printNode(n);
		String argumentsCode = "";
		for (Node argument : n.arg) { // consider the arguments in order of appearance
			argumentsCode = nlJoin(argumentsCode,visit(argument));
		}
		String heapCode = "";
		for (Node argument : n.arg) {
			heapCode = nlJoin(
					heapCode,
					// Load argument on the heap
					"lhp", // load $hp
					"sw", // store argument in the heap
					"lhp", // load $hp
					"push 1",
					"add", // increment $hp
					"shp" // store $hp
			);
		}

		return nlJoin(
				argumentsCode, // generate code for arguments
				heapCode, // move arguments on the heap

				"push " + (ExecuteVM.MEMSIZE + n.entry.offset), // push dispatch pointer on the stack
				"lw", // load dispatch pointer
				"lhp", // load $hp
				"sw", // store dispatch pointer in the heap

				"lhp", // push dispatch pointer on the stack

				// Update $hp = $hp + 1
				"lhp", // duplicate top of stack
				"push 1",
				"add", // add 1 to $hp
				"shp" // store $hp
		);
	}

	/*
	 * visitNode method to generate code for EmptyNode
	 * */
	@Override
	public String visitNode(EmptyNode n) throws VoidException {
		if (print) printNode(n);
		return "push -1";
	}
}