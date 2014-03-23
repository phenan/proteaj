package proteaj.pparser;

import proteaj.ir.*;
import proteaj.tast.*;
import proteaj.util.*;

import java.util.*;
import javassist.*;

import static proteaj.pparser.PackratParserCombinators.*;
import static proteaj.pparser.CommonParsers.*;
import static proteaj.pparser.JavaExpressionParsers.*;
import static proteaj.util.CtClassUtil.*;

public class ExpressionParsers {
  public static PackratParser<Expression> expression (final CtClass expected) {
    return depends(new Function<Environment, PackratParser<Expression>>() {
      @Override
      public PackratParser<Expression> apply(Environment env) {
        return expression(expected, env.availableOperators);
      }
    });
  }

  public static PackratParser<Expression> expression (CtClass expected, AvailableOperators operators) {
    return getInstance(operators).getParser(expected);
  }

  public static PackratParser<DefaultValue> defaultArgument (final CtClass expected) {
    return map(expression(expected), new Function<Expression, DefaultValue>() {
      @Override
      public DefaultValue apply(Expression expr) { return new DefaultValue(expr); }
    });
  }

  public static PackratParser<FieldBody> fieldBody (final CtClass expected) {
    return map(expression(expected), new Function<Expression, FieldBody>() {
      @Override
      public FieldBody apply(Expression expr) { return new FieldBody(expr); }
    });
  }

  public static PackratParser<List<Expression>> arguments (final CtBehavior behavior) {
    return depends(new Function<Environment, PackratParser<List<Expression>>>() {
      @Override
      public PackratParser<List<Expression>> apply(Environment env) {
        return arguments(behavior, env.availableOperators);
      }
    });
  }

  public static PackratParser<List<Expression>> arguments (final CtBehavior behavior, AvailableOperators operators) {
    return getInstance(operators).getArgumentsParserFromCache(behavior);
  }

  /* Multiton pattern */

  private static ExpressionParsers getInstance (AvailableOperators operators) {
    if (! instances.containsKey(operators)) instances.put(operators, new ExpressionParsers(operators));
    return instances.get(operators);
  }

  private static Map<AvailableOperators, ExpressionParsers> instances = new WeakHashMap<>();

  private ExpressionParsers(AvailableOperators operators) {
    this.operators = operators;

    this.expressionParsers = new HashMap<>();
    this.operationParsers = new HashMap<>();
    this.defaultParsers = new HashMap<>();
    this.argumentsParsers = new HashMap<>();
    this.readAsExpressionParsers = new HashMap<>();
    this.literalParsers = new HashMap<>();
    this.defaultLiteralParsers = new HashMap<>();
  }

  /* */

  private PackratParser<Expression> getParser (CtClass clazz) {
    TreeMap<Integer, PackratParser<Expression>> tree = getExpressionParsersFromCache(clazz);
    if (! tree.isEmpty()) return tree.firstEntry().getValue();
    else return getDefaultParserFromCache(clazz);
  }

  private PackratParser<Expression> getParser_Ref (final CtClass clazz) {
    return ref(new ParserThunk<Expression>() {
      @Override
      public PackratParser<Expression> evaluate() { return getParser(clazz); }
    });
  }

  private PackratParser<Expression> getParser_Ref(final CtClass clazz, final int priority, final boolean inclusive) {
    return ref(new ParserThunk<Expression>() {
      @Override
      public PackratParser<Expression> evaluate() {
        TreeMap<Integer, PackratParser<Expression>> tree = getExpressionParsersFromCache(clazz);
        Map.Entry<Integer, PackratParser<Expression>> entry = inclusive ? tree.ceilingEntry(priority) : tree.higherEntry(priority);
        if (entry != null) return entry.getValue();
        else return getDefaultParserFromCache(clazz);
      }
    });
  }

  private PackratParser<Expression> getLiteralParser (final CtClass clazz) {
    TreeMap<Integer, PackratParser<Expression>> tree = getReadAsExpressionParsersFromCache(clazz);
    if (! tree.isEmpty()) return tree.firstEntry().getValue();
    else return getDefaultLiteralParserFromCache(clazz);
  }

