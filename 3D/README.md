# Project 2 : C. Elegans Development

**Course**
<br/>
Bioimage Informatics BIO-410, École Polytechnique Fédérale de Lausanne

**Professors**
- Prof. Daniel Sage
- Prof. Arne Seitz

**Authors**
- Arnaud Dhaene, EPFL
- Audrey Ménaësse, EPFL

## Volume evolution during embryonic development
This repository contains the relevant code for three ImageJ PlugIns 
enabling users to approximate volume evolution of C. Elegans embryonic
cells over time from a 4D image Hyperstack.


## Installation
To install these PlugIns, one must download the entire repository and copy the files 
into a Java project in an Eclipse Workspace. The Java project must have the relevant 
`plugins.config` and `build.xml` files as described in the earlier weeks of the 
**Bioimage Informatics BIO-410** course.

The following lines must be added to the 'plugins.config' file :
Plugins>BII_Project, "Preprocessing", Img_Preprocessing("")
 Plugins>BII_Project, "CElegans", C_Elegans_Development("")
 Plugins>BII_Project, "Volume", Volume_Calc("")'


These plugins and classes must be included in an Eclipse workspace 
containing a plugins.config file with the following lines :
	

## Usage
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
	/!\ All images are then hidden to speed up the execution of the plugin. /!\
	In the second dialog box select the following parameters :
		Max number of iterations : 40
		Max Mean Difference (graylevel) : 100
		Threshold (graylevel) : 4000
		Max distance (µm) : 150

	3- Volume Calculation
	Open the "Areas" image.
	Run the volume calculation plugin in Plugins>BII_Project>Volume
	You will obtain both a graph and a table containing the volume of each cell
	in function of time.