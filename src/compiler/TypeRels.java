package compiler;

import compiler.AST.*;
import compiler.lib.*;

import java.util.HashMap;
import java.util.Map;

/*
* Class TypeRels
* Mainly used to control subtype
* */
public class TypeRels {

	//Map of the super type for each type
	public static Map<String, String> superType = new HashMap<>();

<<<<<<< HEAD

	/*
	* Check if the first type is a subtype of the second type
	* */
=======
>>>>>>> 4c65694ac753335811a69f03293c161fe4366047
	public static boolean isSubtype(TypeNode a, TypeNode b) {
		return isIntAndBoolType(a, b) ||
				checkSuperTypeHierarchy(a, b) ||
				isEmptyTypeAndRefType(a, b) ||
				checkMethodOverride(a, b);
	}


	/*
	* Used for compute if then else problem
	* Traverse the inheritance tree of the first type and
	* Check if the second type is a subtype of any the super types.
	* If it is, the common minimum supertype is identified.
	* */
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

	/*
	* Check the subtyping of the bool and int
	* It's necessary for if then else
	* */
	private static boolean isIntAndBoolType(TypeNode a, TypeNode b){
		return ((a instanceof BoolTypeNode) && (b instanceof IntTypeNode)) ||
				((a instanceof BoolTypeNode) && (b instanceof BoolTypeNode)) ||
				((a instanceof IntTypeNode) && (b instanceof IntTypeNode));
	}






	/*
	 * Check if the first type is empty type and the second is refType
	 * Used to the null value
	 * */
	private static boolean isEmptyTypeAndRefType(TypeNode a, TypeNode b){
		return ((a instanceof  EmptyTypeNode) && (b instanceof  RefTypeNode));
	}

	/*
	 *	Check the Hierarchy of type
	 *	Check if the first and the second type is refType using the super type
	 * */
	private static boolean checkSuperTypeHierarchy(TypeNode a, TypeNode b){
		if(!(a instanceof  RefTypeNode) || !(b instanceof  RefTypeNode)) return false;

		var checkTypeA = ((RefTypeNode) a).id;
		var checkTypeB = ((RefTypeNode) b).id;

		if(checkTypeA.equals(checkTypeB)) return true;

        return superType.get(checkTypeA).equals(checkTypeB);
    }


	/*
	 *	Check if the first and the second type is arrowType
	 *	and check if the first is subtype of the second
	 * 	Is an abstraction of the method overriding
	 * */
	private static boolean checkMethodOverride(TypeNode a, TypeNode b){
		if(!(a instanceof ArrowTypeNode checkTypeA) || !(b instanceof ArrowTypeNode checkTypeB)) return false;

        /* Check covariance of the return type */
		if(!isSubtype(checkTypeA.ret, checkTypeB.ret)) return false;

		/* Counter-variance of the parameters type*/
		for(int i = 0; i < checkTypeA.parlist.size(); i++){ // TODO: index i is useless in this case and can be optimized
			if(!isSubtype(checkTypeB.parlist.get(i), checkTypeA.parlist.get(i))){
				return false;
			}
		}
		return true;
	}


}
