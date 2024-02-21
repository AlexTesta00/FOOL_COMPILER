package compiler;

import compiler.AST.*;
import compiler.exc.*;
import compiler.lib.*;
import static compiler.TypeRels.*;

/*
* TypeCheckEASTVisitor Class
* It implements a visitor for type checking for the EAST
* The goal of type checking is to ensure that the operations are used with the correct types
* */
public class TypeCheckEASTVisitor extends BaseEASTVisitor<TypeNode,TypeException> {

	TypeCheckEASTVisitor() { super(true); } // enables incomplete tree exceptions 
	TypeCheckEASTVisitor(boolean debug) { super(true,debug); } // enables print for debugging

	/*
	 * ckvisit method for a TypeNode
	 * Check that a type object is visitable (not incomplete)
	 * If not, throw a type exception
	 * Return the type object
	 * */
	private TypeNode ckvisit(TypeNode t) throws TypeException {
		visit(t);
		return t;
	}

	/*
	 * visitNode method to visit a ProgLetInNode and check its type
	 * Visit each declaration and handle exceptions that may be raised during the visit
	 * Then visit the main expression of the program and return its type
	 * */
	@Override
	public TypeNode visitNode(ProgLetInNode n) throws TypeException {
		if (print) printNode(n);
		for (Node dec : n.declist)
			try {
				visit(dec);
			} catch (IncomplException ignored) {
			} catch (TypeException e) {
				ErrorManager.printError(ErrorManager.ERROR_CODE,
						"Type checking error in a declaration: " + e.text);
			}
		return visit(n.exp);
	}

	/*
	 * visitNode method to visit a ProgNode and check its type
	 * Visit the main expression of the program and return its type
	 * */
	@Override
	public TypeNode visitNode(ProgNode n) throws TypeException {
		if (print) printNode(n);
		return visit(n.exp);
	}

	/*
	 * visitNode method to visit a FunNode and check its type
	 * Visit each declaration and handle exceptions that may be raised during the visit
	 * Then visit the expression of the body function and check that its type is a subtype of the return type
	 * If not, throw a type exception
	 * */
	@Override
	public TypeNode visitNode(FunNode n) throws TypeException {
		if (print) printNode(n,n.id);
		for (Node dec : n.declist)
			try {
				visit(dec);
			} catch (IncomplException ignored) {
			} catch (TypeException e) {
				ErrorManager.printError(ErrorManager.ERROR_CODE,
						"Type checking error in a declaration: " + e.text);
			}
		if ( !isSubtype(visit(n.exp), ckvisit(n.retType)) )
			throw new TypeException("Wrong return type for function " + n.id, n.getLine());
		return null;
	}

	/*
	 * visitNode method to visit a VarNode and check its type
	 * Visit the expression and check that its type is a subtype of the variable's declared type
	 * If not, throw a type exception
	 * */
	@Override
	public TypeNode visitNode(VarNode n) throws TypeException {
		if (print) printNode(n,n.id);
		if ( !isSubtype(visit(n.exp), ckvisit(n.getType())) )
			throw new TypeException("Incompatible value for variable " + n.id, n.getLine());
		return null;
	}

	/*
	 * visitNode method to visit a PrintNode and check its type
	 * Visit the expression to print and return its type
	 * */
	@Override
	public TypeNode visitNode(PrintNode n) throws TypeException {
		if (print) printNode(n);
		return visit(n.exp);
	}

	/*
	 * visitNode method to visit an IfNode and check its type
	 * Visit the condition and check that its type is boolean
	 * Then visit the then branch and the else branch
	 * Return the lowest common ancestor between the types of the then and else branches
	 * If not, throw a type exception
	 * */
	@Override
	public TypeNode visitNode(IfNode n) throws TypeException {
		if (print) printNode(n);
		if ( !(isSubtype(visit(n.cond), new BoolTypeNode())) )
			throw new TypeException("Non boolean condition in if", n.getLine());
		TypeNode t = visit(n.th);
		TypeNode e = visit(n.el);
		TypeNode type = lowestCommonAncestor(t, e);
		if (type == null) {
			throw new TypeException("Incompatible types in then-else branches", n.getLine());
		}
		return type;
	}

