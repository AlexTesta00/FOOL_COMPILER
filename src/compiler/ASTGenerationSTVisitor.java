package compiler;

import java.util.*;

import org.antlr.v4.runtime.ParserRuleContext;
import org.antlr.v4.runtime.tree.ParseTree;

import compiler.AST.*;
import compiler.FOOLParser.*;
import compiler.lib.*;
import static compiler.lib.FOOLlib.*;

/*
* ASTGenerationSTVisitor Class
* It implements a visitor for the AST generation during the syntactic analysis phase
* AST is built by translating the parse tree
* */
public class ASTGenerationSTVisitor extends FOOLBaseVisitor<Node> {

	public static int GLOBAL_SCOPE = 0;
	String indent;
    public boolean print;
	
    ASTGenerationSTVisitor() {}    
    ASTGenerationSTVisitor(boolean debug) { print=debug; } // enables print for debugging
        
    /*
     * printVarAndProdName method for ParserRule context
     * Print the name of the current production and the context name of the current variable
     * */
	private void printVarAndProdName(ParserRuleContext ctx) {
        String prefix = "";
    	Class<?> ctxClass = ctx.getClass(), parentClass = ctxClass.getSuperclass();
        if (!parentClass.equals(ParserRuleContext.class)) // parentClass is the var context (and not ctxClass itself)
        	prefix = lowerizeFirstChar(extractCtxName(parentClass.getName())) + ": production #";
    	System.out.println(indent+prefix+lowerizeFirstChar(extractCtxName(ctxClass.getName())));                               	
    }
        
    /*
     * visit method to visit a ParseTree node
     * Check if the node is null and handle indentation during the visit
     * Return the result of the node visit by the superclass visit method
     * */
	@Override
	public Node visit(ParseTree t) {
    	if (t == null) return null;
        String temp = indent;
        indent = (indent == null) ? "" : indent + "  ";
        Node result = super.visit(t);
        indent = temp;
        return result; 
	}

	/*
	 * visitProg method to visit a Prog context
	 * Return the visit of the program body
	 * */
	@Override
	public Node visitProg(ProgContext c) {
		if (print) printVarAndProdName(c);
		return visit(c.progbody());
	}

	/*
	 * visitLetInProg method to visit a LetInProg context
	 * Visit all class, variable and function declarations
	 * Return a new ProgLetInNode representing the let-in program
	 * */
	@Override
	public Node visitLetInProg(LetInProgContext c) {
		if (print) printVarAndProdName(c);

		// Read all class declarations
		List<DecNode> allDeclaration = new ArrayList<>();
		c.cldec().forEach(declaration -> allDeclaration.add((DecNode) visit(declaration)));

		// Read all declarations of variable and function
		List<DecNode> declist = new ArrayList<>();
		c.dec().forEach((declaration) -> declist.add((DecNode) visit(declaration)));

		// Add all declarations
		allDeclaration.addAll(declist);

		return new ProgLetInNode(allDeclaration, visit(c.exp()));
	}

	/*
	 * visitNoDecProg method to visit a NoDecProg context
	 * (there are no class, variable or function declarations within the program)
	 * Return a new ProgNode containing the visit of the expression of the program
	 * */
	@Override
	public Node visitNoDecProg(NoDecProgContext c) {
		if (print) printVarAndProdName(c);
		return new ProgNode(visit(c.exp()));
	}

	/*
	 * visitVardec method to visit a Vardec context
	 * Check if the node contains a variable identifier
	 * If the declaration is complete, return a new VarNode representing it
	 * The VarNode line of code is set
	 * Otherwise, return null
	 * */
	@Override
	public Node visitVardec(VardecContext c) {
		if (print) printVarAndProdName(c);
		Node n = null;
		if (c.ID() != null) { //non-incomplete ST
			n = new VarNode(c.ID().getText(), (TypeNode) visit(c.type()), visit(c.exp()));
			n.setLine(c.VAR().getSymbol().getLine());
		}
        return n;
	}

	/*
	 * visitFundec method to visit a Fundec context
	 * Visit all parameters and declarations
	 * Check if the node contains a function identifier
	 * If the declaration is complete, return a new FunNode representing it
	 * The FunNode line of code is set
	 * Otherwise, return null
	 * */
	@Override
	public Node visitFundec(FundecContext c) {
		if (print) printVarAndProdName(c);

		// Read all parameters
		List<ParNode> parList = new ArrayList<>();
		for (int i = 1; i < c.ID().size(); i++) {
			ParNode p = new ParNode(c.ID(i).getText(),(TypeNode) visit(c.type(i)));
			p.setLine(c.ID(i).getSymbol().getLine());
			parList.add(p);
		}

		// Read all declarations
		List<DecNode> decList = new ArrayList<>();
		for (DecContext dec : c.dec()) decList.add((DecNode) visit(dec));

		Node n = null;
		if (!c.ID().isEmpty()) { //non-incomplete ST
			n = new FunNode(c.ID(0).getText(),(TypeNode)visit(c.type(0)),parList,decList,visit(c.exp()));
			n.setLine(c.FUN().getSymbol().getLine());
		}
        return n;
	}

