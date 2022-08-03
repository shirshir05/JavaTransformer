import com.github.javaparser.JavaParser;
import com.github.javaparser.ParseResult;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.NodeList;
import com.github.javaparser.ast.body.*;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * This class parser the java file and call to apply the transformation
 */
public class ParserJavaFile {
    private File mJavaFile = null;


    private ArrayList<String> nameMethodChanges(){
        String line = "";
        String splitBy = ",";
        ArrayList<String> ans = new ArrayList<String>();
        try
        {
            BufferedReader br = new BufferedReader(new FileReader(Common.mRootInputPath +Common.mNameProject + "_methods.csv"));
            while ((line = br.readLine()) != null)   //returns a Boolean value
            {
                if (Objects.equals(line.split(",")[0], mJavaFile.getName().replace("_after_", "_"))) {
                    String[] methods = line.split(splitBy);    // use comma as separator
                    for(String method: methods){
                        ans.add(method.substring(method.lastIndexOf("::")+2));
                    }
                }
            }
        }
        catch (IOException e)
        {
            e.printStackTrace();
        }
        return ans;
    }
    public void inspectSourceCode(File javaFile) {
        this.mJavaFile = javaFile;
        ArrayList<String> mehodsChanges = nameMethodChanges();
        CompilationUnit root = this.getParseUnit(); // parser file
        if (root != null) {
            NodeList<TypeDeclaration<?>> classes = root.getTypes();
            for (TypeDeclaration<?> type : classes) {
                NodeList<BodyDeclaration<?>> ObjectInClass = type.getMembers();
                for (BodyDeclaration<?> ClassObject : ObjectInClass) {
                    if (ClassObject instanceof MethodDeclaration) {// get all method
                        if (mehodsChanges.contains(((MethodDeclaration) ClassObject).getName().toString()))
                            ApplyTransformation(root.clone(), (MethodDeclaration) ClassObject);
                    }else if(ClassObject instanceof ClassOrInterfaceDeclaration){// class in class
                        // TODO: more then one class
                        NodeList<BodyDeclaration<?>> ObjectInClass_2 = ((ClassOrInterfaceDeclaration) ClassObject).getMembers();
                        for (BodyDeclaration<?> ClassObject_2 : ObjectInClass_2) {
                            if (ClassObject_2 instanceof MethodDeclaration ) {// get all method
                                if (mehodsChanges.contains(((MethodDeclaration) ClassObject_2).getName().toString()))
                                    ApplyTransformation(root.clone(), (MethodDeclaration) ClassObject_2);
                            }

                        }

                    }
                }
            }
        }
    }

    public void ApplyTransformation(CompilationUnit com, MethodDeclaration method){
        ExecutorService es = Executors.newCachedThreadPool();

        try {
            new BooleanExchange(method.clone(), com, mJavaFile).run();
            new VariableRenaming(method.clone(), com, mJavaFile).run();
            new PermuteStatement(method.clone(), com, mJavaFile).run();
            new UnusedStatement(method.clone(), com, mJavaFile).run();
            new LoopExchange(method.clone(), com, mJavaFile).run();
            new SwitchToIf(method.clone(), com, mJavaFile).run();
            new TryCatch(method.clone(), com, mJavaFile).run();
            new ReorderCondition(method.clone(), com, mJavaFile).run();;
//            es.execute(new BooleanExchange(method.clone(), com.clone(), mJavaFile));
//            es.execute(new VariableRenaming(method.clone(), com.clone(), mJavaFile));
//            es.execute(new PermuteStatement(method.clone(), com.clone(), mJavaFile));
//            es.execute(new UnusedStatement(method.clone(), com.clone(), mJavaFile));
//            es.execute(new LoopExchange(method.clone(), com.clone(), mJavaFile));
//            es.execute(new SwitchToIf(method.clone(), com.clone(), mJavaFile));
//            es.execute(new TryCatch(method.clone(), com.clone(), mJavaFile));
//            es.execute(new ReorderCondition(method.clone(), com.clone(), mJavaFile));
//            es.shutdown();
//            new LogStatement().visit(method, com, mJavaFile);
        } catch (Exception ex) {
            System.out.println("\n" + "Exception: " + mJavaFile.getPath());
            ex.printStackTrace();
        }
    }


    private CompilationUnit getParseUnit() {
        try {
            JavaParser parser = new JavaParser();
            String txtCode = new String(Files.readAllBytes(this.mJavaFile.toPath()));
            ParseResult<CompilationUnit> parseResult = parser.parse(txtCode);
            return parseResult.getResult().get();
        } catch (Exception ex) {
            System.out.println("\n" + "Exception: " + this.mJavaFile .getPath());
            ex.printStackTrace();
            String error_dir = Common.mSavePath + "java_parser_error.txt";
            Common.saveErrText(error_dir, this.mJavaFile );
            return null;
        }
    }

}