	/*
	 * visitNode method to visit an EqualNode and check its type
	 * Visit the left and right expressions and check that their types are compatible
	 * Return a new BoolTypeNode indicating the result of the equality comparison operation
	 * If not, throw a type exception
	 * */
	@Override
	public TypeNode visitNode(EqualNode n) throws TypeException {
		if (print) printNode(n);
		TypeNode l = visit(n.left);
		TypeNode r = visit(n.right);
		if ( !(isSubtype(l, r) || isSubtype(r, l)) )
			throw new TypeException("Incompatible types in equal", n.getLine());
		return new BoolTypeNode();
	}

	/*
	 * visitNode method to visit a TimesNode and check its type
	 * Visit the left and right expressions and check that their types are both subtypes of the integer type
	 * Return a new IntTypeNode which represents the type of the output of the multiplication operation
	 * If not, throw a type exception
	 * */
	@Override
	public TypeNode visitNode(TimesNode n) throws TypeException {
		if (print) printNode(n);
		if ( !(isSubtype(visit(n.left), new IntTypeNode())
				&& isSubtype(visit(n.right), new IntTypeNode())) )
			throw new TypeException("Non integers in multiplication", n.getLine());
		return new IntTypeNode();
	}

	/*
	 * visitNode method to visit a PlusNode and check its type
	 * Visit the left and right expressions and check that their types are both subtypes of the integer type
	 * Return a new IntTypeNode which represents the type of the output of the sum operation
	 * If not, throw a type exception
	 * */
	@Override
	public TypeNode visitNode(PlusNode n) throws TypeException {
		if (print) printNode(n);
		if ( !(isSubtype(visit(n.left), new IntTypeNode())
				&& isSubtype(visit(n.right), new IntTypeNode())) )
			throw new TypeException("Non integers in sum", n.getLine());
		return new IntTypeNode();
	}

	/*
	 * visitNode method to visit a CallNode and check its type
	 * Visit the entry associated with the call and return its type
	 * Check that it is a MethodTypeNode or an ArrowTypeNode
	 * Check that the number of params passed in the call matches the number of params expected by the method or function
	 * Then visit the args and check that their types are compatible with the types of params expected
	 * Return the return type of the method or function
	 * If not, throw a type exception
	 * */
	@Override
	public TypeNode visitNode(CallNode n) throws TypeException {
		if (print) printNode(n,n.id);
		TypeNode t = visit(n.entry);

		// If the type is a MethodTypeNode, get the functional type
		if (t instanceof MethodTypeNode methodTypeNode) {
			t = methodTypeNode.fun;
		}
		if ( !(t instanceof ArrowTypeNode at) )
			throw new TypeException("Invocation of a non-function " + n.id, n.getLine());

		// Check if the number of parameters is correct
		if ( !(at.parlist.size() == n.arglist.size()) )
			throw new TypeException("Wrong number of parameters in the invocation of " + n.id,n.getLine());

		// Check if the type of parameters is correct
		for (int i = 0; i < n.arglist.size(); i++)
			if ( !(isSubtype(visit(n.arglist.get(i)), at.parlist.get(i))) )
				throw new TypeException("Wrong type for " + (i + 1) + "-th parameter " +
						"in the invocation of " + n.id,n.getLine());

		return at.ret;
	}

	/*
	 * visitNode method to visit an IdNode and check its type
	 * Visit the entry associated with the identifier and return its type
	 * Check that it is an ArrowTypeNode or a MethodTypeNode or a ClassTypeNode and throw a type exception
	 * Return the type
	 * */
	@Override
	public TypeNode visitNode(IdNode n) throws TypeException {
		if (print) printNode(n,n.id);
		TypeNode t = visit(n.entry);

		// Check if the id is not a ArrowTypeNode or MethodTypeNode or ClassTypeNode
		if (t instanceof ArrowTypeNode)
			throw new TypeException("Wrong usage of function identifier " + n.id, n.getLine());
		if (t instanceof MethodTypeNode)
			throw new TypeException("Wrong usage of method identifier " + n.id, n.getLine());
		if (t instanceof ClassTypeNode)
			throw new TypeException("Wrong usage of class identifier " + n.id, n.getLine());

		return t;
	}

	/*
	 * visitNode method to visit a BoolNode and check its type
	 * Return a new BoolTypeNode representing the boolean type
	 * */
	@Override
	public TypeNode visitNode(BoolNode n) {
		if (print) printNode(n,n.val.toString());
		return new BoolTypeNode();
	}

