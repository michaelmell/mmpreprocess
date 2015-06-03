package com.jug.mmpreprocess.oldshit;


import net.imglib2.RandomAccessibleInterval;
import net.imglib2.type.NativeType;
import net.imglib2.type.numeric.NumericType;

/**
 * @author jug
 *
 */
public class VarOfRai<T extends NumericType<T> & NativeType<T> > implements
	OldUnaryOutputOperation<RandomAccessibleInterval<T>, T> {

    /**
     * @see net.imglib2.ops.operation.UnaryOperation#compute(java.lang.Object, java.lang.Object)
     */
    @Override
    public T compute(RandomAccessibleInterval<T> input, T output) {
	output.setZero();
	
	// Var(X) = < < X - <X> >^2 >
	RandomAccessibleInterval<T> tmp;
	tmp = new RaiMeanSubtractor<T>().compute(input);
	tmp = new RaiSquare().compute(tmp); 
	output = new MeanOfRai<T>().compute(tmp);
	
	return output;
    }

    /**
     * @see net.imglib2.ops.operation.UnaryOutputOperation#createEmptyOutput(java.lang.Object)
     */
    @Override
    public T createEmptyOutput(RandomAccessibleInterval<T> in) {
	return in.randomAccess().get().createVariable();
    }

    /**
     * @see net.imglib2.ops.operation.UnaryOutputOperation#compute(java.lang.Object)
     */
    @Override
    public T compute(RandomAccessibleInterval<T> in) {
	T ret = createEmptyOutput(in);
	ret = compute(in, ret);
	return ret;
    }

    /**
     * @see net.imglib2.ops.operation.UnaryOutputOperation#copy()
     */
    @Override
    public OldUnaryOutputOperation<RandomAccessibleInterval<T>, T> copy() {
	// TODO Auto-generated method stub
	return null;
    }
}