  private PackratParser<Expression> getLiteralParser_Ref (final CtClass clazz) {
    return ref(new ParserThunk<Expression>() {
      @Override
      public PackratParser<Expression> evaluate() { return getLiteralParser(clazz); }
    });
  }

  private PackratParser<Expression> getLiteralParser_Ref (final CtClass clazz, final int priority, final boolean inclusive) {
    return ref(new ParserThunk<Expression>() {
      @Override
      public PackratParser<Expression> evaluate() {
        TreeMap<Integer, PackratParser<Expression>> tree = getReadAsExpressionParsersFromCache(clazz);
        Map.Entry<Integer, PackratParser<Expression>> entry = inclusive ? tree.ceilingEntry(priority) : tree.higherEntry(priority);
        if (entry != null) return entry.getValue();
        else return getDefaultLiteralParserFromCache(clazz);
      }
    });
  }

  private List<PackratParser<Expression>> getParsers (CtClass[] types) {
    List<PackratParser<Expression>> list = new ArrayList<>();
    for (CtClass clazz : types) list.add(getParser(clazz));
    return list;
  }



  /* private methods for caching parsers */

  private TreeMap<Integer, PackratParser<Expression>> getExpressionParsersFromCache (CtClass clazz) {
    if (! expressionParsers.containsKey(clazz)) expressionParsers.put(clazz, makeExpressionParsers(clazz));
    return expressionParsers.get(clazz);
  }

  private List<PackratParser<? extends Expression>> getOperationParsersFromCache (Integer priority, List<IROperator> list) {
    List<PackratParser<? extends Expression>> parsers = new ArrayList<>();

    if (! operationParsers.containsKey(priority)) operationParsers.put(priority, new HashMap<IROperator, PackratParser<Operation>>());
    Map<IROperator, PackratParser<Operation>> operationParsersCache = operationParsers.get(priority);

    for (IROperator operator : list) {
      if (! operationParsersCache.containsKey(operator)) {
        operationParsersCache.put(operator, makeOperationParser(operator, priority));
      }
      parsers.add(operationParsersCache.get(operator));
    }

    return parsers;
  }

  private PackratParser<Expression> getDefaultParserFromCache (CtClass clazz) {
    if (! defaultParsers.containsKey(clazz)) defaultParsers.put(clazz, makeDefaultParser(clazz));
    return defaultParsers.get(clazz);
  }

  private PackratParser<List<Expression>> getArgumentsParserFromCache (CtBehavior behavior) {
    if (! argumentsParsers.containsKey(behavior)) argumentsParsers.put(behavior, makeArgumentsParser(behavior));
    return argumentsParsers.get(behavior);
  }

  private TreeMap<Integer, PackratParser<Expression>> getReadAsExpressionParsersFromCache (CtClass clazz) {
    if (! readAsExpressionParsers.containsKey(clazz)) readAsExpressionParsers.put(clazz, makeReadAsExpressionParsers(clazz));
    return readAsExpressionParsers.get(clazz);
  }

  private List<PackratParser<? extends Expression>> getReadAsOperationParsersFromCache (Integer priority, List<IROperator> list) {
    List<PackratParser<? extends Expression>> parsers = new ArrayList<>();

    if (! literalParsers.containsKey(priority)) literalParsers.put(priority, new HashMap<IROperator, PackratParser<? extends Expression>>());
    Map<IROperator, PackratParser<? extends Expression>> literalParsersCache = literalParsers.get(priority);

    for (IROperator operator : list) {
      if (! literalParsersCache.containsKey(operator)) {
        literalParsersCache.put(operator, makeLiteralParser(operator, priority));
      }
      parsers.add(literalParsersCache.get(operator));
    }

    return parsers;
  }

