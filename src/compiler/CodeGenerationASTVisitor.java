package compiler;

import compiler.AST.*;
import compiler.lib.*;
import compiler.exc.*;
import svm.ExecuteVM;

import java.util.ArrayList;
import java.util.List;

import static compiler.lib.FOOLlib.*;

public class CodeGenerationASTVisitor extends BaseASTVisitor<String, VoidException> {

  CodeGenerationASTVisitor() {}
  CodeGenerationASTVisitor(boolean debug) {super(false,debug);} //enables print for debugging

	//This represents the list of lables for each method of the class
	private final List<List<String>> dispatchClassesTables = new ArrayList<>();

	@Override
	public String visitNode(ProgLetInNode n) {
		if (print) printNode(n);
		String declCode = null;
		for (Node dec : n.declist) declCode=nlJoin(declCode,visit(dec));
		return nlJoin(
			"push 0",	
			declCode, // generate code for declarations (allocation)			
			visit(n.exp),
			"halt",
			getCode()
		);
	}

	@Override
	public String visitNode(ProgNode n) {
		if (print) printNode(n);
		return nlJoin(
			visit(n.exp),
			"halt"
		);
	}

	@Override
	public String visitNode(FunNode n) {
		if (print) printNode(n,n.id);
		String declCode = null, popDecl = null, popParl = null;
		for (Node dec : n.declist) {
			declCode = nlJoin(declCode,visit(dec));
			popDecl = nlJoin(popDecl,"pop");
		}
		for (int i=0;i<n.parlist.size();i++) popParl = nlJoin(popParl,"pop");
		String funl = freshFunLabel();
		putCode(
			nlJoin(
				funl+":",
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
				"js"  // jump to to popped address
			)
		);
		return "push "+funl;		
	}

	@Override
	public String visitNode(VarNode n) {
		if (print) printNode(n,n.id);
		return visit(n.exp);
	}

	@Override
	public String visitNode(PrintNode n) {
		if (print) printNode(n);
		return nlJoin(
			visit(n.exp),
			"print"
		);
	}

	@Override
	public String visitNode(IfNode n) {
		if (print) printNode(n);
	 	String l1 = freshLabel();
	 	String l2 = freshLabel();		
		return nlJoin(
			visit(n.cond),
			"push 1",
			"beq "+l1,
			visit(n.el),
			"b "+l2,
			l1+":",
			visit(n.th),
			l2+":"
		);
	}

	@Override
	public String visitNode(EqualNode n) {
		if (print) printNode(n);
	 	String l1 = freshLabel();
	 	String l2 = freshLabel();
		return nlJoin(
			visit(n.left),
			visit(n.right),
			"beq "+l1,
			"push 0",
			"b "+l2,
			l1+":",
			"push 1",
			l2+":"
		);
	}

	@Override
	public String visitNode(TimesNode n) {
		if (print) printNode(n);
		return nlJoin(
			visit(n.left),
			visit(n.right),
			"mult"
		);	
	}

	@Override
	public String visitNode(PlusNode n) {
		if (print) printNode(n);
		return nlJoin(
			visit(n.left),
			visit(n.right),
			"add"				
		);
	}

	@Override
	public String visitNode(CallNode n) {
		if (print) printNode(n,n.id);

		String argCode = null, getAR = null;
		for (int i=n.arglist.size()-1;i>=0;i--) argCode=nlJoin(argCode,visit(n.arglist.get(i)));
		for (int i = 0;i<n.nl-n.entry.nl;i++) getAR=nlJoin(getAR,"lw");
		return nlJoin(
			"lfp", // load Control Link (pointer to frame of function "id" caller)
			argCode, // generate code for argument expressions in reversed order
			"lfp", getAR, // retrieve address of frame containing "id" declaration
                          // by following the static chain (of Access Links)
            "stm", // set $tm to popped value (with the aim of duplicating top of stack)
            "ltm", // load Access Link (pointer to frame of function "id" declaration)
            "ltm", // duplicate top of stack

			(n.entry.type instanceof MethodTypeNode) ? "lw" : "", // Recover address of method in dispatch table for jump
            "push " + n.entry.offset,
			"add", // compute address of "id" declaration
			"lw", // load address of "id" function
            "js"  // jump to popped address (saving address of subsequent instruction in $ra)
		);
	}

