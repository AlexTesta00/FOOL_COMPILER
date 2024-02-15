package compiler;

import compiler.AST.*;
import compiler.exc.*;
import compiler.lib.*;
import static compiler.TypeRels.*;

//visitNode(n) fa il type checking di un Node n e ritorna:
//- per una espressione, il suo tipo (oggetto BoolTypeNode o IntTypeNode)
//- per una dichiarazione, "null"; controlla la correttezza interna della dichiarazione
//(- per un tipo: "null"; controlla che il tipo non sia incompleto) 
//
//visitSTentry(s) ritorna, per una STentry s, il tipo contenuto al suo interno
public class TypeCheckEASTVisitor extends BaseEASTVisitor<TypeNode,TypeException> {

	TypeCheckEASTVisitor() { super(true); } // enables incomplete tree exceptions 
	TypeCheckEASTVisitor(boolean debug) { super(true,debug); } // enables print for debugging

	//checks that a type object is visitable (not incomplete) 
	private TypeNode ckvisit(TypeNode t) throws TypeException {
		visit(t);
		return t;
	} 
	
	@Override
	public TypeNode visitNode(ProgLetInNode n) throws TypeException {
		if (print) printNode(n);
		for (Node dec : n.declist)
			try {
				visit(dec);
			} catch (IncomplException e) { 
			} catch (TypeException e) {
				System.out.println("Type checking error in a declaration: " + e.text);
			}
		return visit(n.exp);
	}

	@Override
	public TypeNode visitNode(ProgNode n) throws TypeException {
		if (print) printNode(n);
		return visit(n.exp);
	}

	@Override
	public TypeNode visitNode(FunNode n) throws TypeException {
		if (print) printNode(n,n.id);
		for (Node dec : n.declist)
			try {
				visit(dec);
			} catch (IncomplException e) { 
			} catch (TypeException e) {
				System.out.println("Type checking error in a declaration: " + e.text);
			}
		if ( !isSubtype(visit(n.exp),ckvisit(n.retType)) ) 
			throw new TypeException("Wrong return type for function " + n.id,n.getLine());
		return null;
	}

	@Override
	public TypeNode visitNode(VarNode n) throws TypeException {
		if (print) printNode(n,n.id);
		if ( !isSubtype(visit(n.exp),ckvisit(n.getType())) )
			throw new TypeException("Incompatible value for variable " + n.id,n.getLine());
		return null;
	}

	@Override
	public TypeNode visitNode(PrintNode n) throws TypeException {
		if (print) printNode(n);
		return visit(n.exp);
	}

	@Override
	public TypeNode visitNode(IfNode n) throws TypeException {
		if (print) printNode(n);
		if ( !(isSubtype(visit(n.cond), new BoolTypeNode())) )
			throw new TypeException("Non boolean condition in if",n.getLine());

		TypeNode t = visit(n.th);
		TypeNode e = visit(n.el);

		TypeNode type = lowestCommonAncestor(t, e);
		if(type == null){
			throw new TypeException("Incompatible types in then-else branches",n.getLine());
		}

		return type;
	}

	@Override
	public TypeNode visitNode(EqualNode n) throws TypeException {
		if (print) printNode(n);
		TypeNode l = visit(n.left);
		TypeNode r = visit(n.right);
		if ( !(isSubtype(l, r) || isSubtype(r, l)) )
			throw new TypeException("Incompatible types in equal",n.getLine());
		return new BoolTypeNode();
	}

	@Override
	public TypeNode visitNode(TimesNode n) throws TypeException {
		if (print) printNode(n);
		if ( !(isSubtype(visit(n.left), new IntTypeNode())
				&& isSubtype(visit(n.right), new IntTypeNode())) )
			throw new TypeException("Non integers in multiplication",n.getLine());
		return new IntTypeNode();
	}

	@Override
	public TypeNode visitNode(PlusNode n) throws TypeException {
		if (print) printNode(n);
		if ( !(isSubtype(visit(n.left), new IntTypeNode())
				&& isSubtype(visit(n.right), new IntTypeNode())) )
			throw new TypeException("Non integers in sum",n.getLine());
		return new IntTypeNode();
	}

	@Override
	public TypeNode visitNode(CallNode n) throws TypeException {
		if (print) printNode(n,n.id);

		TypeNode t = visit(n.entry);

		//If is a method, get the functional type
		if(t instanceof  MethodTypeNode methodTypeNode){
			t = methodTypeNode.fun;
		}
		if ( !(t instanceof ArrowTypeNode at) )
			throw new TypeException("Invocation of a non-function " + n.id, n.getLine());

		//Check if the number of parameters is correct
		if ( !(at.parlist.size() == n.arglist.size()) )
			throw new TypeException("Wrong number of parameters in the invocation of " + n.id,n.getLine());

		//Check if the type of parameters is correct
		for (int i = 0; i < n.arglist.size(); i++)
			if ( !(isSubtype(visit(n.arglist.get(i)),at.parlist.get(i))) )
				throw new TypeException("Wrong type for " + ( i + 1) + "-th parameter in the invocation of " + n.id,n.getLine());
		return at.ret;
	}

