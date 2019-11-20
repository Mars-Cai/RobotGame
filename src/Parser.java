import java.io.File;
import java.io.FileNotFoundException;
import java.util.*;
import java.util.regex.*;
import javax.swing.JFileChooser;

/**
 * The parser and interpreter. The top level parse function, a main method for testing, and several
 * utility methods are provided. You need to implement parseProgram and all the rest of the parser.
 */
public class Parser {

  static HashMap<String, Integer> variables = new HashMap<String, Integer>(); // Holds variables

  /**
   * Top level parse method, called by the World
   */
  static RobotProgramNode parseFile(File code) {
    Scanner scan = null;
    try {
      scan = new Scanner(code);

      // the only time tokens can be next to each other is
      // when one of them is one of (){},;
      scan.useDelimiter("\\s+|(?=[{}(),;])|(?<=[{}(),;])");

      RobotProgramNode n = parseProgram(scan); // You need to implement this!!!

      scan.close();
      return n;
    } catch (FileNotFoundException e) {
      System.out.println("Robot program source file not found");
    } catch (ParserFailureException e) {
      System.out.println("Parser error:");
      System.out.println(e.getMessage());
      scan.close();
    }
    return null;
  }

  /** For testing the parser without requiring the world */

  public static void main(String[] args) {
    if (args.length > 0) {
      for (String arg : args) {
        File f = new File(arg);
        if (f.exists()) {
          System.out.println("Parsing '" + f + "'");
          RobotProgramNode prog = parseFile(f);
          System.out.println("Parsing completed ");
          if (prog != null) {
            System.out.println("================\nProgram:");
            System.out.println(prog);
          }
          System.out.println("=================");
        } else {
          System.out.println("Can't find file '" + f + "'");
        }
      }
    } else {
      while (true) {
        JFileChooser chooser = new JFileChooser(".");// System.getProperty("user.dir"));
        int res = chooser.showOpenDialog(null);
        if (res != JFileChooser.APPROVE_OPTION) {
          break;
        }
        RobotProgramNode prog = parseFile(chooser.getSelectedFile());
        System.out.println("Parsing completed");
        if (prog != null) {
          System.out.println("Program: \n" + prog);
        }
        System.out.println("=================");
      }
    }
    System.out.println("Done");
  }

  // Useful Patterns

  static Pattern NUMPAT = Pattern.compile("-?\\d+"); // ("-?(0|[1-9][0-9]*)");
  static Pattern OPENPAREN = Pattern.compile("\\(");
  static Pattern CLOSEPAREN = Pattern.compile("\\)");
  static Pattern OPENBRACE = Pattern.compile("\\{");
  static Pattern CLOSEBRACE = Pattern.compile("\\}");

  // Added patterns
  static Pattern ACT =
      Pattern.compile("move|wait|turnAround|turnL|turnR|shieldOn|shieldOff|takeFuel");
  static Pattern COND = Pattern.compile("eq|gt|lt|and|or|not");
  static Pattern SENS =
      Pattern.compile("barrelFB|barrelLR|fuelLeft|numBarrels|oppFB|oppLR|wallDist");
  static Pattern OP = Pattern.compile("add|sub|mul|div");
  static Pattern ELSE = Pattern.compile("else|elif");
  static Pattern VAR = Pattern.compile("\\$[A-Za-z][A-Za-z0-9]*");

  /**
   * PROG ::= STMT+
   */
  static RobotProgramNode parseProgram(Scanner s) {
    List<RobotProgramNode> nodes = new ArrayList<RobotProgramNode>();
    do {
      nodes.add(parseStmt(s));
    } while (s.hasNext());
    return new programNode(nodes);
  }

  // utility methods for the parser

  private static RobotProgramNode parseStmt(Scanner s) {
    if (s.hasNext("loop"))
      return parseLoop(s);
    if (s.hasNext("if"))
      return parseIf(s);
    if (s.hasNext("while"))
      return parseWhile(s);
    if (s.hasNext(ACT))
      return parseACT(s);
    fail("NO any valid statement found", s);
    return null;
  }

  /*
   * parsing assgn
   */

  static RobotProgramNode parseAssgn(Scanner s) {
    if (s.hasNext(VAR)) {
      String name = s.next();

      if (s.hasNext("=")) {
        s.next();
        expressionNode expression = parseExprNode(s);

        if (s.hasNext(";")) {
          s.next();
          return new assgnNode(name, expression);
        }
        fail("';' not found after variable declaration", s);
      }
      fail("'=' not found after variable name", s);
    }
    fail("Invalid variable name", s);

    return null;
  }