	/*
	 * visitNode method to visit an IntNode and check its type
	 * Return a new IntTypeNode representing the integer type
	 * */
	@Override
	public TypeNode visitNode(IntNode n) {
		if (print) printNode(n,n.val.toString());
		return new IntTypeNode();
	}

 	/*
	 * visitNode method to visit an ArrowTypeNode and check its type is not incomplete
	 * Visit all parameters of the function or method
	 * Then visit the return type and mark it using the -> symbol
	 * Return null
	 * */
	@Override
	public TypeNode visitNode(ArrowTypeNode n) throws TypeException {
		if (print) printNode(n);
		for (Node par : n.parlist) visit(par);
		visit(n.ret,"->"); // marks return type
		return null;
	}

	/*
	 * visitNode method to visit a BoolTypeNode and check its type is not incomplete
	 * Return null
	 * */
	@Override
	public TypeNode visitNode(BoolTypeNode n) {
		if (print) printNode(n);
		return null;
	}

	/*
	 * visitNode method to visit a IntTypeNode and check its type is not incomplete
	 * Return null
	 * */
	@Override
	public TypeNode visitNode(IntTypeNode n) {
		if (print) printNode(n);
		return null;
	}

	/*
	 * visitNode method to visit a STentry object and check that is visitable (not incomplete)
	 * Return the type object contained in the STentry object
	 * If not, throw an exception
	 * */
	@Override
	public TypeNode visitSTentry(STentry entry) throws TypeException {
		if (print) printSTentry("type");
		return ckvisit(entry.type); 
	}

	/*----------------------------------------------OPERATOR EXTENSION------------------------------------------------*/

	/*
	 * visitNode method to visit a LessEqualNode and check its type
	 * Visit the left and right expressions and check that their types are both subtypes of the integer type
	 * Return a new BoolTypeNode indicating the result of the operation
	 * If not, throw a type exception
	 * */
	@Override
	public TypeNode visitNode(LessEqualNode n) throws TypeException {
		if (print) printNode(n);
		if ( !(isSubtype(visit(n.left), new IntTypeNode())
				&& isSubtype(visit(n.right), new IntTypeNode())) ) {
			throw new TypeException("Not integer parameter beside <= operand", n.getLine());
		}
		return new BoolTypeNode();
	}

	/*
	 * visitNode method to visit a GreaterEqualNode and check its type
	 * Visit the left and right expressions and check that their types are both subtypes of the integer type
	 * Return a new BoolTypeNode indicating the result of the operation
	 * If not, throw a type exception
	 * */
	@Override
	public TypeNode visitNode(GreaterEqualNode n) throws TypeException {
		if (print) printNode(n);
		if ( !(isSubtype(visit(n.left), new IntTypeNode())
				&& isSubtype(visit(n.right), new IntTypeNode())) ) {
			throw new TypeException("Not integer parameter beside >= operand", n.getLine());
		}
		return new BoolTypeNode();
	}

	/*
	 * visitNode method to visit an OrNode and check its type
	 * Visit the left and right expressions and check that their types are both subtypes of the boolean type
	 * Return a new BoolTypeNode indicating the result of the logical operation OR
	 * If not, throw a type exception
	 * */
	@Override
	public TypeNode visitNode(OrNode n) throws TypeException {
		if (print) printNode(n);
		if ( !(isSubtype(visit(n.left), new BoolTypeNode())
				&& isSubtype(visit(n.right), new BoolTypeNode())) ) {
			throw new TypeException("Not booleans beside || symbol", n.getLine());
		}
		return new BoolTypeNode();
	}

	/*
	 * visitNode method to visit an AndNode and check its type
	 * Visit the left and right expressions and check that their types are both subtypes of the boolean type
	 * Return a new BoolTypeNode indicating the result of the logical operation AND
	 * If not, throw a type exception
	 * */
	@Override
	public TypeNode visitNode(AndNode n) throws TypeException {
		if (print) printNode(n);
		if ( !(isSubtype(visit(n.left), new BoolTypeNode())
				&& isSubtype(visit(n.right), new BoolTypeNode())) ) {
			throw new TypeException("Not booleans beside && symbol", n.getLine());
		}
		return new BoolTypeNode();
	}

