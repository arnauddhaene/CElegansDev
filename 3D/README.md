# Project 2 : C. Elegans Development

### Course
Bioimage Informatics BIO-410, École Polytechnique Fédérale de Lausanne

### Professors
- Prof. Daniel Sage
- Prof. Arne Seitz

### Authors
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

The following lines must be added to the `plugins.config` file :
```
Plugins>BII_Project, "Preprocessing", Img_Preprocessing("")
Plugins>BII_Project, "Region Growing", Region_Growing("")
Plugins>BII_Project, "Volume", Volume_Calc("")
```
	

## Usage
In order to perform the measurements, please follow the steps below.
The values of the different parameters to use on the demo dataset is given in parentheses.

1. Open ImageJ from an Eclipse Workspace
2. Open the datset to analyse (`real-stack.tif`)
3. Execute the three PlugIns as follows

	### Image Preprocessing
	 1. Run the preprocessing plugin in `Plugins>BII_Project>Preprocessing`
	 2. In the first dialog box, select the original image to get the path 
	where the intermediate images created by the plugin will be saved.
	The `Denoised.tif` and `Shell.tif` images are created and saved automatically.
	 3. A second dialog box appear for the Canny Edge detector, select the 
	following parameters:
		- `Gaussian kernel radius` (2) : gaussian radius for smoothing
		- `Low threshold` (4) : low threshold for non-maximum suppression		 4
		- `High threshold`(8) : high threshold for non-maximum suppression
		- `Gaussian kernel width` :  width of gaussian to compute the gradients
		
	 
	### Region Growing
	1. Open or select the `Denoised.tif` image. 
	2. Go to the central slice in z (13/27) on the last frame (20/20 or 40/40)
	3. Select manually the seed points at the center of each cell with the
	multi-points tool.
	4. Run the 3D region growing plugin in `Plugins>BII_Project>Region Growing`
	5. In the first dialog box, select the original image to get the path 
	to retrieve the intermediate images automatically saved by the preprocessing
	plugin.
	6. In the second dialog box select the following parameters :
		- `Max number of iterations` (40)
		- `Max Mean Difference (graylevel)` (100)
		- `Threshold (graylevel)` (4000)
		- `Max distance (µm)` (150)

	### Volume Calculation
	1. Run the volume calculation plugin in Plugins>BII_Project>Volume
	You will obtain both a graph and a table containing the volume of each cell
	in function of time.
	2. Save the `Result Table` and the graph manually if needed.
	
	
## PlugIns Framework

### Preprocessing
1. The original image is denoised with a 2x2x2 3D median filter before a 1.25x1.25x1.2 3D Gaussian filter.
2. A binary image of the footprint of the embryo in the image is created using :
	- An ImageJ Huang thresholding on the denoised image
	- A 2x2x2 3D median filter to reduce the salt and pepper noise
	- A 3D closing of the image using a 3D ball structural element.
3. A binary image of the membranes of the cells is created using a Canny Edge-detector according to the following steps :
	- A gaussian smoothing
	- The calculation of the gradients in X and Y directions
	- A non-maximum suppression 
		- Below the low threshold
		