  private PackratParser<Expression> getDefaultLiteralParserFromCache (CtClass clazz) {
    if (! defaultLiteralParsers.containsKey(clazz)) defaultLiteralParsers.put(clazz, makeDefaultLiteralParser(clazz));
    return defaultLiteralParsers.get(clazz);
  }

  /* private methods for building parsers */

  private TreeMap<Integer, PackratParser<Expression>> makeExpressionParsers (CtClass clazz) {
    TreeMap<Integer, PackratParser<Expression>> map = new TreeMap<>();

    for (Map.Entry<Integer, List<IROperator>> entry : operators.getOperators(clazz).entrySet()) {
      Integer priority = entry.getKey();
      map.put(priority, choice(getOperationParsersFromCache(priority, entry.getValue())));
    }

    TreeMap<Integer, PackratParser<Expression>> parsers = new TreeMap<>();

    PackratParser<Expression> next = getDefaultParserFromCache(clazz);

    for (Integer priority : map.descendingKeySet()) {
      PackratParser<Expression> cur = choice(map.get(priority), next);
      parsers.put(priority, cur);
      next = cur;
    }

    return parsers;
  }

  private PackratParser<Operation> makeOperationParser (final IROperator operator, final int priority) {
    final IRPattern pattern = operator.getPattern();
    final int length = pattern.getPatternLength();
    final List<Expression> newOperandList = Collections.emptyList();

    PackratParser<List<Expression>> parser = unit(newOperandList);

    for (int i = 0; i < length; i++) {
      if (pattern.isOperator(i)) parser = postfix(parser, pattern.getOperatorKeyword(i));
      else if (pattern.isOperand(i)) parser = map(seq(parser, makeOperandParser(pattern, i, priority)), new Function<Pair<List<Expression>, Expression>, List<Expression>>() {
        @Override
        public List<Expression> apply(Pair<List<Expression>, Expression> pair) {
          List<Expression> list = new ArrayList<>(pair._1);
          list.add(pair._2);
          return list;
        }
      });
      else if (pattern.isAndPredicate(i)) parser = postfix(parser,
          andPredicate(getParser_Ref(pattern.getAndPredicateType(i), priority, pattern.getInclusive(i))));
      else if (pattern.isNotPredicate(i)) parser = postfix(parser,
          notPredicate(getParser_Ref(pattern.getNotPredicateType(i), priority, pattern.getInclusive(i))));
    }

    return effect(map(parser, new Function<List<Expression>, Operation>() {
      @Override
      public Operation apply(List<Expression> operands) { return new Operation(operator, operands); }
    }), throwing(operator));
  }

  private PackratParser<Expression> makeOperandParser (IRPattern pattern, int index, int priority) {
    final CtClass clazz = pattern.getOperandType(index);
    final boolean inclusive = pattern.getInclusive(index);

    PackratParser<Expression> parser = getParser_Ref(clazz, priority, inclusive);

    if (pattern.isVariableOperands(index) || pattern.hasMoreThanOneOperands(index)) {
      final CtClass componentType;
      try { componentType = clazz.getComponentType(); } catch (NotFoundException e) { return error(e); }
      final PackratParser<Expression> componentParser = getParser_Ref(componentType, priority, inclusive);

      final PackratParser<List<Expression>> repParser;
      if (pattern.isVariableOperands(index)) {
        if (! pattern.hasSeparator(index)) repParser = rep(componentParser);
        else repParser = rep(componentParser, pattern.getSeparator(index));
      }
      else {
        if (! pattern.hasSeparator(index)) repParser = rep1(componentParser);
        else repParser = rep1(componentParser, pattern.getSeparator(index));
      }

      return choice(parser, map(repParser, new Function<List<Expression>, Expression>() {
        @Override
        public Expression apply(List<Expression> args) { return new VariableArguments(args, clazz); }
      }));
    }
    else if (pattern.isOptionOperand(index)) {
      final Expression defaultValue;
      try { defaultValue = new StaticMethodCall(pattern.getDefaultMethod(index), Collections.<Expression>emptyList()); }
      catch (NotFoundException e) { return error(e); }
      return optional(parser, defaultValue);
    }
    else return parser;
  }