    /*
	 * visitIntType method to visit an IntType context
	 * Return a new IntTypeNode representing the integer data type
	 * */
	@Override
	public Node visitIntType(IntTypeContext c) {
		if (print) printVarAndProdName(c);
		return new IntTypeNode();
	}

	/*
	 * visitBoolType method to visit a BoolType context
	 * Return a new BoolTypeNode representing the boolean data type
	 * */
	@Override
	public Node visitBoolType(BoolTypeContext c) {
		if (print) printVarAndProdName(c);
		return new BoolTypeNode();
	}

	/*
	 * visitInteger method to visit an Integer context
	 * Return a new IntNode representing the integer value
	 * If the number is preceded by the minus symbol, then the number is negative, so it is negated
	 * */
	@Override
	public Node visitInteger(IntegerContext c) {
		if (print) printVarAndProdName(c);
		int v = Integer.parseInt(c.NUM().getText());
		return new IntNode(c.MINUS() == null ? v : -v);
	}

	/*
	 * visitTrue method to visit a True context
	 * Return a new BoolNode representing the boolean value true
	 * */
	@Override
	public Node visitTrue(TrueContext c) {
		if (print) printVarAndProdName(c);
		return new BoolNode(true);
	}

	/*
	 * visitFalse method to visit a False context
	 * Return a new BoolNode representing the boolean value false
	 * */
	@Override
	public Node visitFalse(FalseContext c) {
		if (print) printVarAndProdName(c);
		return new BoolNode(false);
	}

	/*
	 * visitIf method to visit an If context
	 * Visit the three expressions that, respectively, represent the condition, the then branch and the else branch
	 * Return a new IfNode representing the conditional if statement
	 * The IfNode line of code is set
	 * */
	@Override
	public Node visitIf(IfContext c) {
		if (print) printVarAndProdName(c);
		Node ifNode = visit(c.exp(0));
		Node thenNode = visit(c.exp(1));
		Node elseNode = visit(c.exp(2));
		Node n = new IfNode(ifNode, thenNode, elseNode);
		n.setLine(c.IF().getSymbol().getLine());			
        return n;		
	}

	/*
	 * visitPrint method to visit a Print context
	 * Return a new PrintNode containing the visit of the expression to print
	 * */
	@Override
	public Node visitPrint(PrintContext c) {
		if (print) printVarAndProdName(c);
		return new PrintNode(visit(c.exp()));
	}

	/*
	 * visitPars method to visit a Pars context
	 * Visit the expression enclosed in brackets
	 * Return the visit of the expression
	 * */
	@Override
	public Node visitPars(ParsContext c) {
		if (print) printVarAndProdName(c);
		return visit(c.exp());
	}

	/*
	 * visitId method to visit an Id context
	 * Return a new IdNode representing the identifier
	 * The IdNode line of code is set
	 * */
	@Override
	public Node visitId(IdContext c) {
		if (print) printVarAndProdName(c);
		Node n = new IdNode(c.ID().getText());
		n.setLine(c.ID().getSymbol().getLine());
		return n;
	}

	/*
	 * visitCall method to visit a Call context
	 * Visit all arguments
	 * Return a new CallNode representing the function call
	 * The CallNode line of code is set
	 * */
	@Override
	public Node visitCall(CallContext c) {
		if (print) printVarAndProdName(c);

		// Read all arguments
		List<Node> arglist = new ArrayList<>();
		for (ExpContext arg : c.exp()) arglist.add(visit(arg));

		Node n = new CallNode(c.ID().getText(), arglist);
		n.setLine(c.ID().getSymbol().getLine());
		return n;
	}

	/*----------------------------------------------OPERATOR EXTENSION------------------------------------------------*/

