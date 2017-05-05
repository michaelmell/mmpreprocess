package com.jug.mmpreprocess;

public class CropArea {

	public long top;
	public long left;
	public long bottom;
	public long right;

	public CropArea( final long top, final long left, final long bottom, final long right ) {
		this.top = top;
		this.left = left;
		this.bottom = bottom;
		this.right = right;
	}

	public long getCenterCoordinate() {
		return ( right + left ) / 2;
	}
}

