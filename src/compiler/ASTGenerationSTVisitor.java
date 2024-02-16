package compiler;

import java.util.*;

import org.antlr.v4.runtime.ParserRuleContext;
import org.antlr.v4.runtime.tree.ParseTree;

import compiler.AST.*;
import compiler.FOOLParser.*;
import compiler.lib.*;
import static compiler.lib.FOOLlib.*;

public class ASTGenerationSTVisitor extends FOOLBaseVisitor<Node> {

	public static int GLOBAL_SCOPE = 0;
	String indent;
    public boolean print;
	
    ASTGenerationSTVisitor() {}    
    ASTGenerationSTVisitor(boolean debug) { print=debug; }
        
    private void printVarAndProdName(ParserRuleContext ctx) {
        String prefix="";        
    	Class<?> ctxClass=ctx.getClass(), parentClass=ctxClass.getSuperclass();
        if (!parentClass.equals(ParserRuleContext.class)) // parentClass is the var context (and not ctxClass itself)
        	prefix=lowerizeFirstChar(extractCtxName(parentClass.getName()))+": production #";
    	System.out.println(indent+prefix+lowerizeFirstChar(extractCtxName(ctxClass.getName())));                               	
    }
        
    @Override
	public Node visit(ParseTree t) {
    	if (t==null) return null;
        String temp=indent;
        indent=(indent==null)?"":indent+"  ";
        Node result = super.visit(t);
        indent=temp;
        return result; 
	}

	@Override
	public Node visitProg(ProgContext c) {
		if (print) printVarAndProdName(c);
		return visit(c.progbody());
	}

	@Override
	public Node visitLetInProg(LetInProgContext c) {
		if (print) printVarAndProdName(c);

		//Read all class declaration
		List<DecNode> allDeclaration = new ArrayList<>();
		c.cldec().forEach(declaration -> allDeclaration.add((DecNode) visit(declaration)));

		//Read all declaration of variable and function
		List<DecNode> declist = new ArrayList<>();
		c.dec().forEach((declaration) -> declist.add((DecNode) visit(declaration)));

		//Add all declaration
		allDeclaration.addAll(declist);

		return new ProgLetInNode(allDeclaration, visit(c.exp()));
	}

	@Override
	public Node visitNoDecProg(NoDecProgContext c) {
		if (print) printVarAndProdName(c);
		return new ProgNode(visit(c.exp()));
	}

	@Override
	public Node visitVardec(VardecContext c) {
		if (print) printVarAndProdName(c);
		Node n = null;
		if (c.ID()!=null) { //non-incomplete ST
			n = new VarNode(c.ID().getText(), (TypeNode) visit(c.type()), visit(c.exp()));
			n.setLine(c.VAR().getSymbol().getLine());
		}
        return n;
	}

	@Override
	public Node visitFundec(FundecContext c) {
		if (print) printVarAndProdName(c);
		List<ParNode> parList = new ArrayList<>();
		for (int i = 1; i < c.ID().size(); i++) {
			ParNode p = new ParNode(c.ID(i).getText(),(TypeNode) visit(c.type(i)));
			p.setLine(c.ID(i).getSymbol().getLine());
			parList.add(p);
		}
		List<DecNode> decList = new ArrayList<>();
		for (DecContext dec : c.dec()) decList.add((DecNode) visit(dec));
		Node n = null;
		if (!c.ID().isEmpty()) { //non-incomplete ST
			n = new FunNode(c.ID(0).getText(),(TypeNode)visit(c.type(0)),parList,decList,visit(c.exp()));
			n.setLine(c.FUN().getSymbol().getLine());
		}
        return n;
	}

	@Override
	public Node visitIntType(IntTypeContext c) {
		if (print) printVarAndProdName(c);
		return new IntTypeNode();
	}

	@Override
	public Node visitBoolType(BoolTypeContext c) {
		if (print) printVarAndProdName(c);
		return new BoolTypeNode();
	}

	@Override
	public Node visitInteger(IntegerContext c) {
		if (print) printVarAndProdName(c);
		int v = Integer.parseInt(c.NUM().getText());
		return new IntNode(c.MINUS()==null?v:-v);
	}

	@Override
	public Node visitTrue(TrueContext c) {
		if (print) printVarAndProdName(c);
		return new BoolNode(true);
	}

