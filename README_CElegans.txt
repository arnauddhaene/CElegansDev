# Presentation
The C-Elegans project is an ensemble of three different plugins and 
two dedicated classes to calculate the volume of each cell during a 
C-Elegan embryo development.

# Installation
These plugins and classes must be included in an Eclipse workspace 
containing a plugins.config file with the following lines :
	Plugins>BII_Project, "Preprocessing", Img_Preprocessing("")
	Plugins>BII_Project, "CElegans", C_Elegans_Development("")
	Plugins>BII_Project, "Volume", Volume_Calc("") 