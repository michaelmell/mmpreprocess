package com.jug.mmpreprocess;

import ij.IJ;
import java.io.File;
import java.io.FileFilter;
import org.junit.Test;

import static org.junit.Assert.*;

/**
 * Created by rhaase on 3/1/17.
 */
public class MMPreprocessTest {



    @Test
    public void testWorkingWithADirectoryInput() {

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
        assertEquals("Output folder contains right number of files and folders ", 26, outputfolder.listFiles(MMUtils.folderFilter).length);
        assertEquals("Output folder contains right number of tif image files ", 52, outputfolder.listFiles(MMUtils.tifFilter).length);

        File firstDatasetFolder = new File(outputDir + "/folder_GL01");
        assertTrue("Dataset folder was created ", firstDatasetFolder.exists() && firstDatasetFolder.isDirectory());
        assertEquals("Dataset folder contains right number of files and folders ", 0, firstDatasetFolder.listFiles(MMUtils.folderFilter).length);
        assertEquals("Dataset folder contains right number of tif image files ", 6, firstDatasetFolder.listFiles(MMUtils.tifFilter).length);

        MMUtils.deleteFolderRecursive(outputfolder);
    }


    @Test
    public void testWorkingWithAFileInput() {

        String inputFile = "src/test/resources/file.tif";
        String outputDir = "src/test/resources/file.tif_output";

        String[] args = {
                "mmpreprocess",
                "-i",
                inputFile,
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
        assertEquals("Output folder contains right number of files and folders ", 26, outputfolder.listFiles(MMUtils.folderFilter).length);
        assertEquals("Output folder contains right number of tif image files ", 52, outputfolder.listFiles(MMUtils.tifFilter).length);

        File firstDatasetFolder = new File(outputDir + "/file.tif_GL01");
        assertTrue("Dataset folder was created ", firstDatasetFolder.exists() && firstDatasetFolder.isDirectory());
        assertEquals("Dataset folder contains right number of files and folders ", 0, firstDatasetFolder.listFiles(MMUtils.folderFilter).length);
        assertEquals("Dataset folder contains right number of tif image files ", 6, firstDatasetFolder.listFiles(MMUtils.tifFilter).length);

        MMUtils.deleteFolderRecursive(outputfolder);
    }



    @Test
    public void testWorkingWithAFileInputWithoutCParameters() {

        String inputFile = "src/test/resources/file.tif";
        String outputDir = "src/test/resources/file.tif_output";

        String[] args = {
                "mmpreprocess",
                "-i",
                inputFile,
                "-o",
                outputDir

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
        assertEquals("Output folder contains right number of files and folders ", 26, outputfolder.listFiles(MMUtils.folderFilter).length);
        assertEquals("Output folder contains right number of tif image files ", 52, outputfolder.listFiles(MMUtils.tifFilter).length);

        File firstDatasetFolder = new File(outputDir + "/file.tif_GL01");
        assertTrue("Dataset folder was created ", firstDatasetFolder.exists() && firstDatasetFolder.isDirectory());
        assertEquals("Dataset folder contains right number of files and folders ", 0, firstDatasetFolder.listFiles(MMUtils.folderFilter).length);
        assertEquals("Dataset folder contains right number of tif image files ", 6, firstDatasetFolder.listFiles(MMUtils.tifFilter).length);

        MMUtils.deleteFolderRecursive(outputfolder);
    }


    @Test
    public void testWorkingWithAFileInputStacksOutput() {

        String inputFile = "src/test/resources/file.tif";
        String outputDir = "src/test/resources/file.tif_output";

        String[] args = {
                "mmpreprocess",
                "-i",
                inputFile,
                "-o",
                outputDir,
                "-stackout"


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
        assertEquals("Output folder contains right number of files and folders ", 26, outputfolder.listFiles(MMUtils.folderFilter).length);
        assertEquals("Output folder contains right number of tif image files ", 52, outputfolder.listFiles(MMUtils.tifFilter).length);

        File firstDatasetFolder = new File(outputDir + "/file.tif_GL01");
        assertTrue("Dataset folder was created ", firstDatasetFolder.exists() && firstDatasetFolder.isDirectory());
        assertEquals("Dataset folder contains right number of files and folders ", 0, firstDatasetFolder.listFiles(MMUtils.folderFilter).length);
        assertEquals("Dataset folder contains right number of tif image files ", 1, firstDatasetFolder.listFiles(MMUtils.tifFilter).length);

        MMUtils.deleteFolderRecursive(outputfolder);
    }
}