import com.github.javaparser.JavaParser;
import com.github.javaparser.ParseResult;
import com.github.javaparser.Range;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.DataKey;
import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.stmt.*;
import difflib.Delta;
import difflib.DiffUtils;
import difflib.Patch;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;


public final class Common {

    static int place = 0;
    static String mRootInputPath = "";
    static String mRootOutputPath = "";
    static String mSavePath = "";
    static String mNameProject = "";
    static String mNameFold = "";

    static final DataKey<Integer> VariableId = new DataKey<Integer>() {
    };
    static final DataKey<String> VariableName = new DataKey<String>() {
    };

    static void setOutputPath(Object obj, File javaFile) {
        //assume '/transforms' in output path
        Common.mSavePath = Common.mRootOutputPath.replace("\\transforms","\\transforms\\" + obj.getClass().getSimpleName());
    }

    static ArrayList<MethodDeclaration> applyToPlace(Object transform, MethodDeclaration md, File javaFile, ArrayList<Node> nodeList) {
        ArrayList<MethodDeclaration> ChangeMethod = new ArrayList<MethodDeclaration>();
        // apply to single place
        for (Node node : nodeList) {
            MethodDeclaration newCom = applyByObj(transform, javaFile, md.clone(), node.clone());
            if (newCom != null && Common.checkTransformation(md, newCom, javaFile, false)) {
                ChangeMethod.add(newCom);
            }
        }

        // apply to all place
        if (nodeList.size() > 1 && isAllPlaceApplicable(transform)) {
            MethodDeclaration oldCom = md.clone();
            MethodDeclaration copyMd = md.clone();
            nodeList.forEach((node) -> applyByObj(transform, javaFile, copyMd, node));
            if (Common.checkTransformation(oldCom, copyMd, javaFile, true)) {
                ChangeMethod.add(md);
            }
        }
        return ChangeMethod;
    }

    static MethodDeclaration applyByObj(Object obj, File javaFile, MethodDeclaration md, Node node) {
        MethodDeclaration newCom = null;
        try {
            if (obj instanceof VariableRenaming) {
                newCom = ((VariableRenaming) obj).applyTransformation(md, node);
            } else if (obj instanceof BooleanExchange) {
                newCom = ((BooleanExchange) obj).applyTransformation(md, node);
            } else if (obj instanceof LoopExchange) {
                newCom = ((LoopExchange) obj).applyTransformation(md, node);
            } else if (obj instanceof SwitchToIf) {
                newCom = ((SwitchToIf) obj).applyTransformation(md, node);
            } else if (obj instanceof ReorderCondition) {
                newCom = ((ReorderCondition) obj).applyTransformation(md, node);
            } else if (obj instanceof PermuteStatement) {
                newCom = ((PermuteStatement) obj).applyTransformation(md, node);
            } else if (obj instanceof UnusedStatement) {
                newCom = ((UnusedStatement) obj).applyTransformation(md, node);
            } else if (obj instanceof LogStatement) {
                newCom = ((LogStatement) obj).applyTransformation(md, node);
            } else if (obj instanceof TryCatch) {
                newCom = ((TryCatch) obj).applyTransformation(md, node);
            }
        } catch (Exception ex) {
            System.out.println("\n" + "Exception: " + javaFile.getPath());
            ex.printStackTrace();
        }
        return newCom;
    }

    static Boolean checkTransformation(MethodDeclaration bRoot, MethodDeclaration aRoot,
                                       File javaFile, boolean writeFile) {
        String mdBeforeStr = bRoot.toString().replaceAll("\\s+", "");
        String mdAfterStr = aRoot.toString().replaceAll("\\s+", "");
        if (mdBeforeStr.compareTo(mdAfterStr) == 0) {
            if (writeFile) {
                String no_dir = Common.mSavePath + "no_transformation.txt";
                File targetFile = new File(no_dir);
                Common.saveErrText(no_dir, javaFile);
            }
            return false;
        }
        return true;
    }