  /*
   * parsing ACT nodes
   */

  private static RobotProgramNode parseACT(Scanner s) {
    RobotProgramNode node = null;
    if (s.hasNext("move")) {
      node = parseMove(s);
      if (!gobble(";", s)) {
        fail("';' expected after move", s);
      }
    } else if (s.hasNext("turnL")) {
      node = parseTurnL(s);
      if (!gobble(";", s)) {
        fail("';'expected after turnL", s);
      }
    } else if (s.hasNext("turnR")) {
      node = parseTurnR(s);
      if (!gobble(";", s)) {
        fail("';'expected after turnR", s);
      }
    } else if (s.hasNext("takeFuel")) {
      node = parseTakeFuel(s);
      if (!gobble(";", s)) {
        fail("';'expected after takeFuel", s);
      }
    } else if (s.hasNext("wait")) {
      node = parseWait(s);
      if (!gobble(";", s)) {
        fail("';'expected after wait", s);
      }
    } else if (s.hasNext("shieldOn")) {
      node = parseShieldOn(s);
      if (!gobble(";", s)) {
        fail("';'expected after shieldOn", s);
      }
    } else if (s.hasNext("shieldOff")) {
      node = parseShieldOff(s);
      if (!gobble(";", s)) {
        fail("';'expected after shieldOff", s);
      }
    } else if (s.hasNext("turnAround")) {
      node = parseTurnAround(s);
      if (!gobble(";", s)) {
        fail("';'expected after turnAround", s);
      }
    } else
      fail("expected valid Act node", s);
    return node;
  }


  /*
   * parsing ACT nodes
   */
  private static RobotProgramNode parseMove(Scanner s) {
    expressionNode n = null;
    if (!gobble("move", s)) {
      fail("'move' expected", s);
    }
    if (s.hasNext(OPENPAREN)) {
      if (!gobble(OPENPAREN, s)) {
        fail("'(' expected before move's parameters", s);
      }
      n = parseExprNode(s);
      if (!gobble(CLOSEPAREN, s)) {
        fail("')' expected after move's parameters", s);
      }
    }
    return new moveNode(n);
  }

  private static RobotProgramNode parseTurnL(Scanner s) {
    if (!gobble("turnL", s)) {
      fail("'turnL' expected", s);
    }
    return new turnLNode();
  }

  private static RobotProgramNode parseTurnR(Scanner s) {
    if (!gobble("turnR", s)) {
      fail("'turnR' expected", s);
    }
    return new turnRNode();
  }

  private static RobotProgramNode parseTurnAround(Scanner s) {
    if (!gobble("turnAround", s)) {
      fail("turnAround expected", s);
    }
    return new turnAroundNode();
  }

  private static RobotProgramNode parseShieldOn(Scanner s) {
    if (!gobble("shieldOn", s)) {
      fail("shieldOn expected", s);
    }
    return new shieldOnNode();
  }

  private static RobotProgramNode parseShieldOff(Scanner s) {
    if (!gobble("shieldOff", s)) {
      fail("shieldOff expected", s);
    }
    return new shieldOffNode();
  }

  private static RobotProgramNode parseTakeFuel(Scanner s) {
    if (!gobble("takeFuel", s)) {
      fail("'takeFuel' expected", s);
    }
    return new takeFuelNode();
  }

  private static RobotProgramNode parseWait(Scanner s) {
    expressionNode n = null;
    if (!gobble("wait", s)) {
      fail("expected wait", s);
    }
    if (s.hasNext(OPENPAREN)) {
      if (!gobble(OPENPAREN, s)) {
        fail("expected an ( before wait's parameters", s);
      }
      n = parseExprNode(s);
      if (!gobble(CLOSEPAREN, s)) {
        fail("expected an ) after wait's parameters", s);
      }
    }
    return new waitNode(n);
  }


  /*
   * parsing while nodes
   */

  private static RobotProgramNode parseWhile(Scanner s) {
    if (!gobble("while", s)) {
      fail("'while' expected", s);
    }
    if (!gobble(OPENPAREN, s)) {
      fail("'(' expected atfer while", s);
    }
    conditionNode c = (conditionNode) parseCondition(s);
    if (!gobble(CLOSEPAREN, s)) {
      fail("')' expected after condition", s);
    }
    return new whileNode(c, (blockNode) parseBlock(s));
  }

  /*
   * parsing if nodes
   */

