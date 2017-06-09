package com.jug.mmpreprocess.oldshit;

import org.scijava.ItemIO;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;

import net.imagej.ops.AbstractOp;
import net.imagej.ops.Op;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.type.numeric.NumericType;
import net.imglib2.view.Views;

/**
 * @author jug
 *
 */
@Plugin(type = Op.class, name = "mean of rai")
public class MeanOfRai<T extends NumericType< T >> extends AbstractOp {
	
	@Parameter
	private RandomAccessibleInterval<T> input;

	@Parameter(type = ItemIO.OUTPUT)
	private T output;
	
    public T compute(RandomAccessibleInterval<T> input, T output) {
	output.setZero();
	T numEl = output.createVariable();
	T one   = output.createVariable(); one.setOne();
	for (T el : Views.iterable(input)) {
	    output.add(el);
	    numEl.add(one);
	}
	output.div(numEl);
	return output;
    }

    public T createEmptyOutput(RandomAccessibleInterval<T> in) {
	return in.randomAccess().get().createVariable();
    }

    @Override
   	public void run() {
	T ret = createEmptyOutput(input);
	ret = compute(input, ret);
	output = ret;
    }

}