	/*
	 * visitNode method to visit a DivNode and check its type
	 * Visit the left and right expressions and check that their types are both subtypes of the integer type
	 * Return a new IntTypeNode indicating the result of the division operation
	 * If not, throw a type exception
	 * */
	@Override
	public TypeNode visitNode(DivNode n) throws TypeException {
		if (print) printNode(n);
		if ( !(isSubtype(visit(n.left), new IntTypeNode())
				&& isSubtype(visit(n.right), new IntTypeNode())) ) {
			throw new TypeException("Not integers beside / operation", n.getLine());
		}
		return new IntTypeNode();
	}

	/*
	 * visitNode method to visit a MinusNode and check its type
	 * Visit the left and right expressions and check that their types are both subtypes of the integer type
	 * Return a new IntTypeNode indicating the result of the subtraction operation
	 * If not, throw a type exception
	 * */
	@Override
	public TypeNode visitNode(MinusNode n) throws TypeException {
		if (print) printNode(n);
		if ( !(isSubtype(visit(n.left), new IntTypeNode())
				&& isSubtype(visit(n.right), new IntTypeNode())) ) {
			throw new TypeException("Not integers beside - operation", n.getLine());
		}
		return new IntTypeNode();
	}

	/*
	 * visitNode method to visit a NotNode and check its type
	 * Visit the expression and check that its type is subtype of the boolean type
	 * Return a new BoolTypeNode indicating the result of the logical operation NOT
	 * If not, throw a type exception
	 * */
	@Override
	public TypeNode visitNode(NotNode n) throws TypeException {
		if (print) printNode(n);
		if ( !(isSubtype(visit(n.exp), new BoolTypeNode())) ) {
			throw new TypeException("Not boolean beside ! symbol", n.getLine());
		}
		return new BoolTypeNode();
	}

	/*----------------------------------------------CLASS EXTENSION---------------------------------------------------*/

	/*
	 * visitNode method to visit a ClassNode and check its type
	 * If the class has a superclass, add it as super type in TypeRels Map
	 * Visit all methods of the class and check their types match the types specified when the method was declared
	 * Check that fields and methods defined in the current class are compatible with those defined in the superclass
	 * If not, throw a type exception
	 * Return null
	 * */
	@Override
	public TypeNode visitNode(ClassNode n) throws TypeException {
		if (print) printNode(n,n.id);

		// If the class has a superclass, add it as super type in TypeRels Map
		if (n.superId != null) {
			superType.put(n.id, n.superId);
		}

		// Visit all methods and check if they have a correct type
		n.methodNodeList.forEach((method) -> {
			try {
				visit(method);
			} catch (TypeException e) {
				ErrorManager.printError(ErrorManager.ERROR_CODE,
						"Type checking error in a class declaration: " + e.text);
			}
		});

		// Check if the class has a superclass and if the corresponding entry is present in the ST of the current class
		if (n.superId == null || n.superSTentry == null) {
			return null;
		}

		// Get the type of the current class
		ClassTypeNode classTypeNode = (ClassTypeNode) n.getType();

		// Get the type of the superclass
		ClassTypeNode superTypeNode = (ClassTypeNode) n.superSTentry.type;

		// Check if fields and methods of the current class are in correct position and have a correct type
		for (FieldNode field : n.fieldNodeList) {
			if ((-field.offset - 1) < superTypeNode.allFields.size() &&
					!isSubtype(classTypeNode.allFields.get(-field.offset - 1),
					superTypeNode.allFields.get(-field.offset - 1))) {
				throw new TypeException("Wrong type for field " + field.id, field.getLine());
			}
		}
		for (MethodNode method : n.methodNodeList) {
			if (method.offset < superTypeNode.allFields.size() &&
					!isSubtype(classTypeNode.allMethods.get(method.offset),
							superTypeNode.allMethods.get(method.offset))) {
				throw new TypeException("Wrong type for method " + method.id, method.getLine());
			}
		}

		return null;
	}

	/*
	 * visitNode method to visit a FieldNode and check its type
	 *
	 * */
	@Override
	public TypeNode visitNode(FieldNode n){
		if (print) printNode(n);
		return null;
	}

