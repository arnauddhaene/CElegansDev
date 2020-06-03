import java.awt.Color;

import ij.IJ;
import ij.ImagePlus;
import ij.gui.Plot;
import ij.measure.CurveFitter;
import ij.plugin.PlugIn;
import ij.process.ImageProcessor;

// Open the image bleach-DHE-real.tif
// Open the image c-elegans-bleach-synthetic.tif

public class Bleaching_Correction_ExpCurveFitter implements PlugIn {
	
	public void run(String arg) {
		ImagePlus imp = IJ.getImage();
		int nx = imp.getWidth();
		int ny = imp.getHeight();
		int nt = imp.getNFrames();
		double time[] = new double[nt];
		double mean[] = new double[nt];

		for (int t = 0; t < nt; t++) {
			time[t] = t;
			imp.setPosition(1, 1, t+1);
			mean[t] = imp.getProcessor().getStats().mean;
		}
		CurveFitter fit = new CurveFitter(time, mean);

		fit.doFit(CurveFitter.EXP_WITH_OFFSET, true);
		double p[] = fit.getParams();
		double a = p[0];
		double c = p[2];
		double tau = 1.0/p[1];
		IJ.log("formula " + fit.getFormula());
		IJ.log("residual " + fit.getFitGoodness());
		IJ.log("results " + fit.getResultString());
		IJ.log("Tau/a/c " + tau + "/" + a + "/" + c);

		ImagePlus out = IJ.createHyperStack("Correction", nx, ny, 1, 1, nt, 32);
		out.show();
		
		double corr[] = new double[nt];
		for (int t = 0; t < nt; t++) {
			imp.setPosition(1, 1, t+1);
			out.setPositionWithoutUpdate(1, 1, t+1);
			ImageProcessor ip = imp.getProcessor();
			ImageProcessor op = out.getProcessor();
			double k = (a*Math.exp(-t/tau) + c)/mean[0];
			for(int x = 0; x < nx; x++)
				for (int y = 0; y < ny; y++)
					op.putPixelValue(x, y, ip.getPixelValue(x, y)/k);
			op.resetMinAndMax();
			corr[t] = out.getStatistics().mean;
		}
			
		Plot plot = new Plot("Time response", "Time", "Intensity");
		plot.addPoints(time, mean, Plot.LINE);
		plot.setColor(Color.red);
		plot.addPoints(time, corr, Plot.LINE);
		plot.setColor(Color.green);
		plot.setLimitsToFit(true);
		plot.show();
	}
}
