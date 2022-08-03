import org.apache.commons.io.FileUtils;

import java.io.File;
import java.util.ArrayList;
import java.util.concurrent.Callable;

public class ASTExplorer implements Callable<Void> {

    ASTExplorer(String inpPath, String outPath) {
        if (!inpPath.endsWith("\\")) {
            inpPath += "\\";
        }
        Common.mRootInputPath = inpPath;

        if (!outPath.endsWith("\\")) {
            outPath += "\\";
        }
        Common.mRootOutputPath = outPath;
    }

    @Override
    public Void call() {
        inspectDataset();
        return null;
    }

    private void inspectDataset() {
        String input_dir = Common.mRootInputPath;
        ArrayList<File> javaFiles = new ArrayList<>(
                FileUtils.listFiles(
                        new File(input_dir),
                        new String[]{"java"},
                        true)
        );
        System.out.println(input_dir + " : " + javaFiles.size()/2 );
        int index = 0;

        for(File javaFile:javaFiles){
            try {
                if (javaFile.toString().contains("_after_")) {// after commit
                    new ParserJavaFile().inspectSourceCode(javaFile);
                    index += 1;
                    if (index % 100 == 0) {
                        System.out.println(index);
                    }
                }
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }
        System.out.println("Number transformation: " + Common.place );

    }
}
