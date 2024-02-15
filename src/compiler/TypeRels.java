package compiler;

import compiler.AST.*;
import compiler.lib.*;

import java.util.HashMap;
import java.util.Map;

public class TypeRels {

	//Map of the super type
	public static Map<String, String> superType = new HashMap<>();


	public static boolean isSubtype(TypeNode a, TypeNode b) {
		return isIntAndBoolType(a, b) ||
				checkSuperTypeHierarchy(a, b) ||
				isEmptyTypeAndRefType(a, b) ||
				checkMethodOverride(a, b);
	}

	public static TypeNode lowestCommonAncestor(TypeNode a, TypeNode b){
		if(isSubtype(a, b)) return b;
		if(isSubtype(b, a)) return a;

		if(!(a instanceof RefTypeNode typeA)) return null;

		var superClass = superType.get(typeA.id);

		while (superClass != null){
			var superTypeOfA = new RefTypeNode(superClass);
			if(isSubtype(b, superTypeOfA)) return superTypeOfA;
			superClass = superType.get(superClass);
		}

		return null;
	}

	private static boolean isIntAndBoolType(TypeNode a, TypeNode b){
		return ((a instanceof BoolTypeNode) && (b instanceof IntTypeNode)) ||
				((a instanceof BoolTypeNode) && (b instanceof BoolTypeNode)) ||
				((a instanceof IntTypeNode) && (b instanceof IntTypeNode));
	}

	private static boolean isEmptyTypeAndRefType(TypeNode a, TypeNode b){
		return ((a instanceof  EmptyTypeNode) && (b instanceof  RefTypeNode));
	}

	private static boolean checkSuperTypeHierarchy(TypeNode a, TypeNode b){
		if(!(a instanceof  RefTypeNode) || !(b instanceof  RefTypeNode)) return false;

		var checkTypeA = ((RefTypeNode) a).id;
		var checkTypeB = ((RefTypeNode) b).id;

		if(checkTypeA.equals(checkTypeB)) return true;

		var superClass = superType.get(checkTypeA);
		while (superClass != null){
			if(superClass.equals(checkTypeB)) return true;
			superClass = superType.get(superClass);
		}
		return false;
	}

	private static boolean checkMethodOverride(TypeNode a, TypeNode b){
		if(!(a instanceof ArrowTypeNode checkTypeA) || !(b instanceof ArrowTypeNode checkTypeB)) return false;

        /* Check covariance of the return type*/
		if(!isSubtype(checkTypeA.ret, checkTypeB.ret)) return false;

		/* Counter-variance of the parameters type*/
		for(int i = 0; i < checkTypeA.parlist.size(); i++){
			if(!isSubtype(checkTypeB.parlist.get(i), checkTypeA.parlist.get(i))){
				return false;
			}
		}
		return true;
	}


}
