package compiler;

import java.util.*;
import compiler.lib.*;

/*
* AST Class
* Used for generate Abstract Syntax Three of FOOL Language
* */
public class AST {

	/*
	* ProgLetInNode Class
	* Represent the principal main node of the AST
	* It contains the list of declarations and the main expression
	* */
	public static class ProgLetInNode extends Node {
		final List<DecNode> declist;
		final Node exp;
		ProgLetInNode(List<DecNode> d, Node e) {
			declist = Collections.unmodifiableList(d); 
			exp = e;
		}

		@Override
		public <S,E extends Exception> S accept(BaseASTVisitor<S,E> visitor) throws E {return visitor.visitNode(this);}
	}

	/*
	 * ProgNode Class
	 * Represent the main node on AST
	 * It contains the main expression
	 * */
	public static class ProgNode extends Node {
		final Node exp;
		ProgNode(Node e) {exp = e;}

		@Override
		public <S,E extends Exception> S accept(BaseASTVisitor<S,E> visitor) throws E {return visitor.visitNode(this);}
	}

	/*
	 * FunNode Class
	 * Is an abstraction of the concept of function
	 * It contains:
	 * The name of function
	 * The list of parameters
	 * The return type of function
	 * The list of declaration
	 * The expression contained in body function
	 * */
	public static class FunNode extends DecNode {
		final String id;
		final TypeNode retType;
		final List<ParNode> parlist;
		final List<DecNode> declist; 
		final Node exp;
		FunNode(String i, TypeNode rt, List<ParNode> pl, List<DecNode> dl, Node e) {
	    	id=i; 
	    	retType=rt; 
	    	parlist=Collections.unmodifiableList(pl); 
	    	declist=Collections.unmodifiableList(dl); 
	    	exp=e;
	    }
		
		//void setType(TypeNode t) {type = t;}
		
		@Override
		public <S,E extends Exception> S accept(BaseASTVisitor<S,E> visitor) throws E {return visitor.visitNode(this);}
	}

	/*
	 * ParNode Class
	 * Is an abstraction of the concept of parameter
	 * It contains:
	 * The id of parameter
	 * The type of parameter
	 * */
	public static class ParNode extends DecNode {
		final String id;
		ParNode(String i, TypeNode t) {id = i; type = t;}

		@Override
		public <S,E extends Exception> S accept(BaseASTVisitor<S,E> visitor) throws E {return visitor.visitNode(this);}
	}

	/*
	 * VarNode Class
	 * Is an abstraction of the concept of variable declaration
	 * It contains:
	 * The id of the variable declaration
	 * The expression of the variable declaration
	 * The type of the variable declaration
	 * */
	public static class VarNode extends DecNode {
		final String id;
		final Node exp;
		VarNode(String i, TypeNode t, Node v) {id = i; type = t; exp = v;}

		@Override
		public <S,E extends Exception> S accept(BaseASTVisitor<S,E> visitor) throws E {return visitor.visitNode(this);}
	}

	/*
	 * PrintNode Class
	 * Is used to print
	 * It contains:
	 * The expression to print
	 * */
	public static class PrintNode extends Node {
		final Node exp;
		PrintNode(Node e) {exp = e;}

		@Override
		public <S,E extends Exception> S accept(BaseASTVisitor<S,E> visitor) throws E {return visitor.visitNode(this);}
	}

	/*
	 * IfNode Class
	 * Is an abstraction of the concept of condition
	 * It contains:
	 * The condition
	 * The then clause
	 * The else clause
	 * */
	public static class IfNode extends Node {
		final Node cond;
		final Node th;
		final Node el;
		IfNode(Node c, Node t, Node e) {cond = c; th = t; el = e;}

		@Override
		public <S,E extends Exception> S accept(BaseASTVisitor<S,E> visitor) throws E {return visitor.visitNode(this);}
	}

