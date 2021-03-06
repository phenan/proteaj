package proteaj.ir.primitive;

import proteaj.ir.*;
import proteaj.env.type.CommonTypes;

import javassist.CtClass;

public class PrimitiveReadasSyntax extends IRSyntax {
  public static PrimitiveReadasSyntax getSyntax() {
    if(syntax == null) syntax = new PrimitiveReadasSyntax();
    return syntax;
  }

  private PrimitiveReadasSyntax() {
    super("proteaj.lang.PrimitiveReadasOperators", "(no source)");

    CommonTypes cts = CommonTypes.getInstance();

    loadOperator(cts.identifierType);
    loadOperator(cts.letterType);
    loadOperator(cts.typeType);
  }

  private void loadOperator (CtClass clazz) {
    if (clazz != null) addOperator(new PrimitiveReadasOperator(clazz));
  }

  private static PrimitiveReadasSyntax syntax = null;
}

