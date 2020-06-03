# Presentation
The C-Elegans project is an ensemble of three different plugins and 
two dedicated classes to calculate the volume of each cell during a 
C-Elegan embryo development from a 4D image Hyperstack.

# Installation
These plugins and classes must be included in an Eclipse workspace 
containing a plugins.config file with the following lines :
	Plugins>BII_Project, "Preprocessing", Img_Preprocessing("")
	Plugins>BII_Project, "CElegans", C_Elegans_Development("")
	Plugins>BII_Project, "Volume", Volume_Calc("")

# Usage
The data set to analyze must be open in ImageJ before running any plugin.
	1- Image Preprocessing
	Run the preprocessing plugin in Plugins>BII_Project>Preprocessing
	In the first dialog box, select the original image to get the path 
	where the intermediate images created by the plugin will be saved.
	The "Denoised" and "Shell" images are created and saved automatically.
	A second dialog box appear for the Canny Edge detector, select the 
	following parameters :
		Gaussian kernel radius : 2
		Low threshold :		 4
		High threshold :	 8
		Gaussian kernel radius : 16
		Do not check normalize contrast.
	 
	2- Region Growing
	Open the "Denoised" image. 
	Go to the central slice in z (13/27) on the last frame (20/20 or 40/40)
	Select manually the seed points at the center of each cell with the
	multi-points tool.
	Run the 3D region growing plugin in Plugins>BII_Project>CElegans
	In the first dialog box, select the original image to get the path 
	to retrieve the intermediate images automatically saved by the preprocessing
	plugin.
	In the second dialog box select the following parameters :
		Max number of iterations : 30
		Max Mean Difference (graylevel) : 100
		Threshold (graylevel) : 4000
		Max distance (µm) : 100

	3- Volume Calculation
	Open the "Areas" image.
	Run the volume calculation plugin in Plugins>BII_Project>Volume


# Warning
	
	
	