  private static RobotProgramNode parseIf(Scanner s) {
    ArrayList<conditionNode> conditions = new ArrayList<conditionNode>();
    ArrayList<RobotProgramNode> blocks = new ArrayList<RobotProgramNode>();
    // check the all items that needed, start with 'if'
    if (!gobble("if", s)) {
      fail("expected if", s);
    }
    if (!gobble(OPENPAREN, s)) {
      fail("'(' expected after if", s);
    }
    conditions.add(parseCondition(s));
    if (!gobble(CLOSEPAREN, s)) {
      fail("')' expected after if statement's condition", s);
    }
    blocks.add(parseBlock(s));
    // and then check if there is 'elif'
    while (s.hasNext("elif")) {
      if (!gobble("elif", s)) {
        fail("'elif' expected", s);
      }
      if (!gobble(OPENPAREN, s)) {
        fail("'(' expected after elif", s);
      }
      conditions.add(parseCondition(s));
      if (!gobble(CLOSEPAREN, s)) {
        fail("')' expected after elif's condition", s);
      }
      blocks.add(parseBlock(s));
    }
    // then check if there is "else"
    RobotProgramNode elseNode = new nullNode();

    if (s.hasNext("else"))
      elseNode = parseElse(s);

    // finally return the whole node
    return new ifNode(conditions, blocks, elseNode);
  }

  /*
   * parsing else nodes
   */

  private static RobotProgramNode parseElse(Scanner s) {
    if (!gobble("else", s)) {
      fail("saw 'else' but didn't gobble it for some reason", s);
    }
    return parseBlock(s);
  }


  /*
   * Parsing Condition nodes
   */
  private static conditionNode parseCondition(Scanner s) {
    if (s.hasNext("lt")) {
      return parseLT(s);
    } else if (s.hasNext("gt")) {
      return parseGT(s);
    } else if (s.hasNext("eq")) {
      return parseEQ(s);
    } else if (s.hasNext("or")) {
      return parseOr(s);
    } else if (s.hasNext("and")) {
      return parseAnd(s);
    } else if (s.hasNext("not")) {
      return parseNot(s);
    }
    fail("NO any valid condition node found", s);
    return null;
  }

  private static conditionNode parseLT(Scanner s) {
    if (!gobble("lt", s)) {
      fail("'lt' expected", s);
    }
    if (!gobble(OPENPAREN, s)) {
      fail("'(' expected followed by parameters", s);
    }
    expressionNode lhs = parseExprNode(s);
    if (!gobble(",", s)) {
      fail("expected , between parameters", s);
    }
    expressionNode rhs = parseExprNode(s);
    if (!gobble(CLOSEPAREN, s)) {
      fail("')' expected after parameters", s);
    }
    return new condLTNode(lhs, rhs);
  }

  private static conditionNode parseGT(Scanner s) {
    if (!gobble("gt", s)) {
      fail("expected gt", s);
    }
    if (!gobble(OPENPAREN, s)) {
      fail("'(' expected followed by parameters", s);
    }
    expressionNode lhs = parseExprNode(s);
    if (!gobble(",", s)) {
      fail("expected , between parameters", s);
    }
    expressionNode rhs = parseExprNode(s);
    if (!gobble(CLOSEPAREN, s)) {
      fail("')' expected after parameters", s);
    }
    return new condGTNode(lhs, rhs);
  }

  private static conditionNode parseEQ(Scanner s) {
    if (!gobble("eq", s)) {
      fail("expected eq", s);
    }
    if (!gobble(OPENPAREN, s)) {
      fail("'(' expected followed by parameters", s);
    }
    expressionNode lhs = parseExprNode(s);
    if (!gobble(",", s)) {
      fail("expected , between parameters", s);
    }
    expressionNode rhs = parseExprNode(s);
    if (!gobble(CLOSEPAREN, s)) {
      fail("')' expected after parameters", s);
    }
    return new condEQNode(lhs, rhs);
  }

  private static conditionNode parseOr(Scanner s) {
    if (!gobble("or", s)) {
      fail("expected or", s);
    }
    if (!gobble(OPENPAREN, s)) {
      fail("'(' expected followed by parameters", s);
    }
    conditionNode lhs = parseCondition(s);
    if (!gobble(",", s)) {
      fail("expected , between parameters", s);
    }
    conditionNode rhs = parseCondition(s);
    if (!gobble(CLOSEPAREN, s)) {
      fail("')' expected after parameters", s);
    }
    return new condOrNode(lhs, rhs);
  }