    synchronized  static void saveTransformation(CompilationUnit aRoot, File javaFile) {
        String output_dir = Common.mSavePath + javaFile.getPath().substring(javaFile.getPath().lastIndexOf('\\') + 1);
        String path_dir = javaFile.getPath().substring(0, javaFile.getPath().lastIndexOf('\\')+1) +javaFile.getPath().substring(javaFile.getPath().lastIndexOf('\\') + 1).replace("_after_", "_before_");
        File file_before = new File(path_dir);
        if (file_before.exists()){
            String pathAfter = output_dir.substring(0, output_dir.lastIndexOf(".java")) + "_" + place + "_after.java";
            String pathBefore = output_dir.substring(0, output_dir.lastIndexOf(".java")) + "_" + place + "_before.java";
            CompilationUnit com = getParseUnit(file_before);
            if (com != null){
                writeBeforeCode(com.clone().toString(),pathBefore);
                Common.writeSourceCode(aRoot, pathAfter);
                GetFeatureJavaDiff(pathBefore, pathAfter);
//                FinedDiff(pathBefore, pathAfter);
            }

        }
    }

    static private void GetFeatureJavaDiff(String pathBefore, String pathAfter){
        String NameDir = Common.mSavePath.substring(Common.mSavePath.substring(0,Common.mSavePath.length()-1).lastIndexOf("\\")).replace("\\", "");
        String ansJavaDiff = ".\\ans_from_java_diff_transformation\\" + Common.mNameProject  + "\\" + mNameFold+ "\\" + NameDir + "\\"; ;
//        String ansJavaDiff = "C:\\Users\\shir0\\ans_from_java_diff_transformation\\" + Common.mNameProject  + "\\" + mNameFold+ "\\" + NameDir + "\\"; ;

        File ansJavaDiffFile = new File(ansJavaDiff);
        if (ansJavaDiffFile.exists() || ansJavaDiffFile.mkdirs()) {

            String[] splitNameFile = pathAfter.substring(pathAfter.substring(0,pathAfter.length()-1).lastIndexOf("\\")+1).split("_");
            String IDCommit=  splitNameFile[0] + "_" + splitNameFile[2] + "_" +  splitNameFile[3];
//            String command = "C:\\\\Users\\\\shir0\\\\Anaconda3\\\\python.exe C:\\Users\\shir0\\javadiff\\javadiff\\main.py " + Common.mNameProject +
            String command = "python.exe .\\javadiff\\javadiff\\main.py " + Common.mNameProject +
                    " " + mNameFold + " " + NameDir + " " + IDCommit + " " + pathBefore + " " + pathAfter;
            try {

                Process p = Runtime.getRuntime().exec(command);
                BufferedReader stdInput = new BufferedReader(new
                        InputStreamReader(p.getInputStream()));
                stdInput.readLine();
//
//                BufferedReader stdError = new BufferedReader(new
//                        InputStreamReader(p.getErrorStream()));
                String s = null;

                while ((s = stdInput.readLine()) != null) {
//                    System.out.println(s);
                }
//
//                while ((s = stdError.readLine()) != null) {
////                    System.out.println(s);
//                }
            } catch (IOException e) {
                throw new RuntimeException(e);
            }

        }
        File FileBefore = new File(pathBefore);
        File FileAfter = new File(pathAfter);
        FileBefore.delete();
        FileAfter.delete();

    }

    static private void FinedDiff(String pathBefore, String pathAfter){
        List<String> original;
        List<String> revised;
        File FileBefore = new File(pathBefore);
        File FileAfter = new File(pathAfter);

        try {
            original = Files.readAllLines(FileBefore.toPath());
            revised = Files.readAllLines(FileAfter.toPath());
            Patch<String> patch = DiffUtils.diff(original, revised);
            ArrayList<String> added = new ArrayList<>();
            ArrayList<String> deleted = new ArrayList<>();
            for (Delta<String> delta : patch.getDeltas()) {
                added.addAll(delta.getRevised().getLines());
                deleted.addAll(delta.getOriginal().getLines());
            }

            String NameDir = Common.mSavePath.substring(Common.mSavePath.substring(0,Common.mSavePath.length()-1).lastIndexOf("\\")).replace("\\", "");
//            String PathDir = mRootInputPath + "\\..\\..\\..\\Transformations\\" + Common.mNameProject  + "\\" + mNameFold+ "\\" + NameDir + "\\";
            String PathDir = ".\\diff_after_transformation\\" + Common.mNameProject  + "\\" + mNameFold+ "\\" + NameDir + "\\";
//            String PathDir = "C:\\Users\\shir0\\diff_after_transformation\\" + Common.mNameProject  + "\\" + mNameFold+ "\\" + NameDir + "\\"; ;

            File FileDir =  new File(PathDir);
            if (FileDir.exists() || FileDir.mkdirs()) {
                FileWriter writer = new FileWriter(PathDir + "\\transform.csv",true);
                String[] split_ = pathBefore.substring(pathBefore.lastIndexOf('\\') + 1).split("_");
                String collect = split_[0] + "!@#:";
                collect += split_[2] + "!@#:";
                collect += String.join("!@#+", added);
                collect += String.join("!@#-", deleted);
                collect += "\n";
                writer.write(collect);
                writer.close();
            }
            FileBefore.delete();
            FileAfter.delete();
        } catch (IOException e) {
            e.printStackTrace();
        }

    }


