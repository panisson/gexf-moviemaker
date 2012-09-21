/**
 * Written by André Panisson <panisson@gmail.com>  
 * Copyright (C) 2012 Istituto per l'Interscambio Scientifico I.S.I.  
 * You can contact us by email (isi@isi.it) or write to:  
 * ISI Foundation, Via Alassio 11/c, 10126 Torino, Italy.  
 */

package moviemaker;
import java.awt.image.BufferedImage;
import java.util.concurrent.TimeUnit;

import com.xuggle.mediatool.IMediaWriter;
import com.xuggle.mediatool.ToolFactory;
import com.xuggle.xuggler.IRational;

public class VideoWriter {
	
	private int width;
	private int height;
	private int fps;
	private String output;
	
	private IMediaWriter videowriter;
	private long currentFrame = 0;
	
	public VideoWriter(String output, int width, int height, int fps) {
		
		this.width = width;
		this.height = height;
		this.fps = fps;
		this.output = output;
	}
	
	public void init() {
		// create a IMediaWriter to write the file.
		videowriter = ToolFactory.makeWriter(output);
		IRational frameRate = IRational.make(fps, 1);
		
		videowriter.addVideoStream(0, 0,
				frameRate,
				width, height);
	}
	
	public void addFrame(BufferedImage image) {
		videowriter.encodeVideo(0, image, currentFrame*20, TimeUnit.MILLISECONDS);
		currentFrame++;
	}
	
	public void close() {
		videowriter.close();
	}
	
}