  private static conditionNode parseAnd(Scanner s) {
    if (!gobble("and", s)) {
      fail("expected and", s);
    }
    if (!gobble(OPENPAREN, s)) {
      fail("'(' expected followed by parameters", s);
    }
    conditionNode lhs = parseCondition(s);
    if (!gobble(",", s)) {
      fail("expected , between parameters", s);
    }
    conditionNode rhs = parseCondition(s);
    if (!gobble(CLOSEPAREN, s)) {
      fail("')' expected after parameters", s);
    }
    return new condAndNode(lhs, rhs);
  }

  private static conditionNode parseNot(Scanner s) {
    if (!gobble("not", s)) {
      fail("expected or", s);
    }
    if (!gobble(OPENPAREN, s)) {
      fail("'(' expected followed by one parameter", s);
    }
    conditionNode node = parseCondition(s);
    if (!gobble(CLOSEPAREN, s)) {
      fail("')' expected after parameter", s);
    }
    return new condNotNode(node);
  }


  /*
   * parsing expression nodes
   */

  private static expressionNode parseExprNode(Scanner s) {
    if (s.hasNext(OP))
      return operatorNode(s);
    else if (s.hasNext(SENS))
      return parseSensor(s);
    else if (s.hasNext(NUMPAT))
      return parseNum(s);
    else if (s.hasNext(VAR))
      return parseVar(s);

    fail("expected valid Expression node (operator, sensor or number)", s);
    return null;
  }

  private static expressionNode parseNum(Scanner s) {
    if (s.hasNext(NUMPAT))
      return new numNode(s.nextInt());
    fail("expected numbers when parsing NumNode", s);
    return null;
  }

  /*
   * parsing the operators
   */
  private static operatorNode operatorNode(Scanner s) {
    if (s.hasNext("add"))
      return parseAddNode(s);
    else if (s.hasNext("sub"))
      return parseSubNode(s);
    else if (s.hasNext("mul"))
      return parseMulNode(s);
    else if (s.hasNext("div"))
      return parseDivNode(s);
    fail("expected a valid Operator", s);
    return null;
  }


  private static operatorNode parseAddNode(Scanner s) {
    if (!gobble("add", s)) {
      fail("expected add", s);
    }
    if (!gobble(OPENPAREN, s)) {
      fail("expected ( before parameters", s);
    }
    expressionNode lhs = parseExprNode(s);
    if (!gobble(",", s)) {
      fail("expected , between parameters", s);
    }
    expressionNode rhs = parseExprNode(s);
    if (!gobble(CLOSEPAREN, s)) {
      fail("expected ) after parameters", s);
    }
    return new opAddNode(lhs, rhs);
  }

  private static operatorNode parseSubNode(Scanner s) {
    if (!gobble("sub", s)) {
      fail("expected sub", s);
    }
    if (!gobble(OPENPAREN, s)) {
      fail("expected ( before parameters", s);
    }
    expressionNode lhs = parseExprNode(s);
    if (!gobble(",", s)) {
      fail("expected , between parameters", s);
    }
    expressionNode rhs = parseExprNode(s);
    if (!gobble(CLOSEPAREN, s)) {
      fail("expected ) after parameters", s);
    }
    return new opSubNode(lhs, rhs);
  }

  private static operatorNode parseMulNode(Scanner s) {
    if (!gobble("mul", s)) {
      fail("expected mul", s);
    }
    if (!gobble(OPENPAREN, s)) {
      fail("expected ( before parameters", s);
    }
    expressionNode lhs = parseExprNode(s);
    if (!gobble(",", s)) {
      fail("expected , between parameters", s);
    }
    expressionNode rhs = parseExprNode(s);
    if (!gobble(CLOSEPAREN, s)) {
      fail("expected ) after parameters", s);
    }
    return new opMulNode(lhs, rhs);
  }

  private static operatorNode parseDivNode(Scanner s) {
    if (!gobble("div", s)) {
      fail("expected div", s);
    }
    if (!gobble(OPENPAREN, s)) {
      fail("expected ( before parameters", s);
    }
    expressionNode lhs = parseExprNode(s);
    if (!gobble(",", s)) {
      fail("expected , between parameters", s);
    }
    expressionNode rhs = parseExprNode(s);
    if (!gobble(CLOSEPAREN, s)) {
      fail("expected ) after parameters", s);
    }
    return new opDivNode(lhs, rhs);
  }

  /*
   * Parsing Sensor nodes
   */

