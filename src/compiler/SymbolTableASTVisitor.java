package compiler;

import java.util.*;
import compiler.AST.*;
import compiler.exc.*;
import compiler.lib.*;

public class SymbolTableASTVisitor extends BaseASTVisitor<Void,VoidException> {

	private final Map<String, Map<String, STentry>> classTable = new HashMap<>(); //Map<String, STentry> is the virtual table
	private final List<Map<String, STentry>> symTable = new ArrayList<>();
	private int nestingLevel=0; // current nesting level
	private int decOffset=-2; // counter for offset of local declarations at current nesting level 
	int stErrors=0;

	SymbolTableASTVisitor() {}
	SymbolTableASTVisitor(boolean debug) {super(debug);} // enables print for debugging

	private STentry stLookup(String id) {
		int j = nestingLevel;
		STentry entry = null;
		while (j >= 0 && entry == null) 
			entry = symTable.get(j--).get(id);	
		return entry;
	}

	@Override
	public Void visitNode(ProgLetInNode n) {
		if (print) printNode(n);
		Map<String, STentry> hm = new HashMap<>();
		symTable.add(hm);
	    for (Node dec : n.declist) visit(dec);
		visit(n.exp);
		symTable.remove(0);
		return null;
	}

	@Override
	public Void visitNode(ProgNode n) {
		if (print) printNode(n);
		visit(n.exp);
		return null;
	}
	
	@Override
	public Void visitNode(FunNode n) {
		if (print) printNode(n);
		Map<String, STentry> hm = symTable.get(nestingLevel);
		List<TypeNode> parTypes = new ArrayList<>();  
		for (ParNode par : n.parlist) parTypes.add(par.getType()); 
		STentry entry = new STentry(nestingLevel, new ArrowTypeNode(parTypes,n.retType),decOffset--);
		//inserimento di ID nella symtable
		if (hm.put(n.id, entry) != null) {
			//System.out.println("Fun id " + n.id + " at line "+ n.getLine() +" already declared");
			ErrorManager.printError(ErrorManager.ERROR_CODE,
								"Fun id " + n.id + " at line "+ n.getLine() +" already declared");
			stErrors++;
		} 
		//creare una nuova hashmap per la symTable
		nestingLevel++;
		Map<String, STentry> hmn = new HashMap<>();
		symTable.add(hmn);
		int prevNLDecOffset=decOffset; // stores counter for offset of declarations at previous nesting level 
		decOffset=-2;
		
		int parOffset=1;
		for (ParNode par : n.parlist)
			if (hmn.put(par.id, new STentry(nestingLevel,par.getType(),parOffset++)) != null) {
				//System.out.println("Par id " + par.id + " at line "+ n.getLine() +" already declared");
				ErrorManager.printError(ErrorManager.ERROR_CODE,
						"Par id " + par.id + " at line "+ n.getLine() +" already declared");
				stErrors++;
			}
		for (Node dec : n.declist) visit(dec);
		visit(n.exp);
		//rimuovere la hashmap corrente poiche' esco dallo scope               
		symTable.remove(nestingLevel--);
		decOffset=prevNLDecOffset; // restores counter for offset of declarations at previous nesting level 
		return null;
	}
	
	@Override
	public Void visitNode(VarNode n) {
		if (print) printNode(n);
		visit(n.exp);
		Map<String, STentry> hm = symTable.get(nestingLevel);
		STentry entry = new STentry(nestingLevel,n.getType(),decOffset--);
		//inserimento di ID nella symtable
		if (hm.put(n.id, entry) != null) {
			//System.out.println("Var id " + n.id + " at line "+ n.getLine() +" already declared");
			ErrorManager.printError(ErrorManager.ERROR_CODE,
					"Var id " + n.id + " at line "+ n.getLine() +" already declared");
			stErrors++;
		}
		return null;
	}

	@Override
	public Void visitNode(PrintNode n) {
		if (print) printNode(n);
		visit(n.exp);
		return null;
	}

	@Override
	public Void visitNode(IfNode n) {
		if (print) printNode(n);
		visit(n.cond);
		visit(n.th);
		visit(n.el);
		return null;
	}
	
	@Override
	public Void visitNode(EqualNode n) {
		if (print) printNode(n);
		visit(n.left);
		visit(n.right);
		return null;
	}
	
	@Override
	public Void visitNode(TimesNode n) {
		if (print) printNode(n);
		visit(n.left);
		visit(n.right);
		return null;
	}
	
	@Override
	public Void visitNode(PlusNode n) {
		if (print) printNode(n);
		visit(n.left);
		visit(n.right);
		return null;
	}

	@Override
	public Void visitNode(CallNode n) {
		if (print) printNode(n);
		STentry entry = stLookup(n.id);
		if (entry == null) {
			//System.out.println("Fun id " + n.id + " at line "+ n.getLine() + " not declared");
			ErrorManager.printError(ErrorManager.ERROR_CODE,
					"Fun id " + n.id + " at line "+ n.getLine() + " not declared");
			stErrors++;
		} else {
			n.entry = entry;
			n.nl = nestingLevel;
		}
		for (Node arg : n.arglist) visit(arg);
		return null;
	}

	@Override
	public Void visitNode(IdNode n) {
		if (print) printNode(n);
		STentry entry = stLookup(n.id);
		if (entry == null) {
			//System.out.println("Var or Par id " + n.id + " at line "+ n.getLine() + " not declared");
			ErrorManager.printError(ErrorManager.ERROR_CODE,
					"Var or Par id " + n.id + " at line "+ n.getLine() + " not declared");
			stErrors++;
		} else {
			n.entry = entry;
			n.nl = nestingLevel;
		}
		return null;
	}

