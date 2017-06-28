package com.jug.mmpreprocess;

import static org.junit.Assert.assertEquals;

import com.jug.mmpreprocess.oldshit.MeanOfRai;
import com.jug.mmpreprocess.oldshit.RaiMeanSubtractor;
import com.jug.mmpreprocess.oldshit.RaiSquare;
import com.jug.mmpreprocess.oldshit.VarOfRai;

import net.imagej.ImageJ;
import net.imagej.patcher.LegacyInjector;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.img.array.ArrayImgs;
import net.imglib2.type.numeric.real.FloatType;

import org.junit.Test;

import ij.IJ;

/**
 * Created by rhaase on 3/1/17.
 */
public class MMPreprocessTest2 {

	static {
		LegacyInjector.preinit();
	}
	
   @Test
    public void testPluginVarOfRai() {
	    	    	
		final ImageJ ij = new ImageJ();		

		// create input
		int w = 160; 
		int h = 96;
		float[] pixels = new float[w * h];
		for (int y = 0; y < h; y++) {
		  for (int x = 0; x < w; x++) {
		    pixels[y * w + x] = x + y;
		  }
		}
		
		RandomAccessibleInterval<FloatType> input = ArrayImgs.floats(pixels, w, h);
		
		// copied of VarOfRai::compute
		RandomAccessibleInterval<FloatType> tmp;
		tmp = (RandomAccessibleInterval<FloatType>) ij.op().run(RaiMeanSubtractor.class, input);
		tmp = (RandomAccessibleInterval<FloatType>) ij.op().run(RaiSquare.class, tmp); 
		FloatType output = (FloatType) ij.op().run(MeanOfRai.class, tmp);
		
		//returns expected value
		IJ.log("Calling content of Var of Rai directly: ");
		IJ.log(output.toString());
    		
		// calls VarOfRai via ij.op()
		FloatType r = (FloatType) ij.op().run(VarOfRai.class, input);

		// returns zero
		IJ.log("Var of Rai via ij.op(): ");
		IJ.log(r.toString());

		// fails
        assertEquals("Var of Rai: ", output, r);
    }


}