  private static sensorNode parseSensor(Scanner s) {
    if (s.hasNext("fuelLeft")) {
      if (!gobble("fuelLeft", s)) {
        fail("expected fuelLeft", s);
      }
      return new sensFuelLeftNode();
    } else if (s.hasNext("oppLR")) {
      if (!gobble("oppLR", s)) {
        fail("expected oppLR", s);
      }
      return new sensOppLRNode();
    } else if (s.hasNext("oppFB")) {
      if (!gobble("oppFB", s)) {
        fail("expected oppFB", s);
      }
      return new sensOppFBNode();
    } else if (s.hasNext("numBarrels")) {
      if (!gobble("numBarrels", s)) {
        fail("expected numBarrels", s);
      }
      return new sensNumBarrelsNode();
    } else if (s.hasNext("barrelLR")) {
      return parseSensBarrelLRNode(s);
    } else if (s.hasNext("barrelFB")) {
      return parseSensBarrelFBNode(s);
    } else if (s.hasNext("wallDist")) {
      if (!gobble("wallDist", s)) {
        fail("expected wallDist", s);
      }
      return new sensWallDistNode();
    }
    fail("expected a valid SEN", s);
    return null;
  }

  private static sensorNode parseSensBarrelLRNode(Scanner s) {
    if (!gobble("barrelLR", s)) {
      fail("expected barrelLR", s);
    }
    expressionNode n = new numNode(0);
    if (s.hasNext(OPENPAREN)) {
      if (!gobble(OPENPAREN, s)) {
        fail("expected ( before barrelLR parameters", s);
      }
      n = parseExprNode(s);
      if (!gobble(CLOSEPAREN, s)) {
        fail("expected ) after barrelLR parameters", s);
      }
    }
    return new sensBarrelLRNode(n);
  }

  private static sensorNode parseSensBarrelFBNode(Scanner s) {
    if (!gobble("barrelFB", s)) {
      fail("expected barrelFB", s);
    }
    expressionNode n = new numNode(0);
    if (s.hasNext(OPENPAREN)) {
      if (!gobble(OPENPAREN, s)) {
        fail("expected ( before barrelFB parameters", s);
      }
      n = parseExprNode(s);
      if (!gobble(CLOSEPAREN, s)) {
        fail("expected ) after barrelFB parameters", s);
      }
    }
    return new sensBarrelFBNode(n);
  }

  private static expressionNode parseVar(Scanner s) {
    String name = s.next();

    // If the variable already exists, get its expression. Else, create a new variable with the
    // expression 0
    if (variables.containsKey(name)) {
      return new variableNode(name);
    }

    fail("Variables must be declared before they are used in the program", s);
    variables.put(name, 0);
    return new variableNode(name);
  }



  private static RobotProgramNode parseLoop(Scanner s) {
    if (!gobble("loop", s)) {
      fail("'loop' expected", s);
    }
    return new loopNode(parseBlock(s));
  }


  private static RobotProgramNode parseBlock(Scanner s) {
    if (!gobble(OPENBRACE, s)) {
      fail("'{' expected", s);
    }
    List<RobotProgramNode> nodes = new ArrayList<RobotProgramNode>();
    do {
      nodes.add(parseStmt(s));
    } while (!s.hasNext(CLOSEBRACE));
    if (!gobble(CLOSEBRACE, s)) {
      fail("'}' expected", s);
    }
    return new blockNode(nodes);
  }

  /**
   * Report a failure in the parser.
   */
  static void fail(String message, Scanner s) {
    String msg = message + "\n   @ ...";
    for (int i = 0; i < 5 && s.hasNext(); i++) {
      msg += " " + s.next();
    }
    throw new ParserFailureException(msg + "...");
  }

  /**
   * Requires that the next token matches a pattern if it matches, it consumes and returns the
   * token, if not, it throws an exception with an error message
   */
  static String require(String p, String message, Scanner s) {
    if (s.hasNext(p)) {
      return s.next();
    }
    fail(message, s);
    return null;
  }

  static String require(Pattern p, String message, Scanner s) {
    if (s.hasNext(p)) {
      return s.next();
    }
    fail(message, s);
    return null;
  }

  /**
   * Requires that the next token matches a pattern (which should only match a number) if it
   * matches, it consumes and returns the token as an integer if not, it throws an exception with an
   * error message
   */
  static int requireInt(String p, String message, Scanner s) {
    if (s.hasNext(p) && s.hasNextInt()) {
      return s.nextInt();
    }
    fail(message, s);
    return -1;
  }

  static int requireInt(Pattern p, String message, Scanner s) {
    if (s.hasNext(p) && s.hasNextInt()) {
      return s.nextInt();
    }
    fail(message, s);
    return -1;
  }

