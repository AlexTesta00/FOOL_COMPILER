package compiler;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

import compiler.AST.*;
import compiler.exc.*;
import compiler.lib.*;

public class SymbolTableASTVisitor extends BaseASTVisitor<Void,VoidException> {

	private final int GLOBAL_SCOPE = 0;
	private final Map<String, Map<String, STentry>> classTable = new HashMap<>(); //Map<String, STentry> is the virtual table
	private final List<Map<String, STentry>> symTable = new ArrayList<>();
	private int nestingLevel = 0; // current nesting level
	private int decOffset = -2; // counter for offset of local declarations at current nesting level
	int stErrors = 0;

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
			n.nestingLevel = nestingLevel;
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

		//Create a classTypeNode variable, that's represent the type of the class
		ClassTypeNode classTypeNode = new ClassTypeNode();

		//Temporary classTypeNode for the super class
		ClassTypeNode temporaryClassTypeNode = new ClassTypeNode();

		//Check if exist the super class
		if(n.superId != null){
			//Check if the super class is already declared
			if(classTable.containsKey(n.superId)){
				STentry superParentSTEntry = symTable.get(GLOBAL_SCOPE).get(n.superId);
				ClassTypeNode superParentTypeNode = (ClassTypeNode) superParentSTEntry.type;
				temporaryClassTypeNode = new ClassTypeNode(superParentTypeNode);
				n.superSTentry = superParentSTEntry;
			}else{
				ErrorManager.printError(ErrorManager.WARNING_CODE,
						"Class: " + n.superId + " at line: " + n.getLine() + " isn't declared");
				stErrors++;
			}
		}

		//Add the parent class in the current class
		classTypeNode = temporaryClassTypeNode;

		//This is for the checking of the class and subclass
		n.setType(classTypeNode);

		//Create the entry for the class
		STentry sTentry = new STentry(GLOBAL_SCOPE, classTypeNode, decOffset--);

		//Put the entry in the global scope for checking duplicate
		Map<String, STentry> globalSymbolTable = symTable.get(GLOBAL_SCOPE);
		if(globalSymbolTable.put(n.id, sTentry) != null){
			ErrorManager.printError(ErrorManager.WARNING_CODE,
					"Class: " + n.id + " at line: " + n.getLine() + " is already declared");
			stErrors++;
		}

		//Offset for visit the fields in class
		AtomicInteger classFieldsOffset = new AtomicInteger(-1);

		//Container of the fields names
		Set<String> fieldsContainer = new HashSet<>();

		//Container of method for the class
		Set<String> methodContainer = new HashSet<>();

		//Utils for store old decOffset
		int oldStepDecOffset = 0;

		//Add the class to the class and symbol table
		Map<String, STentry> virtualClassTable = new HashMap<>();
		if(n.superId != null){
			virtualClassTable.putAll(classTable.get(n.superId)); //Put all is in super class
		}

		this.classTable.put(n.id, virtualClassTable);
		this.symTable.add(virtualClassTable);

		//This is it because the fields and method for the scope in class is a nesting level one
		nestingLevel++;

		//Compute the field offset
		if(n.superId != null){
			ClassTypeNode superType = (ClassTypeNode) symTable.get(GLOBAL_SCOPE).get(n.superId).type;
			classFieldsOffset = new AtomicInteger(-superType.allFields.size() - 1);
		}

		/* Fields in class */
		//Add the fields in the fieldsContainer and visit all fields
		for(FieldNode field: n.fieldNodeList){
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

			STentry fieldEntry = new STentry(nestingLevel, field.getType(), classFieldsOffset.getAndDecrement());
			//Check the super class
			if(n.superId != null && virtualClassTable.containsKey(fieldId)){
				STentry ovverrideField = virtualClassTable.get(fieldId);
				if(ovverrideField.type instanceof  MethodTypeNode){
					ErrorManager.printError(ErrorManager.ERROR_CODE,
							"Cannot override method: " + fieldId + " with a field: " + fieldId);
					stErrors++;
				}else{
					fieldEntry = new STentry(nestingLevel, field.getType(), ovverrideField.offset);
					classTypeNode.allFields.set(-fieldEntry.offset - 1, fieldEntry.type);
				}
			}else{
				classTypeNode.allFields.add(-fieldEntry.offset - 1, fieldEntry.type);
			}

			//Add field to the virtual table
			virtualClassTable.put(fieldId, fieldEntry);
			field.offset = fieldEntry.offset;

		}


		/* Method in class */
		oldStepDecOffset = decOffset;
		decOffset = 0;

		//Check the super class
		if(n.superId != null){
			ClassTypeNode superClassType = (ClassTypeNode) symTable.get(GLOBAL_SCOPE).get(n.superId).type;
			decOffset = superClassType.allMethods.size();
		}

		for(MethodNode method: n.methodNodeList) {
			//Chek if the method is already declared
			String methodId = method.id;
			if (methodContainer.contains(methodId)) {
				ErrorManager.printError(ErrorManager.WARNING_CODE,
						"Method: " + methodId + " at line: " + n.getLine() + " is already declared");
				stErrors++;
			} else {
				methodContainer.add(methodId);
			}

			//Visit the method and add in the virtual table
			visit(method);
			classTypeNode.allMethods.add(method.offset,
					((MethodTypeNode) symTable.get(nestingLevel).get(methodId).type).fun);
		}