	/*
	 * visitTimesDiv method visit a TimesDiv context
	 * Check if the node is a TimesNode or a DivNode
	 * Depending on the operation, return a new TimesNode or DivNode
	 * Its line of code is set
	 * */
	@Override
	public Node visitTimesDiv(TimesDivContext c) {
		if (print) printVarAndProdName(c);
		// Check if it's TimesNode or DivNode
		if (c.TIMES() != null) {
			Node timesNode = new TimesNode(visit(c.exp(0)), visit(c.exp(1)));
			timesNode.setLine(c.TIMES().getSymbol().getLine());
			return timesNode;
		}
		// It's not a TimesNode return DivNode
		Node divNode = new DivNode(visit(c.exp(0)), visit(c.exp(1)));
		divNode.setLine(c.DIV().getSymbol().getLine());
		return divNode;
	}

	/*
	 * visitPlusMinus method to visit a PlusMinus context
	 * Check if the node is a PlusNode or a MinusNode
	 * Depending on the operation, return a new PlusNode or MinusNode
	 * Its line of code is set
	 * */
	@Override
	public Node visitPlusMinus(PlusMinusContext c) {
		if (print) printVarAndProdName(c);
		// Check if it's PlusNode or MinusNode
		if (c.PLUS() != null) {
			Node plusNode = new PlusNode(visit(c.exp(0)), visit(c.exp(1)));
			plusNode.setLine(c.PLUS().getSymbol().getLine());
			return plusNode;
		}
		// It's not a PlusNode return MinusNode
		Node minusNode = new MinusNode(visit(c.exp(0)), visit(c.exp(1)));
		minusNode.setLine(c.MINUS().getSymbol().getLine());
		return minusNode;
	}

	/*
	 * visitComp method to visit a Comp context
	 * Check if the node is an EqualNode, a GreaterEqualNode or a LessEqualNode
	 * Depending on the comparison operation, return a new EqualNode or GreaterEqualNode or LessEqualNode
	 * Its line of code is set
	 * */
	@Override
	public Node visitComp(CompContext c) {
		if (print) printVarAndProdName(c);
		// Check if it's EqualNode or GreaterEqualNode or LessEqualNode
		if (c.EQ() != null) {
			Node equalNode = new EqualNode(visit(c.exp(0)), visit(c.exp(1)));
			equalNode.setLine(c.EQ().getSymbol().getLine());
			return equalNode;
		} else if (c.GE() != null) {
			Node greaterNode = new GreaterEqualNode(visit(c.exp(0)), visit(c.exp(1)));
			greaterNode.setLine(c.GE().getSymbol().getLine());
			return greaterNode;
		}
		// It's not an EqualNode or a GreaterEqualNode return LessEqualNode
		Node lessEqualNodeEqNode = new LessEqualNode(visit(c.exp(0)), visit(c.exp(1)));
		lessEqualNodeEqNode.setLine(c.LE().getSymbol().getLine());
		return lessEqualNodeEqNode;
	}

	/*
	 * visitAndOr method to visit an AndOr context
	 * Check if the node is an AndNode or an OrNode
	 * Depending on the logic operation, return a new AndNode or OrNode
	 * Its line of code is set
	 * */
	@Override
	public Node visitAndOr(AndOrContext c){
		if (print) printVarAndProdName(c);
		// Check if it's AndNode or OrNode
		if (c.AND() != null) {
			Node andNode = new AndNode(visit(c.exp(0)), visit(c.exp(1)));
			andNode.setLine(c.AND().getSymbol().getLine());
			return andNode;
		}
		// It's not an AndNode return OrNode
		Node orNode = new OrNode(visit(c.exp(0)), visit(c.exp(1)));
		orNode.setLine(c.OR().getSymbol().getLine());
		return orNode;
	}

	/*
	 * visitNot method to visit a Not context
	 * Return a new NotNode representing the NOT logical operation
	 * The NotNode line of code is set
	 * */
	@Override
	public Node visitNot(final NotContext c) {
		if (print) printVarAndProdName(c);
		Node notNode = new NotNode(visit(c.exp()));
		notNode.setLine(c.NOT().getSymbol().getLine());
		return notNode;
	}

	/*----------------------------------------------CLASS EXTENSION---------------------------------------------------*/