  /**
   * Checks whether the next token in the scanner matches the specified pattern, if so, consumes the
   * token and return true. Otherwise returns false without consuming anything.
   */
  static boolean checkFor(String p, Scanner s) {
    if (s.hasNext(p)) {
      s.next();
      return true;
    } else {
      return false;
    }
  }

  static boolean checkFor(Pattern p, Scanner s) {
    if (s.hasNext(p)) {
      s.next();
      return true;
    } else {
      return false;
    }
  }

  static boolean gobble(String string, Scanner s) {
    if (s.hasNext(string)) {
      s.next();
      return true;
    }
    return false;
  }

  static boolean gobble(Pattern p, Scanner s) {
    if (s.hasNext(p)) {
      s.next();
      return true;
    } else {
      return false;
    }
  }
}


/*
 * the move nodes.
 */

class moveNode implements ACTNode {

  private expressionNode exp;

  public moveNode(expressionNode e) {
    exp = e;
  }

  @Override
  public void execute(Robot robot) {
    if (exp == null) {
      robot.move();
    } else {
      int stop = exp.evaluate(robot);
      for (int i = 0; i < stop; i++) {
        robot.move();
      }
    }
  }

  @Override
  public String toString() {
    if (exp != null) {
      return "move(" + exp + ");";
    }
    return "move;";
  }
}


/*
 * turn around node
 */
class turnAroundNode implements ACTNode {

  @Override
  public void execute(Robot robot) {
    robot.turnAround();
  }

  public String toString() {
    return "turn around";
  }
}


/*
 * shield off node
 */
class shieldOffNode implements ACTNode {
  @Override
  public void execute(Robot robot) {
    robot.setShield(false);
  }

  public String toString() {
    return "shield off";
  }
}


/*
 * shield on node
 */
class shieldOnNode implements ACTNode {
  @Override
  public void execute(Robot robot) {
    robot.setShield(true);
  }

  public String toString() {
    return "shield on";
  }
}


/*
 * turn left node
 */
class turnLNode implements ACTNode {
  @Override
  public void execute(Robot robot) {
    robot.turnLeft();
  }

}


/*
 * turn right node
 */
class turnRNode implements ACTNode {
  @Override
  public void execute(Robot robot) {
    robot.turnRight();
  }

  public String toString() {
    return "turn right";
  }
}
/*
 * take fuel node
 */


class takeFuelNode implements ACTNode {
  @Override
  public void execute(Robot robot) {
    robot.takeFuel();
  }

  public String toString() {
    return "take fuel";
  }
}


/*
 * wait node
 */
class waitNode implements ACTNode {

  expressionNode exp;

  public waitNode(expressionNode e) {
    exp = e;
  }

  public void execute(Robot robot) {
    if (exp == null) {
      robot.idleWait();
    } else {
      int stop = exp.evaluate(robot);
      for (int i = 0; i > stop; i++) {
        robot.idleWait();
      }
    }
  }

  @Override
  public String toString() {
    if (exp != null) {
      return "wait(" + exp + ");";
    }
    return "wait;";
  }
}


class programNode implements RobotProgramNode {

  List<RobotProgramNode> children;

  public programNode(List<RobotProgramNode> nodes) {
    children = nodes;
  }

  @Override
  public void execute(Robot robot) {
    for (RobotProgramNode n : children) {
      n.execute(robot);
    }
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    for (RobotProgramNode n : children) {
      sb.append(n);
    }
    return sb.toString();
  }
}


/*
 * the Loop Node
 */

class loopNode implements RobotProgramNode {

  private RobotProgramNode block;

  public loopNode(RobotProgramNode node) {
    block = node;
  }

  @Override
  public void execute(Robot robot) {
    block.execute(robot);
  }

  @Override
  public String toString() {
    return "loop " + block;
  }
}
/*
 * the If Node
 */


class ifNode implements RobotProgramNode {

  private List<conditionNode> conditions;
  private List<RobotProgramNode> blocks;
  private RobotProgramNode elseNode;

  public ifNode(List<conditionNode> c, List<RobotProgramNode> bl, RobotProgramNode e) {
    conditions = c;
    blocks = bl;
    elseNode = e;
  }

  @Override
  public void execute(Robot robot) {
    int size = conditions.size();
    for (int i = 0; i <= size; i++) {
      if (i == size) {
        if (!(elseNode instanceof nullNode))
          elseNode.execute(robot);
        else
          break;
      } else if (conditions.get(i).evaluate(robot)) {
        blocks.get(i).execute(robot);
        break;
      }
    }
  }

