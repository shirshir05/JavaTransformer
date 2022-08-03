import com.github.javaparser.ast.*;
import com.github.javaparser.ast.body.*;
import com.github.javaparser.ast.expr.*;
import com.github.javaparser.ast.visitor.TreeVisitor;

import java.io.File;
import java.util.ArrayList;

public class VariableRenaming implements Runnable{
    final private ArrayList<Node> mVariableNodes = new ArrayList<>();
    private int mVariableCounter = 0;
    private MethodDeclaration mMd;
    private CompilationUnit mCom;
    private File mJavaFile;

    public VariableRenaming(MethodDeclaration md, CompilationUnit com, File mJavaFile){
        this.mMd = md;
        this.mCom = com;
        this.mJavaFile = mJavaFile;
    }

    @Override
    public void run() {
        locateVariableRenaming(mMd, mJavaFile); // define the var that we work on it in Object Common
        ArrayList<MethodDeclaration> ChangeMethod = Common.applyToPlace(this, mMd, mJavaFile, mVariableNodes);
        Common.ApplyTransformation(ChangeMethod, mCom, mJavaFile);
    }

    public void locateVariableRenaming(MethodDeclaration com, File mJavaFile) {
        Common.setOutputPath(this, mJavaFile);
        new TreeVisitor() {
            @Override
            public void process(Node node) {
                if (isTargetVariable(node, com)) {
                    node.setData(Common.VariableId, mVariableCounter++);
                    node.setData(Common.VariableName, node.toString());
                    mVariableNodes.add(node);
                }
            }
        }.visitPreOrder(com);
    }

    private boolean isTargetVariable(Node node, MethodDeclaration com) {
        return (node instanceof SimpleName &&
                (node.getParentNode().orElse(null) instanceof Parameter
                        || node.getParentNode().orElse(null) instanceof VariableDeclarator));
    }

    public MethodDeclaration applyTransformation(MethodDeclaration md, Node varNode) {
        new TreeVisitor() {
            @Override
            public void process(Node node) {
                String oldName = varNode.getData(Common.VariableName);
                if (node.toString().equals(oldName)) {
                    String newName = "var" + varNode.getData(Common.VariableId);
                    if (node instanceof SimpleName
                            && !(node.getParentNode().orElse(null) instanceof MethodDeclaration)
                            && !(node.getParentNode().orElse(null) instanceof ClassOrInterfaceDeclaration)) {
                        ((SimpleName) node).setIdentifier(newName);
                    }
                }
            }
        }.visitPreOrder(md);
        return md;
    }

}