	/*
	 * EqualNode Class
	 * Is an abstraction of the concept of equality
	 * It contains:
	 * The left expression
	 * The right expression
	 * */
	public static class EqualNode extends Node {
		final Node left;
		final Node right;
		EqualNode(Node l, Node r) {left = l; right = r;}

		@Override
		public <S,E extends Exception> S accept(BaseASTVisitor<S,E> visitor) throws E {return visitor.visitNode(this);}
	}

	/*
	 * TimesNode Class
	 * Is an abstraction of the concept of times operation
	 * It contains:
	 * The left expression
	 * The right expression
	 * */
	public static class TimesNode extends Node {
		final Node left;
		final Node right;
		TimesNode(Node l, Node r) {left = l; right = r;}

		@Override
		public <S,E extends Exception> S accept(BaseASTVisitor<S,E> visitor) throws E {return visitor.visitNode(this);}
	}

	/*
	 * PlusNode Class
	 * Is an abstraction of the concept of plus operation
	 * It contains:
	 * The left expression
	 * The right expression
	 * */
	public static class PlusNode extends Node {
		final Node left;
		final Node right;
		PlusNode(Node l, Node r) {left = l; right = r;}

		@Override
		public <S,E extends Exception> S accept(BaseASTVisitor<S,E> visitor) throws E {return visitor.visitNode(this);}
	}

	/*
	 * CallNode Class
	 * Is an abstraction of the concept of a function call
	 * It contains:
	 * The id of the caller function
	 * The list of arguments
	 * The symbol table entry
	 * The number of the nesting level for the function context
	 * */
	public static class CallNode extends Node {
		final String id;
		final List<Node> arglist;
		STentry entry;
		int nl;
		CallNode(String i, List<Node> p) {
			id = i; 
			arglist = Collections.unmodifiableList(p);
		}

		@Override
		public <S,E extends Exception> S accept(BaseASTVisitor<S,E> visitor) throws E {return visitor.visitNode(this);}
	}

	/*
	 * IdNode Class
	 * Is an abstraction of the concept of id for a variable
	 * It contains:
	 * The id of the variable
	 * The entry for the symbol table
	 * The number of the nesting level
	 * */
	public static class IdNode extends Node {
		final String id;
		STentry entry;
		int nl;
		IdNode(String i) {id = i;}

		@Override
		public <S,E extends Exception> S accept(BaseASTVisitor<S,E> visitor) throws E {return visitor.visitNode(this);}
	}

	/*
	 * BoolNode Class
	 * Is an abstraction of the concept of boolean value
	 * It contains:
	 * The boolean value
	 * */
	public static class BoolNode extends Node {
		final Boolean val;
		BoolNode(boolean n) {val = n;}

		@Override
		public <S,E extends Exception> S accept(BaseASTVisitor<S,E> visitor) throws E {return visitor.visitNode(this);}
	}

	/*
	 * IntNode Class
	 * Is an abstraction of the concept of integer value
	 * It contains:
	 * The integer value
	 * */
	public static class IntNode extends Node {
		final Integer val;
		IntNode(Integer n) {val = n;}

		@Override
		public <S,E extends Exception> S accept(BaseASTVisitor<S,E> visitor) throws E {return visitor.visitNode(this);}
	}

	/*
	 * ArrowTypeNode Class
	 * Is an abstraction of the concept of the arrow type for a function
	 * It contains:
	 * The list of parameters of function
	 * The type of the return
	 * */
	public static class ArrowTypeNode extends TypeNode {
		final List<TypeNode> parlist;
		final TypeNode ret;
		ArrowTypeNode(List<TypeNode> p, TypeNode r) {
			parlist = Collections.unmodifiableList(p); 
			ret = r;
		}

		@Override
		public <S,E extends Exception> S accept(BaseASTVisitor<S,E> visitor) throws E {return visitor.visitNode(this);}
	}

	/*
	 * BoolTypeNode Class
	 * Is an abstraction of the concept of boolean value
	 * */
	public static class BoolTypeNode extends TypeNode {

