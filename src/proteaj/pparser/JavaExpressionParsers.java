package proteaj.pparser;

import javassist.*;

import proteaj.tast.*;
import proteaj.util.Pair;

import static proteaj.pparser.PackratParserCombinators.*;
import static proteaj.pparser.CommonParsers.*;
import static proteaj.pparser.ExpressionParsers.*;
import static proteaj.util.CtClassUtil.*;
import static proteaj.util.Modifiers.*;

public class JavaExpressionParsers {
  public static final PackratParser<Expression> ref_JavaExpression = ref(new ParserThunk<Expression>() {
    @Override
    public PackratParser<Expression> evaluate() {
      return javaExpression;
    }
  });

  private static final PackratParser<Expression> ref_DotAccess = ref(new ParserThunk<Expression>() {
    @Override
    public PackratParser<Expression> evaluate() { return dotAccess; }
  });

  private static final PackratParser<AssignExpression> assignment =
      bind(postfix(ref_DotAccess, "="), left -> map(expression(left.type), right -> new AssignExpression(left, right)));

  private static final PackratParser<ArrayLength> arrayLength =
      bind(postfix(postfix(ref_DotAccess, "."), "length"), expr -> {
        if (expr.type.isArray()) return unit(new ArrayLength(expr));
        else return failure("not array type");
      });

  private static final PackratParser<Pair<Expression, String>> exprDotIdentifier = infix(ref_DotAccess, ".", identifier);

  private static final PackratParser<MethodCall> methodCall =
      bind(exprDotIdentifier, pair -> depends(env ->
          foreach(env.getInstanceMethods(pair._1.type, pair._2), method -> methodCallArgs(pair._1, method), "method " + pair._2 + " is not found in " + pair._1.type.getName())));

  private static PackratParser<MethodCall> methodCallArgs (Expression receiver, CtMethod method) {
    return effect(bind(arguments(method), args -> {
      try { return unit(new MethodCall(receiver, method, args)); }
      catch (NotFoundException e) { return error(e); }
    }), throwing(method));
  }

  private static final PackratParser<FieldAccess> fieldAccess =
      bind(exprDotIdentifier, pair -> depends(env -> getInstanceField(pair._1, pair._2, env)));

  private static final PackratParser<Expression> arrayIndex = enclosed("[", expression(CtClass.intType), "]");

  private static final PackratParser<ArrayAccess> arrayAccess =
      bind(seq(ref_DotAccess, arrayIndex), pair -> {
        if (pair._1.type.isArray()) try {
          return unit(new ArrayAccess(pair._1, pair._2));
        } catch (NotFoundException e) { return error(e); }
        else return failure("not array type");
      });

  private static final PackratParser<StaticMethodCall> abbStaticMethodCall =
      bind(identifier, s -> depends(env -> foreach(env.getStaticMethods(env.thisClass, s), method -> staticMethodCallArgs(method), "undefined method: " + s)));

  private static PackratParser<StaticMethodCall> staticMethodCallArgs (CtMethod method) {
    return effect(bind(arguments(method), args -> {
      try { return unit(new StaticMethodCall(method, args)); }
      catch (NotFoundException e) { return error(e); }
    }), throwing(method));
  }

  private static final PackratParser<MethodCall> abbInstanceMethodCall =
      bind(identifier, s -> depends(env -> env.isStatic() ? failure("cannot abbreviate a receiver of an instance method") : foreach(env.getInstanceMethods(env.thisClass, s), method -> methodCallArgs(env.get("this"), method), "undefined method: " + s)));

  private static final PackratParser<Expression> abbMethodCall =
      choice(abbStaticMethodCall, abbInstanceMethodCall);

  private static final PackratParser<Expression> variable =
      bind(identifier, s -> depends(env -> env.contains(s) ? unit(env.get(s)) : failure("unknown variable: " + s, 5)));

  private static final PackratParser<Pair<CtClass, String>> classDotIdentifier = infix(className, ".", identifier);

  private static final PackratParser<StaticMethodCall> staticMethodCall =
      bind(classDotIdentifier, pair -> depends(env -> foreach(env.getStaticMethods(pair._1, pair._2), method -> staticMethodCallArgs(method), "suitable static method is not found")));

  private static final PackratParser<StaticFieldAccess> staticFieldAccess =
      bind(classDotIdentifier, pair -> depends(env -> getStaticField(pair._1, pair._2, env)));