  private PackratParser<Expression> makeDefaultParser (final CtClass clazz) {
    PackratParser<Expression> parenthesized = enclosed("(", getParser_Ref(clazz), ")");

    PackratParser<CastExpression> rCast = bind(enclosed("(", infix(typeName, "->", optional(typeName, clazz)), ")"),
        new Function<Pair<CtClass, CtClass>, PackratParser<CastExpression>>() {
          @Override
          public PackratParser<CastExpression> apply(final Pair<CtClass, CtClass> pair) {
            return castBody(clazz, pair._1, pair._2);
          }
        });

    PackratParser<CastExpression> lCast = bind(enclosed("(", infix(optional(typeName, clazz), "<-", typeName), ")"),
        new Function<Pair<CtClass, CtClass>, PackratParser<CastExpression>>() {
          @Override
          public PackratParser<CastExpression> apply(Pair<CtClass, CtClass> pair) {
            return castBody(clazz, pair._2, pair._1);
          }
        });

    PackratParser<Expression> javaExpr = bind(javaExpression, new Function<Expression, PackratParser<Expression>>() {
      @Override
      public PackratParser<Expression> apply(Expression expr) {
        try {
          if (expr.getType().subtypeOf(clazz) || clazz == CtClass.voidType) return unit(expr);
          else return failure("type mismatch: expected " + clazz.getName() + " but found " + expr.getType().getName());
        } catch (NotFoundException e) { return error(e); }
      }
    });

    PackratParser<NullLiteral> nullLiteral = map(keyword("null"), new Function<String, NullLiteral>() {
      @Override
      public NullLiteral apply(String s) { return NullLiteral.instance; }
    });

    PackratParser<Expression> readAs = prefix(whitespaces, getLiteralParser(clazz));

    return choice(parenthesized, rCast, lCast, javaExpr, nullLiteral, readAs);
  }

  private PackratParser<CastExpression> castBody (final CtClass clazz, final CtClass from, final CtClass to) {
    return bind(getParser_Ref(from), new Function<Expression, PackratParser<CastExpression>>() {
      @Override
      public PackratParser<CastExpression> apply(Expression expr) {
        try {
          if (! to.subtypeOf(clazz))
            return failure("type mismatch: expected" + clazz.getName() + " but found " + to.getName());
          if (! isCastable(from, to))
            return failure(from + " cannot cast to " + to.getName());
        } catch (NotFoundException e) { return error(e); }
        return unit(new CastExpression(to, expr));
      }
    });
  }

  private PackratParser<List<Expression>> makeArgumentsParser (final CtBehavior behavior) {
    final CtClass[] argTypes;
    try { argTypes = behavior.getParameterTypes(); } catch (NotFoundException e) { return error(e); }

    final int length = argTypes.length;
    if (length == 0) return map(keywords("(", ")"), new Function<String[], List<Expression>>() {
      @Override
      public List<Expression> apply(String[] pair) { return Collections.emptyList(); }
    });

    List<PackratParser<Expression>> argParsers = getParsers(argTypes);

    PackratParser<List<Expression>> normalParser = sequence(argParsers, ",");

    if (Modifiers.hasVarArgs(behavior)) {
      final CtClass arrayType = argTypes[length - 1];
      final CtClass componentType;
      try { componentType = arrayType.getComponentType(); } catch (NotFoundException e) { return error(e); }

      if (length == 1) {
        PackratParser<List<Expression>> varArgsParser = map(rep(getParser_Ref(componentType), ","), new Function<List<Expression>, List<Expression>>() {
          @Override
          public List<Expression> apply(List<Expression> es) {
            List<Expression> args = new ArrayList<>();
            args.add(new VariableArguments(es, arrayType));
            return args;
          }
        });
        return enclosed("(", choice(normalParser, varArgsParser), ")");
      }
      else {
        PackratParser<List<Expression>> otherArgsParser = sequence(argParsers.subList(0, length - 1), ",");

        PackratParser<List<Expression>> varArgsNParser =
            map(infix(otherArgsParser, ",", rep1(getParser_Ref(componentType), ",")), new Function<Pair<List<Expression>, List<Expression>>, List<Expression>>() {
              @Override
              public List<Expression> apply(Pair<List<Expression>, List<Expression>> pair) {
                List<Expression> args = new ArrayList<>(pair._1);
                args.add(new VariableArguments(pair._2, arrayType));
                return args;
              }
            });

        PackratParser<List<Expression>> varArgs0Parser = map(otherArgsParser, new Function<List<Expression>, List<Expression>>() {
          @Override
          public List<Expression> apply(List<Expression> args) {
            List<Expression> vArgs = new ArrayList<>(args);
            vArgs.add(new VariableArguments(arrayType));
            return vArgs;
          }
        });

        return enclosed("(", choice(normalParser, varArgsNParser, varArgs0Parser), ")");
      }
    }
    else return enclosed("(", normalParser, ")");
  }

