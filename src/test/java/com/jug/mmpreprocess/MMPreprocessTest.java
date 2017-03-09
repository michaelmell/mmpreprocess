package com.jug.mmpreprocess;

import ij.IJ;
import ij.ImagePlus;
import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import org.junit.Test;
import org.python.google.common.io.Files;

import static org.junit.Assert.*;

/**
 * Created by rhaase on 3/1/17.
 */
public class MMPreprocessTest {



    @Test
    public void testWorkingWithADirectoryInput() {

        String inputDir = "src/test/resources/folder";
        String outputDir = "src/test/resources/folder/output";


        File outputfolder = new File(outputDir);
        if (outputfolder.exists()) {
            MMUtils.deleteFolderRecursive(outputfolder);
        }

        String[] args = {
                "mmpreprocess",
                "-i",
                inputDir,
                "-o",
                outputDir,
                "-c",
                "2",
                "-cmin",
                "1",
                "-so"

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


        outputfolder = new File(outputDir);
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


        File outputfolder = new File(outputDir);
        if (outputfolder.exists()) {
            MMUtils.deleteFolderRecursive(outputfolder);
        }

        String[] args = {
                "mmpreprocess",
                "-i",
                inputFile,
                "-o",
                outputDir,
                "-c",
                "2",
                "-cmin",
                "1",
                "-so"

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


        outputfolder = new File(outputDir);
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

        File outputfolder = new File(outputDir);
        if (outputfolder.exists()) {
            MMUtils.deleteFolderRecursive(outputfolder);
        }

        String[] args = {
                "mmpreprocess",
                "-i",
                inputFile,
                "-o",
                outputDir,
                "-so"

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


        outputfolder = new File(outputDir);
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


        File outputfolder = new File(outputDir);
        if (outputfolder.exists()) {
            MMUtils.deleteFolderRecursive(outputfolder);
        }

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


        outputfolder = new File(outputDir);
        assertTrue("Output folder was created ", outputfolder.exists() && outputfolder.isDirectory());
        assertEquals("Output folder contains right number of files and folders ", 26, outputfolder.listFiles(MMUtils.folderFilter).length);
        assertEquals("Output folder contains right number of tif image files ", 52, outputfolder.listFiles(MMUtils.tifFilter).length);

        File firstDatasetFolder = new File(outputDir + "/file.tif_GL01");
        assertTrue("Dataset folder was created ", firstDatasetFolder.exists() && firstDatasetFolder.isDirectory());
        assertEquals("Dataset folder contains right number of files and folders ", 0, firstDatasetFolder.listFiles(MMUtils.folderFilter).length);
        assertEquals("Dataset folder contains right number of tif image files ", 1, firstDatasetFolder.listFiles(MMUtils.tifFilter).length);

        MMUtils.deleteFolderRecursive(outputfolder);
    }

    @Test
    public void worksWithT_inFilname() throws IOException {

        String originalInputFile = "src/test/resources/file.tif";
        String inputFile = "src/test/resources/file_test.tif";

        Files.copy(new File(originalInputFile), new File(inputFile));


        String outputDir = "src/test/resources/file.tif_output";


        File outputfolder = new File(outputDir);
        if (outputfolder.exists()) {
            MMUtils.deleteFolderRecursive(outputfolder);
        }

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


        outputfolder = new File(outputDir);
        assertTrue("Output folder was created ", outputfolder.exists() && outputfolder.isDirectory());
        assertEquals("Output folder contains right number of files and folders ", 26, outputfolder.listFiles(MMUtils.folderFilter).length);
        assertEquals("Output folder contains right number of tif image files ", 52, outputfolder.listFiles(MMUtils.tifFilter).length);

        File firstDatasetFolder = new File(outputDir + "/file_test.tif_GL01");
        assertTrue("Dataset folder was created ", firstDatasetFolder.exists() && firstDatasetFolder.isDirectory());
        assertEquals("Dataset folder contains right number of files and folders ", 0, firstDatasetFolder.listFiles(MMUtils.folderFilter).length);
        assertEquals("Dataset folder contains right number of tif image files ", 1, firstDatasetFolder.listFiles(MMUtils.tifFilter).length);

        MMUtils.deleteFolderRecursive(outputfolder);
    }
/*
    @Test
    public void tempTest() {
        String inputDir = "/Users/rhaase/temp/moma_test2/1_registered/moma_test2.tif";
        String outputDir = "/Users/rhaase/temp/moma_test2/2_split";

        String[] args = {
                "mmpreprocess",
                "-i",
                inputDir,
                "-o",
                outputDir,
                "-tmin",
                "1",
                "-tmax",
                "40",
                "-vt",
                "0.001",
                "-lo",
                "40",
                "-cw",
                "100"
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

        File firstDatasetFolder = new File(outputDir + "/moma_test2.tif_GL01");
        assertTrue("Dataset folder was created ", firstDatasetFolder.exists() && firstDatasetFolder.isDirectory());
        assertEquals("Dataset folder contains right number of files and folders ", 0, firstDatasetFolder.listFiles(MMUtils.folderFilter).length);
        assertEquals("Dataset folder contains right number of tif image files ", 1, firstDatasetFolder.listFiles(MMUtils.tifFilter).length);


        ImagePlus imp = IJ.openImage(firstDatasetFolder.listFiles(MMUtils.tifFilter)[0].getAbsolutePath());
        assertEquals(imp.getNChannels(), 2);
        assertEquals(imp.getNFrames(), 40);

        //MMUtils.deleteFolderRecursive(outputfolder);
    }*/
}