	@Override
	public Node visitFalse(FalseContext c) {
		if (print) printVarAndProdName(c);
		return new BoolNode(false);
	}

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

	@Override
	public Node visitPrint(PrintContext c) {
		if (print) printVarAndProdName(c);
		return new PrintNode(visit(c.exp()));
	}

	@Override
	public Node visitPars(ParsContext c) {
		if (print) printVarAndProdName(c);
		return visit(c.exp());
	}

	@Override
	public Node visitId(IdContext c) {
		if (print) printVarAndProdName(c);
		Node n = new IdNode(c.ID().getText());
		n.setLine(c.ID().getSymbol().getLine());
		return n;
	}

	@Override
	public Node visitCall(CallContext c) {
		if (print) printVarAndProdName(c);		
		List<Node> arglist = new ArrayList<>();
		for (ExpContext arg : c.exp()) arglist.add(visit(arg));
		Node n = new CallNode(c.ID().getText(), arglist);
		n.setLine(c.ID().getSymbol().getLine());
		return n;
	}

	/*----------------------------------------------OPERATOR EXTENSION------------------------------------------------*/
	@Override
	public Node visitTimesDiv(TimesDivContext c) {
		if (print) printVarAndProdName(c);
		//Check if it's TimesNode or DivNode
		if(c.TIMES() != null){
			Node timesNode = new TimesNode(visit(c.exp(0)), visit(c.exp(1)));
			timesNode.setLine(c.TIMES().getSymbol().getLine());		// setLine added
			return timesNode;
		}
		//It's not a TimesNode return DivNode
		Node divNode = new DivNode(visit(c.exp(0)), visit(c.exp(1)));
		divNode.setLine(c.DIV().getSymbol().getLine());
		return divNode;
	}


	@Override
	public Node visitPlusMinus(PlusMinusContext c) {
		if (print) printVarAndProdName(c);
		//Check if it's PlusNode or MinusNode
		if(c.PLUS() != null){
			Node plusNode = new PlusNode(visit(c.exp(0)), visit(c.exp(1)));
			plusNode.setLine(c.PLUS().getSymbol().getLine());
			return plusNode;
		}
		//It's not a PlusNode return MinusNode
		Node minusNode = new MinusNode(visit(c.exp(0)), visit(c.exp(1)));
		minusNode.setLine(c.MINUS().getSymbol().getLine());
		return minusNode;
	}


	@Override
	public Node visitComp(CompContext c) {
		if (print) printVarAndProdName(c);
		//Check if it's EqNode or GENode or LENode
		if(c.EQ() != null){
			Node equalNode = new EqualNode(visit(c.exp(0)), visit(c.exp(1)));
			equalNode.setLine(c.EQ().getSymbol().getLine());
			return equalNode;
		}else if(c.GE() != null){
			Node greaterNode = new GreaterEqualNode(visit(c.exp(0)), visit(c.exp(1)));
			greaterNode.setLine(c.GE().getSymbol().getLine());
			return greaterNode;
		}
		//It's not a EqNode or GENode return LENode
		Node lessEqualNodeEqNode = new LessEqualNode(visit(c.exp(0)), visit(c.exp(1)));
		lessEqualNodeEqNode.setLine(c.LE().getSymbol().getLine());
		return lessEqualNodeEqNode;
	}

	public Node visitAndOr(AndOrContext c){
		if (print) printVarAndProdName(c);
		//Check if it's AndNode or OrNode
		if(c.AND() != null){
			Node andNode = new AndNode(visit(c.exp(0)), visit(c.exp(1)));
			andNode.setLine(c.AND().getSymbol().getLine());
			return andNode;
		}
		//It's not a andNode return OrNode
		Node orNode = new OrNode(visit(c.exp(0)), visit(c.exp(1)));
		orNode.setLine(c.OR().getSymbol().getLine());
		return orNode;
	}

	@Override
	public Node visitNot(final NotContext c) {
		if (print) printVarAndProdName(c);
		Node notNode = new NotNode(visit(c.exp()));
		notNode.setLine(c.NOT().getSymbol().getLine());
		return notNode;
	}

	/*----------------------------------------------CLASS EXTENSION---------------------------------------------------*/