	@Override
	public Void visitNode(BoolNode n) {
		if (print) printNode(n, n.val.toString());
		return null;
	}

	@Override
	public Void visitNode(IntNode n) {
		if (print) printNode(n, n.val.toString());
		return null;
	}

	/*----------------------------------------------OPERATOR EXTENSION------------------------------------------------*/
	@Override
	public Void visitNode(LessEqualNode n) {
		if (print) printNode(n);
		visit(n.left);
		visit(n.right);
		return null;
	}

	@Override
	public Void visitNode(GreaterEqualNode n) {
		if (print) printNode(n);
		visit(n.left);
		visit(n.right);
		return null;
	}

	@Override
	public Void visitNode(OrNode n) {
		if (print) printNode(n);
		visit(n.left);
		visit(n.right);
		return null;
	}

	@Override
	public Void visitNode(AndNode n) {
		if (print) printNode(n);
		visit(n.left);
		visit(n.right);
		return null;
	}

	@Override
	public Void visitNode(DivNode n) {
		if (print) printNode(n);
		visit(n.left);
		visit(n.right);
		return null;
	}

	@Override
	public Void visitNode(MinusNode n) {
		if (print) printNode(n);
		visit(n.left);
		visit(n.right);
		return null;
	}

	@Override
	public Void visitNode(NotNode n) {
		if (print) printNode(n);
		visit(n.exp);
		return null;
	}

	/*----------------------------------------------CLASS EXTENSION---------------------------------------------------*/

	@Override
	public Void visitNode(ClassNode n) throws VoidException {
		if (print) printNode(n, n.id);

		//TODO: For ereditareties

		//Create a classTypeNode variable, that's represent the type of the class
		ClassTypeNode classTypeNode = new ClassTypeNode(new ArrayList<>(), new ArrayList<>());

		//Create the entry for the class
		STentry sTentry = new STentry(0, classTypeNode, decOffset--);

		//Offset for visit the fields in class
		int classFieldsOffset = -1;

		//Container of the fields names
		Set<String> fieldsContainer = new HashSet<>();

		//Container of method for the class
		List<String> methodContainer = new ArrayList<>();

		//Utils for store old decOffset
		int oldStepDecOffset = 0;

		//Check if the class is already declared in global scope (at nesting level 0)
		if(symTable.get(nestingLevel).containsKey(n.id)){
			ErrorManager.printError(ErrorManager.WARNING_CODE,
					"Class: " + n.id + " at line: " + n.getLine() + " is already declared");
		}

		//Add the class to the class and symbol table
		Map<String, STentry> virtualClassTable = new HashMap<>();
		this.classTable.put(n.id, virtualClassTable);
		this.symTable.add(virtualClassTable);

		//This is it because the fields and method for the scope in class is a nesting level one
		nestingLevel++;

		/* Fields in class */
		//Add the fields in the fieldsContainer and visit all fields
		n.fieldNodeList.forEach((field) ->{
			String fieldId = field.id;
			if (fieldsContainer.contains(fieldId)) {
				ErrorManager.printError(ErrorManager.WARNING_CODE,
						"Field: " + fieldId + " at line: " + n.getLine() + " is already declared");
				stErrors++;
			}else{
				fieldsContainer.add(fieldId);
			}

			//Visit the field and add in the virtual table
			visit(field);

			STentry fieldEntry = new STentry(nestingLevel, field.getType(), classFieldsOffset);
			classTypeNode.allFields.add(-fieldEntry.offset - 1, fieldEntry.type);
			virtualClassTable.put(fieldId, fieldEntry);
		});

		/* Method in class */
		oldStepDecOffset = decOffset;
		decOffset = 0;
		n.methodNodeList.forEach((method) -> {
			//Chek if the method is already declared
			String methodId = method.id;
			if (methodContainer.contains(methodId)) {
				ErrorManager.printError(ErrorManager.WARNING_CODE,
						"Method: " + methodId + " at line: " + n.getLine() + " is already declared");
				stErrors++;
			}else{
				methodContainer.add(methodId);
			}

			//Visit the method and add in the virtual table
			visit(method);
			classTypeNode.allMethods.add(method.offset,
										((MethodTypeNode) symTable.get(nestingLevel).get(methodId).type).fun);
		});
		//Return to the old offset
		decOffset = oldStepDecOffset;
		symTable.remove(nestingLevel--);

		return null;
	}

	@Override
	public Void visitNode(MethodNode n) throws VoidException {
		return null;
	}

	@Override
	public Void visitNode(ClassCallNode node) throws VoidException {
		return null;
	}

	@Override
	public Void visitNode(NewNode n) throws VoidException {
		return null;
	}

	@Override
	public Void visitNode(EmptyNode n) throws VoidException {
		return null;
	}

	/*----------------------------------------------ERROR MANAGER------------------------------------------------*/
	private static class ErrorManager{
		public static int ERROR_CODE = 1;
		public static int WARNING_CODE = 0;
		public static final String ANSI_RESET = "\u001B[0m";
		public static final String ANSI_RED = "\u001B[31m";
		public static final String ANSI_YELLOW = "\u001B[33m";
		private static void printError(int code, String msg){
			if(code == ERROR_CODE){
				System.out.println(ANSI_RED + msg + ANSI_RESET);
			}else{
				System.out.println(ANSI_YELLOW + msg + ANSI_RESET);
			}
		}
	}
}