  @Override
  public String toString() {

    StringBuilder sb = new StringBuilder();

    int n = conditions.size();

    sb.append("if(");
    sb.append(conditions.get(0).toString() + ")");
    sb.append(blocks.get(0).toString());

    for (int i = 1; i < n; i++) {
      sb.append("elif(");
      sb.append(conditions.get(i).toString() + ")");
      sb.append(blocks.get(i).toString());
    }

    if (!(elseNode instanceof nullNode)) {
      sb.append("else");
      sb.append(elseNode.toString());
    }
    return sb.toString();
  }
}


/*
 * the While Node
 */

class whileNode implements RobotProgramNode {

  private conditionNode condition;
  private RobotProgramNode block;

  public whileNode(conditionNode c, RobotProgramNode b) {
    condition = c;
    block = b;
  }

  @Override
  public void execute(Robot robot) {
    while (condition.evaluate(robot)) {
      block.execute(robot);
    }
  }

  @Override
  public String toString() {
    return "while(" + condition.toString() + ")" + block;
  }
}


/*
 * the Block Node
 */

class blockNode implements RobotProgramNode {

  List<RobotProgramNode> commands;

  public blockNode(List<RobotProgramNode> l) {
    commands = l;
  }

  @Override
  public void execute(Robot robot) {
    for (RobotProgramNode n : commands) {
      n.execute(robot);
    }
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("{ ");
    for (RobotProgramNode n : commands) {
      sb.append(n);
    }
    sb.append(" } ");
    return sb.toString();
  }
}


/*
 * Null node
 */

class nullNode implements RobotProgramNode {

  @Override
  public void execute(Robot robot) {
    System.out.println("Do not try to execute a null node");
  }

  @Override
  public String toString() {
    return "Do not try to toString a null node";
  }

}


/*
 * all nodes for condition part
 */

class condLTNode implements conditionNode {
  private expressionNode lhs;
  private expressionNode rhs;

  public condLTNode(expressionNode l, expressionNode r) {
    lhs = l;
    rhs = r;
  }

  @Override
  public boolean evaluate(Robot robot) {
    if (lhs.evaluate(robot) < rhs.evaluate(robot)) {
      return true;
    }
    return false;
  }

  @Override
  public String toString() {
    return "lt(" + lhs + ", " + rhs + ")";
  }
}


class condAndNode implements conditionNode {

  private conditionNode lhs;
  private conditionNode rhs;

  public condAndNode(conditionNode l, conditionNode r) {
    lhs = l;
    rhs = r;
  }

  @Override
  public boolean evaluate(Robot robot) {
    return (lhs.evaluate(robot) && rhs.evaluate(robot));
  }

  @Override
  public String toString() {
    return "and (" + lhs + ", " + rhs + " )";
  }

}


class condEQNode implements conditionNode {
  private expressionNode lhs; // sensor
  private expressionNode rhs; // expression

  public condEQNode(expressionNode l, expressionNode r) {
    lhs = l;
    rhs = r;
  }

  @Override
  public boolean evaluate(Robot robot) {
    if (lhs.evaluate(robot) == rhs.evaluate(robot)) {
      return true;
    }
    return false;
  }

  @Override
  public String toString() {
    return "eq(" + lhs + ", " + rhs + ")";
  }
}


class condGTNode implements conditionNode {
  private expressionNode lhs;
  private expressionNode rhs;

  public condGTNode(expressionNode l, expressionNode r) {
    lhs = l;
    rhs = r;
  }

  @Override
  public boolean evaluate(Robot robot) {
    if (lhs.evaluate(robot) > rhs.evaluate(robot)) {
      return true;
    }
    return false;
  }

  @Override
  public String toString() {
    return "gt(" + lhs + ", " + rhs + ")";
  }
}


class condNotNode implements conditionNode {

  private conditionNode cond;

  public condNotNode(conditionNode c) {
    cond = c;
  }

  @Override
  public boolean evaluate(Robot robot) {
    return !cond.evaluate(robot);
  }

  @Override
  public String toString() {
    return "not(" + cond + ")";
  }
}


class condOrNode implements conditionNode {

  private conditionNode lhs;
  private conditionNode rhs;

  public condOrNode(conditionNode l, conditionNode r) {
    lhs = l;
    rhs = r;
  }

  @Override
  public boolean evaluate(Robot robot) {
    return (lhs.evaluate(robot) || rhs.evaluate(robot));
  }

  @Override
  public String toString() {
    return "or(" + lhs + ", " + rhs + " )";
  }
}


/*
 * all nodes for opretor
 */
class opAddNode implements operatorNode {

  private expressionNode lhs;
  private expressionNode rhs;

  public opAddNode(expressionNode l, expressionNode r) {
    lhs = l;
    rhs = r;
  }