	@Override
	public TypeNode visitNode(IdNode n) throws TypeException {
		if (print) printNode(n,n.id);
		TypeNode t = visit(n.entry);

		//Check if the id isn't a ArrowTypeNode or MethodTypeNode or ClassTypeNode
		if (t instanceof ArrowTypeNode)
			throw new TypeException("Wrong usage of function identifier " + n.id,n.getLine());

		if (t instanceof MethodTypeNode)
			throw new TypeException("Wrong usage of method identifier " + n.id,n.getLine());

		if (t instanceof ClassTypeNode)
			throw new TypeException("Wrong usage of class identifier " + n.id,n.getLine());

		return t;
	}

	@Override
	public TypeNode visitNode(BoolNode n) {
		if (print) printNode(n,n.val.toString());
		return new BoolTypeNode();
	}

	@Override
	public TypeNode visitNode(IntNode n) {
		if (print) printNode(n,n.val.toString());
		return new IntTypeNode();
	}

// gestione tipi incompleti	(se lo sono lancia eccezione)
	
	@Override
	public TypeNode visitNode(ArrowTypeNode n) throws TypeException {
		if (print) printNode(n);
		for (Node par: n.parlist) visit(par);
		visit(n.ret,"->"); //marks return type
		return null;
	}

	@Override
	public TypeNode visitNode(BoolTypeNode n) {
		if (print) printNode(n);
		return null;
	}

	@Override
	public TypeNode visitNode(IntTypeNode n) {
		if (print) printNode(n);
		return null;
	}

// STentry (ritorna campo type)

	@Override
	public TypeNode visitSTentry(STentry entry) throws TypeException {
		if (print) printSTentry("type");
		return ckvisit(entry.type); 
	}


	/*----------------------------------------------OPERATOR EXTENSION------------------------------------------------*/
	@Override
	public TypeNode visitNode(LessEqualNode n) throws TypeException {
		if (print) printNode(n);
		if (!(isSubtype(visit(n.left), new IntTypeNode())
				&& isSubtype(visit(n.right), new IntTypeNode()))) {
			throw new TypeException("Not integer parameter beside <= operand", n.getLine());
		}
		return new BoolTypeNode();
	}

	@Override
	public TypeNode visitNode(GreaterEqualNode n) throws TypeException {
		if (print) printNode(n);
		if (!(isSubtype(visit(n.left), new IntTypeNode())
				&& isSubtype(visit(n.right), new IntTypeNode()))) {
			throw new TypeException("Not integer parameter beside >= operand", n.getLine());
		}
		return new BoolTypeNode();
	}

	@Override
	public TypeNode visitNode(OrNode n) throws TypeException {
		if (print) printNode(n);
		if (!(isSubtype(visit(n.left), new BoolTypeNode())
				&& isSubtype(visit(n.right), new BoolTypeNode()))) {
			throw new TypeException("Not booleans beside || symbol", n.getLine());
		}
		return new BoolTypeNode();
	}

	@Override
	public TypeNode visitNode(AndNode n) throws TypeException {
		if (print) printNode(n);
		if (!(isSubtype(visit(n.left), new BoolTypeNode())
				&& isSubtype(visit(n.right), new BoolTypeNode()))) {
			throw new TypeException("Not booleans beside && symbol", n.getLine());
		}
		return new BoolTypeNode();
	}

	@Override
	public TypeNode visitNode(DivNode n) throws TypeException {
		if (print) printNode(n);
		if (!(isSubtype(visit(n.left), new IntTypeNode())
				&& isSubtype(visit(n.right), new IntTypeNode()))) {
			throw new TypeException("Not integers beside / operation", n.getLine());
		}
		return new IntTypeNode();
	}

	@Override
	public TypeNode visitNode(MinusNode n) throws TypeException {
		if (print) printNode(n);
		if (!(isSubtype(visit(n.left), new IntTypeNode())
				&& isSubtype(visit(n.right), new IntTypeNode()))) {
			throw new TypeException("Not integers beside - operation", n.getLine());
		}
		return new IntTypeNode();
	}

	@Override
	public TypeNode visitNode(NotNode n) throws TypeException {
		if (print) printNode(n);
		if (!(isSubtype(visit(n.exp), new BoolTypeNode()))) {
			throw new TypeException("Not boolean beside ! symbol", n.getLine());
		}
		return new BoolTypeNode();
	}

	/*----------------------------------------------CLASS EXTENSION---------------------------------------------------*/