	/*
	 * visitNode method to visit a MethodNode and check its type
	 * Visit each declaration of method in the class and handle exceptions that may be raised during the visit
	 * Check that the type of the method expression is a subtype of the method's return type
	 * Return null
	 * */
	@Override
	public TypeNode visitNode(MethodNode n) throws TypeException {
		if (print) printNode(n,n.id);

		for (Node dec : n.decList)
			try {
				visit(dec);
			} catch (IncomplException ignored) {
			} catch (TypeException e) {
				ErrorManager.printError(ErrorManager.WARNING_CODE,
						"Type checking error in a method declaration: " + e.text);
			}

		// Check if the type of the expression is a subtype of the return type
		if ( !isSubtype(visit(n.exp),ckvisit(n.retType)) )
			throw new TypeException("Wrong return type for function " + n.id, n.getLine());

		return null;
	}

	/*
	 * visitNode method to visit a ClassCallNode and check its type
	 * Visit the entry associated with the method and return its type
	 * Check that it is a method type or an arrow type
	 * Check that the number of params passed in the call matches the number of params expected by the method or function
	 * Then visit the args and check that their types are compatible with the types of params expected
	 * Return the return type of the method or function
	 * If not, throw a type exception
	 * */
	@Override
	public TypeNode visitNode(ClassCallNode n) throws TypeException {
		if (print) printNode(n,n.id);
		TypeNode t = visit(n.methodEntry);

		// Check if the type is MethodTypeNode and get the functional type
		if (t instanceof MethodTypeNode methodTypeNode) {
			t = methodTypeNode.fun;
		}
		// Check if the type is an ArrowTypeNode
		if ( !(t instanceof ArrowTypeNode at) )
			throw new TypeException("Invocation of a non-function " + n.id, n.getLine());

		// Check if the number of parameters is correct
        if ( !(at.parlist.size() == n.arg.size()) )
			throw new TypeException("Wrong number of parameters in the invocation of " + n.id, n.getLine());

		// Check if the type of parameters is correct
		for (int i = 0; i < n.arg.size(); i++)
			if ( !(isSubtype(visit(n.arg.get(i)),at.parlist.get(i))) )
				throw new TypeException("Wrong type for " + (i + 1) + "-th parameter " +
						"in the invocation of " + n.id, n.getLine());

		return at.ret;
	}

	/*
	 * visitNode method to visit a NewNode and check its type
	 * Visit the class entry and return its type
	 * Check that it is a class type
	 * Check that the number of parameters is correct and that their types are correct
	 * Return a new RefTypeNode with the NewNode identifier
	 * If not, throw a type exception
	 * */
	@Override
	public TypeNode visitNode(NewNode n) throws TypeException {
		if (print) printNode(n,n.id);
		TypeNode t = visit(n.entry);

		// Check if the type is ClassTypeNode
		if ( !(t instanceof  ClassTypeNode classTypeNode) ) {
			throw new TypeException("Invocation of a non-class constructor " + n.id, n.getLine());
		}

		// Check if the number of parameters in class constructor is correct
		if ( !(classTypeNode.allFields.size() == n.arg.size()) )
			throw new TypeException("Wrong number of parameters in the invocation of class constructor "
					+ n.id, n.getLine());

		// Check if the type of parameters in class constructor is correct
		for (int i = 0; i < n.arg.size(); i++)
			if ( !(isSubtype(visit(n.arg.get(i)), classTypeNode.allFields.get(i))) )
				throw new TypeException("Wrong type for " + (i + 1) + "-th parameter " +
						"in the invocation of class constructor " + n.id, n.getLine());

		return new RefTypeNode(n.id);
	}

	/*
	 * visitNode method to visit an EmptyNode and check its type
	 * Return a new EmptyTypeNode
	 * */
	@Override
	public TypeNode visitNode(EmptyNode n) throws TypeException {
		return new EmptyTypeNode();
	}

	/*
	 * visitNode method to visit a ClassTypeNode and check its type
	 * Return null
	 * */
	@Override
	public TypeNode visitNode(ClassTypeNode n){
		if (print) printNode(n);
		return null;
	}

	/*
	 * visitNode method to visit a MethodTypeNode and check its type
	 * Return null
	 * */
	@Override
	public TypeNode visitNode(MethodTypeNode n){
		if (print) printNode(n);
		return null;
	}

	/*
	 * visitNode method to visit a RefTypeNode and check its type
	 * Return null
	 * */
	@Override
	public TypeNode visitNode(RefTypeNode n){
		if (print) printNode(n);
		return null;
	}

	/*
	 * visitNode method to visit a EmptyTypeNode and check its type
	 * Return null
	 * */
	@Override
	public TypeNode visitNode(EmptyTypeNode n){
		if (print) printNode(n);
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