  @Override
  public int evaluate(Robot robot) {
    return lhs.evaluate(robot) + rhs.evaluate(robot);
  }

  @Override
  public String toString() {
    return "add(" + lhs + ", " + rhs + ")";
  }
}


class opSubNode implements operatorNode {

  private expressionNode lhs;
  private expressionNode rhs;

  public opSubNode(expressionNode l, expressionNode r) {
    lhs = l;
    rhs = r;
  }

  @Override
  public int evaluate(Robot robot) {
    return lhs.evaluate(robot) - rhs.evaluate(robot);
  }

  @Override
  public String toString() {
    return "sub(" + lhs + ", " + rhs + ")";
  }
}


class opMulNode implements operatorNode {

  private expressionNode lhs;
  private expressionNode rhs;

  public opMulNode(expressionNode l, expressionNode r) {
    lhs = l;
    rhs = r;
  }

  @Override
  public int evaluate(Robot robot) {
    return lhs.evaluate(robot) * rhs.evaluate(robot);
  }

  @Override
  public String toString() {
    return "mul(" + lhs + ", " + rhs + ")";
  }
}


class opDivNode implements operatorNode {

  private expressionNode lhs;
  private expressionNode rhs;

  public opDivNode(expressionNode l, expressionNode r) {
    lhs = l;
    rhs = r;
  }

  @Override
  public int evaluate(Robot robot) {
    return Math.round(lhs.evaluate(robot) / rhs.evaluate(robot));
  }

  @Override
  public String toString() {
    return "div(" + lhs + ", " + rhs + ")";
  }
}


/*
 * the num pat nodes
 */
class numNode implements expressionNode {

  private int val;

  public numNode(int v) {
    val = v;
  }

  @Override
  public int evaluate(Robot robot) {
    return val;
  }

  @Override
  public String toString() {
    return "" + val;
  }
}


/*
 * the sensors nodes
 */
class sensFuelLeftNode implements sensorNode {

  @Override
  public int evaluate(Robot robot) {
    return robot.getFuel();
  }

  @Override
  public String toString() {
    return "fuelLeft";
  }
}


class sensOppLRNode implements sensorNode {

  @Override
  public int evaluate(Robot robot) {
    return robot.getOpponentLR();
  }

  @Override
  public String toString() {
    return "oppLR";
  }
}


class sensOppFBNode implements sensorNode {

  @Override
  public int evaluate(Robot robot) {
    return robot.getOpponentFB();
  }

  @Override
  public String toString() {
    return "oppFB";
  }
}


class sensNumBarrelsNode implements sensorNode {

  @Override
  public int evaluate(Robot robot) {
    return robot.numBarrels();
  }

  @Override
  public String toString() {
    return "numBarrels";
  }
}


class sensWallDistNode implements sensorNode {

  @Override
  public int evaluate(Robot robot) {
    return robot.getDistanceToWall();
  }

  @Override
  public String toString() {
    return "wallDist";
  }
}


class sensBarrelLRNode implements sensorNode {

  private expressionNode exp;

  public sensBarrelLRNode(expressionNode e) {
    exp = e;
  }

  @Override
  public int evaluate(Robot robot) {
    return robot.getBarrelLR(exp.evaluate(robot));
  }

  @Override
  public String toString() {
    return "barrelLR";
  }
}


class sensBarrelFBNode implements sensorNode {

  private expressionNode exp;

  public sensBarrelFBNode(expressionNode e) {
    exp = e;
  }

  @Override
  public int evaluate(Robot robot) {
    return robot.getBarrelLR(exp.evaluate(robot));
  }

  @Override
  public String toString() {
    return "barrelFB";
  }
}


/*
 * assgn node
 */
class assgnNode implements RobotProgramNode {

  private String name;
  private expressionNode expression;

  public assgnNode(String name, expressionNode expression) {
    this.name = name;
    this.expression = expression;
  }

  public void setExpression(expressionNode expression) {
    this.expression = expression;
  }

  public void setName(String name) {
    this.name = name;
  }

  public void execute(Robot robot) {
    Parser.variables.put(this.name, this.expression.evaluate(robot));
  }

  public String toString() {
    return name.toString() + " = " + expression.toString();
  }
}


class variableNode implements expressionNode {
  private String name;

  public variableNode(String name) {
    this.name = name;
  }

  public int evaluate(Robot robot) {
    return Parser.variables.get(this.name);
  }

  public String toString() {
    return this.name;
  }
}

// You could add the node classes here, as long as they are not declared public (or private)
