Starburst
=========

#Image generation program
Evolved from [Starburst-processing](https://github.com/g-rocket/Starburst-processing/)

#Key bindings
* `p` = open generate property change dialog
* `s` = open seed property change dialog
* `m` = save multiple images with the current properties
* `v` = save current image
* `q` = quit
* any other key = generate new image

See source code for more cool settings

#To compile
    git clone https://github.com/g-rocket/Starburst.git
    cd Starburst
    mvn clean compile assembly:single
The jar it builds will be in the `target` folder and will be called Starburst&#8209;CURRENT_VERSION_NUMBER.jar
