import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.expr.BooleanLiteralExpr;
import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.stmt.BlockStmt;
import com.github.javaparser.ast.stmt.ForStmt;
import com.github.javaparser.ast.stmt.Statement;
import com.github.javaparser.ast.stmt.WhileStmt;
import com.github.javaparser.ast.visitor.TreeVisitor;
import com.github.javaparser.ast.visitor.VoidVisitorAdapter;

import java.io.File;
import java.util.ArrayList;

public class LoopExchange implements Runnable{
    final private ArrayList<Node> mLoopNodes = new ArrayList<>();
    private MethodDeclaration mMd;
    private CompilationUnit mCom;
    private File mJavaFile;

    public LoopExchange(MethodDeclaration md, CompilationUnit com, File mJavaFile){
        this.mMd = md;
        this.mCom = com;
        this.mJavaFile = mJavaFile;
    }
    @Override
    public void run() {
        locateLoops(mMd, mJavaFile);
        ArrayList<MethodDeclaration> ChangeMethod = Common.applyToPlace(this, mMd, mJavaFile, mLoopNodes);
        Common.ApplyTransformation(ChangeMethod, mCom, mJavaFile);
    }

    private void locateLoops(MethodDeclaration md, File mJavaFile) {
        Common.setOutputPath(this, mJavaFile);
        new TreeVisitor() {
            @Override
            public void process(Node node) {
                if (node instanceof WhileStmt || node instanceof ForStmt) {
                    mLoopNodes.add(node);
                }
            }
        }.visitPreOrder(md);
        //System.out.println("LoopNodes : " + mLoopNodes.size());
    }

    public MethodDeclaration applyTransformation(MethodDeclaration md, Node loopNode) {
        new TreeVisitor() {
            @Override
            public void process(Node node) {
                if (node.equals(loopNode)) {
                    if (loopNode instanceof WhileStmt) {
                        ForStmt nodeForStmt = new ForStmt();
                        nodeForStmt.setCompare(((WhileStmt) node).getCondition());
                        nodeForStmt.setBody(((WhileStmt) node).getBody());
                        node.replace(nodeForStmt);
                    } else if (loopNode instanceof ForStmt) {
                        if (((ForStmt) node).getInitialization().size() != 0) {
                            BlockStmt outerBlockStmt = new BlockStmt();
                            for (Expression exp : ((ForStmt) node).getInitialization()) {
                                outerBlockStmt.addStatement(exp);
                            }
                            WhileStmt nodeWhileStmt = getWhileStmt(node);
                            outerBlockStmt.addStatement(nodeWhileStmt);
                            node.replace(outerBlockStmt);
                        } else {
                            node.replace(getWhileStmt(node));
                        }
                    }
                }
            }
        }.visitPreOrder(md);
        return md;
    }

    private WhileStmt getWhileStmt(Node loopNode) {
        WhileStmt nodeWhileStmt = new WhileStmt();
        nodeWhileStmt.setCondition(((ForStmt) loopNode).getCompare().orElse(new BooleanLiteralExpr(true)));
        if (((ForStmt) loopNode).getBody().getChildNodes().size() == 0 && ((ForStmt) loopNode).getUpdate().size() == 0) {
            //i.e. for(?;?;); or for(?;?;){}
            nodeWhileStmt.setBody(((ForStmt) loopNode).getBody());
        } else {
            BlockStmt innerBlockStmt;
            if (((ForStmt) loopNode).getBody().getChildNodes().size() != 0) {
                //i.e. for(?;?;?){...}
                Statement forStmtBody = ((ForStmt) loopNode).getBody();
                if (forStmtBody instanceof BlockStmt) {
                    innerBlockStmt = (BlockStmt) forStmtBody;
                } else {
                    innerBlockStmt = new BlockStmt();
                    innerBlockStmt.addStatement(forStmtBody);
                }
            } else {
                //i.e. for(?;?;...); or for(?;?;...){}
                innerBlockStmt = new BlockStmt();
            }
            for (Expression exp : ((ForStmt) loopNode).getUpdate()) {
                innerBlockStmt.addStatement(exp);
            }
            nodeWhileStmt.setBody(innerBlockStmt);
        }
        return nodeWhileStmt;
    }

}