		//Return to the old offset
		decOffset = oldStepDecOffset;
		symTable.remove(nestingLevel--);

		return null;
	}

	/*
	* Visit the field in a class
	* */
	@Override
	public Void visitNode(FieldNode n){
		if(print) printNode(n);
		return null;
	}

	@Override
	public Void visitNode(MethodNode n) throws VoidException {
		if(print) printNode(n);

		//The local symbolTable
		Map<String, STentry> localTable = symTable.get(nestingLevel);

		//Get the param of the method
		List<TypeNode> params = n.parList.stream().map(ParNode::getType).toList();

		//Create a new MethodTypeNode to add this at the symbol table
		MethodTypeNode methodTypeNode = new MethodTypeNode(new ArrowTypeNode(params, n.retType));

		//Create the STentry to adding at the symbol table
		STentry sTentry = new STentry(nestingLevel, methodTypeNode, decOffset++);

		//Check if is present a super class
		if(localTable.containsKey(n.id)){
			var ovverrideMethod = localTable.get(n.id);
			if(ovverrideMethod != null && ovverrideMethod.type instanceof MethodTypeNode){
				sTentry = new STentry(nestingLevel, methodTypeNode, ovverrideMethod.offset);
				decOffset--;
			}else{
				ErrorManager.printError(ErrorManager.ERROR_CODE,
						"Cannot override a class method: " + n.id);
				stErrors++;
			}
		}

		//Update offset and put the entry in local symbol table
		n.offset = sTentry.offset;
		localTable.put(n.id, sTentry);

		//Create a new table for the symbol table
		Map<String, STentry> currentMethodTable = new HashMap<>();

		//Update the nesting level
		nestingLevel++;

		//Adding currentMethodTable to the symbol table
		symTable.add(currentMethodTable);

		//Adjoust the offset
		int oldStep = decOffset;
		decOffset = -2;

		AtomicInteger parOffset = new AtomicInteger(1);

		//For all parameters, create the STentry and add it to the symbol table.
		//If the parameter is alredy exist in the symbol table, manage error
		n.parList.forEach((param) -> {
			STentry parEntry = new STentry(nestingLevel, param.getType(), parOffset.getAndIncrement());
			if(currentMethodTable.put(param.id, parEntry) != null){
				ErrorManager.printError(ErrorManager.WARNING_CODE,
						"Par: " + param.id + " at line: " + n.getLine() + " already declared");
				stErrors++;
			}
		});

		//Visit all node
		n.decList.forEach(this::visit);
		visit(n.exp);

		//Restore the symbol table
		symTable.remove(nestingLevel--);
		decOffset = oldStep;

		return null;
	}

	@Override
	public Void visitNode(ClassCallNode n) throws VoidException {
		if(print) printNode(n);

		STentry sTentry = stLookup(n.id);

		//Check if the id of class is present in the symbolTable
		if(sTentry == null){
			ErrorManager.printError(ErrorManager.ERROR_CODE, "Id: " + n.id + "wasn't declared");
			stErrors++;
			//If the class is in the symbol table, check the RefType
		}else if(sTentry.type instanceof RefTypeNode){
			n.entry = sTentry;
			n.nestingLevel = nestingLevel;
			Map<String, STentry> virtualTable = this.classTable.get(((RefTypeNode) sTentry.type).id);
			//If the class is RefType, check it's in virtuale table
			if(virtualTable.containsKey(n.methodId)){
				n.methodEntry = virtualTable.get(n.methodId);
			}else{
				ErrorManager.printError(ErrorManager.WARNING_CODE,
						"Id: " + n.id + " at line: " + n.getLine() + " has no method: " + n.methodId);
				stErrors++;
			}
		}else{
			ErrorManager.printError(ErrorManager.WARNING_CODE,
					"Id: " + n.id + " at line: " + n.getLine() + " isn't RefType");
			stErrors++;
		}
		//Visit all arguments
		n.arg.forEach(this::visit);

		return null;
	}

	@Override
	public Void visitNode(NewNode n) throws VoidException {
		if(print) printNode(n);

		//Check if the class exist
		if(!this.classTable.containsKey(n.id)){
			ErrorManager.printError(ErrorManager.ERROR_CODE,
					"Class: " + n.id + " was not declared");
			stErrors++;
		}

		//If the class exist, set the entry
		n.entry = symTable.get(GLOBAL_SCOPE).get(n.id);
		n.nestingLevel = nestingLevel;

		//Visit all arguments
		n.arg.forEach(this::visit);
		return null;
	}

	@Override
	public Void visitNode(EmptyNode n) throws VoidException {
		if(print) printNode(n);
		return null;
	}

	@Override
	public Void visitNode(ClassTypeNode n) throws VoidException {
		if(print) printNode(n);
		return null;
	}

	@Override
	public Void visitNode(MethodTypeNode n) throws VoidException {
		if(print) printNode(n);
		return null;
	}

	@Override
	public Void visitNode(RefTypeNode n) throws VoidException {
		if(print) printNode(n);
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