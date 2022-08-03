import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.expr.BinaryExpr;
import com.github.javaparser.ast.visitor.TreeVisitor;
import com.github.javaparser.ast.visitor.VoidVisitorAdapter;

import java.io.File;
import java.util.ArrayList;

public class ReorderCondition implements Runnable{
    private ArrayList<Node> mOperatorNodes = new ArrayList<>();
    private MethodDeclaration mMd;
    private CompilationUnit mCom;
    private File mJavaFile;

    public ReorderCondition(MethodDeclaration md, CompilationUnit com, File mJavaFile){
        this.mMd = md;
        this.mCom = com;
        this.mJavaFile = mJavaFile;
    }
    @Override
    public void run() {
        locateOperators(mMd, mJavaFile);
        ArrayList<MethodDeclaration> ChangeMethod = Common.applyToPlace(this, mMd, mJavaFile, mOperatorNodes);
        Common.ApplyTransformation(ChangeMethod, mCom, mJavaFile);
    }

    private void locateOperators(MethodDeclaration md, File mJavaFile) {
        Common.setOutputPath(this, mJavaFile);
        new TreeVisitor() {
            @Override
            public void process(Node node) {
                if (node instanceof BinaryExpr && isAugmentationApplicable(((BinaryExpr) node).getOperator())) {
                    mOperatorNodes.add(node);
                }
            }
        }.visitPreOrder(md);
        //System.out.println("OperatorNodes : " + mOperatorNodes.size());
    }

    public MethodDeclaration applyTransformation(MethodDeclaration md, Node opNode) {
        new TreeVisitor() {
            @Override
            public void process(Node node) {
                if (node.equals(opNode)) {
                    BinaryExpr replNode = (BinaryExpr) opNode.clone();
                    switch (((BinaryExpr) node).getOperator()) {
                        case LESS:
                            replNode.setOperator(BinaryExpr.Operator.GREATER);
                            replNode.setLeft(((BinaryExpr) node).getRight());
                            replNode.setRight(((BinaryExpr) node).getLeft());
                            break;
                        case LESS_EQUALS:
                            replNode.setOperator(BinaryExpr.Operator.GREATER_EQUALS);
                            replNode.setLeft(((BinaryExpr) node).getRight());
                            replNode.setRight(((BinaryExpr) node).getLeft());
                            break;
                        case GREATER:
                            replNode.setOperator(BinaryExpr.Operator.LESS);
                            replNode.setLeft(((BinaryExpr) node).getRight());
                            replNode.setRight(((BinaryExpr) node).getLeft());
                            break;
                        case GREATER_EQUALS:
                            replNode.setOperator(BinaryExpr.Operator.LESS_EQUALS);
                            replNode.setLeft(((BinaryExpr) node).getRight());
                            replNode.setRight(((BinaryExpr) node).getLeft());
                            break;
                        case EQUALS:
                        case NOT_EQUALS:
                        case OR:
                        case AND:
                        case PLUS:
                        case MULTIPLY:
                            replNode.setLeft(((BinaryExpr) node).getRight());
                            replNode.setRight(((BinaryExpr) node).getLeft());
                            break;
                    }
                    node.replace(replNode);
                }
            }
        }.visitPreOrder(md);
        return md;
    }

    private boolean isAugmentationApplicable(BinaryExpr.Operator op) {
        switch (op) {
            case LESS:
            case LESS_EQUALS:
            case GREATER:
            case GREATER_EQUALS:
            case EQUALS:
            case NOT_EQUALS:
            case OR:
            case AND:
            case PLUS:
            case MULTIPLY:
                return true;
        }
        return false;
    }

}