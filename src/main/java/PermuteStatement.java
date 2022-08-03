import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.ast.expr.SimpleName;
import com.github.javaparser.ast.stmt.EmptyStmt;
import com.github.javaparser.ast.stmt.ExpressionStmt;
import com.github.javaparser.ast.stmt.Statement;
import com.github.javaparser.ast.visitor.TreeVisitor;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;


public class PermuteStatement implements Runnable{
    private ArrayList<ArrayList<Node>> mBasicBlockNodes = new ArrayList<>();
    final private ArrayList<Node> mDummyNodes = new ArrayList<>();
    private MethodDeclaration mMd;
    private CompilationUnit mCom;
    private File mJavaFile;

    public PermuteStatement(MethodDeclaration md, CompilationUnit com, File mJavaFile){
        this.mMd = md;
        this.mCom = com;
        this.mJavaFile = mJavaFile;
    }
    @Override
    public void run() {
        mBasicBlockNodes = locateBasicBlockStatements(mMd, mJavaFile);
        mDummyNodes.add(new EmptyStmt());
        ArrayList<MethodDeclaration> ChangeMethod = Common.applyToPlace(this, mMd, mJavaFile, mDummyNodes);
        Common.ApplyTransformation(ChangeMethod, mCom, mJavaFile);
    }

    private ArrayList<ArrayList<Node>> locateBasicBlockStatements(MethodDeclaration md, File mJavaFile) {
        Common.setOutputPath(this, mJavaFile);
        ArrayList<Node> innerStatementNodes = new ArrayList<>();
        ArrayList<ArrayList<Node>> basicBlockNodes = new ArrayList<>();
        new TreeVisitor() {
            @Override
            public void process(Node node) {
                if (node instanceof ExpressionStmt
                        && node.findAll(MethodCallExpr.class).size() == 0
                        && !Common.isNotPermeableStatement(node)) {
                    innerStatementNodes.add(node);
                } else {
                    if (innerStatementNodes.size() > 1) {
                        basicBlockNodes.add(new ArrayList<>(innerStatementNodes));
                    }
                    innerStatementNodes.clear();
                }
            }
        }.visitBreadthFirst(md);
        return basicBlockNodes;
    }

    public MethodDeclaration applyTransformation(MethodDeclaration md, Node unused) {
        MethodDeclaration newMd = null;
        int cnt = 0;
        for (int k = 0; k < mBasicBlockNodes.size(); k++) {
            ArrayList<Node> basicBlockNodes = mBasicBlockNodes.get(k);
            for (int i = 0; i < basicBlockNodes.size(); i++) {
                for (int j = i + 1; j < basicBlockNodes.size(); j++) {
                    Statement stmt_i = (Statement) basicBlockNodes.get(i);
                    Statement stmt_j = (Statement) basicBlockNodes.get(j);
                    if (stmt_i.getParentNode().equals(stmt_j.getParentNode())) {
                        List<SimpleName> iIdentifiers = stmt_i.findAll(SimpleName.class);
                        List<SimpleName> jIdentifiers = stmt_j.findAll(SimpleName.class);
                        List<SimpleName> ijIdentifiers = iIdentifiers.stream()
                                .filter(jIdentifiers::contains).collect(Collectors.toList());
                        if (ijIdentifiers.size() == 0) { //dependency check between i & j statement
                            List<SimpleName> bIdentifiers = new ArrayList<>();
                            for (int b = i + 1; b < j; b++) {
                                Statement stmt_b = (Statement) basicBlockNodes.get(b);
                                bIdentifiers.addAll(stmt_b.findAll(SimpleName.class));
                            }
                            List<SimpleName> ibIdentifiers = iIdentifiers.stream()
                                    .filter(bIdentifiers::contains).collect(Collectors.toList());
                            if (ibIdentifiers.size() == 0) { //dependency check among i & internal statements
                                List<SimpleName> jbIdentifiers = jIdentifiers.stream()
                                        .filter(bIdentifiers::contains).collect(Collectors.toList());
                                if (jbIdentifiers.size() == 0) { //dependency check among j & internal statements
                                    newMd = swapStatementNodes(md, k, i, j, ++cnt);
                                }
                            }
                        }
                    }
                }
            }
        }
        return newMd;
    }

    private MethodDeclaration swapStatementNodes(MethodDeclaration md, int k, int i, int j, int cnt) {
        MethodDeclaration newCom = md.clone();
        ArrayList<ArrayList<Node>> statementNodes = locateBasicBlockStatements(newCom, null);
        Statement stmt_i = (Statement) statementNodes.get(k).get(i);
        Statement stmt_j = (Statement) statementNodes.get(k).get(j);
        stmt_i.replace(stmt_j.clone());
        stmt_j.replace(stmt_i.clone());
        return newCom;
    }

}