		@Override
		public <S,E extends Exception> S accept(BaseASTVisitor<S,E> visitor) throws E {return visitor.visitNode(this);}
	}

	/*
	 * IntTypeNode Class
	 * Is an abstraction of the concept of integer value
	 * */
	public static class IntTypeNode extends TypeNode {

		@Override
		public <S,E extends Exception> S accept(BaseASTVisitor<S,E> visitor) throws E {return visitor.visitNode(this);}
	}


	/*----------------------------------------------OPERATOR EXTENSION------------------------------------------------*/
	/*
	 * LessEqualNode Class
	 * Is an abstraction of the concept of the less equal evaluation
	 * It contains:
	 * The left expression
	 * The right expression
	 * */
	public static class LessEqualNode extends Node{
		final Node left;
		final Node right;

		LessEqualNode(final Node left, final Node right){
			this.left = left;
			this.right = right;
		}
		@Override
		public <S, E extends Exception> S accept(BaseASTVisitor<S, E> visitor) throws E {
			return visitor.visitNode(this);
		}
	}

	/*
	 * GreaterEqualNode Class
	 * Is an abstraction of the concept of the greater equal evaluation
	 * It contains:
	 * The left expression
	 * The right expression
	 * */
	public static class GreaterEqualNode extends Node{
		final Node left;
		final Node right;

		GreaterEqualNode(final Node left, final Node right){
			this.left = left;
			this.right = right;
		}
		@Override
		public <S, E extends Exception> S accept(BaseASTVisitor<S, E> visitor) throws E {
			return visitor.visitNode(this);
		}
	}

	/*
	 * OrNode Class
	 * Is an abstraction of the concept of or evaluation
	 * It contains:
	 * The left expression
	 * The right expression
	 * */
	public static class OrNode extends Node{
		final Node left;
		final Node right;

		OrNode(final Node left, final Node right){
			this.left = left;
			this.right = right;
		}
		@Override
		public <S, E extends Exception> S accept(BaseASTVisitor<S, E> visitor) throws E {
			return visitor.visitNode(this);
		}
	}

	/*
	 * AndNode Class
	 * Is an abstraction of the concept of and evaluation
	 * It contains:
	 * The left expression
	 * The right expression
	 * */
	public static class AndNode extends Node{
		final Node left;
		final Node right;

		AndNode(final Node left, final Node right){
			this.left = left;
			this.right = right;
		}
		@Override
		public <S, E extends Exception> S accept(BaseASTVisitor<S, E> visitor) throws E {
			return visitor.visitNode(this);
		}
	}

	/*
	 * DivNode Class
	 * Is an abstraction of the concept of div operation
	 * It contains:
	 * The left expression
	 * The right expression
	 * */
	public static class DivNode extends Node{
		final Node left;
		final Node right;

		DivNode(final Node left, final Node right){
			this.left = left;
			this.right = right;
		}
		@Override
		public <S, E extends Exception> S accept(BaseASTVisitor<S, E> visitor) throws E {
			return visitor.visitNode(this);
		}
	}

	/*
	 * MinusNode Class
	 * Is an abstraction of the concept of minus operation
	 * It contains:
	 * The left expression
	 * The right expression
	 * */
	public static class MinusNode extends Node{
		final Node left;
		final Node right;

		MinusNode(final Node left, final Node right){
			this.left = left;
			this.right = right;
		}
		@Override
		public <S, E extends Exception> S accept(BaseASTVisitor<S, E> visitor) throws E {
			return visitor.visitNode(this);
		}
	}

	/*
	 * NotNode Class
	 * Is an abstraction of the concept of negation for expression
	 * It contains:
	 * The expression to negate
	 * */
	public static class NotNode extends Node{
		final Node exp;

		NotNode(final Node exp){
			this.exp = exp;
		}
		@Override
		public <S, E extends Exception> S accept(BaseASTVisitor<S, E> visitor) throws E {
			return visitor.visitNode(this);
		}
	}

