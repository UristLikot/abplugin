# abplugin
Sponge ban plugin with SQL sync.
Works with **MariaDB**.

Depend on https://ore.spongepowered.org/pxlpowered/spotlin

#### Set up:

Open `config\sponge\global.conf`

 Find sql section
 `sql {}`

 Add **jdbc** alias **main**, for example:


     main="jdbc:mariadb://chip:1234@192.168.0.95:3306"

#### Usage:
Avalible commands:

/aban (player) (time) (reason)

/uban (player)

/refbans

Time is entered in hours.

#### Permissons for admin:

abplugin.aban

abplugin.uban

abplugin.refbans
