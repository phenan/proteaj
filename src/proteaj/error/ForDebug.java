package proteaj.error;

import proteaj.tast.Expression;
import proteaj.codegen.JavaCodeGenerator;

import java.util.HashSet;
import java.util.Set;

public class ForDebug {
  public static void print(String msg) {
    if (verbose) {
//      if (! msgs.contains(msg)) {
//        msgs.add(msg);
        System.out.println(msg);
//      }
    }
  }

  public static void print (Object o) {
    if (verbose) {
      print("[ parse success ] " + o.toString());
    }
  }

  public static void setVerboseFlag() {
    verbose = true;
    msgs = new HashSet<>();
  }

  private static boolean verbose = false;
  private static Set<String> msgs;
}
