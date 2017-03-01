package com.jug.mmpreprocess;

import ij.IJ;
import java.io.File;
import java.io.FileFilter;
import java.io.FilenameFilter;
import org.junit.Test;

import static org.junit.Assert.*;

/**
 * Created by rhaase on 3/1/17.
 */
public class MMPreprocessTest {

    private FileFilter tifFilter = new FileFilter() {
        @Override
        public boolean accept(File pathname) {
            return pathname.getName().contains(".tif");
        }
    };

    private FileFilter folderFilter = new FileFilter() {
        @Override
        public boolean accept(File pathname) {
            return pathname.isDirectory();
        }
    };


    // delete files / folders recursively
    private boolean cleanUp(File file) {
        boolean result = true;
        if (file.isDirectory()) {
            for (File sub : file.listFiles()) {
                result = result & cleanUp(sub);
            }
        }
        return result & file.delete();
    }

    @Test
    public void testWorkingWithADirectory() {

        String inputDir = "src/test/resources/folder";
        String outputDir = "src/test/resources/folder/output";

        String[] args = {
                "mmpreprocess",
                "-i",
                inputDir,
                "-o",
                outputDir,
                "-c",
                "2",
                "-cmin",
                "1"

        };

        // -----------------------------------------
        // for tracing
        for (String param : args) {
            IJ.log("mmp params " + param);
        }

        // -----------------------------------------
        // Actually run MMPreprocess
        MMPreprocess.running_as_Fiji_plugin = true;
        MMPreprocess.main(args);


        File outputfolder = new File(outputDir);
        assertTrue("Output folder was created ", outputfolder.exists() && outputfolder.isDirectory());
        assertTrue("Output folder contains right number of files and folders ", outputfolder.listFiles(folderFilter).length == 26);
        assertTrue("Output folder contains right number of tif image files ", outputfolder.listFiles(tifFilter).length == 52);

        File firstDatasetFolder = new File(outputDir + "/folder_GL01");
        assertTrue("Dataset folder was created ", firstDatasetFolder.exists() && firstDatasetFolder.isDirectory());
        assertTrue("Dataset folder contains right number of files and folders ", firstDatasetFolder.listFiles(folderFilter).length == 0);
        assertTrue("Dataset folder contains right number of tif image files ", firstDatasetFolder.listFiles(tifFilter).length == 6);

        cleanUp(outputfolder);
    }
}