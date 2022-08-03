import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.body.VariableDeclarator;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.ast.stmt.*;
import com.github.javaparser.ast.visitor.VoidVisitorAdapter;

import java.io.File;
import java.util.ArrayList;
import java.util.Random;

public class TryCatch implements Runnable{
    final private ArrayList<Node> mDummyNodes = new ArrayList<>();
    private MethodDeclaration mMd;
    private CompilationUnit mCom;
    private File mJavaFile;

    public TryCatch(MethodDeclaration md, CompilationUnit com, File mJavaFile){
        this.mMd = md;
        this.mCom = com;
        this.mJavaFile = mJavaFile;
    }
    @Override
    public void run() {
        Common.setOutputPath(this, mJavaFile);
        mDummyNodes.add(new EmptyStmt());
        ArrayList<MethodDeclaration> ChangeMethod = Common.applyToPlace(this, mMd, mJavaFile, mDummyNodes);
        Common.ApplyTransformation(ChangeMethod, mCom, mJavaFile);
    }

    public MethodDeclaration applyTransformation(MethodDeclaration md, Node unused) {

        BlockStmt blockStmt = new BlockStmt();
        BlockStmt tcBlockStmt = new BlockStmt();
        try{
            for (Statement statement : md.getBody().get().getStatements()) {
                boolean flag = true;
                if (Common.isNotPermeableStatement(statement)
                        || statement.findAll(MethodCallExpr.class).size() == 0) {
                    flag = false;
                } else if (statement instanceof ExpressionStmt) {
                    for (Node node : statement.getChildNodes()) {
                        if (node.findFirst(VariableDeclarator.class).isPresent()) {
                            flag = false;
                            break;
                        }
                    }
                }
                if (flag) {
                    tcBlockStmt.addStatement(statement);
                }
                blockStmt.addStatement(statement);
            }

            if (tcBlockStmt.getStatements().size() > 0) {
                int min = 0, max = tcBlockStmt.getStatements().size() - 1;
                int place = new Random().nextInt(max - min + 1) + min;
                Statement tcStmt = tcBlockStmt.getStatements().get(place);
                blockStmt.replace(tcStmt, getTryCatchStatement(tcStmt));
            }

            if (md.findFirst(MethodDeclaration.class).isPresent()) {
                md.setBody(blockStmt);
            }
            return md;
        }catch (Exception e){
            return null;
        }
    }

    private Statement getTryCatchStatement(Statement stmt) {
        String tryStr = "try {\n" +
                stmt + "\n" +
                "} catch (Exception ex) {\n" +
                "ex.printStackTrace();\n" +
                "}";
        return StaticJavaParser.parseStatement(tryStr);
    }
}