	@Override
	public String visitNode(IdNode n) {
		if (print) printNode(n,n.id);
		String getAR = null;
		for (int i = 0; i<n.nestingLevel -n.entry.nl; i++) getAR=nlJoin(getAR,"lw");
		return nlJoin(
			"lfp", getAR, // retrieve address of frame containing "id" declaration
			              // by following the static chain (of Access Links)
			"push "+n.entry.offset, "add", // compute address of "id" declaration
			"lw" // load value of "id" variable
		);
	}

	@Override
	public String visitNode(BoolNode n) {
		if (print) printNode(n,n.val.toString());
		return "push "+(n.val?1:0);
	}

	@Override
	public String visitNode(IntNode n) {
		if (print) printNode(n,n.val.toString());
		return "push "+n.val;
	}

	/*----------------------------------------------OPERATOR EXTENSION------------------------------------------------*/

	@Override
	public String visitNode(LessEqualNode n) throws VoidException {
		if (print) printNode(n);
		String firstLabel = freshLabel();
		String secondLabel = freshLabel();
		return nlJoin(
				visit(n.left),
				visit(n.right),
				"bleq " + firstLabel,
				"push 0",
				"b " + secondLabel,
				firstLabel + ":",
				"push 1",
				secondLabel + ":"

		);
	}

	@Override
	public String visitNode(GreaterEqualNode n) throws VoidException {
		if (print) printNode(n);
		String firstLabel = freshLabel();
		String secondLabel = freshLabel();
		return nlJoin(
				visit(n.left),
				visit(n.right),
				"push 1",
				"sub",
				"bleq " + firstLabel,
				"push 1",
				"b " + secondLabel,
				firstLabel + ":",
				"push 0",
				secondLabel + ":"
		);
	}

	@Override
	public String visitNode(OrNode n) throws VoidException {
		if (print) printNode(n);
		String firstLabel = freshLabel();
		String secondLabel = freshLabel();
		return nlJoin(
				visit(n.left),
				"push 1",
				"beq " + firstLabel,
				visit(n.right),
				"push 1",
				"beq " + firstLabel,
				"push 0",
				"b " + secondLabel,
				firstLabel + ":",
				"push 1",
				secondLabel + ":"
		);
	}

	@Override
	public String visitNode(AndNode n) throws VoidException {
		if (print) printNode(n);
		String firstLabel = freshLabel();
		String secondLabel = freshLabel();
		return nlJoin(
				visit(n.left),
				"push 0",
				"beq " + firstLabel,
				visit(n.right),
				"push 0",
				"beq " + firstLabel,
				"push 1",
				"b " + secondLabel,
				firstLabel + ":",
				"push 0",
				secondLabel + ":"
		);
	}

	@Override
	public String visitNode(DivNode n) throws VoidException {
		if (print) printNode(n);
		return nlJoin(
				visit(n.left),
				visit(n.right),
				"div"
		);
	}

	@Override
	public String visitNode(MinusNode n) throws VoidException {
		if (print) printNode(n);
		return nlJoin(
				visit(n.left),
				visit(n.right),
				"sub"
		);
	}

	@Override
	public String visitNode(NotNode n) throws VoidException {
		if (print) printNode(n);
		final String firstLabel = freshLabel();
		final String secondLabel = freshLabel();
		return nlJoin(
				visit(n.exp),
				"push 0",
				"beq " + firstLabel,
				"push 0",
				"b " + secondLabel,
				firstLabel + ":",
				"push 1",
				secondLabel + ":"
		);
	}

	/*----------------------------------------------CLASS EXTENSION---------------------------------------------------*/

