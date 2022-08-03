import java.io.File;
import java.io.IOException;


public class Main {
    public static void main(String[] args) throws IOException {
        /*
        This project do tramnsformation only to test file
         */

        //build simple lists of the lines of the two testfiles

//compute the patch: this is the diffutils part
//        Patch<String> patch = DiffUtils.diff(original, revised);

        /* root folder for input  -> '~/methods'
         * root folder for output -> '~/transforms'
         *
         * extracted single method of project should be in 'methods' folder
         * separate folder for each refactoring will be created in 'transforms' folder
         */

        // TODO: move the file from the cluster (in WriteFileCommit -> Files -> Name projects) to remote desktop (test_file_for_transformation)
//        String nameProject = "tika";

        String numberFold =  args[0];
        String nameProject =  args[1];

        System.out.println(numberFold);
//        String inpPath = "C:\\Users\\shir0\\test_file_for_transformation\\" + nameProject + "\\" + numberFold + "\\";
//        String outPath = "C:\\Users\\shir0\\transformation\\" + nameProject + "\\" + numberFold + "\\transforms";

        String inpPath = ".\\test_file_for_transformation\\" + nameProject + "\\" + numberFold + "\\";
        String outPath = ".\\transformation\\" + nameProject + "\\" + numberFold + "\\transforms";

        File theDir = new File(outPath);
        if (!theDir.exists()){
            theDir.mkdirs();
        }

//        String ansJavaDiff = "C:\\Users\\shir0\\ans_from_java_diff_transformation" + "\\" + nameProject + "\\" + numberFold + "\\";
        String ansJavaDiff = ".\\ans_from_java_diff_transformation" + "\\" + nameProject + "\\" + numberFold + "\\";

        File ansJavaDiffFile = new File(ansJavaDiff);
        if (!ansJavaDiffFile.exists()){
            ansJavaDiffFile.mkdirs();
        }

        Common.mNameProject = nameProject;
        Common.mNameFold = numberFold;

        new ASTExplorer(inpPath,outPath).call();

    }
}