  private TreeMap<Integer, PackratParser<Expression>> makeReadAsExpressionParsers (CtClass clazz) {
    TreeMap<Integer, PackratParser<Expression>> map = new TreeMap<>();

    for (Map.Entry<Integer, List<IROperator>> entry : operators.getReadAsOperators(clazz).entrySet()) {
      Integer priority = entry.getKey();
      map.put(priority, choice(getReadAsOperationParsersFromCache(priority, entry.getValue())));
    }

    TreeMap<Integer, PackratParser<Expression>> parsers = new TreeMap<>();

    PackratParser<Expression> next = getDefaultLiteralParserFromCache(clazz);

    for (Integer priority : map.descendingKeySet()) {
      PackratParser<Expression> cur = choice(map.get(priority), next);
      parsers.put(priority, cur);
      next = cur;
    }

    return parsers;
  }

  private PackratParser<? extends Expression> makeLiteralParser (final IROperator operator, Integer priority) {
    final IRPattern pattern = operator.getPattern();
    if (pattern.isDummy()) return makePrimitiveLiteralParser(operator.getReturnType());

    final int length = pattern.getPatternLength();
    final List<Expression> newOperandList = Collections.emptyList();

    PackratParser<List<Expression>> parser = unit(newOperandList);

    for (int i = 0; i < length; i++) {
      if (pattern.isOperator(i)) parser = postfix(parser, element(pattern.getOperatorKeyword(i)));
      else if (pattern.isOperand(i)) parser = map(seq(parser, makeReadAsOperandParser(pattern, i, priority)), new Function<Pair<List<Expression>, Expression>, List<Expression>>() {
        @Override
        public List<Expression> apply(Pair<List<Expression>, Expression> pair) {
          List<Expression> list = new ArrayList<>(pair._1);
          list.add(pair._2);
          return list;
        }
      });
      else if (pattern.isAndPredicate(i)) parser = postfix(parser,
          andPredicate(getLiteralParser_Ref(pattern.getAndPredicateType(i), priority, pattern.getInclusive(i))));
      else if (pattern.isNotPredicate(i)) parser = postfix(parser,
          notPredicate(getLiteralParser_Ref(pattern.getNotPredicateType(i), priority, pattern.getInclusive(i))));
    }

    return effect(map(parser, new Function<List<Expression>, Expression>() {
      @Override
      public Expression apply(List<Expression> operands) { return new Operation(operator, operands); }
    }), throwing(operator));
  }

