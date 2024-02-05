package compiler;

import java.util.*;

import org.antlr.v4.runtime.ParserRuleContext;
import org.antlr.v4.runtime.tree.ParseTree;

import compiler.AST.*;
import compiler.FOOLParser.*;
import compiler.lib.*;
import static compiler.lib.FOOLlib.*;

public class ASTGenerationSTVisitor extends FOOLBaseVisitor<Node> {

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
		List<DecNode> declist = new ArrayList<>();
		for (DecContext dec : c.dec()) declist.add((DecNode) visit(dec));
		return new ProgLetInNode(declist, visit(c.exp()));
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
		if (c.ID().size()>0) { //non-incomplete ST
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
}