	/*
	 * visitCldec method to visit a Cldec context
	 * Check if the node contains a class identifier; otherwise, return null
	 * Check if a superclass exists and get the name
	 * Visit all fields and methods
	 * Return a new ClassNode representing the class declaration
	 * The ClassNode line of code is set
	 * */
	@Override
	public Node visitCldec(CldecContext ctx) {
		if (print) printVarAndProdName(ctx);
		if (ctx.ID().isEmpty()) return null;

		// Check if the superclass exists and get the name
		String superClass = (ctx.EXTENDS() != null) ? ctx.ID(1).getText() : null;

		// Compute the padding
		int computedPadding = (superClass != null) ? 2 : 1;

		// Read all fields
		List<FieldNode> allFields = new ArrayList<>();
		for (int i = computedPadding; i < ctx.ID().size(); i++) {
			FieldNode fieldNode = new FieldNode(ctx.ID(i).getText(), (TypeNode) visit(ctx.type(i - computedPadding)));
			fieldNode.setLine(ctx.ID(i).getSymbol().getLine());
			allFields.add(fieldNode);
		}

		// Read all methods
		List<MethodNode> allMethods = new ArrayList<>();
		ctx.methdec().forEach((method) -> allMethods.add((MethodNode) visit(method)));

		String classId = ctx.ID(GLOBAL_SCOPE).getText();
		ClassNode classNode = new ClassNode(classId, allFields, allMethods, superClass);
		classNode.setLine(ctx.ID(GLOBAL_SCOPE).getSymbol().getLine());
		return classNode;
	}

	/*
	 * visitMethdec method to visit a Methdec context
	 * Check if the node contains a method identifier; otherwise, return null
	 * Visit all parameters and methods
	 * Return a new MethodNode representing the method declaration
	 * The MethodNode line of code is set
	 * */
	@Override
	public Node visitMethdec(MethdecContext ctx) {
		if (print) printVarAndProdName(ctx);
		if (ctx.ID().isEmpty()) return null;

		// Read all parameters
		List<ParNode> allParameters = new ArrayList<>();
		for (int i = 1; i < ctx.ID().size(); i++) {
			ParNode parNode = new ParNode(ctx.ID(i).getText(), (TypeNode) visit(ctx.type(i)));
			parNode.setLine(ctx.ID(i).getSymbol().getLine());
			allParameters.add(parNode);
		}

		// Read all declarations
		List<DecNode> allDeclarations = new ArrayList<>();
		ctx.dec().forEach((declaration) -> allDeclarations.add((DecNode) visit(declaration)));

		String methodId = ctx.ID(GLOBAL_SCOPE).getText();
		TypeNode typeNode = (TypeNode) visit(ctx.type(0));
		MethodNode methodNode = new MethodNode(methodId, allParameters, allDeclarations, typeNode, visit(ctx.exp()));
		methodNode.setLine(ctx.ID(GLOBAL_SCOPE).getSymbol().getLine());
		return methodNode;
	}

	/*
	 * visitNew method to visit a New context
	 * Check if the node contains a class identifier; otherwise, return null
	 * Visit all arguments
	 * Return a new NewNode representing the new class
	 * The NewNode line of code is set
	 * */
	@Override
	public Node visitNew(NewContext ctx) {
		if(print) printVarAndProdName(ctx);
		if (ctx.ID() == null) return null;

		// Read all arguments
		List<Node> allArguments = new ArrayList<>();
		ctx.exp().forEach((arg) -> allArguments.add(visit(arg)));

		final NewNode newNode = new NewNode(ctx.ID().getText(), allArguments);
		newNode.setLine(ctx.ID().getSymbol().getLine());
		return newNode;
	}

	/*
	 * visitNull method to visit a Null context
	 * Return a new EmptyNode representing the null value
	 * */
	@Override
	public Node visitNull(NullContext ctx) {
		if(print) printVarAndProdName(ctx);
		return new EmptyNode();
	}

	/*
	 * visitDotCall method to visit a DotCall context
	 * Check if the node contains exactly 2 identifiers; otherwise, return null
	 * Visit all arguments
	 * Return a new ClassCallNode representing the method call on the class
	 * The ClassCallNode line of code is set
	 * */
	@Override
	public Node visitDotCall(DotCallContext ctx) {
		if(print) printVarAndProdName(ctx);
		if (ctx.ID().size() != 2) return null;

		// Read all arguments
		List<Node> allArguments = new ArrayList<>();
		ctx.exp().forEach((arg) -> allArguments.add(visit(arg)));

		ClassCallNode classCallNode = new ClassCallNode(ctx.ID(0).getText(), ctx.ID(1).getText(), allArguments);
		classCallNode.setLine(ctx.ID(0).getSymbol().getLine());
		return classCallNode;
	}

	/*
	 * visitIdType method to visit an IdType context
	 * Return a new RefTypeNode representing the type reference to the class
	 * The RefTypeNode line of code is set
	 * */
	@Override
	public Node visitIdType(IdTypeContext ctx) {
		if(print) printVarAndProdName(ctx);
		RefTypeNode refTypeNode = new RefTypeNode(ctx.ID().getText());
		refTypeNode.setLine(ctx.ID().getSymbol().getLine());
		return refTypeNode;
	}
}