    static public void ApplyTransformation(ArrayList<MethodDeclaration> ChangeMethod, CompilationUnit com, File mJavaFile){
        for(MethodDeclaration MethodChange : ChangeMethod){
            Optional<Range> RangeMethodChange = MethodChange.getRange();
            CompilationUnit CloneRoot = com.clone();
            CloneRoot.walk(MethodDeclaration.class, e -> {
                ChangeMethod(e, MethodChange, RangeMethodChange);
            });
            if (!CloneRoot.toString().equals(com.clone().toString())){
                saveTransformation(CloneRoot, mJavaFile);
            }
        }
    }

    static CompilationUnit getParseUnit(File mJavaFile) {
        try {
            JavaParser parser = new JavaParser();
            String txtCode = new String(Files.readAllBytes(mJavaFile.toPath()));
            ParseResult<CompilationUnit> parseResult = parser.parse(txtCode);
            return parseResult.getResult().get();
        } catch (Exception ex) {
            System.out.println("\n" + "Exception: " + mJavaFile .getPath());
            ex.printStackTrace();
            String error_dir = Common.mSavePath + "java_parser_error.txt";
            Common.saveErrText(error_dir, mJavaFile );
            return null;
        }
    }

    static private void ChangeMethod(MethodDeclaration e, MethodDeclaration MethodChange, Optional<Range> RangeMethodChange){
        MethodDeclaration Value = null;
        Optional<Range> RangeCheck = e.getRange();
        if (RangeCheck.isPresent() && RangeMethodChange.isPresent()) {
            if (RangeCheck.get().begin == RangeMethodChange.get().begin &&
                    RangeCheck.get().end == RangeMethodChange.get().end) {
                Value = e;
            }
        }
        Optional<BlockStmt> body = MethodChange.getBody();
        if (body.isPresent() && Value !=null) {
            Value.setBody(body.get());
            Value.setParameters(MethodChange.getParameters());

        }
    }

    static void saveErrText(String error_dir, File javaFile) {
        try {
            File targetFile = new File(error_dir);
            if (targetFile.getParentFile().exists() || targetFile.getParentFile().mkdirs()) {
                if (targetFile.exists() || targetFile.createNewFile()) {
                    Files.write(Paths.get(error_dir),
                            (javaFile.getPath() + "\n").getBytes(),
                            StandardOpenOption.APPEND);
                }
            }
        } catch (IOException ioEx) {
            ioEx.printStackTrace();
        }
    }

    static void writeSourceCode(CompilationUnit md, String codePath) {
        try {
            place += 1;
            Files.write(Paths.get(codePath), md.toString().getBytes());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    static void writeBeforeCode(String tfSourceCode, String codePath) {
        File targetFile = new File(codePath).getParentFile();
        if (targetFile.exists() || targetFile.mkdirs()) {
            try (PrintStream ps = new PrintStream(codePath)) {
                ps.println(tfSourceCode);
            } catch (FileNotFoundException ex) {
                System.out.println(ex.toString());
            }
        }
    }


    static boolean isNotPermeableStatement(Node node) {
        return (node instanceof EmptyStmt
                || node instanceof LabeledStmt
                || node instanceof BreakStmt
                || node instanceof ContinueStmt
                || node instanceof ReturnStmt);
    }

    static boolean isAllPlaceApplicable(Object obj) {
        return (
                obj instanceof VariableRenaming
                || obj instanceof BooleanExchange
                || obj instanceof LoopExchange
                || obj instanceof SwitchToIf
                || obj instanceof ReorderCondition
        );
    }
}