	@Override
	public TypeNode visitNode(ClassNode n) throws TypeException {
		if(print) printNode(n, n.id);

		//If a class have a super class, change it type in type map
		if(n.superId != null){
			superType.put(n.id, n.superId);
		}

		//Check if the methods in class have a correct type
		//Visit all method of the class
		n.methodNodeList.forEach((method) ->{
			try {
				visit(method);
			}catch (TypeException e){
				ErrorManager.printError(ErrorManager.ERROR_CODE,
						"Class declaration error (type checking): " + e.text);
			}
		});

		if(n.superId == null || n.superSTentry == null){
			return null;
		}

		ClassTypeNode classTypeNode = (ClassTypeNode) n.getType();
		ClassTypeNode superTypeNode = (ClassTypeNode) n.superSTentry.type;

		//Check fields and methods are in correct position and have a correct subtypes
		for(FieldNode field: n.fieldNodeList){
			if ((-field.offset - 1) < superTypeNode.allFields.size() &&
					!isSubtype(classTypeNode.allFields.get(-field.offset - 1),
					superTypeNode.allFields.get(-field.offset - 1))) {
				throw new TypeException("Wrong type for field " + field.id, field.getLine());
			}
		}

		for(MethodNode method: n.methodNodeList){
			if (method.offset < superTypeNode.allFields.size() &&
					!isSubtype(classTypeNode.allMethods.get(method.offset),
							superTypeNode.allMethods.get(method.offset))) {
				throw new TypeException("Wrong type for method " + method.id, method.getLine());
			}
		}


		return null;
	}

	@Override
	public TypeNode visitNode(FieldNode n){
		if (print) printNode(n);
		return null;
	}

	@Override
	public TypeNode visitNode(MethodNode n) throws TypeException {
		if (print) printNode(n,n.id);
		for (Node dec : n.decList)
			try {
				visit(dec);
			} catch (IncomplException ignored) {
			} catch (TypeException e) {
				ErrorManager.printError(ErrorManager.WARNING_CODE,
						"Type checking error in a declaration: " + e.text);
			}
		if ( !isSubtype(visit(n.exp),ckvisit(n.retType)) )
			throw new TypeException("Wrong return type for function " + n.id,n.getLine());
		return null;
	}

	@Override
	public TypeNode visitNode(ClassCallNode n) throws TypeException {
		if (print) printNode(n,n.id);

		TypeNode t = visit(n.methodEntry);

		//If is a method, get the functional type
		if(t instanceof  MethodTypeNode methodTypeNode){
			t = methodTypeNode.fun;
		}
		if ( !(t instanceof ArrowTypeNode at) )
			throw new TypeException("Invocation of a non-function " + n.id, n.getLine());

		//Check if the number of parameters is correct
        if ( !(at.parlist.size() == n.arg.size()) )
			throw new TypeException("Wrong number of parameters in the invocation of " + n.id,n.getLine());

		//Check if the type of parameters is correct
		for (int i = 0; i < n.arg.size(); i++)
			if ( !(isSubtype(visit(n.arg.get(i)),at.parlist.get(i))) )
				throw new TypeException("Wrong type for " + ( i + 1) + "-th parameter in the invocation of " + n.id,n.getLine());
		return at.ret;
	}

	@Override
	public TypeNode visitNode(NewNode n) throws TypeException {
		if (print) printNode(n,n.id);

		TypeNode t = visit(n.entry);

		//Check if the type is ClassTypeNode
		if(!(t instanceof  ClassTypeNode classTypeNode)){
			throw new TypeException("Invocation of a non-class constructor " + n.id, n.getLine());
		}

		//Check if the number of parameters is correct in class constructor
		if (!(classTypeNode.allFields.size() == n.arg.size()))
			throw new TypeException("Wrong number of parameters in the invocation of a class constructor " + n.id,n.getLine());

		//Check if the type of parameters is correct in class constructor
		for (int i = 0; i < n.arg.size(); i++)
			if ( !(isSubtype(visit(n.arg.get(i)), classTypeNode.allFields.get(i))) )
				throw new TypeException("Wrong type for " + ( i + 1) + "-th parameter in the invocation of constructor " + n.id,n.getLine());
		return new RefTypeNode(n.id);
	}

	@Override
	public TypeNode visitNode(EmptyNode n) throws TypeException {
		return new EmptyTypeNode();
	}

	@Override
	public TypeNode visitNode(ClassTypeNode n){
		if (print) printNode(n);
		return null;
	}

	@Override
	public TypeNode visitNode(MethodTypeNode n){
		if (print) printNode(n);
		return null;
	}

	@Override
	public TypeNode visitNode(RefTypeNode n){
		if (print) printNode(n);
		return null;
	}

	@Override
	public TypeNode visitNode(EmptyTypeNode n){
		if (print) printNode(n);
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