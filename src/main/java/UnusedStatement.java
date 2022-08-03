import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.stmt.BlockStmt;
import com.github.javaparser.ast.stmt.EmptyStmt;
import com.github.javaparser.ast.stmt.Statement;
import com.github.javaparser.ast.visitor.VoidVisitorAdapter;

import java.io.File;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Random;


public class UnusedStatement implements Runnable{
    private ArrayList<Node> mDummyNodes = new ArrayList<>();
    private MethodDeclaration mMd;
    private CompilationUnit mCom;
    private File mJavaFile;

    public UnusedStatement(MethodDeclaration md, CompilationUnit com, File mJavaFile){
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
            int min = 0, max = blockStmt.getStatements().size() - 1;
            int place = new Random().nextInt(max - min + 1) + min;
            blockStmt.addStatement(place, getUnusedStatement());
            if (md.findFirst(MethodDeclaration.class).isPresent()) {
                md.setBody(blockStmt);
            }
            return md;
        }
        catch (Exception ex) {
//            ex.printStackTrace();
            return null;
        }
    }

    private Statement getUnusedStatement() {
        String timestamp = new Timestamp(System.currentTimeMillis()).toString();
        String unusedStr = "String dummy_timestamp = \"" + timestamp + "\";";
        return StaticJavaParser.parseStatement(unusedStr);
    }
}