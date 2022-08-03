import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.expr.BinaryExpr;
import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.stmt.*;
import com.github.javaparser.ast.visitor.TreeVisitor;

import java.io.File;
import java.util.ArrayList;


public class SwitchToIf implements Runnable{
    private ArrayList<Node> mSwitchNodes = new ArrayList<>();
    private MethodDeclaration mMd;
    private CompilationUnit mCom;
    private File mJavaFile;

    public SwitchToIf(MethodDeclaration md, CompilationUnit com, File mJavaFile){
        this.mMd = md;
        this.mCom = com;
        this.mJavaFile = mJavaFile;
    }
    @Override
    public void run() {
        locateConditionals(mMd, mJavaFile);
        ArrayList<MethodDeclaration> ChangeMethod = Common.applyToPlace(this, mMd, mJavaFile, mSwitchNodes);
        Common.ApplyTransformation(ChangeMethod, mCom, mJavaFile);
    }

    private void locateConditionals(MethodDeclaration md, File mJavaFile) {
        Common.setOutputPath(this, mJavaFile);
        new TreeVisitor() {
            @Override
            public void process(Node node) {
                if (node instanceof SwitchStmt) {
                    mSwitchNodes.add(node);
                }
            }
        }.visitPreOrder(md);
        //System.out.println("SwitchNodes : " + mSwitchNodes.size());
    }

    public MethodDeclaration applyTransformation(MethodDeclaration md, Node switchNode) {
        new TreeVisitor() {
            @Override
            public void process(Node node) {
                if (node.equals(switchNode)) {
                    ArrayList<Object> ifStmts = new ArrayList<>();
                    if (((SwitchStmt) node).getEntries().size() == 0) {
                        // empty
                        ifStmts.add(getIfStmt(node, null));
                    } else {
                        BlockStmt defaultBlockStmt = null;
                        for (SwitchEntry switchEntry : ((SwitchStmt) node).getEntries()) {
                            if (switchEntry.getLabels().size() != 0) {
                                // cases
                                ifStmts.add(getIfStmt(node, switchEntry));
                            } else {
                                if (((SwitchStmt) node).getEntries().size() == 1) {
                                    // default without cases
                                    ifStmts.add(getIfStmt(node, switchEntry));
                                } else {
                                    // default with cases
                                    defaultBlockStmt = getBlockStmt(switchEntry);
                                }
                            }
                        }
                        if (defaultBlockStmt != null) ifStmts.add(defaultBlockStmt); // default at end with cases
                        for (int i = 0; i < ifStmts.size() - 1; i++) {
                            ((IfStmt) ifStmts.get(i)).setElseStmt((Statement) ifStmts.get(i + 1));
                        }
                    }
                    node.replace((IfStmt) ifStmts.get(0));
                }
            }
        }.visitPreOrder(md);
        return md;
    }

    private Expression getBinaryExpr(Node switchNode, SwitchEntry switchEntry) {
        BinaryExpr binaryExpr = new BinaryExpr();
        binaryExpr.setLeft(((SwitchStmt) switchNode).getSelector());
        binaryExpr.setOperator(BinaryExpr.Operator.EQUALS);
        if (switchEntry != null && switchEntry.getLabels().size() != 0) {
            binaryExpr.setRight(switchEntry.getLabels().get(0)); // case(?)
        } else {
            binaryExpr.setRight(((SwitchStmt) switchNode).getSelector()); // only default
        }
        return binaryExpr;
    }

    private BlockStmt getBlockStmt(SwitchEntry switchEntry) {
        BlockStmt blockStmt = new BlockStmt();
        if (switchEntry != null) {
            switchEntry.getStatements().forEach((stmt) -> {
                if (!(stmt instanceof BreakStmt)) blockStmt.addStatement(stmt);
            });
        }
        return blockStmt;
    }

    private IfStmt getIfStmt(Node switchNode, SwitchEntry switchEntry) {
        IfStmt ifStmt = new IfStmt();
        ifStmt.setCondition(getBinaryExpr(switchNode, switchEntry));
        ifStmt.setThenStmt(getBlockStmt(switchEntry));
        return ifStmt;
    }

}