	@Override
	public Node visitCldec(CldecContext ctx) {
		if(print) printVarAndProdName(ctx);
		if(ctx.ID().isEmpty()) return null;

		List<FieldNode> allFields = new ArrayList<>();
		List<MethodNode> allMethods = new ArrayList<>();

		//Check if exist the super class, if the class exist get the name
		String superClass = (ctx.EXTENDS() != null) ? ctx.ID(1).getText() : null;

		//Compute the padding
		int computedPadding = (superClass != null) ? 2 : 1;

		//Visit all fields and store it in a temp list
		for(int i = computedPadding; i < ctx.ID().size(); i++){
			//Add field in allFields list
			FieldNode fieldNode = new FieldNode(ctx.ID(i).getText(), (TypeNode) visit(ctx.type(i - computedPadding)));
			//Update the number of the line in the filed
			fieldNode.setLine(ctx.ID(i).getSymbol().getLine());
			allFields.add(fieldNode);
		}

		//Visit all methods and store it in a temp list
		ctx.methdec().forEach((method) -> allMethods.add((MethodNode) visit(method)));

		//Create the class node
		//Take the class id
		String classId = ctx.ID(GLOBAL_SCOPE).getText();
		ClassNode classNode = new ClassNode(classId, allFields, allMethods, superClass);
		classNode.setLine(ctx.ID(GLOBAL_SCOPE).getSymbol().getLine());
		return classNode;
	}

	@Override
	public Node visitMethdec(MethdecContext ctx) {
		if(print) printVarAndProdName(ctx);
		if(ctx.ID().isEmpty()) return null;

		List<ParNode> allParameters = new ArrayList<>();
		List<DecNode> allDeclarations = new ArrayList<>();

		//Visit all parameters and store it in a temp list
		for(int i = 1; i < ctx.ID().size(); i++){
			//Add field in allFields list
			ParNode fieldNode = new ParNode(ctx.ID(i).getText(), (TypeNode) visit(ctx.type(i)));
			//Update the number of the line in the filed
			fieldNode.setLine(ctx.ID(i).getSymbol().getLine());
			allParameters.add(fieldNode);
		}


		//Visit all declarations and store it in a temp list
		ctx.dec().forEach((declaration) -> allDeclarations.add((DecNode) visit(declaration)));

		//Create the MethodNode

		//Get the id
		String methodId = ctx.ID(GLOBAL_SCOPE).getText();
		//Get the type
		TypeNode typeNode = (TypeNode) visit(ctx.type(0));

		MethodNode methodNode = new MethodNode(methodId, allParameters, allDeclarations, typeNode, visit(ctx.exp()));
		methodNode.setLine(ctx.ID(GLOBAL_SCOPE).getSymbol().getLine());
		return methodNode;
	}

	@Override
	public Node visitNew(NewContext ctx) {
		if(print) printVarAndProdName(ctx);
		if(ctx.ID() == null) return null;
		List<Node> allArguments = new ArrayList<>();

		//Visit all arguments
		ctx.exp().forEach((arg) -> allArguments.add(visit(arg)));

		//Create the NewNode
		final NewNode newNode = new NewNode(ctx.ID().getText(), allArguments);
		newNode.setLine(ctx.ID().getSymbol().getLine());
		return newNode;
	}

	@Override
	public Node visitNull(NullContext ctx) {
		if(print) printVarAndProdName(ctx);
		return new EmptyNode();
	}

	@Override
	public Node visitDotCall(DotCallContext ctx) {
		if(print) printVarAndProdName(ctx);
		if(ctx.ID().size() != 2) return null;

		List<Node> allArguments = new ArrayList<>();

		//Visit all arguments
		ctx.exp().forEach((arg) -> allArguments.add(visit(arg)));

		ClassCallNode classCallNode = new ClassCallNode(ctx.ID(0).getText(), ctx.ID(1).getText(), allArguments);
		classCallNode.setLine(ctx.ID(0).getSymbol().getLine());
		return classCallNode;
	}

	@Override
	public Node visitIdType(IdTypeContext ctx) {
		if(print) printVarAndProdName(ctx);

		//Get the ref of the id
		RefTypeNode refTypeNode = new RefTypeNode(ctx.ID().getText());
		refTypeNode.setLine(ctx.ID().getSymbol().getLine());
		return refTypeNode;
	}
}
