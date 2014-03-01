package proteaj.pparser;

import proteaj.error.*;
import proteaj.io.*;
import proteaj.ir.*;
import proteaj.tast.*;

import java.util.*;
import javassist.*;

import static proteaj.util.Modifiers.*;
import static proteaj.util.CtClassUtil.*;

public class StaticMethodCallParser extends PackratParser<StaticMethodCall> {
  /* StaticMethodCall
   *  : ClassName '.' Identifier Arguments
   */
  @Override
  protected ParseResult<StaticMethodCall> parse(SourceStringReader reader, Environment env) {
    final int pos = reader.getPos();

    // ClassName
    ParseResult<CtClass> className = ClassNameParser.parser.applyRule(reader, env);
    if(className.isFail()) return fail(className, pos, reader);

    // '.'
    ParseResult<String> dot = KeywordParser.getParser(".").applyRule(reader, env);
    if(dot.isFail()) return fail(dot, pos, reader);

    // Identifier
    ParseResult<String> identifier = IdentifierParser.parser.applyRule(reader, env);
    if(identifier.isFail()) fail(identifier, pos, reader);

    int apos = reader.getPos();

    // Arguments
    for(CtMethod method : getMethods(className.get())) try {
      if(! (isStatic(method.getModifiers()) && method.visibleFrom(env.thisClass) && method.getName().equals(identifier.get()))) continue;

      ParseResult<List<Expression>> args = ArgumentsParser.getParser(method).applyRule(reader, env, apos);
      if(! args.isFail()) {
        env.addExceptions(method.getExceptionTypes(), reader.getLine());
        return success(new StaticMethodCall(method, args.get()));
      }
    } catch (NotFoundException e) {
      ErrorList.addError(new NotFoundError(e, reader.filePath, reader.getLine()));
    }

    // fail
    return fail("undefined method : " + identifier.get(), pos, reader);
  }

  @Override
  public String toString() {
    return "StaticMethodCallParser";
  }

  public static final StaticMethodCallParser parser = new StaticMethodCallParser();

  private StaticMethodCallParser() {}
}