	@Override
	public String visitNode(ClassNode n) throws VoidException {
		if (print) printNode(n);

		//Create dispatch tables
		List<String> dispatchTable = new ArrayList<>();
		this.dispatchClassesTables.add(dispatchTable);

		//Check the parent class
		if(n.superSTentry != null){
			List<String> superClassDispatchTable = dispatchClassesTables.get(-n.superSTentry.offset - 2);
			dispatchTable.addAll(superClassDispatchTable);
		}

		//Check the method in order
		n.methodNodeList.forEach((method) ->{
			//Visit the element
			visit(method);

			//Check if the method is of the super class
			if(method.offset < dispatchTable.size()){
				//Read label and Update Dispatch Tables
				dispatchTable.add(method.offset, method.label);
			}else{
				//Read label and Update Dispatch Tables
				dispatchTable.add(method.label);
			}
		});

		String dispatchCode = "";
		for (final String label : dispatchTable) {
			dispatchCode = nlJoin(
					dispatchCode,

					// Store the label of the method in heap
					"push " + label,       // Push label of method
					"lhp",  			   // Push the heap pointer
					"sw",         		   // Store the label of the method in the heap
					"lhp",  			   // Increment heap pointer and push heap pointer
					"push 1",              // Push 1
					"add",                 // Heap pointer + 1
					"shp"                  // Store heap pointer

			);
		}

		return nlJoin(
				"lhp",
				dispatchCode //Add the code of the dispatch table
		);
	}

	@Override
	public String visitNode(MethodNode n) throws VoidException {
		if (print) printNode(n);

		//Generate label for the new address
        n.label = freshLabel();

		String declCode = null;
		String popDecl = null;
		String popParl = null;
		for (Node dec : n.decList) {
			declCode = nlJoin(declCode,visit(dec));
			popDecl = nlJoin(popDecl,"pop");
		}
		for (int i=0;i<n.parList.size();i++) popParl = nlJoin(popParl,"pop");

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
						"js"  // jump to popped address
				)
		);
		return null;
	}

	@Override
	public String visitNode(ClassCallNode n) throws VoidException {
		if (print) printNode(n,n.id);
		String argCode = null;
		String getAR = null;
		for (int i = n.arg.size()-1; i >= 0; i--) argCode = nlJoin(argCode, visit(n.arg.get(i)));
		for (int i = 0; i<n.nestingLevel - n.entry.nl; i++) getAR = nlJoin(getAR,"lw");
		return nlJoin(
				"lfp", // load Control Link (pointer to frame of function "id" caller)
				argCode, // generate code for argument expressions in reversed order
				"lfp", getAR, // retrieve address of frame containing "id" declaration
				// by following the static chain (of Access Links)
				"push " + n.entry.offset, //push the class offset on the stack
				"add", //Put the class offset to ar register
				"lw", //load the class
				"stm", // set $tm to popped value (with the aim of duplicating top of stack)
				"ltm", // Put the class address on the stack
				"ltm", // duplicate top of stack
				"lw", // load dispatch table
				"push " + n.methodEntry.offset, // push the offset of the method on the stack
				"add", // add the offset of the method to dispatch table
				"lw", // load address of method
				"js"  // jump to popped address (saving address of subsequent instruction in $ra)
		);
	}

	@Override
	public String visitNode(NewNode n) throws VoidException {
		if (print) printNode(n);

		//Call it on the all arguments in order of their appears
		String argumentsCode = "";
		for(Node argument: n.arg){
			argumentsCode = nlJoin(
					argumentsCode,
					visit(argument)
			);
		}

		String heapCode = "";
		for(Node argument: n.arg){
			heapCode = nlJoin(
					heapCode,
					//Load argument for the heap
					"lhp", // push hp register on the stack
					"sw",  // store argument for the heap
					"lhp", // push hp register on the stack
					"push 1", // push 1 on the stack
					"add", // add 1 to the hp register
					"shp" // store hp register
			);
		}

		return nlJoin(
				argumentsCode, // create arguments
				heapCode, // manage arguments on the heap
				"push " + (ExecuteVM.MEMSIZE + n.entry.offset), // push class address on the stack
				"lw", // load dispatch table address
				"lhp", // push hp register on the stack
				"sw", // store dispatch table address on the heap
				"lhp", // push hp register object address on the stack
				"lhp", // push hp register on the stack
				"push 1", // push 1 on the stack
				"add", // add 1 to hp register
				"shp" // store in hp register
		);
	}

	@Override
	public String visitNode(EmptyNode n) throws VoidException {
		if (print) printNode(n);
		return "push -1";
	}
}