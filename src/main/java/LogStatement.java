import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.stmt.BlockStmt;
import com.github.javaparser.ast.stmt.EmptyStmt;
import com.github.javaparser.ast.stmt.Statement;

import java.io.File;
import java.util.ArrayList;

public class LogStatement implements Runnable{
    private ArrayList<Node> mDummyNodes = new ArrayList<>();
    private MethodDeclaration mMd;
    private CompilationUnit mCom;
    private File mJavaFile;

    public LogStatement(MethodDeclaration md, CompilationUnit com, File mJavaFile){
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
        try{
            for (Statement statement : md.getBody().get().getStatements()) {
                blockStmt.addStatement(statement);
            }
            blockStmt.addStatement(0, getLogStatement()); //beginning of stmt
            md.setBody(blockStmt);
            return md;
        }
        catch (Exception ex) {
            return null;
        }
    }



    private Statement getLogStatement() {
        String logStr = "System.out.println(\"dummy log\");";
        return StaticJavaParser.parseStatement(logStr);
    }
}