  private static PackratParser<FieldAccess> getInstanceField (Expression expr, String name, Environment env) {
    CtField field;
    try { field = expr.type.getField(name); } catch (NotFoundException e) {
      return failure("field " + name + " is not found in " + expr.type.getName(), 10);
    }
    if (! env.isVisible(field)) return failure("field " + expr.type.getName() + '.' + name + " is not visible from " + env.thisClass.getName(), 10);
    if (isStatic(field)) return failure("field " + expr.type.getName() + '.' + name + " is a static field", 10);

    try { return unit(new FieldAccess(expr, field)); } catch (NotFoundException e) {
      return error(e);
    }
  }

  private static PackratParser<StaticFieldAccess> getStaticField (CtClass clazz, String name, Environment env) {
    CtField field;
    try { field = clazz.getField(name); } catch (NotFoundException e) {
      return failure("field " + name + " is not found in " + clazz.getName(), 10);
    }
    if (! env.isVisible(field)) return failure("field " + clazz.getName() + '.' + name + " is not visible from " + env.thisClass.getName(), 10);
    if (! isStatic(field)) return failure("field " + clazz.getName() + '.' + name + " is not a static field", 10);

    try { return unit(new StaticFieldAccess(field)); } catch (NotFoundException e) {
      return error(e);
    }
  }

  private static final PackratParser<NewExpression> newObject =
      bind(prefix("new", className), clazz -> foreach(clazz.getDeclaredConstructors(), c -> effect(map(arguments(c), args -> new NewExpression(c, args)), throwing(c)), "suitable constructor is not found"));

  private static final PackratParser<NewArrayExpression> newArray =
      bind(seq(prefix("new", className), rep1(arrayIndex), rep(keywords("[", "]"))), triad -> depends(env -> {
        int dim = triad._2.size() + triad._3.size();
        CtClass arrayType = env.getArrayType(triad._1, dim);
        return unit(new NewArrayExpression(arrayType, triad._2));
      }));


  private static final PackratParser<ArrayInitializer> arrayInit =
      bind(prefix("new", typeName), type -> {
        CtClass component;
        try { component = type.getComponentType(); } catch (NotFoundException e) { return error(e); }
        return map(enclosed("{", rep(expression(component), ","), "}"), list -> new ArrayInitializer(type, list));
      });

  private static final PackratParser<CastExpression> cast =
      bind(seq(enclosed("(", typeName, ")"), ref_DotAccess), pair -> {
        try {
          if (isCastable(pair._2.type, pair._1)) return unit(new CastExpression(pair._1, pair._2));
          else return failure(pair._2.type.getName() + " cannot cast to " + pair._1.getName(), 10);
        } catch (NotFoundException e) { return error(e); }
      });

  private static final PackratParser<Expression> parenthesized =
      enclosed("(", ref_JavaExpression, ")");

  private static final PackratParser<IntLiteral> intLiteral = map(integer, IntLiteral::new);

  private static final PackratParser<IntLiteral> hexLiteral = map(hexadecimal, IntLiteral::new);

  private static final PackratParser<FloatLiteral> floatLiteral = map(floatConst, FloatLiteral::new);

  private static final PackratParser<DoubleLiteral> doubleLiteral = map(decimal, DoubleLiteral::new);

  private static final PackratParser<BooleanLiteral> trueLiteral = map(keyword("true"), s -> new BooleanLiteral(true));

  private static final PackratParser<BooleanLiteral> falseLiteral = map(keyword("false"), s -> new BooleanLiteral(false));

  private static final PackratParser<BooleanLiteral> booleanLiteral = choice(trueLiteral, falseLiteral);

  private static final PackratParser<StringLiteral> stringLiteral = map(string, StringLiteral::new);

  private static final PackratParser<CharLiteral> charLiteral = map(character, CharLiteral::new);

  private static final PackratParser<ClassLiteral> classLiteral = map(postfix(postfix(className, "."), "class"), ClassLiteral::new);

  private static final PackratParser<Expression> literal =
      choice(hexLiteral, floatLiteral, doubleLiteral, intLiteral, booleanLiteral, stringLiteral, charLiteral, classLiteral);

  private static final PackratParser<Expression> primary =
      choice(abbMethodCall, variable, staticMethodCall, staticFieldAccess, newObject, newArray, arrayInit, parenthesized, literal);

  private static final PackratParser<Expression> dotAccess =
      choice(arrayAccess, methodCall, arrayLength, fieldAccess, primary);

  private static final PackratParser<Expression> javaExpression =
      choice(assignment, cast, ref_DotAccess);
}