  private PackratParser<? extends Expression> makePrimitiveLiteralParser (final CtClass type) {
    if (type.equals(IRCommonTypes.getIdentifierType())) {
      return bind(identifier, new Function<String, PackratParser<NewExpression>>() {
        @Override
        public PackratParser<NewExpression> apply(String s) {
          final CtConstructor constructor;
          try { constructor = getConstructor(type, IRCommonTypes.getStringType()); } catch (NotFoundException e) {
            return error(e);
          }
          return unit(new NewExpression(constructor, Arrays.<Expression>asList(new StringLiteral(s))));
        }
      });
    }
    else if (type.equals(IRCommonTypes.getLetterType())) {
      return bind(letter, new Function<Character, PackratParser<NewExpression>>() {
        @Override
        public PackratParser<NewExpression> apply(Character character) {
          final CtConstructor constructor;
          try { constructor = getConstructor(type, CtClass.charType); } catch (NotFoundException e) {
            return error(e);
          }
          return unit(new NewExpression(constructor, Arrays.<Expression>asList(new CharLiteral(character))));
        }
      });

    }
    else if (type.equals(IRCommonTypes.getTypeType())) {
      return bind(typeName, new Function<CtClass, PackratParser<NewExpression>>() {
        @Override
        public PackratParser<NewExpression> apply(CtClass clazz) {
          final CtConstructor constructor;
          try { constructor = getConstructor(type, IRCommonTypes.getClassType()); } catch (NotFoundException e) {
            return error(e);
          }
          return unit(new NewExpression(constructor, Arrays.<Expression>asList(new ClassLiteral(clazz))));
        }
      });
    }
    else {
      assert false;
      throw new RuntimeException("unknown primitive readas operator");
    }
  }

  private PackratParser<Expression> makeReadAsOperandParser (IRPattern pattern, int index, int priority) {
    final CtClass clazz = pattern.getOperandType(index);
    final boolean inclusive = pattern.getInclusive(index);

    PackratParser<Expression> parser = getLiteralParser_Ref(clazz, priority, inclusive);

    if (pattern.isVariableOperands(index) || pattern.hasMoreThanOneOperands(index)) {
      final CtClass componentType;
      try { componentType = clazz.getComponentType(); } catch (NotFoundException e) { return error(e); }
      final PackratParser<Expression> componentParser = getLiteralParser_Ref(componentType, priority, inclusive);

      final PackratParser<List<Expression>> repParser;
      if (pattern.isVariableOperands(index)) {
        if (! pattern.hasSeparator(index)) repParser = rep(componentParser);
        else repParser = rep(componentParser, element(pattern.getSeparator(index)));
      }
      else {
        if (! pattern.hasSeparator(index)) repParser = rep1(componentParser);
        else repParser = rep1(componentParser, element(pattern.getSeparator(index)));
      }

      return choice(parser, map(repParser, new Function<List<Expression>, Expression>() {
        @Override
        public Expression apply(List<Expression> args) { return new VariableArguments(args, clazz); }
      }));
    }
    else if (pattern.isOptionOperand(index)) {
      final Expression defaultValue;
      try { defaultValue = new StaticMethodCall(pattern.getDefaultMethod(index), Collections.<Expression>emptyList()); }
      catch (NotFoundException e) { return error(e); }
      return optional(parser, defaultValue);
    }
    else return parser;
  }

  private PackratParser<Expression> makeDefaultLiteralParser (final CtClass clazz) {
    PackratParser<Expression> parenthesized = enclosed("(", getLiteralParser_Ref(clazz), ")");
    return choice(parenthesized, bind(untilWhitespace, new Function<String, PackratParser<Expression>>() {
      @Override
      public PackratParser<Expression> apply(String s) {
        return failure("fail to parse as a " + clazz.getName() + " literal : " + s);
      }
    }));
  }

  private final AvailableOperators operators;

  private Map<CtClass, TreeMap<Integer, PackratParser<Expression>>> expressionParsers;
  private Map<Integer, Map<IROperator, PackratParser<Operation>>> operationParsers;
  private Map<CtClass, PackratParser<Expression>> defaultParsers;
  private Map<CtBehavior, PackratParser<List<Expression>>> argumentsParsers;
  private Map<CtClass, TreeMap<Integer, PackratParser<Expression>>> readAsExpressionParsers;
  private Map<Integer, Map<IROperator, PackratParser<? extends Expression>>> literalParsers;
  private Map<CtClass, PackratParser<Expression>> defaultLiteralParsers;
}