	/*----------------------------------------------CLASS EXTENSION---------------------------------------------------*/

	/*
	 * FieldNode Class
	 * Is an abstraction of the concept of a field in a class
	 * It contains:
	 * The id of the field
	 * The type of the field
	 * */
	public static class FieldNode extends DecNode{
		final String id;
		public FieldNode(String id, TypeNode typeNode){
			this.id = id;
			this.type = typeNode;
		}

		@Override
		public <S, E extends Exception> S accept(BaseASTVisitor<S, E> visitor) throws E {
			return visitor.visitNode(this);
		}
	}

	/*
	 * MethodNode Class
	 * Is an abstraction of the concept of a method in a class
	 * It contains:
	 * The id of the method
	 * The parameter list of the method
	 * The list of declaration in method
	 * The return type of the method
	 * The body (exp) of the method
	 * */
	public static class MethodNode extends DecNode{
		final String id;
		final List<ParNode> parList;
		final List<DecNode> decList;
		final TypeNode retType;
		final Node exp;
		//TODO

		public MethodNode(String id, List<ParNode> parList, List<DecNode> decList, TypeNode retType, Node body){
			this.id = id;
			this.parList = Collections.unmodifiableList(parList); //parList is immutable
			this.decList = Collections.unmodifiableList(decList); //decList is immutable
			this.retType = retType;
			this.exp = body;
		}
		@Override
		public <S, E extends Exception> S accept(BaseASTVisitor<S, E> visitor) throws E {
			return visitor.visitNode(this);
		}
	}

	/*
	* ClassNode Class
	* Is an abstraction of the concept of a class
	* It contains:
	* The id of the class
	* The filed or attribute list
	* The method list
	* */
	public static class ClassNode extends DecNode{
		final String id;
		final List<FieldNode> fieldNodeList;
		final List<MethodNode> methodNodeList;

		public ClassNode(String id, List<FieldNode> field, List<MethodNode> methodList){
			this.id = id;
			this.fieldNodeList = Collections.unmodifiableList(field); //fieldNodeList is immutable
			this.methodNodeList = Collections.unmodifiableList(methodList); //methodList is immutable
		}

		@Override
		public <S, E extends Exception> S accept(BaseASTVisitor<S, E> visitor) throws E {
			return visitor.visitNode(this);
		}
	}

	/*
	 * ClassCallNode Class
	 * Is an abstraction of the concept of call for a class
	 * It contains:
	 * The id of the call
	 * The id of the method
	 * The list of arguments
	 * */
	public static class ClassCallNode extends DecNode{

		final String id;
		final String idMethod;
		final List<Node> arg;
		//TODO

		public ClassCallNode(String id, String idMethod, List<Node> arg){
			this.id = id;
			this.idMethod = idMethod;
			this.arg = Collections.unmodifiableList(arg);
		}

		@Override
		public <S, E extends Exception> S accept(BaseASTVisitor<S, E> visitor) throws E {
			return visitor.visitNode(this);
		}
	}

	/*
	 * NewNode Class
	 * Is an abstraction of the concept of new instance
	 * It contains:
	 * The id of the new class instance
	 * The list of arguments
	 * */
	public static class NewNode extends DecNode{
		final String id;
		final List<Node> arg;

		//TODO

		public NewNode(String id, List<Node> arg){
			this.id = id;
			this.arg = Collections.unmodifiableList(arg);
		}

		@Override
		public <S, E extends Exception> S accept(BaseASTVisitor<S, E> visitor) throws E {
			return visitor.visitNode(this);
		}
	}

	/*
	 * EmptyNode Class
	 * Is an abstraction of the concept of the null operand
	 * */
	public static class EmptyNode extends DecNode{

		@Override
		public <S, E extends Exception> S accept(BaseASTVisitor<S, E> visitor) throws E {
			return visitor.visitNode(this);
		}
	}


}